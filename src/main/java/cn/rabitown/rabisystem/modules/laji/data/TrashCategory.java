package cn.rabitown.rabisystem.modules.laji.data;

import org.bukkit.Material;

public enum TrashCategory {
    ALL("全部", Material.NETHER_STAR),
    FOOD("食物饮品", Material.APPLE),
    COMBAT("战斗用品", Material.DIAMOND_SWORD),
    TOOLS("工具日用", Material.DIAMOND_PICKAXE),
    BLOCKS("可放置物", Material.GRASS_BLOCK),
    MATERIALS("原材料", Material.IRON_INGOT),
    OTHERS("其他", Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);

    private final String name;
    private final Material icon;

    TrashCategory(String name, Material icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() { return name; }
    public Material getIcon() { return icon; }
}