package cn.rabitown.rabisystem.modules.spirit.data;

import cn.rabitown.rabisystem.modules.spirit.achievement.Achievement;
import org.bukkit.Material;

public enum SpiritSkin {

    // --- 在这里添加新皮肤，菜单会自动更新 ---
    DEFAULT("DEFAULT", "本我 · 小小悦灵", "小精灵的原始形态", Material.ALLAY_SPAWN_EGG, null),

    FOX_RED("FOX_RED", "幻化 · 青丘红狐", "灵动可爱的红色小狐狸", Material.FOX_SPAWN_EGG, Achievement.BREED_FOX_RED),

    FOX_SNOW("FOX_SNOW", "幻化 · 雪域灵仙", "来自冰原的洁白身影", Material.SNOW_BLOCK, Achievement.BREED_FOX_SNOW),

    PARROT("PARROT", "幻化 · 彩羽信使", "五彩斑斓的飞行家", Material.PARROT_SPAWN_EGG, Achievement.BREED_PARROT),

    AXOLOTL("AXOLOTL", "幻化 · 水域精灵", "粉嫩的水中治愈者", Material.AXOLOTL_BUCKET, Achievement.BREED_AXOLOTL);

    // --- 字段定义 ---
    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final Achievement requiredAchievement; // 解锁需要的成就，null代表默认解锁

    SpiritSkin(String id, String displayName, String description, Material icon, Achievement requiredAchievement) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.requiredAchievement = requiredAchievement;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getIcon() { return icon; }
    public Achievement getRequiredAchievement() { return requiredAchievement; }

    // 辅助查找
    public static SpiritSkin fromId(String id) {
        for (SpiritSkin s : values()) {
            if (s.id.equals(id)) return s;
        }
        return DEFAULT;
    }
}