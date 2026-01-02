package cn.rabitown.rabisystem.modules.spirit.data;

import org.bukkit.Material;
import java.util.Arrays;

public enum SpiritEffectType {

    NONE("0", "无特效", 0, Material.GLASS_BOTTLE, "不展示任何视觉效果", EffectSource.DEFAULT),

    // --- 等级解锁 (LEVEL) ---
    BOND("1", "羁绊 · 灵光乍现", 1, Material.WHITE_DYE, "小精灵周身闪烁着微弱的星光", EffectSource.LEVEL),
    RESONANCE("2", "共鸣 · 以太波动", 30, Material.LIGHT_BLUE_DYE, "飞行时留下淡蓝色的魔法残留", EffectSource.LEVEL),
    COVENANT("3", "灵契 · 契约守望", 80, Material.PURPLE_DYE, "脚下展开虚幻的契约法阵", EffectSource.LEVEL),
    BINARY_STAR("4", "双星 · 星界辉光", 100, Material.NETHER_STAR, "两颗璀璨的星辰交替环绕", EffectSource.LEVEL),

    // --- 抽奖限定 (LOTTERY) ---
    GALAXY("lottery_1", "星河 · 璀璨流转", 0, Material.ENDER_EYE, "环绕着微缩的银河系", EffectSource.LOTTERY),
    VOLCANO("lottery_2", "熔火 · 灰烬新生", 0, Material.MAGMA_CREAM, "周身散发着不熄的余烬", EffectSource.LOTTERY);
    private final String id;
    private final String name;
    private final int requiredLevel;
    private final Material icon;
    private final String description;
    private final EffectSource source;

    public enum EffectSource {
        DEFAULT, // 默认自带
        LEVEL,   // 等级解锁
        LOTTERY  // 抽奖获取
    }

    SpiritEffectType(String id, String name, int requiredLevel, Material icon, String description, EffectSource source) {
        this.id = id;
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.icon = icon;
        this.description = description;
        this.source = source;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getRequiredLevel() { return requiredLevel; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
    public EffectSource getSource() { return source; }

    public static SpiritEffectType fromId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.id.equals(id))
                .findFirst()
                .orElse(NONE);
    }
}