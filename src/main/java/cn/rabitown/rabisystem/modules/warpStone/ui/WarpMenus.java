package cn.rabitown.rabisystem.modules.warpStone.ui;

import cn.rabitown.rabisystem.modules.warpStone.data.WarpStone;
import cn.rabitown.rabisystem.modules.warpStone.manager.WarpManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WarpMenus {

    public static final String TITLE_MENU = ChatColor.DARK_AQUA + ">> " + ChatColor.GOLD + "传送石" + ChatColor.DARK_AQUA + " <<";
    public static final String TITLE_SETTING_PREFIX = ChatColor.DARK_AQUA + ">> " + ChatColor.GOLD + "设置: ";
    public static final String TITLE_ICON_SELECTOR = ChatColor.DARK_AQUA + ">> " + ChatColor.GOLD + "请点击背包物品作为图标" + ChatColor.DARK_AQUA + " <<";

    private final WarpManager manager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public WarpMenus(WarpManager manager, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    public void openWarpMenu(Player player, int page) {
        openWarpMenu(player, page, false);
    }

    public void openWarpMenu(Player player, int page, boolean fromSpirit) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MENU);

        // --- Row 0: Navigation ---
        if (fromSpirit) {
            inv.setItem(4, createGuiItem(Material.IRON_DOOR, "§c⬅ 返回小精灵"));
        }

        // --- Row 1: Separation Glass ---
        ItemStack glass = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 9; i < 18; i++) inv.setItem(i, glass);

        // --- Data Sorting ---
        List<WarpStone> myStones = new ArrayList<>();
        List<WarpStone> otherStones = new ArrayList<>();

        for (WarpStone s : manager.getWarpStones().values()) {
            if (s.getOwner().equals(player.getUniqueId())) {
                myStones.add(s);
            } else if (s.isPublic() || s.getWhitelist().contains(player.getUniqueId())) {
                otherStones.add(s);
            }
        }

        // --- Row 2: My Stones (Index 18-26) ---
        // 显示最多 9 个自己的传送石
        for (int i = 0; i < Math.min(9, myStones.size()); i++) {
            inv.setItem(18 + i, createStoneIcon(myStones.get(i)));
        }

        // --- Row 3+: Other Stones (Index 27-53) ---
        // 区域大小: 54 - 27 = 27 格
        int slotsPerPage = 27;
        int totalOthers = otherStones.size();
        int totalPages = (int) Math.ceil((double) totalOthers / slotsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, totalOthers);

        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(27 + (i - startIndex), createStoneIcon(otherStones.get(i)));
        }

        // Pagination buttons (Row 0 Corners)
        if (page > 1) inv.setItem(0, createGuiItem(Material.ARROW, ChatColor.YELLOW + "上一页"));
        inv.setItem(8, createGuiItem(Material.PAPER, "第 " + page + " 页")); // Right corner just shows page
        if (page < totalPages) inv.setItem(8, createGuiItem(Material.ARROW, ChatColor.YELLOW + "下一页")); // Overwrite if next exists

        player.openInventory(inv);
    }

    private ItemStack createStoneIcon(WarpStone s) {
        ItemStack item = new ItemStack(s.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + s.getName());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "stone_key"), PersistentDataType.STRING, s.getName());
        item.setItemMeta(meta);
        return item;
    }

    public void openSettingsMenu(Player player, WarpStone stone) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE_SETTING_PREFIX + stone.getName());
        ItemStack toggle = new ItemStack(stone.isPublic() ? Material.REDSTONE_TORCH : Material.LEVER);
        ItemMeta tMeta = toggle.getItemMeta();
        tMeta.setDisplayName(stone.isPublic() ? ChatColor.GREEN + "状态: 公开 (点击切换)" : ChatColor.RED + "状态: 私有 (点击切换)");
        toggle.setItemMeta(tMeta);
        inv.setItem(0, toggle);

        inv.setItem(1, createGuiItem(Material.PLAYER_HEAD, ChatColor.YELLOW + "分享给玩家"));
        inv.setItem(6, createGuiItem(Material.FURNACE, ChatColor.YELLOW + "更改图标"));

        player.openInventory(inv);
    }

    public void openIconSelector(Player player, WarpStone stone) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE_ICON_SELECTOR);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "请直接点击你背包里的物品");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "点击后，该物品的材质将作为新图标",
                ChatColor.GRAY + "不需要把物品放进这里",
                ChatColor.RED + "当前传送石: " + stone.getName()
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "target_stone"), PersistentDataType.STRING, stone.getName());
        info.setItemMeta(meta);

        inv.setItem(4, info);
        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private String getFormattedDim(World w) {
        if (w == null) return ChatColor.GRAY + "维度: 未知";
        switch (w.getEnvironment()) {
            case NORMAL: return ChatColor.GREEN + "维度: 主世界";
            case NETHER: return ChatColor.RED + "维度: 下界";
            case THE_END: return ChatColor.LIGHT_PURPLE + "维度: 末界";
            default: return ChatColor.WHITE + "维度: " + w.getName();
        }
    }
}