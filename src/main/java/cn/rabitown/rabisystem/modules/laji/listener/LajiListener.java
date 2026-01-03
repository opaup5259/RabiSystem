package cn.rabitown.rabisystem.modules.laji.listener;

import cn.rabitown.rabisystem.modules.laji.LajiModule;
import cn.rabitown.rabisystem.modules.laji.data.TrashCategory;
import cn.rabitown.rabisystem.modules.laji.data.TrashItem;
import cn.rabitown.rabisystem.modules.laji.manager.LajiManager;
import cn.rabitown.rabisystem.modules.laji.ui.DropGuiHolder;
import cn.rabitown.rabisystem.modules.laji.ui.PublicBinHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LajiListener implements Listener {

    private final LajiModule module;

    public LajiListener(LajiModule module) {
        this.module = module;
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (module.getManager().isTrashCanStructure(clicked)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            Block lectern = (clicked.getType() == Material.LECTERN) ? clicked : clicked.getRelative(0, 1, 0);
            player.playSound(lectern.getLocation(), Sound.BLOCK_BARREL_OPEN, 1f, 1f);
            module.getManager().openDropGui(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof DropGuiHolder) {
            handleDropGuiClick(event);
        } else if (inv.getHolder() instanceof PublicBinHolder) {
            handlePublicBinClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DropGuiHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < 18) { event.setCancelled(true); return; }
            }
        } else if (event.getInventory().getHolder() instanceof PublicBinHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        LajiManager manager = module.getManager();

        if (manager.getSwitchingMenus().contains(uuid)) {
            manager.getSwitchingMenus().remove(uuid);
        } else {
            if (event.getInventory().getHolder() instanceof DropGuiHolder || event.getInventory().getHolder() instanceof PublicBinHolder) {
                player.playSound(player.getLocation(), Sound.BLOCK_BARREL_CLOSE, 1f, 1f);
            }
        }

        if (event.getInventory().getHolder() instanceof DropGuiHolder) {
            Inventory inv = event.getInventory();
            boolean droppedAny = false;
            for (int i = 18; i < 54; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    manager.addTrashInfo(item, player.getName(), System.currentTimeMillis());
                    manager.getTrashItems().add(0, new TrashItem(item, System.currentTimeMillis(), player.getName()));
                    droppedAny = true;
                }
            }
            manager.getTrashItems().sort(Comparator.comparingLong(TrashItem::getTimestamp).reversed());
            if (droppedAny) {
                player.sendActionBar(Component.text("垃圾已丢弃！", NamedTextColor.GREEN));
                module.getConfigManager().saveTrashItems(manager.getTrashItems());
            }
        }
    }

    private void handleDropGuiClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        Player player = (Player) event.getWhoClicked();
        if (slot < 18) {
            event.setCancelled(true);
            if (slot == 4) {
                module.getManager().getSwitchingMenus().add(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.getManager().openPublicBinGui(player, 0, TrashCategory.ALL));
            }
        }
    }

    private void handlePublicBinClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        PublicBinHolder holder = (PublicBinHolder) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();
        LajiManager manager = module.getManager();

        if (slot >= 36) {
            if (slot == 40) {
                manager.getSwitchingMenus().add(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> manager.openDropGui(player));
            } else if (slot == 45 && event.getCurrentItem().getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                manager.openPublicBinGui(player, holder.getPage() - 1, holder.getCurrentCategory());
            } else if (slot == 53 && event.getCurrentItem().getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                manager.openPublicBinGui(player, holder.getPage() + 1, holder.getCurrentCategory());
            } else if (slot == 46) switchCategory(player, TrashCategory.FOOD);
            else if (slot == 47) switchCategory(player, TrashCategory.BLOCKS);
            else if (slot == 48) switchCategory(player, TrashCategory.COMBAT);
            else if (slot == 49) switchCategory(player, TrashCategory.ALL);
            else if (slot == 50) switchCategory(player, TrashCategory.TOOLS);
            else if (slot == 51) switchCategory(player, TrashCategory.MATERIALS);
            else if (slot == 52) switchCategory(player, TrashCategory.OTHERS);
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        List<TrashItem> filteredList;
        if (holder.getCurrentCategory() == TrashCategory.ALL) {
            filteredList = new ArrayList<>(manager.getTrashItems());
        } else {
            filteredList = manager.getTrashItems().stream()
                    .filter(t -> manager.getCategory(t.getItem().getType()) == holder.getCurrentCategory())
                    .collect(Collectors.toList());
        }

        int indexInPage = slot;
        int realIndex = holder.getPage() * 36 + indexInPage;

        if (realIndex < filteredList.size()) {
            TrashItem trash = filteredList.get(realIndex);

            ItemMeta clickedMeta = clickedItem.getItemMeta();
            Long clickedTime = null;
            if (clickedMeta != null) {
                clickedTime = clickedMeta.getPersistentDataContainer().get(new NamespacedKey(module.getPlugin(), "drop_time"), PersistentDataType.LONG);
            }

            if (clickedTime == null || !clickedTime.equals(trash.getTimestamp())) {
                player.sendMessage(Component.text("手慢了！该垃圾已被其他人捡走或列表已更新。", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                manager.openPublicBinGui(player, holder.getPage(), holder.getCurrentCategory());
                return;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(Component.text("你的背包已满！", NamedTextColor.RED));
                return;
            }

            manager.getTrashItems().remove(trash);
            ItemStack toGive = trash.getItem().clone();
            manager.cleanTrashInfo(toGive);
            player.getInventory().addItem(toGive);

            player.sendActionBar(Component.text("捡回了垃圾！", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);

            manager.openPublicBinGui(player, holder.getPage(), holder.getCurrentCategory());
            module.getConfigManager().saveTrashItems(manager.getTrashItems());
        }
    }

    private void switchCategory(Player player, TrashCategory category) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        module.getManager().openPublicBinGui(player, 0, category);
    }
}