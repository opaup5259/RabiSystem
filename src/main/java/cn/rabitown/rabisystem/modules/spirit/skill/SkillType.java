package cn.rabitown.rabisystem.modules.spirit.skill;

import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;

public enum SkillType {

    // --- 星光系 (STAR) - 辅助/生活 ---
    STAR_HEAL("star_heal", "星光抚慰", "STAR", Type.ACTIVE, Material.GLISTERING_MELON_SLICE, 30, 20, null, // <--- 增加 null
            "§7强制安抚周围10米内的生物，", "§7使其暂时(5s)失去攻击欲望。", "§7消耗: §b30 MP", "§7冷却: §f20s"),

    STAR_LIGHT("star_light", "荧光指引", "STAR", Type.QUICK, Material.GLOW_INK_SAC, 10, 5, null, // <--- 增加 null
            "§7给予主人夜视效果(30秒)。", "§7消耗: §b10 MP", "§7冷却: §f5s"),

    STAR_PASSIVE_MP("star_passive_mp", "星辉流转", "STAR", Type.PASSIVE, Material.AMETHYST_SHARD, 0, 0, null, // <--- 增加 null
            "§7[被动] MP自然恢复速度 +20%。"),

    // [修改] 快捷工具 I (无前置)
    STAR_TOOLS_I("star_tools_1", "便携工坊 I", "STAR", Type.QUICK, Material.CRAFTING_TABLE, 0, 1, null, // <--- 增加 null
            "§7[快捷] 打开便携工作面板。", "§7包含: 工作台, 熔炉, 织布机, 制图台", "§7* 熔炉仅在装备此技能时运作"),

    // [修改] 快捷工具 II (添加前置 STAR_TOOLS_I)
    STAR_TOOLS_II("star_tools_2", "便携工坊 II", "STAR", Type.QUICK, Material.SMITHING_TABLE, 0, 1, "star_tools_1", // <--- 这里填上前置ID
            "§7[快捷] 打开高级便携面板。", "§7包含: Lv.1所有设施 +", "§7烟熏炉, 高炉, 切石机, 锻造台", "§7* 设施仅在装备此技能时运作"),

    // --- 暗影系 (SHADOW) - 战斗 ---
    SHADOW_STRIKE("shadow_strike", "暗影突袭", "SHADOW", Type.ACTIVE, Material.NETHERITE_SWORD, 50, 15, null,
            "§7命令小精灵对目标造成一次强力伤害。", "§7消耗: §b50 MP", "§7冷却: §f15s"),

    SHADOW_SHIELD("shadow_shield", "虚空护盾", "SHADOW", Type.QUICK, Material.OBSIDIAN, 40, 60, null,
            "§7获得短暂的抗性提升。", "§7消耗: §b40 MP", "§7冷却: §f60s"),

    SHADOW_PASSIVE_DRAIN("shadow_passive_drain", "噬灵", "SHADOW", Type.PASSIVE, Material.SCULK_VEIN, 0, 0, null,
            "§7[被动] 攻击时有概率恢复少量MP。"),

    SHADOW_WALKER("shadow_walker", "灵界行者", "SHADOW", Type.ACTIVE, Material.PHANTOM_MEMBRANE, 100, 120, null,
            "§7遁入灵界 10秒。", "§7效果: 隐身,无敌,极速", "§7消耗: §b100 MP", "§7冷却: §f120s"),

    SHADOW_MARK("shadow_mark", "暗影标记", "SHADOW", Type.QUICK, Material.SPECTRAL_ARROW, 20, 15, null,
            "§7标记周围12米内的敌对生物。", "§7使其高亮显示 10秒。", "§7消耗: §b20 MP", "§7冷却: §f15s");

    public enum Type { ACTIVE, QUICK, PASSIVE }

    private final String id;
    private final String name;
    private final String treeType;
    private final Type type;
    private final Material icon;
    private final int cost;
    private final int cooldown;
    private final String prerequisite; // [新增] 前置技能ID
    private final List<String> description;

    SkillType(String id, String name, String treeType, Type type, Material icon, int cost, int cooldown, String prerequisite, String... desc) {
        this.id = id;
        this.name = name;
        this.treeType = treeType;
        this.type = type;
        this.icon = icon;
        this.cost = cost;
        this.cooldown = cooldown;
        this.prerequisite = prerequisite; // 赋值
        this.description = Arrays.asList(desc);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTreeType() { return treeType; }
    public Type getType() { return type; }
    public Material getIcon() { return icon; }
    public int getCost() { return cost; }
    public int getCooldown() { return cooldown; }
    public String getPrerequisite() { return prerequisite; }
    public List<String> getDescription() { return description; }

    public static SkillType fromId(String id) {
        for (SkillType s : values()) if (s.id.equals(id)) return s;
        return null;
    }
}