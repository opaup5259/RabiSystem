package cn.rabitown.rabisystem.modules.spirit.ui;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.achievement.Achievement;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritSkin;
import cn.rabitown.rabisystem.modules.spirit.skill.SkillType;
import cn.rabitown.rabisystem.modules.spirit.utils.HolidayUtil;
import cn.rabitown.rabisystem.modules.spirit.utils.LevelSystem;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class SpiritMenus {

    public static final String MAIN_TITLE = "§0§l🧚 灵契链接";
    public static final String BACKPACK_TITLE = "§8§l🎒 小精灵的包包";
    public static final String FOOD_BAG_TITLE = "§8§l🥪 零食袋";
    public static final String GRAVITY_TITLE = "§0§l🌌 虚空引力管理";
    public static final String SIGNIN_TITLE = "§0§l🗓 灵契月历";
    public static final String RANK_TITLE = "§0§l🏆 签到荣耀榜";
    public static final String ACHIEVEMENT_TITLE = "§8§l🏆 灵契成就录";
    public static final String LEVEL_TREE_TITLE = "§0§l🌳 技能树"; // 新增标题
    public static final String TOOLS_TITLE = "§8§l🛠 便携工坊";

    // --- 幻化菜单布局配置 ---
    // 定义皮肤图标可以放置的槽位 (跳过了中间的装饰格)
    private static final int[] SKIN_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    /**
     * 打开主菜单 (默认第一页)
     */
    public static void openMainMenu(Player player, SpiritProfile profile) {
        openMainMenu(player, profile, 1);
    }

    /**
     * 打开主菜单 (指定页码)
     */
    public static void openMainMenu(Player player, SpiritProfile profile, int page) {
        SpiritHolder holder = new SpiritHolder(profile.getOwnerId(), "MAIN", page);
        // 如果是第二页，标题加个后缀提示
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(MAIN_TITLE + (page > 1 ? " (P2)" : "")));

        long now = System.currentTimeMillis();
        long expireTime = profile.getReunionExpireTime();
        int level = profile.getLevel();

        // 统一背景板
        ItemStack whiteGlass = createSpacer(Material.WHITE_STAINED_GLASS_PANE);
        ItemStack blackGlass = createSpacer(Material.BLACK_STAINED_GLASS_PANE);

        if (page == 1) {
            // ==================== Page 1 ====================

            // --- Row 0 ---
            inv.setItem(0, whiteGlass);
            inv.setItem(1, whiteGlass);
            inv.setItem(2, whiteGlass);
            inv.setItem(3, whiteGlass);
            inv.setItem(4, createBundlePreview(profile)); // 小精灵背包
            inv.setItem(5, whiteGlass);
            inv.setItem(6, whiteGlass);
            inv.setItem(7, createSignInIcon(profile)); // 岁月铭刻 (签到)
            inv.setItem(8, createLotteryIcon(profile)); // 星界祈愿

            // --- Row 1 ---
            // 技能树
            inv.setItem(9, createItem(Material.TORCHFLOWER, "§6🌳 技能树", "§7查看各阶段的觉醒能力", "§7选择激活的技能树", "","§e▶ 点击进入技能树界面"));
            // 灵核
            inv.setItem(10, createCoreIcon(player, profile));
            // 日程
            inv.setItem(11, createScheduleIcon(profile));
            // 真名刻印
            inv.setItem(12, createItem(Material.NAME_TAG, "§d§l🏷 真名刻印",
                    "§7§o『名字是灵魂的锚点，』",
                    "§8§m-----------------------",
                    "§e[✦ 灵魂羁绊 ✦]",
                    "§f赋予小精灵独一无二的 §d真名§f。",
                    "§f当前名字: §r" + profile.getName(),
                    "§c消耗: 命名牌 x1",
                    "§8§m-----------------------",
                    "§e▶ 请携带命名牌点击"
            ));

            // 灵韵流光 (特效)
            ItemStack effectIcon = new ItemStack(Material.NETHER_STAR);
            ItemMeta effectMeta = effectIcon.getItemMeta();
            effectMeta.displayName(Component.text("§d§l✨ 灵韵流光").decoration(TextDecoration.ITALIC, false));
            effectMeta.lore(Arrays.asList(
                    Component.text("§7§o『它是星辰的碎片，是环绕你身侧的微光。』").decoration(TextDecoration.ITALIC, false),
                    Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false),
                    Component.text("§e[✦ 灵子形态 ✦]").decoration(TextDecoration.ITALIC, false),
                    Component.text("§f当前特效: §d" + profile.getActiveEffect().getName()).decoration(TextDecoration.ITALIC, false),
                    Component.text("§f调整小精灵周身的粒子光环。").decoration(TextDecoration.ITALIC, false),
                    Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false),
                    Component.text("§e▶ 点击配置灵韵").decoration(TextDecoration.ITALIC, false)
            ));
            effectIcon.setItemMeta(effectMeta);
            inv.setItem(13, effectIcon);

            // 幻形之镜
            if (profile.hasAnyUnlockedSkin()) {
                ItemStack skinIcon = new ItemStack(Material.AMETHYST_CLUSTER);
                ItemMeta skinMeta = skinIcon.getItemMeta();
                skinMeta.displayName(Component.text("§b§l🦋 幻形之镜").decoration(TextDecoration.ITALIC, false));
                skinMeta.lore(Arrays.asList(
                        Component.text("§7§o『唯有灵魂始终如一。』").decoration(TextDecoration.ITALIC, false),
                        Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false),
                        Component.text("§e[✦ 幻化之镜 ✦]").decoration(TextDecoration.ITALIC, false),
                        Component.text("§f当前形态: §a" + getSkinDisplayName(profile.getCurrentSkin())).decoration(TextDecoration.ITALIC, false),
                        Component.text("§f改变小精灵的实体形态。").decoration(TextDecoration.ITALIC, false),
                        Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false),
                        Component.text("§e▶ 点击进行幻化").decoration(TextDecoration.ITALIC, false)
                ));
                skinIcon.setItemMeta(skinMeta);
                inv.setItem(14, skinIcon);
            } else {
                ItemStack locked = new ItemStack(Material.STRUCTURE_VOID);
                ItemMeta lMeta = locked.getItemMeta();
                lMeta.displayName(Component.text("§8🔒 幻形之镜 (未解锁)").decoration(TextDecoration.ITALIC, false));
                lMeta.lore(Arrays.asList(
                        Component.text("§7当你在成就铭刻的道路上"),
                        Component.text("§7有所建树时，此功能将自动开启。")
                ));
                locked.setItemMeta(lMeta);
                inv.setItem(14, locked);
            }

            inv.setItem(15, whiteGlass); // 空白
            inv.setItem(16, whiteGlass); // 空白

            // 成就铭刻 (动态计算)
            int totalAchs = Achievement.values().length;
            int unlockedCount = profile.getUnlockedAchievements().size();
            int progressPercent = (totalAchs > 0) ? (int) ((double) unlockedCount / totalAchs * 100) : 0;
            inv.setItem(17, createItem(Material.WRITABLE_BOOK, "§e🏆 成就铭刻",
                    "§7§o『凡走过必留下痕迹，凡经历必化作星光。』",
                    "§8§m-----------------------",
                    "§7当前进度: §a" + progressPercent + "%",
                    "§7已解锁: §f" + unlockedCount + " / " + totalAchs,
                    "",
                    "§e▶ 点击查看里程碑"
            ));

            // --- Row 2 ---
            fillRow(inv, 18, Material.BLACK_STAINED_GLASS_PANE);

            // --- Row 3 ---
            inv.setItem(27, createItem(Material.IRON_PICKAXE, "§e🗡 装备栏位", "§7(右侧) 放置主手物品", "§7小精灵将手持该物品"));
            placePhysicalSlot(inv, 28, profile.getEquipItem(0), "§7[主手装备]", 10, profile.getLevel()); // Slot 28 物理槽

            inv.setItem(29, blackGlass);

            // 主动技能
            ItemStack activeItem;
            if (profile.getActiveSkillId() != null) {
                SkillType skill = SkillType.fromId(profile.getActiveSkillId());
                if (skill != null) {
                    activeItem = createItem(skill.getIcon(), "§6★ 主动技能: " + skill.getName(),
                            "§7" + String.join("\n§7", skill.getDescription()),
                            "", "§e[Shift+F 触发]");
                    ItemMeta meta = activeItem.getItemMeta();
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    activeItem.setItemMeta(meta);
                } else {
                    activeItem = createItem(Material.STRUCTURE_VOID, "§7主动技能槽", "§7(数据异常)");
                }
            } else {
                activeItem = createItem(Material.STRUCTURE_VOID, "§7主动技能槽", "§7在技能树中点击【主动技能】装备");
            }
            inv.setItem(30, activeItem);

            // 快捷技能
            List<String> quicks = profile.getQuickSkillIds();
            for (int i = 0; i < 2; i++) {
                int slot = 31 + i;
                ItemStack quickItem;
                if (i < quicks.size()) {
                    SkillType skill = SkillType.fromId(quicks.get(i));
                    if (skill != null) {
                        quickItem = createItem(skill.getIcon(), "§b⚡ 快捷技能: " + skill.getName(),
                                "§7" + String.join("\n§7", skill.getDescription()),
                                "", "§e[点击释放]");
                    } else {
                        quickItem = createItem(Material.STRUCTURE_VOID, "§7快捷技能槽", "§7(数据异常)");
                    }
                } else {
                    quickItem = createItem(Material.STRUCTURE_VOID, "§7快捷技能槽", "§7在技能树中点击【快捷技能】装备");
                }
                inv.setItem(slot, quickItem);
            }

            inv.setItem(33, blackGlass);
            inv.setItem(34, createItem(Material.HAY_BLOCK, "§6🥪 零食收纳", "§7放置食物在右侧"));
            ItemStack foodFirst = (profile.getFoodBag().length > 0) ? profile.getFoodBag()[0] : null;
            placePhysicalSlot(inv, 35, foodFirst, "§a🍎 快捷投喂 [B]", 10, profile.getLevel()); // Slot 35 物理槽

            // --- Row 4 ---
            fillRow(inv, 36, Material.BLACK_STAINED_GLASS_PANE);

            // --- Row 5 ---
            inv.setItem(45, whiteGlass);
            inv.setItem(46, whiteGlass);
            inv.setItem(47, whiteGlass);
            inv.setItem(48, whiteGlass);
            // 传送石界面入口
            inv.setItem(49, createItem(Material.LODESTONE, "§b§l🌌 传送石", "§7管理你的传送锚点", "§e▶ 点击进入"));
            inv.setItem(50, whiteGlass);
            inv.setItem(51, whiteGlass);
            inv.setItem(52, whiteGlass);
            inv.setItem(53, createItem(Material.ARROW, "§f下一页 ▶", "§7前往功能页"));

        } else {
            // ==================== Page 2 ====================

            // --- Row 0 ---
            fillRow(inv, 0, Material.BLACK_STAINED_GLASS_PANE);

            // --- Row 1 (能力开关) ---
            // 1. 生命反哺 (Heal Back) - Lv.1
            double healAmount = LevelSystem.getHealAmount(level);
            int healMoodCost = LevelSystem.getHealMoodCost(level);
            String moodCostText = (healMoodCost == 0) ? "§a无消耗 (Lv.50特性)" : "§c-" + healMoodCost + " 点";
            placeAbilitySwitch(inv, 9, Material.GOLDEN_APPLE, "生命反哺", 1, level, profile.isHealBackEnabled(),
                    "§7§o『以灵之血，补契约者之缺。』",
                    "§8§m-----------------------",
                    "§e[✦ 当前属性]",
                    "§7触发条件: §f生命 < 12.0",
                    "§7治疗效果: §a+" + (int)healAmount + " HP §7(每5秒)",
                    "§7心情消耗: " + moodCostText,
                    "§8§m-----------------------",
                    "§7“它并不理解痛苦，却能感受到你的虚弱。",
                    "§7即便燃尽微弱的荧光，也想拉住你下坠的衣角。”",
                    "");

            // 2. 灵力共鸣 (Resonance) - Lv.30
            String strTier = LevelSystem.getResonanceTierName(level);
            int strCdSeconds = (int) (LevelSystem.getResonanceCooldown(level) / 1000);
            int strDuration = LevelSystem.getResonanceDurationTicks(level) / 20;
            placeAbilitySwitch(inv, 10, Material.DRAGON_BREATH, "灵力共鸣", 30, level, profile.isResonanceEnabled(),
                    "§7§o『灵魂的波长若能重叠，凡铁亦可斩钢。』",
                    "§8§m-----------------------",
                    "§e[✦ 当前属性]",
                    "§7触发条件: §f造成攻击",
                    "§7共鸣效果: §b力量 " + strTier + " §7(持续 " + strDuration + "s)",
                    "§7冷却时间: §f" + strCdSeconds + " 秒",
                    "§8§m-----------------------",
                    "§7“听，那是灵魂交织的旋律。",
                    "§7当你们心意相通，世界的星辰也会为你助阵。”",
                    "");

            // 3. 灵力迸发 (Burst) - Lv.50
            double burstDmg = LevelSystem.getBurstDamage(level);
            int burstCdSeconds = (int) (LevelSystem.getBurstCooldown(level) / 1000);
            placeAbilitySwitch(inv, 11, Material.END_CRYSTAL, "灵力迸发", 50, level, profile.isBurstEnabled(),
                    "§7§o『星屑汇聚之时，即是审判降临之刻。』",
                    "§8§m-----------------------",
                    "§e[✦ 当前属性]",
                    "§7触发条件: §f攻击/被击",
                    "§7迸发伤害: §6" + (int)burstDmg + " 点真实伤害",
                    "§7冷却时间: §f" + burstCdSeconds + " 秒",
                    "§7蓄力时间: §b3 秒",
                    "§8§m-----------------------",
                    "§7“平日里它收敛锋芒，只在你身后起舞。",
                    "§7但若有敌意逼近，它将化作你手中锋利的长枪。”",
                    "");

            // 4. 灵魂代偿 (Soul Compensate) - Lv.80
            placeAbilitySwitch(inv, 12, Material.TOTEM_OF_UNDYING, "灵魂代偿", 80, level, profile.isSoulCompensateEnabled(),
                    "§7§o『这是终极的契约——以此身破碎，换你无恙。』",
                    "§8§m-----------------------",
                    "§e[✦ 能力详解]",
                    "§7触发条件: §c致死伤害",
                    "§7守护效果: §a免疫死亡 §7+ §d强力Buff",
                    "§7触发代价: §c-50 心情 §7& §c10分钟 重聚",
                    "§8§m-----------------------",
                    "§7“星辰陨落是为了让黎明升起。",
                    "§7它将化作最亮的流星，坠入你名为‘生’的梦里。”",
                    "");

            inv.setItem(13, blackGlass); // 空白 (改为黑玻璃)
            inv.setItem(14, blackGlass); // 空白
            inv.setItem(15, blackGlass); // 空白
            inv.setItem(16, blackGlass); // 空白
            inv.setItem(17, createVoidGravityIcon(profile)); // 虚空引力

            // --- Row 2 ---
            fillRow(inv, 18, Material.BLACK_STAINED_GLASS_PANE);

            // --- Row 3 ---
            fillRow(inv, 27, Material.BLACK_STAINED_GLASS_PANE);

            // --- Row 4 ---
            // 在线时长统计
            inv.setItem(36, createItem(Material.SPYGLASS, "§b§l📊 在线时长录",
                    "§7§o『 窥探现世灵力波动，",
                    "§7§o   知晓何人活跃于此。 』",
                    "§8§m-----------------------",
                    "§e[✦ 功能 ✦]",
                    "§f查看当前在线玩家的统计数据。",
                    "§8§m-----------------------",
                    "§e▶ 点击查看"
            ));
            // 摸鱼时长统计
            inv.setItem(37, createItem(Material.FISHING_ROD, "§e§l🐟 摸鱼排行",
                    "§7§o『 偷得浮生半日闲。 』",
                    "§8§m-----------------------",
                    "§e[✦ 功能 ✦]",
                    "§f查看谁是最大的懒虫。",
                    "§8§m-----------------------",
                    "§e▶ 点击查看"
            ));

            inv.setItem(38, whiteGlass);
            inv.setItem(39, whiteGlass);
            inv.setItem(40, whiteGlass);
            inv.setItem(41, whiteGlass);
            inv.setItem(42, whiteGlass);

            // 认知干扰
            boolean hideState = profile.isHideOthers();
            ItemStack barrier = createItem(hideState ? Material.BARRIER : Material.HEAVY_CORE,
                    "§b🛡 认知干扰 (屏蔽他人)",
                    "§7当前状态: " + (hideState ? "§a[✔ 已开启]" : "§c[✘ 已关闭]"),
                    "§7开启后，你将 §c看不到 §7其他玩家的小精灵",
                    "",
                    "§e▶ 点击切换"
            );
            if (hideState) addGlow(barrier);
            inv.setItem(43, barrier);

            // 收回小精灵 / 重聚
            if (expireTime > now) {
                ItemStack reuniting = new ItemStack(Material.SOUL_LANTERN);
                ItemMeta meta = reuniting.getItemMeta();
                meta.displayName(Component.text("§c§l⚡ 灵魂重聚中...").decoration(TextDecoration.ITALIC, false));
                long remainingMillis = expireTime - now;
                long mins = (remainingMillis / 1000) / 60;
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7§o『破碎的灵魂正在灯火中缓慢聚合。』").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§7小精灵的灵体正在灯笼中缓慢修复。").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§7剩余时间: §f" + mins + " 分 ").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                reuniting.setItemMeta(meta);
                inv.setItem(44, reuniting);
            } else if (profile.isSummoned()) {
                inv.setItem(44, createItem(Material.SOUL_LANTERN, "§c§l⚛ 回归灵契空间",
                        "§7§o『暂时的分别，是为了更好的重逢。』",
                        "§8§m-----------------------",
                        "§e[✦ 灵魂休眠 ✦]",
                        "§f将小精灵送回灵契空间休息。",
                        "§f(双击潜行可再次呼唤)",
                        "§8§m-----------------------",
                        "§e▶ 点击收回小精灵"
                ));
            }

            // --- Row 5 ---
            inv.setItem(45, blackGlass);
            inv.setItem(46, blackGlass);
            inv.setItem(47, blackGlass);
            inv.setItem(48, whiteGlass);
            inv.setItem(49, whiteGlass);
            inv.setItem(50, whiteGlass);
            inv.setItem(51, whiteGlass);
            inv.setItem(52, createItem(Material.ARROW, "§f◀ 上一页", "§7返回主页"));
            inv.setItem(53, blackGlass);
        }

        player.openInventory(inv);
    }

    /**
     * 打开背包界面
     */
    public static void openBackpack(Player player, SpiritProfile profile) {
        int size = profile.getBackpackSize();
        // 确保 size 至少为 9，否则放不下按钮 (虽然 profile 逻辑里最小也是 9)
        if (size < 9) size = 9;

        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "BACKPACK"), size, Component.text(BACKPACK_TITLE));

        // 1. 加载物品（注意：要避开最后一个格子）
        ItemStack[] stored = profile.getBackpack();
        if (stored != null) {
            // 只读取前 (size-1) 个物品，防止把按钮位置的数据覆盖，或者把以前存的数据挤掉
            for (int i = 0; i < size - 1; i++) {
                if (i < stored.length && stored[i] != null) {
                    inv.setItem(i, stored[i]);
                }
            }
        }

        // 2. [新增] 放置返回按钮 (锁定在最后一格)
        inv.setItem(size - 1, createReturnButton());

        player.openInventory(inv);
    }

    /**
     * 打开食物包界面 (固定9格)
     */
    public static void openFoodBag(Player player, SpiritProfile profile) {
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "FOOD"), 9, Component.text(FOOD_BAG_TITLE));

        int unlockedSlots = getUnlockedFoodSlots(profile.getLevel());
        ItemStack[] stored = profile.getFoodBag();

        for (int i = 0; i < 9; i++) {
            // A. 最后一格：返回按钮
            if (i == 8) {
                inv.setItem(i, createReturnButton());
                continue;
            }

            // B. 已解锁区域
            if (i < unlockedSlots) {
                if (stored != null && i < stored.length && stored[i] != null) {
                    inv.setItem(i, stored[i]);
                }
            }
            // C. 未解锁区域：填充白色玻璃板
            else {
                ItemStack glass = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                ItemMeta meta = glass.getItemMeta();
                meta.displayName(Component.text("§7[未解锁槽位] Lv." + getNextUnlockLevel(i)).decoration(TextDecoration.ITALIC, false));
                glass.setItemMeta(meta);
                inv.setItem(i, glass);
            }
        }
        player.openInventory(inv);
    }

    private static ItemStack createReturnButton() {
        // 使用深色橡木门或者屏障作为图标，这里用 橡木门 图标比较像“出口”
        ItemStack item = new ItemStack(Material.IRON_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c⬅ 返回主界面").decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("§7点击返回灵契主界面").decoration(TextDecoration.ITALIC, false)
        ));
        // =========================================================
        // 打上 "lsc_gui_button" 标签
        // =========================================================
        // 1. 获取 Key (必须和 MenuListener 里判断的 Key 一致)
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(RabiSystem.getInstance(), "lsc_gui_button");

        // 2. 写入数据 (类型为 BYTE, 值为 1)
        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        // =========================================================

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 计算已解锁的食物格子数量
     * 默认1格，每提升一个阶段加1格
     */
    public static int getUnlockedFoodSlots(int level) {
        // 1-9: 1格
        if (level < 10) return 1;
        // 10-29: 2格
        if (level < 30) return 2;
        // 30-49: 3格
        if (level < 50) return 3;
        // 50-79: 4格
        if (level < 80) return 4;
        // 80+: 5格
        return 5;
        // 最大5格，因为 UI 只有9格，还要留给返回按钮和装饰
    }

    /**
     * 获取某格子解锁需要的等级 (用于玻璃板提示)
     */
    private static int getNextUnlockLevel(int slotIndex) {
        return switch (slotIndex) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 50;
            case 4 -> 80;
            default -> 999;
        };
    }

    // --- 辅助构建方法 ---
    private static void fillRow(Inventory inv, int start, Material mat) {
        for (int i = 0; i < 9; i++) {
            inv.setItem(start + i, createSpacer(mat));
        }
    }

    private static ItemStack createSpacer(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createBundlePreview(SpiritProfile profile) {
        ItemStack bundle = new ItemStack(Material.BUNDLE);
        org.bukkit.inventory.meta.BundleMeta meta = (org.bukkit.inventory.meta.BundleMeta) bundle.getItemMeta();

        meta.displayName(Component.text("§6🎒 小精灵的包包").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

        // 过滤掉 null 物品，否则 setContents 可能报错或显示异常
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack is : profile.getBackpack()) {
            if (is != null && is.getType() != Material.AIR) {
                items.add(is);
            }
        }

        // 应用原生预览
        meta.setItems(items);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7点击打开完整背包"));
        lore.add(Component.text("§8(悬停查看内部物品)"));
        meta.lore(lore);

        bundle.setItemMeta(meta);
        return bundle;
    }

    public static ItemStack createCoreIcon(Player player, SpiritProfile profile) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text("§b🔮灵核 · 羁绊之心").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        // 获取数据
        int level = profile.getLevel();
        // 注意：这里展示的是“当前等级内的进度”，而不是总经验
        double currentExp = LevelSystem.getCurrentLevelExp(profile.getExp());
        int maxExp = LevelSystem.getExpToNextLevel(level);
        String stageName = LevelSystem.getStageName(level);

        // --- 心情进度条构建 ---
        int moodBars = profile.getMood() / 10;
        StringBuilder moodBarStr = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            moodBarStr.append(i < moodBars ? "§d■" : "§7■"); // 用粉色心心
        }
        moodBarStr.append("§8]");

        // --- 修复心情描述逻辑 (从小到大判断) ---
        String moodText;
        int mood = profile.getMood();
        if (mood < 30) moodText = "它蜷缩在角落，看起来非常低落...";
        else if (mood < 50) moodText = "它无精打采地飘浮着...";
        else if (mood < 80) moodText = "它正安静地注视着你。";
        else moodText = "它兴奋地在你身边转圈圈，心情超棒！";

        List<Component> lore = Arrays.asList(
                // 等级行
                Component.text(String.format("§7等级: §eLv.%d (%s)", level, stageName)),
                // 心情行 (保留进度条)
                Component.text("§7心情: " + moodBarStr + " §d" + mood),
                // 生命行 (强制转为整数)
                Component.text(String.format("§7生命: §a%d/%d", (int) profile.getHealth(), (int) profile.getMaxHealth())),
                Component.text("§7灵力(MP): §b" + (int)profile.getMana() + " §7/ " + (int)profile.getMaxMana()),
                Component.empty(),
                // 经验行 (直接显示数值)
                Component.text(String.format("§7当前经验: §f%.0f §7/ §7%d", currentExp, maxExp)),
                Component.empty(),
                Component.text("§o\"" + moodText + "\"").color(NamedTextColor.GRAY)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createScheduleIcon(SpiritProfile profile) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e📅 今日日程").decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        // 基础经验进度显示
        lore.add(Component.text("§7摸摸头: §f" + profile.getDailyPetExp() + "§8/30 §f|潜行+右键")); // [cite: 37, 112]
        lore.add(Component.text("§7自然陪伴: §f" + profile.getDailyCompanionExp() + "§8/120 §f|陪伴就是最好的爱")); //
        lore.add(Component.text("§7社交互动: §f" + profile.getDailySocialExp() + "§8/30 §f|让小精灵与其他小精灵玩耍")); //
        lore.add(Component.text("§7美食投喂: §f" + profile.getDailyFeedExp() + "§8/30")); // [cite: 36]
        lore.add(Component.text("§7生命反哺: §f" + profile.getDailyHealExp() + "§8/60")); //
        lore.add(Component.text("§7助战增幅: §f" + profile.getDailyBuffExp())); // [cite: 38]
        lore.add(Component.text("§7造成伤害: §f" + profile.getDailyDamageExp())); // [cite: 38]

        lore.add(Component.text("§8§m-----------------------"));

        // 额外加成显示
        lore.add(Component.text("§7额外经验加成剩余: §b" + (int) profile.getExtraExpBonus()));
        lore.add(Component.text("§7经验加成卡: §e" + profile.getExpBonusCard()));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // 放置物理槽位
    private static void placePhysicalSlot(Inventory inv, int slot, ItemStack currentItem, String name, int reqLv, int currentLv) {
        if (currentLv < reqLv) {
            ItemStack lock = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = lock.getItemMeta();
            meta.displayName(Component.text("§c🔒 " + name.replaceAll("§.", "")).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(Component.text("§c需要等级: Lv." + reqLv), Component.text("§7该能力目前未解锁。")));
            lock.setItemMeta(meta);
            inv.setItem(slot, lock);
        } else {
            if (currentItem != null) {
                inv.setItem(slot, currentItem);
            }
        }
    }

    /**
     * 放置能力开关 (支持自定义 Lore)
     *
     * @param descLore 可变参数，传入多行描述文本
     */
    private static void placeAbilitySwitch(Inventory inv, int slot, Material mat, String name, int reqLv, int currentLv, boolean state, String... descLore) {
        if (currentLv < reqLv) {
            ItemStack lock = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = lock.getItemMeta();
            meta.displayName(Component.text("§c🔒 " + name).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(
                    Component.text("§c解锁条件: Lv." + reqLv).decoration(TextDecoration.ITALIC, false),
                    Component.text("§7该能力目前未解锁。").decoration(TextDecoration.ITALIC, false)
            ));
            lock.setItemMeta(meta);
            inv.setItem(slot, lock);
        } else {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            String color = state ? "§a" : "§7";
            meta.displayName(Component.text(color + name).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7当前状态: " + (state ? "§a[✔ 已激活]" : "§c[✘ 已关闭]")).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§e▶ 点击切换").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            if (descLore != null) {
                for (String line : descLore) {
                    lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);
            if (state) addGlow(item);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }
    }

    /**
     * 修改 createSwitch 以符合统一风格 (自动饮食使用)
     */
    private static ItemStack createSwitch(Material mat, String name, boolean state, int reqLv, int currentLv) {
        if (currentLv < reqLv) {
            return createItem(Material.STRUCTURE_VOID, "§c🔒 " + name.replaceAll("§.", ""), "§c需要等级: Lv." + reqLv);
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("§7状态: " + (state ? "§a开启" : "§c关闭")).decoration(TextDecoration.ITALIC, false),
                Component.text("§e点击切换").decoration(TextDecoration.ITALIC, false)
        ));
        if (state) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        if (lore.length > 0) {
            List<Component> cLore = new ArrayList<>();
            for (String s : lore) cLore.add(Component.text(s).decoration(TextDecoration.ITALIC, false));
            meta.lore(cLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    // 自定义 Holder 用于识别
    public static class SpiritHolder implements org.bukkit.inventory.InventoryHolder {
        private final UUID owner;
        private final String type;
        private Calendar viewCalendar;
        private int page = 1;
        private FilterType filterType = FilterType.ALL;
        // 技能树页码
        private int starPage = 0;
        private int shadowPage = 0;

        public SpiritHolder(UUID owner, String type) {
            this.owner = owner;
            this.type = type;
        }

        public SpiritHolder(UUID owner, String type, Calendar cal) {
            this(owner, type);
            this.viewCalendar = cal;
        }

        public SpiritHolder(UUID owner, String type, int page) {
            this(owner, type);
            this.page = page;
        }

        public SpiritHolder(UUID owner, String type, int page, FilterType filterType) {
            this(owner, type, page);
            this.filterType = filterType;
        }
        public int getStarPage() { return starPage; }
        public void setStarPage(int page) { this.starPage = page; }
        public int getShadowPage() { return shadowPage; }
        public void setShadowPage(int page) { this.shadowPage = page; }

        @Override
        public Inventory getInventory() { return null; }
        public UUID getOwner() { return owner; }
        public String getType() { return type; }
        public Calendar getViewCalendar() { return viewCalendar; }
        public int getPage() { return page; }
        public FilterType getFilterType() { return filterType; }
    }

    /**
     * 打开虚空引力管理界面 (54格)
     */
    public static void openGravityMenu(Player player, SpiritProfile profile) {
        // 使用 GRAVITY 类型标识
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "GRAVITY"), 54, Component.text(GRAVITY_TITLE));

        // --- Row 0-3: 物品过滤放置区 ---
        // 这里显示当前已过滤的物品
        List<Material> filterList = profile.getFilterList();
        for (int i = 0; i < Math.min(filterList.size(), 36); i++) {
            inv.setItem(i, new ItemStack(filterList.get(i)));
        }

        // --- Row 4: 分隔线 ---
        fillRow(inv, 36, Material.BLACK_STAINED_GLASS_PANE);

        // --- Row 5: 功能控制区 ---
        // Slot 45: 能力总开关 (红石灯)
        inv.setItem(45, createGravitySwitch(profile.isVoidGravityEnabled()));

        // Slot 46: 过滤开关 (红石火把)
        inv.setItem(46, createFilterSwitch(profile.isFilterEnabled()));

        // Slot 47: 模式切换 (钟)
        inv.setItem(47, createModeSwitch(profile.isWhitelistMode()));

        inv.setItem(48, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(49, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(51, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(50, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(50, createSpacer(Material.WHITE_STAINED_GLASS_PANE));

        // Slot 53: 返回主菜单
        inv.setItem(53, createItem(Material.IRON_DOOR, "§c⬅ 返回主界面", "§7回到灵契链接主菜单"));

        player.openInventory(inv);
    }

    // --- 虚空引力专用组件构建 ---
    private static ItemStack createVoidGravityIcon(SpiritProfile profile) {
        int level = profile.getLevel();
        boolean unlocked = level >= 30;

        // 如果未解锁，显示结构空位
        if (!unlocked) {
            ItemStack lock = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = lock.getItemMeta();
            meta.displayName(Component.text("§c🔒 虚空引力").decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(
                    Component.text("§c解锁条件: Lv.30"),
                    Component.text("§7能力描述: 自动吸取周围掉落物至背包。")
            ));
            lock.setItemMeta(meta);
            return lock;
        }

        // 已解锁状态显示
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§d🌌 虚空引力").decoration(TextDecoration.ITALIC, false));

        // 计算当前范围
        double range = profile.getPickupRange();
        String mode = profile.isFilterEnabled() ?
                (profile.isWhitelistMode() ? "§f[白名单]" : "§f[黑名单]") : "§7[未开启过滤]";

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7当前引力范围: §b" + (int) range + " 格"));
        lore.add(Component.text("§7引力状态: " + (profile.isVoidGravityEnabled() ? "§a激活" : "§c停用")));
        lore.add(Component.text("§7过滤模式: " + mode));
        lore.add(Component.empty());
        lore.add(Component.text("§e▶ 点击进入详细设置页面"));

        // 如果已开启且有加成，可以增加光效
        if (profile.isVoidGravityEnabled()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createGravitySwitch(boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.REDSTONE_TORCH : Material.SOUL_TORCH); // 亮起的红石灯或熄灭的
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e🌌 引力总开关").decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("§7当前状态: " + (enabled ? "§a开启" : "§c关闭")),
                Component.text("§e点击切换状态")
        ));
        if (enabled) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createFilterSwitch(boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.REDSTONE_TORCH : Material.LEVER); // 亮起火把或拉杆
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6🔍 过滤功能").decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("§7当前状态: " + (enabled ? "§a激活" : "§c停用")),
                Component.text("§e点击切换过滤开关")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createModeSwitch(boolean isWhitelist) {
        ItemStack item = new ItemStack(Material.BELL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§b⚖ 过滤模式").decoration(TextDecoration.ITALIC, false));

        String currentMode = isWhitelist ? "§f[白名单] §a只拾取过滤物品" : "§f[黑名单] §c不拾取过滤物品";
        String nextMode = isWhitelist ? "§f[黑名单] 不拾取过滤物品" : "§f[白名单] 只拾取过滤物品";

        meta.lore(Arrays.asList(
                Component.text("§7当前模式: " + currentMode),
                Component.empty(),
                Component.text("§e点击切换为:"),
                Component.text("§7" + nextMode)
        ));
        item.setItemMeta(meta);
        return item;
    }



    // --- 获取所有可展示的特效 ---
    // 排除 NONE，并按枚举顺序排列
    public static List<SpiritEffectType> getDisplayEffects() {
        return Arrays.stream(SpiritEffectType.values())
                .filter(t -> t != SpiritEffectType.NONE)
                .collect(Collectors.toList());
    }
    /**
     * 打开特效菜单 (自动排列版)
     */
    public static void openEffectsMenu(Player p, SpiritProfile profile) {
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "EFFECTS"), 54, Component.text("§3§l自定义特效"));

        // 1. 功能区 (Row 0)
        ItemStack torchType = new ItemStack(profile.isEffectsEnabled() ? Material.REDSTONE_TORCH : Material.LEVER);
        ItemMeta torchMeta = torchType.getItemMeta();
        torchMeta.displayName(Component.text("§e特效总开关").decoration(TextDecoration.ITALIC, false));
        torchMeta.lore(Arrays.asList(
                Component.text("§7当前状态: " + (profile.isEffectsEnabled() ? "§a已开启" : "§c已关闭")).decoration(TextDecoration.ITALIC, false),
                Component.text("§e▶ 点击切换").decoration(TextDecoration.ITALIC, false)
        ));
        torchType.setItemMeta(torchMeta);

        inv.setItem(4, torchType); // 开关放在中间
        inv.setItem(8, createItem(Material.IRON_DOOR, "§c返回主界面"));

        // 2. 黑色分割线 (Row 1)
        fillRow(inv, 9, Material.BLACK_STAINED_GLASS_PANE);

        // 3. 自动生成特效按钮 (Row 2+)
        List<SpiritEffectType> effects = getDisplayEffects();

        // 起始 Slot，从第 18 格开始
        int startSlot = 18;

        for (int i = 0; i < effects.size(); i++) {
            // 防止越界
            if (startSlot + i >= 54) break;

            SpiritEffectType type = effects.get(i);
            boolean isUnlocked = profile.isEffectUnlocked(type);
            boolean isActive = profile.getActiveEffect() == type;

            inv.setItem(startSlot + i, createAutoEffectIcon(type, isUnlocked, isActive));
        }

        p.openInventory(inv);
    }

    /**
     * 自动生成特效图标
     */
    private static ItemStack createAutoEffectIcon(SpiritEffectType type, boolean unlocked, boolean active) {
        // --- 情况 A: 未解锁 ---
        if (!unlocked) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("§c未解锁特效").decoration(TextDecoration.ITALIC, false));

            // 动态生成获取途径描述
            String sourceDesc = "未知来源";
            switch (type.getSource()) {
                case LEVEL:
                    sourceDesc = "灵契等级达到 Lv." + type.getRequiredLevel();
                    break;
                case LOTTERY:
                    sourceDesc = "星界祈愿(抽奖) 获得";
                    break;
                case DEFAULT:
                    sourceDesc = "默认";
                    break;
            }

            meta.lore(Arrays.asList(
                    Component.text("§7获取途径:").decoration(TextDecoration.ITALIC, false),
                    Component.text("§f" + sourceDesc).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            return item;
        }

        // --- 情况 B: 已解锁 ---
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();

        String prefix = active ? "§a§l[使用中] " : "§e";
        meta.displayName(Component.text(prefix + type.getName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + type.getDescription()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));

        if (active) {
            lore.add(Component.text("§a✔ 当前正在展示此特效").decoration(TextDecoration.ITALIC, false));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(Component.text("§e▶ 点击切换至此特效").decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 核心：打开签到日历界面 (54格)
     */
    public static void openSignInMenu(Player player, SpiritProfile profile, Calendar viewCal) {
        String monthKey = viewCal.get(Calendar.YEAR) + "-" + (viewCal.get(Calendar.MONTH) + 1);
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "SIGNIN", viewCal), 54, Component.text(SIGNIN_TITLE + " - " + monthKey));

        int maxDays = viewCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar realNow = Calendar.getInstance();
        int realToday = realNow.get(Calendar.DAY_OF_MONTH);
        int realMonth = realNow.get(Calendar.MONTH);
        int realYear = realNow.get(Calendar.YEAR);

        Set<Integer> signedDays = profile.getCheckInHistory().computeIfAbsent(monthKey, k -> new HashSet<>());

        // Row 0: 导航与统计 (保持原有逻辑)
        inv.setItem(0, createItem(Material.ARROW, "§f◀ 上一月"));
        inv.setItem(3, createItem(Material.PAPER, "§e📊 签到统计",
                "§7本月已签: §f" + signedDays.size() + " 天",
                "§7剩余补签卡: §b" + profile.getReplacementCards() + " 张"));
        inv.setItem(4, createItem(Material.IRON_DOOR, "§c⬅ 返回主界面"));
        inv.setItem(5, createSingInLeaderboardIcon(profile));
        inv.setItem(8, createItem(Material.ARROW, "§f▶ 下一月"));

        for (int day = 1; day <= 31; day++) {
            int slot = calculateCalendarSlot(day);
            if (slot == -1) continue;
            if (day > maxDays) {
                inv.setItem(slot, createSpacer(Material.BLACK_STAINED_GLASS_PANE));
                continue;
            }

            Calendar cellCal = (Calendar) viewCal.clone();
            cellCal.set(Calendar.DAY_OF_MONTH, day);

            Solar solar = Solar.fromCalendar(cellCal);
            Lunar lunar = solar.getLunar();
            String weekDay = lunar.getWeekInChinese();
            String yi = String.join(", ", lunar.getDayYi());
            String ji = String.join(", ", lunar.getDayJi());

            String holidayName = HolidayUtil.getHolidayName(cellCal);
            boolean isSigned = signedDays.contains(day);
            boolean isToday = (viewCal.get(Calendar.YEAR) == realYear &&
                    viewCal.get(Calendar.MONTH) == realMonth &&
                    day == realToday);

            // --- 样式逻辑：如果是节日显示下界之星 ---
            Material mat;
            String title;
            boolean hasGlow = false;

            if (holidayName != null) {
                mat = Material.NETHER_STAR; // 节日显示为下界之星
                title = "§d§l✨第 " + day + " 天 - " + holidayName;
                hasGlow = true;
            } else if (isSigned) {
                mat = Material.BEACON;
                title = "§a第 " + day + " 天 (已签到)";
            } else if (isToday) {
                mat = Material.GOLD_BLOCK;
                title = "§6§l⭐ 第 " + day + " 天 (今日契约) ⭐";
                hasGlow = true;
            } else if (cellCal.before(realNow)) {
                mat = Material.PAPER;
                title = "§f第 " + day + " 天 (漏签)";
            } else {
                mat = Material.STRUCTURE_VOID;
                title = "§7第 " + day + " 天 (未开启)";
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(title).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7星期" + weekDay).decoration(TextDecoration.ITALIC, false));
//            if (holidayName != null) {
//                lore.add(Component.text("§d§l⚡ 特殊节日奖励已激活").decoration(TextDecoration.ITALIC, false));
//            }
            lore.add(Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false));

            // --- 核心修改：处理 宜/忌 的换行 ---
            addSplitAlmanac(lore, "§a宜: §7", yi, 16); // 每行约16个汉字长度
            addSplitAlmanac(lore, "§c忌: §7", ji, 16);

            lore.add(Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false));

            if (isSigned) {
                lore.add(Component.text("§7状态: §a契约已达成").decoration(TextDecoration.ITALIC, false));
            } else if (isToday) {
                lore.add(Component.text("§e▶ 点击签到").decoration(TextDecoration.ITALIC, false));
            } else if (cellCal.before(realNow)) {
                lore.add(Component.text("§c状态: 漏签").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§e▶ 点击补签").decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            if (hasGlow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        // 背景填充逻辑
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                int col = i % 9;
                if (col == 0 || col == 8) {
                    inv.setItem(i, createSpacer(Material.BLACK_STAINED_GLASS_PANE));
                } else {
                    inv.setItem(i, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
                }
            }
        }

        player.openInventory(inv);
    }

    /**
     * 辅助方法：自动切割长文本并添加到 Lore
     *
     * @param lore   目标 Lore 列表
     * @param prefix 前缀（如 "宜: "）
     * @param text   原始内容
     * @param maxLen 每行最大长度
     */
    private static void addSplitAlmanac(List<Component> lore, String prefix, String text, int maxLen) {
        if (text == null || text.isEmpty()) {
            lore.add(Component.text(prefix + "无").decoration(TextDecoration.ITALIC, false));
            return;
        }

        String[] parts = text.split(", ");
        StringBuilder currentLine = new StringBuilder(prefix);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] + (i == parts.length - 1 ? "" : ", ");

            // 如果当前行加上新内容超过限制，则先存入当前行并开启新行
            if (currentLine.length() + part.length() > maxLen + 4) { // +4 补偿颜色代码长度
                lore.add(Component.text(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                currentLine = new StringBuilder("    §7").append(part); // 换行缩进并保持内容颜色
            } else {
                currentLine.append(part);
            }
        }

        if (currentLine.length() > 0) {
            lore.add(Component.text(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
        }
    }

    /**
     * 签到排行榜 (3行)
     */
    public static void openLeaderboardMenu(Player player, SpiritProfile profile) {
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "RANK"), 27, Component.text(RANK_TITLE));
        fillRow(inv, 0, Material.WHITE_STAINED_GLASS_PANE);
        fillRow(inv, 18, Material.WHITE_STAINED_GLASS_PANE);

        // 示例：此处应从缓存或数据库获取前五名，这里展示排布
        inv.setItem(11, createItem(Material.PLAYER_HEAD, "§eNo.1 签到大师", "§7累计签到: §f120次"));
        inv.setItem(12, createItem(Material.PLAYER_HEAD, "§7No.2 灵契先锋", "§7累计签到: §f115次"));
        inv.setItem(13, createItem(Material.PLAYER_HEAD, "§6No.3 勤奋主人", "§7累计签到: §f100次"));

        inv.setItem(22, createItem(Material.PAPER, "§b我的排名", "§7累计签到总数: §f" + profile.getTotalCheckIns()));
        inv.setItem(26, createItem(Material.IRON_DOOR, "§c⬅ 返回日历"));

        player.openInventory(inv);
    }

    // --- 辅助方法 ---
    private static int calculateCalendarSlot(int day) {
        int row = (day - 1) / 7; // 第 0-4 行
        int col = (day - 1) % 7; // 第 0-6 列
        // 日期从第二行开始，所以起始偏移是 10 (Row 1, Col 1)
        int slot = 10 + (row * 9) + col;
        return slot < 54 ? slot : -1;
    }

    private static ItemStack createSignInIcon(SpiritProfile profile) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e§l🗓 岁月铭刻").decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("§7§o『每一日的陪伴，都是时光长河中闪耀的砂砾。』").decoration(TextDecoration.ITALIC, false),
                Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false),
                Component.text("§e[✦ 每日签到 ✦]").decoration(TextDecoration.ITALIC, false),
                Component.text("§f连续契约: §a" + profile.getConsecutiveDays() + " §f天").decoration(TextDecoration.ITALIC, false),
                Component.text("§f累计契约: §b" + profile.getTotalCheckIns() + " §f天").decoration(TextDecoration.ITALIC, false),
                Component.text("§f补签卡片: §d" + profile.getReplacementCards() + " §f张").decoration(TextDecoration.ITALIC, false),
                Component.text("§8§m-----------------------").decoration(TextDecoration.ITALIC, false),
                Component.text("§e▶ 点击翻阅契约书").decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 直接读取 data.yml 中的 checkin.total 进行排序
     */
    private static ItemStack createSingInLeaderboardIcon(SpiritProfile viewerProfile) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6§l🏆 签到排行").decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // --- 数据准备 ---
        FileConfiguration data = RabiSystem.getInstance().getSpiritModule().getDataConfig();

        // 定义一个简单的数据类来存我们需要的信息
        class RankData {
            String playerName;
            String spiritName;
            int total;
            int consecutive;

            public RankData(String pName, String sName, int t, int c) {
                this.playerName = (pName == null ? "未知玩家" : pName);
                this.spiritName = sName;
                this.total = t;
                this.consecutive = c;
            }

            // 获取展示用的名字：Player (Spirit)
            public String getDisplayName() {
                return playerName + " §7(" + spiritName + "§7)";
            }
        }

        List<RankData> allData = new ArrayList<>();

        if (data.contains("spirits")) {
            for (String uuidStr : data.getConfigurationSection("spirits").getKeys(false)) {
                try {
                    String path = "spirits." + uuidStr;
                    // 1. 获取玩家 ID (从 UUID 反查)
                    // 注意：离线玩家获取名字可能为null，做个兜底
                    org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuidStr));
                    String pName = op.getName();

                    // 2. 获取精灵昵称
                    String sName = data.getString(path + ".name", "小精灵");

                    // 3. 获取数据
                    int total = data.getInt(path + ".checkin.total", 0);
                    int consecutive = data.getInt(path + ".checkin.consecutive", 0);

                    // 只要有数据就加入列表
                    if (total > 0 || consecutive > 0) {
                        allData.add(new RankData(pName, sName, total, consecutive));
                    }
                } catch (Exception e) {
                    // 忽略无效数据
                }
            }
        }

        // --- 榜单 A：累计签到 Top 5 ---
        lore.add(Component.text("§e§l📊 累计签到 Top 5").decoration(TextDecoration.ITALIC, false));

        // 排序：累计次数从大到小
        allData.sort((a, b) -> b.total - a.total);

        if (allData.isEmpty()) {
            lore.add(Component.text("§7  暂无数据...").decoration(TextDecoration.ITALIC, false));
        } else {
            for (int i = 0; i < Math.min(5, allData.size()); i++) {
                RankData entry = allData.get(i);
                if (entry.total <= 0) break; // 过滤掉 0 次的

                String prefix;
                switch (i) {
                    case 0 -> prefix = "§e🥇 ";
                    case 1 -> prefix = "§7🥈 ";
                    case 2 -> prefix = "§6🥉 ";
                    default -> prefix = "§f" + (i + 1) + ". ";
                }
                lore.add(Component.text(prefix + "§f" + entry.getDisplayName() + " §7- §a" + entry.total + "次")
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        lore.add(Component.text("").decoration(TextDecoration.ITALIC, false));

        // --- 榜单 B：连续签到 Top 3 (坚持榜) ---
        lore.add(Component.text("§c§l🔥 连签坚持榜 Top 3").decoration(TextDecoration.ITALIC, false));

        // 排序：连续天数从大到小
        allData.sort((a, b) -> b.consecutive - a.consecutive);

        boolean hasConsecutive = false;
        for (int i = 0; i < Math.min(3, allData.size()); i++) {
            RankData entry = allData.get(i);
            if (entry.consecutive <= 0) break; // 过滤掉 0 天的
            hasConsecutive = true;

            // 连签榜前面加个火苗图标
            lore.add(Component.text("§c🔥 §f" + entry.getDisplayName() + " §7- §c" + entry.consecutive + "天")
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (!hasConsecutive) {
            lore.add(Component.text("§7  还没人达成连签成就...").decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.text("§7-----------------------").decoration(TextDecoration.ITALIC, false));

        // --- 底部：个人信息 ---
        // 找自己的总排名
        allData.sort((a, b) -> b.total - a.total); // 重新排回总榜顺序找排名
        int myRank = -1;
        // 使用名字匹配 (因为 RankData 里存的是名字)
        String myName = viewerProfile.getName(); // 注意：这里 SpiritProfile 的 getName 是 "玩家的小精灵" 还是 "精灵名"？
        // 等等！SpiritProfile 的 getName() 返回的是精灵的名字！
        // 我们需要用 ownerId 反查玩家名来匹配，或者直接用 UUID 匹配更稳妥。
        // 为了方便，这里我们直接在下面展示 viewerProfile 的实时数据即可，不必强求算出排名数字（因为可能会重名）。

        lore.add(Component.text("§7我的累计: §f" + viewerProfile.getTotalCheckIns() + " 次").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§7我的连签: §f" + viewerProfile.getConsecutiveDays() + " 天").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§e▶ 每日打卡，领取好礼！").decoration(TextDecoration.ITALIC, false));

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // --- 新增：成就过滤器枚举 ---
    public enum FilterType {
        ALL("全部显示", Material.COMPASS, "§7默认排序: 待领取 > 未完成 > 已完成"),
        INCOMPLETE("待办事项", Material.WRITABLE_BOOK, "§7显示待领取和未完成的成就"),
        COMPLETED("已完成", Material.ENCHANTED_BOOK, "§7仅显示已领取奖励的成就");

        final String name;
        final Material icon;
        final String desc;

        FilterType(String name, Material icon, String desc) {
            this.name = name;
            this.icon = icon;
            this.desc = desc;
        }

        public FilterType next() {
            int nextOrd = (this.ordinal() + 1) % values().length;
            return values()[nextOrd];
        }
    }

    /**
     * 打开成就菜单
     *
     * @param page 当前页码 (从 1 开始)
     */
    public static void openAchievementMenu(Player player, SpiritProfile profile, int page, FilterType filterType) {
        // 1. 获取并处理成就列表
        List<Achievement> processedList = getSortedAchievements(profile, filterType);

        // 2. 分页计算
        int pageSize = 36;
        int totalAchs = processedList.size();
        int totalPages = (int) Math.ceil((double) totalAchs / pageSize);
        if (totalPages == 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Inventory inv = Bukkit.createInventory(
                new SpiritHolder(profile.getOwnerId(), "ACHIEVEMENT", page, filterType),
                54,
                Component.text(ACHIEVEMENT_TITLE + " - " + filterType.name + " (" + page + "/" + totalPages + ")")
        );

        // --- Row 0: 导航区 ---

        // Slot 0: 上一页
        if (page > 1) {
            inv.setItem(0, createItem(Material.ARROW, "§f◀ 上一页", "§7前往第 " + (page - 1) + " 页"));
        } else {
            inv.setItem(0, createItem(Material.ARROW, "§7已经是第一页了"));
        }

        // Slot 3: 统计信息 (这里修复了变量名错误！)
        int unlockedTotal = profile.getUnlockedAchievements().size();
        int allTotal = Achievement.values().length;
        // [修复] 将 unlockedCount 改为 unlockedTotal
        int progressPercent = (int) ((double) unlockedTotal / allTotal * 100);

        inv.setItem(3, createItem(Material.OAK_SIGN, "§e📊 数据统计",
                "§7总进度: §f" + progressPercent + "%",
                "§7已解锁: §a" + unlockedTotal + " §7/ §f" + allTotal));

        // Slot 4: 返回主菜单
        inv.setItem(4, createItem(Material.IRON_DOOR, "§c⬅ 返回主界面", "§7回到灵契链接"));

        // Slot 5: 分类过滤器
        inv.setItem(5, createFilterIcon(filterType));

        // Slot 8: 下一页
        if (page < totalPages) {
            inv.setItem(8, createItem(Material.ARROW, "§f下一页 ▶", "§7前往第 " + (page + 1) + " 页"));
        } else {
            inv.setItem(8, createItem(Material.ARROW, "§7已经是最后一页了"));
        }

        // 填充背景
        ItemStack whiteGlass = createSpacer(Material.WHITE_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, whiteGlass);
        }
        ItemStack blackGlass = createSpacer(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 9; i < 18; i++) inv.setItem(i, blackGlass);

        // --- Row 2+: 成就内容渲染 ---
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalAchs);

        for (int i = startIndex; i < endIndex; i++) {
            Achievement ach = processedList.get(i);
            int slot = 18 + (i - startIndex);
            inv.setItem(slot, createAchievementIcon(ach, profile));
        }

        player.openInventory(inv);
    }

    // 为了兼容旧代码，保留旧签名的重载方法 (默认为 ALL)
    public static void openAchievementMenu(Player player, SpiritProfile profile, int page) {
        openAchievementMenu(player, profile, page, FilterType.ALL);
    }

    /**
     * 核心逻辑：获取排序和过滤后的成就列表
     */
    public static List<Achievement> getSortedAchievements(SpiritProfile profile, FilterType filterType) {
        List<Achievement> all = Arrays.asList(Achievement.values());
        List<Achievement> filtered = new ArrayList<>();

        for (Achievement ach : all) {
            boolean unlocked = profile.getUnlockedAchievements().contains(ach.getId());
            boolean claimed = profile.isClaimed(ach.getId());

            switch (filterType) {
                case INCOMPLETE:
                    if (!claimed) filtered.add(ach); // 待领取 或 未解锁
                    break;
                case COMPLETED:
                    if (claimed) filtered.add(ach); // 已领取
                    break;
                case ALL:
                default:
                    filtered.add(ach);
                    break;
            }
        }

        // 排序: 待领取(0) > 未解锁(1) > 已领取(2)
        filtered.sort((a1, a2) -> {
            int score1 = getAchievementScore(profile, a1);
            int score2 = getAchievementScore(profile, a2);
            return Integer.compare(score1, score2);
        });

        return filtered;
    }

    private static int getAchievementScore(SpiritProfile profile, Achievement ach) {
        boolean unlocked = profile.getUnlockedAchievements().contains(ach.getId());
        boolean claimed = profile.isClaimed(ach.getId());

        if (unlocked && !claimed) return 0; // 待领取：最前
        if (!unlocked) return 1;            // 未解锁：中间
        return 2;                           // 已领取：最后
    }

    private static ItemStack createFilterIcon(FilterType current) {
        ItemStack item = new ItemStack(current.icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§b🔃 分类: " + current.name).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(current.desc).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§7点击切换至: §f" + current.next().name).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 辅助方法：创建成就图标
     */
    /**
     * 辅助方法：创建成就图标
     * (请替换原有的 createAchievementIcon 方法)
     */
    private static ItemStack createAchievementIcon(Achievement ach, SpiritProfile profile) {
        boolean unlocked = profile.getUnlockedAchievements().contains(ach.getId());
        boolean claimed = profile.isClaimed(ach.getId());

        // 1. 决定材质
        Material displayMat;
        if (!unlocked) {
            displayMat = Material.GRAY_DYE; // 未解锁
        } else if (!claimed) {
            displayMat = Material.CHEST_MINECART; // 待领取
        } else {
            displayMat = ach.getIcon().getType(); // 已领取
        }

        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();

        // 2. 决定标题和状态文本
        String titlePrefix;
        String statusText;

        if (!unlocked) {
            titlePrefix = "§7[🔒] ";
            statusText = "§7未达成";
        } else if (!claimed) {
            titlePrefix = "§a§l[🎁] ";
            statusText = "§e▶ 点击领取奖励";
            meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
        } else {
            titlePrefix = "§e[✔] ";
            statusText = "§a已领取";
        }

        meta.displayName(Component.text(titlePrefix + ach.getName()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        // 3. 构建 Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7目标: " + ach.getDescription()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(Component.text(""));

        // 显示奖励内容
        lore.add(Component.text("§f🎁 奖励:").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        if (ach.getItemReward() != null) {
            // --- 修改处：调用 getMaterialName 进行汉化 ---
            String zhName = getMaterialName(ach.getItemReward().getType());
            lore.add(Component.text(" §7- 物品: §d" + zhName + " x" + ach.getItemReward().getAmount()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        if (ach.getExpReward() > 0) {
            lore.add(Component.text(" §7- 经验: §b" + ach.getExpReward()).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }

        lore.add(Component.text(""));
        lore.add(Component.text(statusText).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    /**
     * 成就奖励物品汉化表
     * (请将此方法添加在 SpiritMenus 类中)
     */
    private static String getMaterialName(Material m) {
        return switch (m) {
            // 基础资源
            case SWEET_BERRIES -> "甜浆果";
            case IRON_INGOT -> "铁锭";
            case GOLD_INGOT -> "金锭";
            case DIAMOND -> "钻石";
            case NETHERITE_SCRAP -> "下界合金碎片";
            case NETHER_STAR -> "下界之星";
            case EMERALD -> "绿宝石";
            case REDSTONE -> "红石粉";
            case GLOWSTONE_DUST -> "萤石粉";

            // 工具与装备
            case NAME_TAG -> "命名牌";
            case CLOCK -> "时钟";
            case EXPERIENCE_BOTTLE -> "附魔之瓶";
            case FIREWORK_ROCKET -> "烟花火箭";
            case DIAMOND_SWORD -> "钻石剑";
            case BOW -> "弓";
            case TOTEM_OF_UNDYING -> "不死图腾";

            // 食物
            case CAKE -> "蛋糕";
            case BREAD -> "面包";
            case GOLDEN_APPLE -> "金苹果";
            case ENCHANTED_GOLDEN_APPLE -> "附魔金苹果";
            case MILK_BUCKET -> "牛奶桶";
            case COOKIE -> "曲奇";

            // 功能方块与杂项
            case WHITE_WOOL -> "白色羊毛";
            case POTION -> "药水"; // 原版 POTION 默认为水瓶，但成就叫并肩作战，叫药水较好
            case DRAGON_BREATH -> "龙息";
            case JUKEBOX -> "唱片机";
            case HOPPER -> "漏斗";
            case ITEM_FRAME -> "物品展示框";
            case CHEST -> "箱子";
            case DIAMOND_BLOCK -> "钻石块";
            case FIREWORK_STAR -> "烟火之星";
            case SOUL_LANTERN -> "灵魂灯笼";

            // 如果有漏掉的，默认返回英文名
            default -> m.name();
        };
    }

    /**
     * 打开幻化菜单 (自动生成版)
     * 这里会调用 createAutoSkinIcon
     */
    public static void openSkinsMenu(Player p, SpiritProfile profile) {
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "SKINS"), 27, Component.text("§b§l🦋 幻形之镜"));

        // 1. 填充背景
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, createSpacer(Material.BLACK_STAINED_GLASS_PANE));
        }
        inv.setItem(26, createItem(Material.IRON_DOOR, "§c返回主界面"));

        // 2. 自动生成图标 (这里就是调用的地方！)
        SpiritSkin[] skins = SpiritSkin.values();

        for (int i = 0; i < skins.length; i++) {
            // 防止越界
            if (i >= SKIN_SLOTS.length) break;

            int slot = SKIN_SLOTS[i];
            SpiritSkin skin = skins[i];

            // ---> 调用 createAutoSkinIcon <---
            inv.setItem(slot, createAutoSkinIcon(profile, skin));
        }

        p.openInventory(inv);
    }

    private static ItemStack createSkinIcon(SpiritProfile profile, String skinId, Material mat, String name, String desc) {
        boolean unlocked = skinId.equals("DEFAULT") || profile.getUnlockedSkins().contains(skinId);
        boolean current = profile.getCurrentSkin().equals(skinId);

        if (!unlocked) return createItem(Material.GRAY_DYE, "§7🔒 " + name.replaceAll("§.", ""), "§7(通过繁育成就解锁)");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((current ? "§a§l" : "§e") + name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + desc));
        lore.add(Component.text(""));
        lore.add(Component.text(current ? "§b✨ 当前正在使用" : "§e▶ 点击幻化"));
        if (current) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // 简单的名字映射
    private static String getSkinDisplayName(String id) {
        return switch (id) {
            case "FOX_RED" -> "青丘红狐";
            case "FOX_SNOW" -> "雪域灵仙";
            case "PARROT" -> "彩羽信使";
            case "AXOLOTL" -> "水域精灵";
            default -> "本我 · 小小悦灵";
        };
    }

    /**
     * 自动生成皮肤图标，并写入 NBT 数据
     */
    private static ItemStack createAutoSkinIcon(SpiritProfile profile, SpiritSkin skin) {

        boolean isDefault = skin == SpiritSkin.DEFAULT;
        boolean unlocked = profile.isSkinUnlocked(skin.getId());
        boolean current = profile.getCurrentSkin().equals(skin.getId());

        // 未解锁状态
        if (!unlocked) {
            String source = (skin.getRequiredAchievement() != null) ?
                    "§7(通过成就: " + skin.getRequiredAchievement().getName() + " 解锁)" :
                    "§7(未知解锁途径)";

            // 为了让 MenuListener 也能识别这是哪个皮肤（即使未解锁，点击时也可以提示信息）
            // 我们依然可以把 ID 写进去，或者仅仅显示一把锁
            return createItem(Material.GRAY_DYE,
                    "§7🔒 " + skin.getDisplayName().replaceAll("§.", ""),
                    source);
        }

        // 已解锁状态
        ItemStack item = new ItemStack(skin.getIcon());
        ItemMeta meta = item.getItemMeta();

        // 1. 设置标题
        meta.displayName(Component.text((current ? "§a§l" : "§e") + skin.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        // 2. 设置 Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + skin.getDescription()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text(current ? "§b✨ 当前正在使用" : "§e▶ 点击幻化").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // 3. 当前使用高亮
        if (current) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // 4. 【核心】写入皮肤 ID 到 PDC (PersistentDataContainer)
        // 这样监听器就不需要判断 Slot，而是直接读这个 ID
        NamespacedKey key = new NamespacedKey(RabiSystem.getInstance(), "skin_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skin.getId());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 给物品打上“我是按钮”的标签，防止被存入背包
     */
    private ItemStack markAsButton(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        // 这里的 plugin 实例获取方式可能需要根据你的代码调整，或者直接传参
        // 如果没有静态 instance，可以用 cn.rabitown.LanternSpiritCovenant.getInstance()
        org.bukkit.NamespacedKey key = new NamespacedKey(RabiSystem.getInstance(), "lsc_gui_button");

        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 创建星界祈愿图标
     */
    private static ItemStack createLotteryIcon(SpiritProfile profile) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§d§l✨ 星界祈愿").decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7引导星辰之力，抽取神秘奖励。").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("§f当前祈愿次数: §e" + profile.getLotteryChances() + " §f次").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));

        if (profile.getLotteryChances() > 0) {
            lore.add(Component.text("§e▶ 点击开启灵性星阵").decoration(TextDecoration.ITALIC, false));
            // 增加附魔流光效果
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(Component.text("§c[!] 你目前没有祈愿次数。").decoration(TextDecoration.ITALIC, false));
//            lore.add(Component.text("§7可以通过等级提升或节日活动获得。").decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // 重载方法 (默认打开成长页面)
    public static void openLevelTreeMenu(Player player, SpiritProfile profile) {
        openLevelTreeMenu(player, profile, 0, 0);
    }

    /**
     * 打开技能树界面 (支持独立分页)
     * @param starPage 星光系页码 (从0开始)
     * @param shadowPage 暗影系页码 (从0开始)
     */
    public static void openLevelTreeMenu(Player player, SpiritProfile profile, int starPage, int shadowPage) {
        // 使用 SpiritHolder 存储两个页码状态
        SpiritHolder holder = new SpiritHolder(profile.getOwnerId(), "LEVEL_TREE");
        holder.setStarPage(starPage);
        holder.setShadowPage(shadowPage);

        Inventory inv = Bukkit.createInventory(holder, 54, Component.text(LEVEL_TREE_TITLE));

        // ==========================================
        // Row 1: 顶部功能栏 (0-8)
        // ==========================================
        // 0-3: 白色玻璃板
        for (int i = 0; i <= 3; i++) inv.setItem(i, createSpacer(Material.WHITE_STAINED_GLASS_PANE));

        // 4: 返回主界面
        inv.setItem(4, createItem(Material.IRON_DOOR, "§c⬅ 返回主界面"));

        // 5-6: 白色玻璃板
        inv.setItem(5, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(6, createSpacer(Material.WHITE_STAINED_GLASS_PANE));

        // 7: 技能点数
        int starSpent = profile.getSpentPointsByTree("STAR");
        int shadowSpent = profile.getSpentPointsByTree("SHADOW");
        int totalSpent = profile.getSpentSkillPoints();
        int available = profile.getAvailableSkillPoints();
        inv.setItem(7, createItem(Material.EXPERIENCE_BOTTLE, "§e技能点数",
                "§7已用点数: §e" + starSpent + "§7/§5" + shadowSpent + "§7(§f" + totalSpent + "§7)",
                "§7剩余点数: §a" + available,
                "",
                "§7通过提升羁绊阶段与抽奖可获得技能点数"));

        // 8: 重置技能树
        inv.setItem(8, createItem(Material.TNT, "§c§l重置技能树",
                "§7遗忘所有技能并返还点数",
                "§c警告: 操作不可逆！",
                "", "§e▶ 点击重置"));

        // ==========================================
        // Row 2: 星光系表头 (9-17)
        // ==========================================
        // 9: 星光图标
        ItemStack starIcon = createItem(Material.NETHER_STAR, "§e✨ 星光系 (生活/辅助)",
                "§7包含治疗、光照、被动恢复等技能",
                "§7下面显示该系别的技能列表");
        starIcon = addGlow(starIcon);
        inv.setItem(9, starIcon);

        // 10-17: 黑色玻璃板
        for (int i = 10; i <= 17; i++) inv.setItem(i, createSpacer(Material.BLACK_STAINED_GLASS_PANE));

        // ==========================================
        // Row 3: 星光技能展示区 (18-26)
        // ==========================================
        renderSkillRow(inv, profile, "STAR", starPage, 18);

        // ==========================================
        // Row 4: 暗影技能展示区 (27-35)
        // ==========================================
        renderSkillRow(inv, profile, "SHADOW", shadowPage, 27);

        // ==========================================
        // Row 5: 暗影系表头 (36-44)
        // ==========================================
        // 36: 暗影图标 (倒数第二行第一格)
        ItemStack shadowIcon = createItem(Material.NETHERITE_SWORD, "§5⚔ 暗影系 (战斗/增幅)",
                "§7包含伤害、护盾、战斗被动等技能",
                "§7上面显示该系别的技能列表");
        shadowIcon = addGlow(shadowIcon);
        inv.setItem(36, shadowIcon);

        // 37-44: 黑色玻璃板
        for (int i = 37; i <= 44; i++) inv.setItem(i, createSpacer(Material.BLACK_STAINED_GLASS_PANE));

        // ==========================================
        // Row 6: 阶段成长栏 (45-53)
        // ==========================================
        // 45-46: 白色玻璃板
        inv.setItem(45, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(46, createSpacer(Material.WHITE_STAINED_GLASS_PANE));

        // 47-51: 阶段图标 (初识 -> 双星)
        int currentLv = profile.getLevel();
        // 阶段 1: 初识 (Lv.1)
        inv.setItem(47, createStageIcon(1, currentLv, "初识", Material.LIME_DYE,
                "§7解锁条件: §fLv.1",
                "",
                "§f[解锁能力]",
                "§7- 召唤/改名/基础交互",
                "§7- 生命反哺 I：消耗 2 HP 恢复主人 1 HP",
                "§7- 基础背包 (9格)",
                "§7- 零食袋 (1格)"));

        // 阶段 2: 羁绊 (Lv.10)
        inv.setItem(48, createStageIcon(10, currentLv, "羁绊", Material.CYAN_DYE,
                "§7解锁条件: §fLv.10",
                "",
                "§f[解锁能力]",
                "§7- 生命反哺II：消耗 2 HP 恢复主人 2 HP。",
                "§7- 自然眷顾I：自然回血速度 60s -> 50s",
                "§7- 背包扩容 (18格)",
                "§7- 小精灵主手格解锁",
                "§7- 食物包解锁、开放自动饮食功能"));

        // 阶段 3: 共鸣 (Lv.30)
        inv.setItem(49, createStageIcon(30, currentLv, "共鸣", Material.LAPIS_LAZULI,
                "§7解锁条件: §fLv.30",
                "",
                "§f[解锁能力]",
                "§7- 自定义特效功能解锁",
                "§7- 虚空引力 (自动拾取)",
                "§7- 灵力共鸣I：主人对非玩家生物造成伤害时，给予主人「力量 I」(10s)。CD 80s",
                "§7- 自然眷顾II：血量上限提升至15"));

        // 阶段 4: 灵契 (Lv.50)
        inv.setItem(50, createStageIcon(50, currentLv, "灵契", Material.PURPLE_DYE,
                "§7解锁条件: §fLv.50",
                "",
                "§f[解锁能力]",
                "§7- 灵力迸发 (主动伤害技能)",
                "§7- 共鸣强化 (力量II BUFF)",
                "§7- 背包扩容 (27格)",
                "§7- 血量上限提升 (20点)"));

        // 阶段 5: 双星 (Lv.80 / Lv.100)
        inv.setItem(51, createStageIcon(80, currentLv, "双星", Material.NETHER_STAR,
                "§7解锁条件: §fLv.80 / Lv.100",
                "",
                "§f[解锁能力]",
                "§7- 灵魂代偿 (Lv.80 免死)",
                "§7- 血量上限提升 (25点)",
                "§7- 灵力迸发II (伤害提升)",
                "§7- 终极背包 (54格, Lv.100)"));

        // 52-53: 白色玻璃板
        inv.setItem(52, createSpacer(Material.WHITE_STAINED_GLASS_PANE));
        inv.setItem(53, createSpacer(Material.WHITE_STAINED_GLASS_PANE));

        player.openInventory(inv);
    }

    /**
     * 渲染单行技能列表
     * @param startSlot 该行的起始槽位索引
     */
    private static void renderSkillRow(Inventory inv, SpiritProfile profile, String treeType, int page, int startSlot) {
        // 1. 获取该系别的所有技能
        List<SkillType> allSkills = Arrays.stream(SkillType.values())
                .filter(s -> s.getTreeType().equals(treeType))
                .collect(Collectors.toList());

        int pageSize = 8; // 每行显示8个技能，最后一个是翻页键
        int totalSkills = allSkills.size();
        int maxPages = (int) Math.ceil((double) totalSkills / pageSize);
        if (maxPages == 0) maxPages = 1;

        // 循环页码逻辑
        if (page >= maxPages) page = 0;

        // 2. 填充技能 (前8格)
        int startIndex = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int currentSlot = startSlot + i;
            int skillIndex = startIndex + i;

            if (skillIndex < totalSkills) {
                inv.setItem(currentSlot, createSkillIcon(profile, allSkills.get(skillIndex)));
            } else {
                // 空位显示空气或玻璃? 为了美观不放东西，或者放浅灰玻璃板占位
                // 按照ASCII图，空位是空白
            }
        }

        // 3. 放置“查看更多”按钮 (该行最后一格: startSlot + 8)
        int btnSlot = startSlot + 8;
        ItemStack viewMore = createItem(Material.OAK_SIGN, "§b查看更多 (" + (page + 1) + "/" + maxPages + ")",
                "§7点击切换" + ("STAR".equals(treeType) ? "星光" : "暗影") + "系技能列表",
                "§e▶ 下一页");
        inv.setItem(btnSlot, viewMore);
    }

    private static ItemStack createSkillIcon(SpiritProfile profile, SkillType skill) {
        boolean unlocked = profile.isSkillUnlocked(skill.getId());
        boolean preUnlocked = (skill.getPrerequisite() == null || profile.isSkillUnlocked(skill.getPrerequisite()));
        boolean canUnlock = profile.getAvailableSkillPoints() > 0;

        ItemStack item = new ItemStack(unlocked ? skill.getIcon() : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        String status;
        if (unlocked) {
            status = "§a[已学习]";
        } else if (!preUnlocked) {
            // 获取前置技能名称
            SkillType pre = SkillType.fromId(skill.getPrerequisite());
            String preName = (pre != null) ? pre.getName() : "???";
            status = "§c[锁定] 需前置: " + preName;
        } else {
            status = canUnlock ? "§e[可学习]" : "§c[点数不足]";
        }
        String action;
        if (unlocked) {
            if (skill.getType() == SkillType.Type.PASSIVE) action = "§7(被动生效中)";
            else action = "§b▶ 点击装备/卸下";
        } else {
            action = canUnlock ? "§e▶ 点击消耗 1 点数学习" : "§7无法学习";
        }

        meta.displayName(Component.text((unlocked ? "§a" : "§7") + skill.getName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7类型: " + skill.getType().name()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        for(String line : skill.getDescription()) {
            lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(status).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(action).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        // 高亮已装备
        if (unlocked) {
            if (skill.getId().equals(profile.getActiveSkillId()) || profile.getQuickSkillIds().contains(skill.getId())) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                List<Component> newLore = meta.lore();
                newLore.add(Component.text("§6✨ 正在使用中").decoration(TextDecoration.ITALIC, false));
                meta.lore(newLore);
            }
        }

        // 写入 SkillID
        NamespacedKey key = new NamespacedKey(RabiSystem.getInstance(), "skill_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skill.getId());

        item.setItemMeta(meta);
        return item;
    }

    // --- 辅助方法：创建阶段图标 ---
    private static ItemStack createStageIcon(int reqLv, int currentLv, String name, Material mat, String... lore) {
        boolean unlocked = currentLv >= reqLv;
        // 未解锁显示灰色染料，解锁显示对应材质
        ItemStack item = new ItemStack(unlocked ? mat : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        String prefix = unlocked ? "§a§l" : "§7";
        String suffix = unlocked ? " [已达成]" : " [未解锁]";
        meta.displayName(Component.text(prefix + name + "阶段" + suffix).decoration(TextDecoration.ITALIC, false));

        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }

        if (unlocked) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.lore(compLore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 打开便携工坊菜单 (1行)
     * level 1: 基础4个
     * level 2: 进阶8个
     */
    public static void openQuickToolsMenu(Player player, SpiritProfile profile, int toolLevel) {
        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "TOOLS"), 9, Component.text(TOOLS_TITLE));

        // Level 1: 工作台(0), 熔炉(1), 织布机(2), 制图台(3)
        inv.setItem(0, createItem(Material.CRAFTING_TABLE, "§e工作台", "§7点击打开"));
        inv.setItem(1, createItem(Material.FURNACE, "§e熔炉", "§7点击打开虚拟熔炉"));
        inv.setItem(2, createItem(Material.LOOM, "§e织布机", "§7点击打开"));
        inv.setItem(3, createItem(Material.CARTOGRAPHY_TABLE, "§e制图台", "§7点击打开"));

        // Level 2: 烟熏炉(4), 高炉(5), 锻造台(6), 切石机(7)
        if (toolLevel >= 2) {
            inv.setItem(4, createItem(Material.SMOKER, "§e烟熏炉", "§7点击打开虚拟烟熏炉", "§7(烧煮食物速度 x2)"));
            inv.setItem(5, createItem(Material.BLAST_FURNACE, "§e高炉", "§7点击打开虚拟高炉", "§7(烧炼矿物速度 x2)"));
            inv.setItem(6, createItem(Material.SMITHING_TABLE, "§e锻造台", "§7点击打开"));
            inv.setItem(7, createItem(Material.STONECUTTER, "§e切石机", "§7点击打开"));
        } else {
            // 锁住的槽位
            for (int i = 4; i <= 7; i++) {
                inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, "§7[未解锁]", "§7需要装备【便携工坊 II】"));
            }
        }

        // 返回键 (Slot 8)
        inv.setItem(8, createItem(Material.IRON_DOOR, "§c关闭面板"));

        player.openInventory(inv);
    }

    /**
     * 打开虚拟熔炉界面
     * type: FURNACE, SMOKER, BLAST_FURNACE
     */
    public static void openVirtualFurnace(Player player, SpiritProfile profile, String furnaceType) {
        // 创建一个标准的熔炉界面 (InventoryType.FURNACE)
        // 注意：Bukkit API 中 createInventory 不支持直接创建带烧炼逻辑的容器，
        // 我们只能创建一个外观是熔炉的容器，逻辑由 Task 模拟。
        org.bukkit.event.inventory.InventoryType type = org.bukkit.event.inventory.InventoryType.FURNACE;
        if ("SMOKER".equals(furnaceType)) type = org.bukkit.event.inventory.InventoryType.SMOKER;
        if ("BLAST_FURNACE".equals(furnaceType)) type = org.bukkit.event.inventory.InventoryType.BLAST_FURNACE;

        Inventory inv = Bukkit.createInventory(new SpiritHolder(profile.getOwnerId(), "VF_" + furnaceType), type, Component.text("§0虚拟 " + getFurnaceName(furnaceType)));

        // 加载数据
        ItemStack[] savedItems;
        switch (furnaceType) {
            case "SMOKER": savedItems = profile.getVirtualSmokerItems(); break;
            case "BLAST_FURNACE": savedItems = profile.getVirtualBlastItems(); break;
            default: savedItems = profile.getVirtualFurnaceItems(); break;
        }

        if (savedItems != null) {
            if (savedItems[0] != null) inv.setItem(0, savedItems[0]); // Input
            if (savedItems[1] != null) inv.setItem(1, savedItems[1]); // Fuel
            if (savedItems[2] != null) inv.setItem(2, savedItems[2]); // Output
        }

        player.openInventory(inv);
    }

    private static String getFurnaceName(String type) {
        return switch (type) {
            case "SMOKER" -> "烟熏炉";
            case "BLAST_FURNACE" -> "高炉";
            default -> "熔炉";
        };
    }

    /**
     * [新增] 启动菜单自动刷新任务
     * 在插件 onEnable 时调用一次即可
     */
    public static void startMenuUpdater(SpiritModule module) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    // 检查玩家当前打开的界面
                    Inventory topInv = p.getOpenInventory().getTopInventory();
                    if (topInv.getHolder() instanceof SpiritHolder holder) {
                        // 只刷新主菜单 (MAIN)
                        if ("MAIN".equals(holder.getType())) {
                            SpiritProfile profile = module.getSpiritManager().getProfile(holder.getOwner());

                            // 刷新 Slot 36 (灵核)
                            // 这里调用刚刚改为 public 的 createCoreIcon
                            ItemStack newCore = createCoreIcon(p, profile);
                            topInv.setItem(36, newCore);
                        }
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 20L, 20L); // 每秒刷新一次 (20 ticks)
    }
}