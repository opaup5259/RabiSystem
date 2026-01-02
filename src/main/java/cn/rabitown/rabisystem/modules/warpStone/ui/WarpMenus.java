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
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MENU);
        List<WarpStone> allStones = new ArrayList<>();

        for (WarpStone s : manager.getWarpStones().values()) {
            boolean isMine = s.getOwner().equals(player.getUniqueId());
            boolean isShared = s.getWhitelist().contains(player.getUniqueId());
            boolean isPublic = s.isPublic();
            if (isMine || isShared || isPublic) allStones.add(s);
        }

        allStones.sort((s1, s2) -> {
            boolean s1Mine = s1.getOwner().equals(player.getUniqueId());
            boolean s2Mine = s2.getOwner().equals(player.getUniqueId());
            if (s1Mine && !s2Mine) return -1;
            if (!s1Mine && s2Mine) return 1;
            return Long.compare(s1.getCreated(), s2.getCreated());
        });

        int slotsPerPage = 36;
        int totalStones = allStones.size();
        int totalPages = (int) Math.ceil((double) totalStones / slotsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        if (page > 1) inv.setItem(6, createGuiItem(Material.ARROW, ChatColor.YELLOW + "上一页"));
        inv.setItem(7, createGuiItem(Material.PAPER, "第 " + page + " 页"));
        if (page < totalPages) inv.setItem(8, createGuiItem(Material.ARROW, ChatColor.YELLOW + "下一页"));

        ItemStack glass = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 9; i < 18; i++) inv.setItem(i, glass);

        int startIndex = (page - 1) * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, totalStones);

        for (int i = startIndex; i < endIndex; i++) {
            WarpStone s = allStones.get(i);
            ItemStack item = new ItemStack(s.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + s.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.AQUA + String.format("坐标: %d, %d, %d", s.getLocation().getBlockX(), s.getLocation().getBlockY(), s.getLocation().getBlockZ()));
            lore.add(getFormattedDim(s.getLocation().getWorld()));
            lore.add(ChatColor.GRAY + "所有者: " + Bukkit.getOfflinePlayer(s.getOwner()).getName());

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "stone_key"), PersistentDataType.STRING, s.getName());
            item.setItemMeta(meta);
            inv.setItem(18 + (i - startIndex), item);
        }
        player.openInventory(inv);
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