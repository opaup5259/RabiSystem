package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.utils.ExperienceSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.HolidayUtil;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager.check;

public class SignInManager {

    private final SpiritModule module;

    public SignInManager(SpiritModule module) {
        this.module = module;
    }

    /**
     * 执行签到或补签动作
     */
    public static void processSignIn(Player player, SpiritProfile profile, int day, Calendar viewCal) {

        /* =========================
           1. 构造目标日期（安全校验）
           ========================= */
        Calendar now = Calendar.getInstance();

        Calendar targetCal = Calendar.getInstance();
        targetCal.clear();
        targetCal.set(
                viewCal.get(Calendar.YEAR),
                viewCal.get(Calendar.MONTH),
                day
        );

        // 安全校验：防止跨月位移
        if (targetCal.get(Calendar.DAY_OF_MONTH) != day ||
                targetCal.get(Calendar.MONTH) != viewCal.get(Calendar.MONTH)) {
            player.sendMessage("§c[!] 非法签到日期");
            return;
        }

        /* =========================
           2. 记录 Key
           ========================= */
        String monthKey = targetCal.get(Calendar.YEAR) + "-" + (targetCal.get(Calendar.MONTH) + 1);
        Set<Integer> record = profile.getCheckInHistory().computeIfAbsent(monthKey, k -> new HashSet<>());

        if (record.contains(day)) {
            player.sendMessage("§e[!] 你已经签到过这一天了喵~");
            return;
        }

        /* =========================
           3. 今日 / 补签判定
           ========================= */
        boolean isToday = targetCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                targetCal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                targetCal.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH);

        if (!isToday) {
            if (profile.getReplacementCards() <= 0) {
                player.sendMessage("§c[!] 补签失败：你没有补签卡了喵~");
                return;
            }
            profile.setReplacementCards(profile.getReplacementCards() - 1);
        } else {
            // 只有今日签到（非补签）才赠送抽奖次数
            profile.addLotteryChances(1);
        }

        /* =========================
           4. 执行数据写入 并重新计算连续签到
           ========================= */
        record.add(day); // 先写入记录，以便后续计算连签
        profile.setTotalCheckIns(profile.getTotalCheckIns() + 1);
        profile.setLastCheckInMillis(System.currentTimeMillis());
        // 无论今日签到还是补签，都重新从今天开始向前回溯计算，确保持续天数准确
        recalculateConsecutiveDays(profile);

        /* =========================
           5. 奖励逻辑计算 数据保存与经验发放
           ========================= */
        String holidayName = HolidayUtil.getHolidayName(targetCal);
        boolean isHoliday = (holidayName != null && isToday);
        int exp = isHoliday ? 60 : 30;
//        record.add(day);
//        profile.setTotalCheckIns(profile.getTotalCheckIns() + 1);
        ExperienceSystem.grantExp(player, profile, ExperienceSystem.ExpType.SIGNIN, exp);
        profile.addMood(isHoliday ? 15 : 5); // 节假日心情大好
        SpiritUtils.getConfigManager().saveProfile(profile);

        // ➕ 触发签到成就检查
        check(player, profile, "signin_update");
        if (isHoliday) {
            check(player, profile, "signin_holiday");
            profile.setLotteryChances(profile.getLotteryChances() + 1);
        }

        /* =========================
           6. 随机物品奖励（多级池）
           ========================= */
        giveRandomReward(player, isHoliday);

        /* =========================
           7. 节日 & 消息反馈
           ========================= */
        if (isHoliday) {
            String dateKey = HolidayUtil.getFullDateKey(targetCal);
            if (!profile.getReceivedHolidayCards().contains(dateKey)) {
                profile.setReplacementCards(profile.getReplacementCards() + 1);
                profile.getReceivedHolidayCards().add(dateKey);
            }
            player.sendMessage("§d§l✨ 节日快乐！§f" + holidayName + "的星光在眷顾你们。");
            player.sendMessage("§e§l🎁 奖励已翻倍，并获得了一张时光补签卡与额外一次抽奖次数！");
        } else {
            String msg = isToday ?
                    "§a§l[✔] 契约达成！§f今日份的羁绊已深深铭刻，，免费获得一次抽奖次数。" :
                    "§b§l[✔] 时光回溯！§f成功弥补了遗失的契约。";
            player.sendMessage(msg);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }
    /**
     * [新增算法] 回溯计算连续签到天数
     * 从今天开始，向前倒推，直到遇到未签到的日期为止。
     */
    private static void recalculateConsecutiveDays(SpiritProfile profile) {
        Calendar pointer = Calendar.getInstance(); // 从今天开始
        int streak = 0;

        // 为了防止死循环或性能问题，限制最大回溯天数（例如365天）
        for (int i = 0; i < 365; i++) {
            String monthKey = pointer.get(Calendar.YEAR) + "-" + (pointer.get(Calendar.MONTH) + 1);
            int day = pointer.get(Calendar.DAY_OF_MONTH);

            Set<Integer> monthRecord = profile.getCheckInHistory().get(monthKey);

            // 检查这一天是否签到
            if (monthRecord != null && monthRecord.contains(day)) {
                streak++;
                // 指针向前推一天
                pointer.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                // 遇到断签
                // 特殊情况：如果我是今天刚上线，还没点签到，但昨天是签了的。
                // 这种情况下，streak 应该是 昨天的连签数 (等待今天续上)。
                // 但如果我正在进行的是“补签”操作（补的以前的），而今天还没签，
                // 那么 streak 应该显示截止到昨天的连签数。

                // 本算法逻辑：
                // 如果今天签了，streak = 1 + 昨天的...
                // 如果今天没签，streak = 0 + 昨天的... (因为 pointer 是从今天开始的)
                // 只有当 pointer 还在“今天”时，允许“今天没签”不打断计数，而是直接去看昨天？
                // 不，标准逻辑通常是：连续签到是指“最近一段连续的日子”。
                // 如果今天没签，当前的“连续状态”确实是中断的（或者说是待续的）。
                // 为了显示友好，通常 UI 会显示“已连续X天”，如果今天签了就变成 X+1。

                // 在这里，我们采用严格连续：从今天倒推，如果今天没签，就看昨天。
                if (i == 0) {
                    // 如果是循环的第一天（即今天）发现没签，我们不归零，而是给玩家看“截至昨天”的连签数据
                    // 这样玩家补签昨天后，连签数能恢复。
                    pointer.add(Calendar.DAY_OF_MONTH, -1);
                    continue;
                }

                // 如果不是今天，说明真的断了
                break;
            }
        }

        profile.setConsecutiveDays(streak);
    }

    /**
     * 增强版：随机奖励池
     * 包含 4 个稀有度等级，且支持节假日数量翻倍
     */
    private static void giveRandomReward(Player player, boolean isHoliday) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int chance = r.nextInt(100);
        int multiplier = isHoliday ? 2 : 1; // 节假日奖励翻倍

        ItemStack item;
        String rarityPrefix;

        if (chance < 5) { // 5% 传说池
            Material[] epics = {Material.DIAMOND, Material.NETHERITE_SCRAP, Material.ENCHANTED_GOLDEN_APPLE};
            item = new ItemStack(epics[r.nextInt(epics.length)], 1 * multiplier);
            rarityPrefix = "§6§l【传说】";
        } else if (chance < 15) { // 10% 稀有池
            Material[] rares = {Material.GOLD_INGOT, Material.IRON_BLOCK, Material.EXPERIENCE_BOTTLE};
            item = new ItemStack(rares[r.nextInt(rares.length)], r.nextInt(2, 5) * multiplier);
            rarityPrefix = "§b§l【稀有】";
        } else if (chance < 45) { // 30% 优秀池
            Material[] uncommons = {Material.COOKED_BEEF, Material.GOLDEN_CARROT, Material.SLIME_BALL, Material.ENDER_PEARL};
            item = new ItemStack(uncommons[r.nextInt(uncommons.length)], r.nextInt(4, 9) * multiplier);
            rarityPrefix = "§a§l【优秀】";
        } else { // 55% 普通池
            Material[] commons = {Material.SWEET_BERRIES, Material.BREAD, Material.APPLE, Material.WHEAT};
            item = new ItemStack(commons[r.nextInt(commons.length)], r.nextInt(8, 17) * multiplier);
            rarityPrefix = "§f【普通】";
        }

        // 发放物品
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        // --- 新增：物品获得提示 ---
        String itemName = getItemFriendlyName(item.getType());
        player.sendMessage("§8>> " + rarityPrefix + " §f小精灵在角落里发现了: §e" + itemName + " §7x" + item.getAmount());
    }

    /**
     * 获取物品的友好中文名（简化处理）
     */
    private static String getItemFriendlyName(Material mat) {
        return switch (mat) {
            case DIAMOND -> "钻石";
            case NETHERITE_SCRAP -> "下界合金碎片";
            case ENCHANTED_GOLDEN_APPLE -> "附魔金苹果";
            case GOLD_INGOT -> "金锭";
            case IRON_BLOCK -> "铁块";
            case EXPERIENCE_BOTTLE -> "附魔之瓶";
            case COOKED_BEEF -> "熟牛肉";
            case GOLDEN_CARROT -> "金胡萝卜";
            case SLIME_BALL -> "粘液球";
            case ENDER_PEARL -> "末影珍珠";
            case SWEET_BERRIES -> "甜浆果";
            case BREAD -> "面包";
            case APPLE -> "苹果";
            case WHEAT -> "小麦";
            default -> mat.name();
        };
    }
}