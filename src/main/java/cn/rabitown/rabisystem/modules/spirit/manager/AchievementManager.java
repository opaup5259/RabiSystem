package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.achievement.Achievement;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class AchievementManager {

    private final SpiritModule module;

    public AchievementManager(SpiritModule module) {
        this.module = module;
    }

    /**
     * 核心检查入口：根据变化的统计项，检查对应成就
     */
    public static void check(Player player, SpiritProfile profile, String statKey) {
        if (player == null || !player.isOnline()) return;

        switch (statKey) {
            // --- 🌟 成长类 ---
            case "level_update": // 当等级变化时调用
                int lv = profile.getLevel();
                if (lv >= 10) unlock(player, profile, Achievement.GROW_10);
                if (lv >= 30) unlock(player, profile, Achievement.GROW_30);
                if (lv >= 50) unlock(player, profile, Achievement.GROW_50);
                if (lv >= 80) unlock(player, profile, Achievement.GROW_80);
                if (lv >= 100) unlock(player, profile, Achievement.GROW_100);
                break;

            case "first_summon":
                unlock(player, profile, Achievement.GROW_1);
                break;

            case "name_update":
                unlock(player, profile, Achievement.NAME_1);
                break;

            case "mood_update":
                if (profile.getMood() >= 100) unlock(player, profile, Achievement.MOOD_100);
                break;

            // --- 🍖 交互类 (依赖 addStat 计数) ---
            case "pet_count":
                checkThreshold(player, profile, Achievement.PET_1, 1, statKey);
                checkThreshold(player, profile, Achievement.PET_100, 100, statKey);
                break;

            case "feed_count":
                checkThreshold(player, profile, Achievement.FEED_10, 10, statKey);
                checkThreshold(player, profile, Achievement.FEED_100, 100, statKey);
                break;

            case "feed_luxury":
                unlock(player, profile, Achievement.FEED_LUXURY);
                break;

            case "feed_gross":
                unlock(player, profile, Achievement.FEED_GROSS);
                break;

            case "auto_eat_count":
                checkThreshold(player, profile, Achievement.AUTO_EAT, 50, statKey);
                break;

            // --- ⚔️ 战斗类 ---
            case "buff_trigger":
                unlock(player, profile, Achievement.BUFF_1);
                break;
            case "burst_trigger":
                unlock(player, profile, Achievement.BURST_1);
                break;
            case "burst_kill":
                unlock(player, profile, Achievement.BURST_KILL);
                break;
            case "soul_compensate":
                unlock(player, profile, Achievement.SAVE_LIFE);
                break;
            case "kill_mob":
                checkThreshold(player, profile, Achievement.MONSTER_HUNTER, 100, statKey);
                break;
            case "boss_fight":
                unlock(player, profile, Achievement.BOSS_FIGHT);
                break;

            // --- 🗓️ 签到与日常 ---
            case "signin_update":
                if (profile.getConsecutiveDays() >= 7) unlock(player, profile, Achievement.SIGNIN_7);
                if (profile.getTotalCheckIns() >= 30) unlock(player, profile, Achievement.SIGNIN_30);
                break;
            case "signin_holiday":
                unlock(player, profile, Achievement.SIGNIN_HOLIDAY);
                break;

            // --- 🎵 其他 ---
            case "gravity_pickup":
                checkThreshold(player, profile, Achievement.GRAVITY_PICKUP, 64, statKey);
                break;
            case "gravity_filter":
                unlock(player, profile, Achievement.GRAVITY_FILTER);
                break;
            case "effect_unlock":
                unlock(player, profile, Achievement.EFFECT_UNLOCK);
                break;
        }
    }

    // 辅助检查数值阈值
    private static void checkThreshold(Player p, SpiritProfile profile, Achievement ach, int target, String statKey) {
        if (profile.getStat(statKey) >= target) {
            unlock(p, profile, ach);
        }
    }

    /**
     * 达成成就（广播通报 + 标记状态）
     */
    public static void unlock(Player p, SpiritProfile profile, Achievement ach) {
        // 如果已经解锁过，直接返回
        if (profile.getUnlockedAchievements().contains(ach.getId())) return;

        // 1. 标记为已解锁
        profile.addUnlockedAchievement(ach.getId());

        // 2. 发送原版成就弹窗 (Toast) - 仅玩家自己可见
        sendToast(p, ach);

        // 3. 构建全服广播消息 (带悬浮查看功能)

        // 3a. 构建奖励文本
        StringBuilder rewardText = new StringBuilder();
        if (ach.getExpReward() > 0) {
            rewardText.append("§b经验 x").append(ach.getExpReward()).append(" ");
        }
        if (ach.getItemReward() != null) {
            String friendlyName = getFriendlyName(ach.getItemReward().getType());
            rewardText.append("§d").append(friendlyName).append(" x").append(ach.getItemReward().getAmount());
        }
        if (rewardText.length() == 0) {
            rewardText.append("§7(无实质奖励)");
        }

        // 3b. 构建悬浮内容 (Hover)
        Component hoverContent = Component.text()
                .append(Component.text("§6§l" + ach.getName()))
                .append(Component.newline())
                .append(Component.text("§7" + ach.getDescription()))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("§f🎁 奖励: "))
                .append(Component.text(rewardText.toString()))
                .build();

        // 3c. 构建广播消息主体
        Component broadcastMsg = Component.text()
                .append(Component.text("§8[§d灵契§8] §f恭喜玩家 "))
                .append(Component.text(p.getName()).color(NamedTextColor.AQUA))
                .append(Component.text(" §f达成了成就 "))
                .append(Component.text("§e[" + ach.getName() + "]")
                        .hoverEvent(HoverEvent.showText(hoverContent))) // 添加悬浮事件
                .build();

        // 4. 发送广播和音效
        Bukkit.broadcast(broadcastMsg);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // 保存数据
        getConfig().saveProfile(profile);
    }

    /**
     * 领取奖励（由 GUI 点击触发）
     */
    public static void claimReward(Player p, SpiritProfile profile, Achievement ach) {
        // 安全检查
        if (!profile.getUnlockedAchievements().contains(ach.getId())) {
            p.sendMessage("§c你还没有达成这个成就哦！");
            return;
        }
        if (profile.isClaimed(ach.getId())) {
            p.sendMessage("§c这个奖励已经领过啦！");
            return;
        }

        // 1. 标记为已领取
        profile.setClaimed(ach.getId());

        // 2. 发放物品
        if (ach.getItemReward() != null) {
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(ach.getItemReward().clone());
            if (!left.isEmpty()) {
                for (ItemStack drop : left.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
                p.sendMessage("§e[背包已满] 奖励已掉落在脚下。");
            }
        }

        // 3. 发放经验
        if (ach.getExpReward() > 0) {
            profile.addExp(ach.getExpReward());
        }

        // 4. 反馈
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        p.sendMessage("§a 成功领取成就奖励！");

        getConfig().saveProfile(profile);
    }

    /**
     * 发送原版成就弹窗的黑科技
     */
    private static void sendToast(Player player, Achievement ach) {

        try {
            // 使用 Title 模拟视觉冲击 (最稳妥方案)
            player.sendTitle("§e🏆 达成成就", "§f" + ach.getName(), 10, 40, 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取物品友好名称
     */
    private static String getFriendlyName(Material mat) {
        return switch (mat) {
            case DIAMOND -> "钻石";
            case NETHERITE_SCRAP -> "下界合金碎片";
            case ENCHANTED_GOLDEN_APPLE -> "附魔金苹果";
            case GOLD_INGOT -> "金锭";
            case IRON_INGOT -> "铁锭";
            case EMERALD -> "绿宝石";
            case EXPERIENCE_BOTTLE -> "附魔之瓶";
            case NAME_TAG -> "命名牌";
            case CAKE -> "蛋糕";
            case CLOCK -> "时钟";
            case WHITE_WOOL -> "羊毛";
            case BREAD -> "面包";
            case GOLDEN_APPLE -> "金苹果";
            case MILK_BUCKET -> "牛奶桶";
            case COOKIE -> "曲奇";
            case POTION -> "药水";
            case FIREWORK_ROCKET -> "烟花火箭";
            case DIAMOND_SWORD -> "钻石剑";
            case TOTEM_OF_UNDYING -> "不死图腾";
            case BOW -> "弓";
            case DRAGON_BREATH -> "龙息";
            case JUKEBOX -> "唱片机";
            case REDSTONE -> "红石";
            case HOPPER -> "漏斗";
            case ITEM_FRAME -> "物品展示框";
            case CHEST -> "箱子";
            case DIAMOND_BLOCK -> "钻石块";
            case FIREWORK_STAR -> "烟火之星";
            case SOUL_LANTERN -> "灵魂灯笼";
            case GLOWSTONE_DUST -> "荧石粉";

            // 幻化相关
            case FOX_SPAWN_EGG -> "狐狸刷怪蛋";
            case PARROT_SPAWN_EGG -> "鹦鹉刷怪蛋";
            case AXOLOTL_BUCKET -> "美西螈桶";
            case SNOW_BLOCK -> "雪块";

            default -> mat.name();
        };
    }

    private static ConfigManager getConfig() {
        return RabiSystem.getInstance().getSpiritModule().getConfigManager();
    }
}