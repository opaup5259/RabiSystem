package cn.rabitown.rabisystem.modules.spirit.achievement;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Achievement {

    // --- 🌟 成长与羁绊 (Growth) ---
    GROW_1("grow_1", "初识灵光", "第一次成功召唤小精灵", 50, makeItem(Material.SWEET_BERRIES, 5)),
    GROW_10("grow_10", "懵懂学步", "灵契等级达到 Lv.10", 100, makeItem(Material.IRON_INGOT, 5)),
    GROW_30("grow_30", "心意相通", "灵契等级达到 Lv.30", 300, makeItem(Material.GOLD_INGOT, 3)),
    GROW_50("grow_50", "心灵共鸣", "灵契等级达到 Lv.50", 500, makeItem(Material.DIAMOND, 3)),
    GROW_80("grow_80", "灵魂契约", "灵契等级达到 Lv.80", 1000, makeItem(Material.NETHERITE_SCRAP, 1)),
    GROW_100("grow_100", "双星闪耀", "灵契等级达到 Lv.100 (满级)", 2000, makeItem(Material.NETHER_STAR, 1)),

    NAME_1("name_1", "赋予真名", "使用命名牌给小精灵改名", 100, makeItem(Material.NAME_TAG, 1)),
    MOOD_100("mood_100", "开心果", "心情值达到 100 (满值)", 50, makeItem(Material.CAKE, 1)),
    TIME_ONLINE("time_online", "长情告白", "累计召唤时长达到 100 小时", 800, makeItem(Material.CLOCK, 1)),

    // --- 🍖 交互与饮食 (Interaction) ---
    PET_1("pet_1", "温柔的触碰", "第一次抚摸小精灵", 20, makeItem(Material.EXPERIENCE_BOTTLE, 1)),
    PET_100("pet_100", "撸灵狂魔", "累计抚摸小精灵 100 次", 200, makeItem(Material.WHITE_WOOL, 10)),

    FEED_10("feed_10", "投喂体验", "累计投喂任意食物 10 次", 50, makeItem(Material.BREAD, 5)),
    FEED_100("feed_100", "大胃王", "累计投喂食物 100 次", 300, makeItem(Material.GOLDEN_APPLE, 3)),
    FEED_LUXURY("feed_luxury", "奢华盛宴", "投喂一次附魔金苹果", 500, makeItem(Material.ENCHANTED_GOLDEN_APPLE, 1)),
    FEED_GROSS("feed_gross", "饥不择食", "投喂一次腐肉或蜘蛛眼", 10, makeItem(Material.MILK_BUCKET, 1)),

    AUTO_EAT("auto_eat", "懒人福音", "触发自动进食 50 次", 150, makeItem(Material.COOKIE, 16)),

    // --- ⚔️ 战斗与守护 (Combat) ---
    BUFF_1("buff_1", "并肩作战", "触发 1 次灵力共鸣(力量Buff)", 50, makeItem(Material.POTION, 1)), // 此处简单给个药水瓶，具体可以是力量药水
    BURST_1("burst_1", "星屑审判", "触发 1 次灵力迸发", 100, makeItem(Material.FIREWORK_ROCKET, 5)),
    BURST_KILL("burst_kill", "最后一击", "灵力迸发直接击杀生物", 200, makeItem(Material.DIAMOND_SWORD, 1)),
    SAVE_LIFE("save_life", "誓约之盾", "触发 1 次灵魂代偿(免死)", 1000, makeItem(Material.TOTEM_OF_UNDYING, 1)),

    MONSTER_HUNTER("monster_hunter", "灵契猎人", "携带期间累计击杀 100 只怪物", 300, makeItem(Material.BOW, 1)),
    BOSS_FIGHT("boss_fight", "屠龙勇士的伙伴", "携带期间击杀末影龙或凋零", 1000, makeItem(Material.DRAGON_BREATH, 1)),

    // --- 🎵 社交与探索 (Social & Exploration) ---
    MEET_FRIEND("meet_friend", "你好呀！", "与其他小精灵互动 1 次", 50, makeItem(Material.EMERALD, 1)),
    PARTY_TIME("party_time", "舞力全开", "小精灵进入跳舞状态", 100, makeItem(Material.JUKEBOX, 1)),
    MUSIC_PADORU("music_padoru", "圣诞快乐?", "小精灵哼唱《Padoru》", 50, makeItem(Material.REDSTONE, 10)),

    GRAVITY_PICKUP("gravity_pickup", "虚空清道夫", "虚空引力累计拾取 64 个物品", 100, makeItem(Material.HOPPER, 1)),
    GRAVITY_FILTER("gravity_filter", "挑食", "启用虚空引力过滤功能", 30, makeItem(Material.ITEM_FRAME, 1)),
    BACKPACK_FULL("backpack_full", "满载而归", "小精灵背包被完全装满", 50, makeItem(Material.CHEST, 2)),

    // --- 🗓️ 日常与特殊 (Daily & Special) ---
    SIGNIN_7("signin_7", "持之以恒", "连续签到达到 7 天", 150, null), // 奖励通过 SignInManager 直接发了补签卡，这里给空或者给额外经验
    SIGNIN_30("signin_30", "月度全勤", "累计签到达到 30 天", 500, makeItem(Material.DIAMOND_BLOCK, 1)),
    SIGNIN_HOLIDAY("signin_holiday", "节日快乐", "在节假日完成签到", 100, makeItem(Material.FIREWORK_STAR, 1)),

    DEATH_WAIT("death_wait", "漫长的等待", "小精灵死亡并进入重聚冷却", 10, makeItem(Material.SOUL_LANTERN, 1)),
    EFFECT_UNLOCK("effect_unlock", "华丽变身", "激活一种非默认特效", 50, makeItem(Material.GLOWSTONE_DUST, 5)),

    // --- 🦋 幻化系列 ---
    BREED_FOX_RED("breed_fox_red", "青丘之缘", "成功繁殖红狐 50 次 (解锁红狐外观)", 500, makeItem(Material.FOX_SPAWN_EGG, 1)),
    BREED_FOX_SNOW("breed_fox_snow", "雪域灵仙", "成功繁殖雪狐 100 次 (解锁白狐外观)", 1000, makeItem(Material.SNOW_BLOCK, 1)),
    BREED_PARROT("breed_parrot", "彩羽信使", "成功繁殖鹦鹉 50 次 (解锁鹦鹉外观)", 500, makeItem(Material.PARROT_SPAWN_EGG, 1)),
    BREED_AXOLOTL("breed_axolotl", "水域精灵", "成功繁殖美西螈 50 次 (解锁美西螈外观)", 500, makeItem(Material.AXOLOTL_BUCKET, 1));

    private final String id;
    private final String name;
    private final String description;
    private final int expReward;
    private final ItemStack itemReward;

    Achievement(String id, String name, String description, int expReward, ItemStack itemReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.expReward = expReward;
        this.itemReward = itemReward;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getExpReward() { return expReward; }
    public ItemStack getItemReward() { return itemReward; }

    // 获取展示用的图标，如果没奖励则显示书本
    public ItemStack getIcon() {
        return itemReward != null ? itemReward.clone() : new ItemStack(Material.BOOK);
    }

    // 辅助构建方法
    private static ItemStack makeItem(Material mat, int amount) {
        return new ItemStack(mat, amount);
    }
}