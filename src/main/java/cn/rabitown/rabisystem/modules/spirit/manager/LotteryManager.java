package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LotteryManager {

    public enum Rarity {
        WHITE("§f凡物", 70),
        AQUA("§b灵光", 20),
        PURPLE("§d星界", 9),
        GOLD("§6§l传世", 1);

        final String prefix;
        final int weight;
        Rarity(String prefix, int weight) { this.prefix = prefix; this.weight = weight; }
        public String getPrefix() { return prefix; }
    }

    // 抽象奖励类
    public static abstract class Reward {
        protected Rarity rarity;
        protected String name;

        public Reward(Rarity rarity, String name) {
            this.rarity = rarity;
            this.name = name;
        }

        public abstract ItemStack getDisplayItem();
        public abstract void grant(Player p, SpiritProfile profile);
        public Rarity getRarity() { return rarity; }
        public String getName() { return name; }

        // 辅助：保存数据
        protected void save(SpiritProfile profile) {
            SpiritUtils.getConfigManager().saveProfile(profile);
        }
    }

    // 1. 普通物品奖励
    public static class ItemReward extends Reward {
        private final ItemStack itemStack;

        public ItemReward(Material mat, int amount, Rarity rarity, String name) {
            super(rarity, name);
            this.itemStack = new ItemStack(mat, amount);
        }

        @Override
        public ItemStack getDisplayItem() { return itemStack.clone(); }

        @Override
        public void grant(Player p, SpiritProfile profile) {
            p.getInventory().addItem(itemStack.clone());
            // 物品虽然存如果不涉及Profile数据变更，其实可以不存Profile，
            // 但为了保险（比如以后统计获得物品数），建议统一保存。
            save(profile);
        }
    }

    // 2. 补签卡奖励
    public static class CardReward extends Reward {
        private final int amount;

        public CardReward(int amount, Rarity rarity) {
            super(rarity, "时光补签卡 x" + amount);
            this.amount = amount;
        }

        @Override
        public ItemStack getDisplayItem() { return new ItemStack(Material.PAPER); }

        @Override
        public void grant(Player p, SpiritProfile profile) {
            profile.addReplacementCards(amount);
            p.sendMessage("§a[!] 补签卡已存入灵契记录。");
            // [关键修复] 立即保存！
            save(profile);
        }
    }

    // 3. 特效奖励
    public static class EffectReward extends Reward {
        private final SpiritEffectType effectType;

        public EffectReward(SpiritEffectType type, Rarity rarity) {
            super(rarity, "特效: " + type.getName());
            this.effectType = type;
        }

        @Override
        public ItemStack getDisplayItem() { return new ItemStack(effectType.getIcon()); }

        @Override
        public void grant(Player p, SpiritProfile profile) {
            if (profile.isEffectUnlocked(effectType)) {
                p.sendMessage("§7你已经拥有此传世特效，自动转化为 1000 经验值。");
                profile.addExp(1000);
            } else {
                profile.addUnlockedEffect(effectType.getId());
                p.sendMessage("§6§l✨ 欧气爆发！解锁了传世特效！快去外观菜单看看吧！");
            }
            // [关键修复] 立即保存！否则重启就没了！
            save(profile);
        }
    }

    private static final List<Reward> LOOT_TABLE = new ArrayList<>();

    static {
        // ==========================================
        //  WHITE - 凡物 (70%) - 基础物资 & 建筑材料
        // ==========================================
        LOOT_TABLE.add(new ItemReward(Material.EXPERIENCE_BOTTLE, 32, Rarity.WHITE, "附魔之瓶"));
        LOOT_TABLE.add(new ItemReward(Material.COOKED_BEEF, 64, Rarity.WHITE, "香煎牛排"));
        LOOT_TABLE.add(new ItemReward(Material.GOLDEN_CARROT, 32, Rarity.WHITE, "金胡萝卜"));
        LOOT_TABLE.add(new ItemReward(Material.IRON_INGOT, 32, Rarity.WHITE, "铁锭"));
        LOOT_TABLE.add(new ItemReward(Material.GOLD_INGOT, 32, Rarity.WHITE, "金锭"));
        LOOT_TABLE.add(new ItemReward(Material.COPPER_BLOCK, 16, Rarity.WHITE, "铜块"));
        LOOT_TABLE.add(new ItemReward(Material.COAL_BLOCK, 8, Rarity.WHITE, "煤炭块"));
        LOOT_TABLE.add(new ItemReward(Material.LAPIS_BLOCK, 8, Rarity.WHITE, "青金石块"));
        LOOT_TABLE.add(new ItemReward(Material.REDSTONE_BLOCK, 16, Rarity.WHITE, "红石块"));
        LOOT_TABLE.add(new ItemReward(Material.OAK_LOG, 64, Rarity.WHITE, "橡木原木"));
        LOOT_TABLE.add(new ItemReward(Material.SPRUCE_LOG, 64, Rarity.WHITE, "云杉原木"));
        LOOT_TABLE.add(new ItemReward(Material.CHERRY_LOG, 64, Rarity.WHITE, "樱花原木")); // 1.20+
        LOOT_TABLE.add(new ItemReward(Material.QUARTZ_BLOCK, 32, Rarity.WHITE, "石英块"));
        LOOT_TABLE.add(new ItemReward(Material.SEA_LANTERN, 16, Rarity.WHITE, "海晶灯"));
        LOOT_TABLE.add(new ItemReward(Material.OCHRE_FROGLIGHT, 8, Rarity.WHITE, "黄蛙明灯"));
        LOOT_TABLE.add(new ItemReward(Material.SLIME_BALL, 16, Rarity.WHITE, "粘液球"));
        LOOT_TABLE.add(new ItemReward(Material.LEATHER, 32, Rarity.WHITE, "皮革"));
        LOOT_TABLE.add(new ItemReward(Material.BOOK, 16, Rarity.WHITE, "书"));
        LOOT_TABLE.add(new ItemReward(Material.FIREWORK_ROCKET, 64, Rarity.WHITE, "烟花火箭"));
        LOOT_TABLE.add(new ItemReward(Material.TUFF_BRICKS, 64, Rarity.WHITE, "凝灰岩砖")); // 1.21

        // ==========================================
        //  AQUA - 灵光 (20%) - 稀有资源 & 功能方块
        // ==========================================
        LOOT_TABLE.add(new CardReward(1, Rarity.AQUA)); // 补签卡
        LOOT_TABLE.add(new ItemReward(Material.DIAMOND, 8, Rarity.AQUA, "钻石"));
        LOOT_TABLE.add(new ItemReward(Material.EMERALD, 32, Rarity.AQUA, "绿宝石"));
        LOOT_TABLE.add(new ItemReward(Material.ANCIENT_DEBRIS, 2, Rarity.AQUA, "远古残骸"));
        LOOT_TABLE.add(new ItemReward(Material.GOLDEN_APPLE, 8, Rarity.AQUA, "金苹果"));
        LOOT_TABLE.add(new ItemReward(Material.ENDER_PEARL, 16, Rarity.AQUA, "末影珍珠"));
        LOOT_TABLE.add(new ItemReward(Material.BLAZE_ROD, 16, Rarity.AQUA, "烈焰棒"));
        LOOT_TABLE.add(new ItemReward(Material.GHAST_TEAR, 8, Rarity.AQUA, "恶魂之泪"));
        LOOT_TABLE.add(new ItemReward(Material.PHANTOM_MEMBRANE, 8, Rarity.AQUA, "幻翼膜"));
        LOOT_TABLE.add(new ItemReward(Material.HEART_OF_THE_SEA, 1, Rarity.AQUA, "海洋之心"));
        LOOT_TABLE.add(new ItemReward(Material.NAUTILUS_SHELL, 8, Rarity.AQUA, "鹦鹉螺壳"));
        LOOT_TABLE.add(new ItemReward(Material.NAME_TAG, 2, Rarity.AQUA, "命名牌"));
        LOOT_TABLE.add(new ItemReward(Material.SADDLE, 1, Rarity.AQUA, "鞍"));
        LOOT_TABLE.add(new ItemReward(Material.HONEY_BLOCK, 8, Rarity.AQUA, "蜂蜜块"));
        LOOT_TABLE.add(new ItemReward(Material.CRYING_OBSIDIAN, 12, Rarity.AQUA, "哭泣黑曜石"));
        LOOT_TABLE.add(new ItemReward(Material.AMETHYST_CLUSTER, 8, Rarity.AQUA, "紫水晶簇"));
        LOOT_TABLE.add(new ItemReward(Material.ECHO_SHARD, 4, Rarity.AQUA, "回响碎片"));
        LOOT_TABLE.add(new ItemReward(Material.DISC_FRAGMENT_5, 3, Rarity.AQUA, "唱片残片(5)"));
        LOOT_TABLE.add(new ItemReward(Material.SHULKER_SHELL, 2, Rarity.AQUA, "潜影壳"));
        LOOT_TABLE.add(new ItemReward(Material.TRIAL_KEY, 2, Rarity.AQUA, "试炼钥匙")); // 1.21
        LOOT_TABLE.add(new ItemReward(Material.CRAFTER, 2, Rarity.AQUA, "合成器")); // 1.21

        // ==========================================
        //  PURPLE - 星界 (9%) - 毕业装备 & 珍贵宝物
        // ==========================================
        LOOT_TABLE.add(new CardReward(3, Rarity.PURPLE)); // 补签卡 x3
        // --- 核心高价值物品 ---
        LOOT_TABLE.add(new ItemReward(Material.NETHERITE_BLOCK, 1, Rarity.PURPLE, "§d下界合金块"));
        LOOT_TABLE.add(new ItemReward(Material.NETHER_STAR, 1, Rarity.PURPLE, "§d下界之星"));
        LOOT_TABLE.add(new ItemReward(Material.CONDUIT, 1, Rarity.PURPLE, "§d潮涌核心"));
        LOOT_TABLE.add(new ItemReward(Material.ELYTRA, 1, Rarity.PURPLE, "§d鞘翅"));
        // --- 其他稀有物品 ---
        LOOT_TABLE.add(new ItemReward(Material.NETHERITE_INGOT, 2, Rarity.PURPLE, "下界合金锭"));
        LOOT_TABLE.add(new ItemReward(Material.ENCHANTED_GOLDEN_APPLE, 2, Rarity.PURPLE, "附魔金苹果"));
        LOOT_TABLE.add(new ItemReward(Material.BEACON, 1, Rarity.PURPLE, "信标"));
        LOOT_TABLE.add(new ItemReward(Material.TRIDENT, 1, Rarity.PURPLE, "三叉戟"));
        LOOT_TABLE.add(new ItemReward(Material.DRAGON_HEAD, 1, Rarity.PURPLE, "龙首"));
        LOOT_TABLE.add(new ItemReward(Material.PIGLIN_HEAD, 1, Rarity.PURPLE, "猪灵头颅"));
        LOOT_TABLE.add(new ItemReward(Material.HEAVY_CORE, 1, Rarity.PURPLE, "沉重核心")); // 1.21 锤子核心
        LOOT_TABLE.add(new ItemReward(Material.BREEZE_ROD, 8, Rarity.PURPLE, "旋风棒")); // 1.21
        LOOT_TABLE.add(new ItemReward(Material.OMINOUS_TRIAL_KEY, 3, Rarity.PURPLE, "不祥试炼钥匙")); // 1.21
        LOOT_TABLE.add(new ItemReward(Material.MUSIC_DISC_PIGSTEP, 1, Rarity.PURPLE, "唱片(Pigstep)"));
        LOOT_TABLE.add(new ItemReward(Material.MUSIC_DISC_OTHERSIDE, 1, Rarity.PURPLE, "唱片(Otherside)"));
        LOOT_TABLE.add(new ItemReward(Material.MUSIC_DISC_RELIC, 1, Rarity.PURPLE, "唱片(Relic)"));
        LOOT_TABLE.add(new ItemReward(Material.MUSIC_DISC_CREATOR, 1, Rarity.PURPLE, "唱片(Creator)")); // 1.21
        LOOT_TABLE.add(new ItemReward(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 1, Rarity.PURPLE, "尖塔纹饰"));
        LOOT_TABLE.add(new ItemReward(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1, Rarity.PURPLE, "寂静纹饰"));
        LOOT_TABLE.add(new ItemReward(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, 1, Rarity.PURPLE, "流浪纹饰")); // 1.21

        // ==========================================
        //  GOLD - 传世 (1%) - 唯一/特效
        // ==========================================
        // 龙蛋
        LOOT_TABLE.add(new ItemReward(Material.DRAGON_EGG, 1, Rarity.GOLD, "§6§l龙蛋"));
        // 技能点数
        LOOT_TABLE.add(new SkillPointReward(Rarity.GOLD));
        // 特效
        for (SpiritEffectType type : SpiritEffectType.values()) {
            if (type.getSource() == SpiritEffectType.EffectSource.LOTTERY) {
                LOOT_TABLE.add(new EffectReward(type, Rarity.GOLD));
            }
        }
    }

    public static Reward getRandomReward() {
        int totalWeight = LOOT_TABLE.stream().mapToInt(i -> i.rarity.weight).sum();
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;
        for (Reward item : LOOT_TABLE) {
            current += item.rarity.weight;
            if (random < current) return item;
        }
        return LOOT_TABLE.get(0);
    }

    public static List<Reward> getDisplayItems(int count) {
        List<Reward> displays = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            displays.add(LOOT_TABLE.get(ThreadLocalRandom.current().nextInt(LOOT_TABLE.size())));
        }
        return displays;
    }

    // 新增奖励类型
    public static class SkillPointReward extends Reward {
        public SkillPointReward(Rarity rarity) { super(rarity, "技能点 x1"); }
        @Override public ItemStack getDisplayItem() { return new ItemStack(Material.EXPERIENCE_BOTTLE); }
        @Override public void grant(Player p, SpiritProfile profile) {
            profile.addExtraSkillPoints(1);
            p.sendMessage("§6[!] 获得了珍贵的技能点！");
            save(profile);
        }
    }
}