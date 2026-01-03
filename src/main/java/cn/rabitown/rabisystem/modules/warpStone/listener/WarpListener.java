package cn.rabitown.rabisystem.modules.warpStone.listener;

import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import cn.rabitown.rabisystem.modules.warpStone.WarpStoneModule;
import cn.rabitown.rabisystem.modules.warpStone.data.WarpStone;
import cn.rabitown.rabisystem.modules.warpStone.manager.WarpManager;
import cn.rabitown.rabisystem.modules.warpStone.ui.WarpMenus;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class WarpListener implements Listener {

    private final WarpStoneModule module;
    private final WarpMenus menus;

    public WarpListener(WarpStoneModule module) {
        this.module = module;
        this.menus = new WarpMenus(module.getWarpManager(), module.getPlugin());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        WarpManager manager = module.getWarpManager();

        // 1. 指南针交互
        if (player.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                menus.openWarpMenu(player, 1);
                return;
            }
        }

        // 2. 传送石交互
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();

            // A: 点击石砖墙 (底座) -> 重定向到磁石
            if (block.getType() == Material.STONE_BRICK_WALL) {
                Block up = block.getRelative(0, 1, 0);
                if (up.getType() == Material.LODESTONE) {
                    if (manager.getWarpStoneAt(up.getLocation()) != null) {
                        block = up;
                    } else {
                        return;
                    }
                }
            }

            // B: 点击磁石 (本体)
            if (block.getType() == Material.LODESTONE) {
                WarpStone stone = manager.getWarpStoneAt(block.getLocation());

                // 已激活
                if (stone != null) {
                    event.setCancelled(true);
                    if (player.isSneaking()) {
                        if (stone.getOwner().equals(player.getUniqueId()) || player.hasPermission("rabi.warpstone.admin")) {
                            menus.openSettingsMenu(player, stone);
                        } else {
                            manager.sendActionBar(player, ChatColor.RED + "你没有权限设置此传送石");
                        }
                    } else {
                        menus.openWarpMenu(player, 1);
                    }
                }
                // 未激活 -> 创建
                else {
                    Block down = block.getRelative(0, -1, 0);
                    // 严格限制: 必须是平滑石头
                    if (down.getType() == Material.SMOOTH_STONE) {
                        event.setCancelled(true);
                        if (!manager.checkLimit(player)) {
                            manager.sendActionBar(player, ChatColor.RED + "你的传送石数量已达上限");
                            return;
                        }
                        // 使用悬浮告示牌输入名称
                        manager.openSignInput(player, WarpManager.InputType.CREATE, block.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        WarpManager manager = module.getWarpManager();

        if (!manager.getSignSessions().containsKey(player.getUniqueId())) return;

        WarpManager.SignSession session = manager.getSignSessions().remove(player.getUniqueId());
        session.signLoc.getBlock().setBlockData(session.originalBlockData); // 恢复方块

        String input = event.getLine(0);

        // 简单过滤默认文本
        if (input == null || input.trim().isEmpty() || input.equals("在此输入")) {
            String fallback = event.getLine(1);
            if (fallback != null && !fallback.startsWith("传送石") && !fallback.startsWith("玩家ID")) {
                input = fallback;
            } else {
                manager.sendActionBar(player, ChatColor.RED + "未检测到输入，操作取消");
                return;
            }
        }
        input = input.trim();

        if (session.type == WarpManager.InputType.CREATE) {
            if (manager.getWarpStones().containsKey(input)) {
                manager.sendActionBar(player, ChatColor.RED + "名称已存在");
                return;
            }
            Location loc = (Location) session.data;
            if (loc.getBlock().getType() != Material.LODESTONE) {
                manager.sendActionBar(player, ChatColor.RED + "磁石已消失，创建失败");
                return;
            }
            manager.createWarpStone(player, input, loc);

        } else if (session.type == WarpManager.InputType.SHARE) {
            WarpStone stone = (WarpStone) session.data;
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(input);
            stone.getWhitelist().add(target.getUniqueId());
            manager.sendActionBar(player, ChatColor.GREEN + "已分享给: " + input);
            module.getConfigManager().saveWarpStones(manager.getWarpStones());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        WarpManager manager = module.getWarpManager();

        // 1. 图标选择器
        if (title.equals(WarpMenus.TITLE_ICON_SELECTOR)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getBottomInventory() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                ItemStack infoItem = event.getView().getTopInventory().getItem(4);
                if (infoItem != null && infoItem.hasItemMeta()) {
                    String stoneName = infoItem.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(module.getPlugin(), "target_stone"), PersistentDataType.STRING);
                    WarpStone stone = manager.getWarpStones().get(stoneName);

                    if (stone != null) {
                        Material newIcon = event.getCurrentItem().getType();
                        stone.setIcon(newIcon);
                        module.getConfigManager().saveWarpStones(manager.getWarpStones());
                        manager.sendActionBar(player, ChatColor.GREEN + "图标已更新为: " + newIcon.name());
                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                    }
                }
            }
            return;
        }

        // 2. 菜单与设置
        if (title.equals(WarpMenus.TITLE_MENU) || title.startsWith(WarpMenus.TITLE_SETTING_PREFIX.substring(0, 10))) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            // 传送菜单
            if (title.equals(WarpMenus.TITLE_MENU)) {
                if (event.getSlot() == 4 && event.getCurrentItem().getType() == Material.IRON_DOOR) {
                    SpiritMenus.openMainMenu(player, SpiritUtils.getSpiritManager().getProfile(player.getUniqueId()), 1);
                    return;
                }
                if (clicked.getType() == Material.ARROW) {
                    if (event.getSlot() == 6 || event.getSlot() == 8) {
                        ItemStack paper = event.getInventory().getItem(7);
                        if (paper != null && paper.hasItemMeta()) {
                            int page = Integer.parseInt(paper.getItemMeta().getDisplayName().replace("第 ", "").replace(" 页", ""));
                            if (event.getSlot() == 6) menus.openWarpMenu(player, Math.max(1, page - 1));
                            if (event.getSlot() == 8) menus.openWarpMenu(player, page + 1);
                        }
                    }
                } else if (clicked.hasItemMeta()) {
                    String name = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(module.getPlugin(), "stone_key"), PersistentDataType.STRING);
                    if (name != null) {
                        WarpStone stone = manager.getWarpStones().get(name);
                        if (stone != null) {
                            Location tpLoc = stone.getLocation().clone().add(0.5, 1, 0.5);
                            tpLoc.setYaw(player.getLocation().getYaw());
                            tpLoc.setPitch(player.getLocation().getPitch());
                            player.teleport(tpLoc);
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            manager.sendActionBar(player, ChatColor.GREEN + "已传送至: " + name);
                        }
                    }
                }
            }
            // 设置菜单
            else if (title.startsWith(WarpMenus.TITLE_SETTING_PREFIX.substring(0, 10))) {
                String stoneName = title.replace(WarpMenus.TITLE_SETTING_PREFIX, "");
                WarpStone stone = manager.getWarpStones().get(stoneName);
                if (stone == null) { player.closeInventory(); return; }

                if (event.getSlot() == 0) { // Toggle Public
                    stone.setPublic(!stone.isPublic());
                    menus.openSettingsMenu(player, stone);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    module.getConfigManager().saveWarpStones(manager.getWarpStones());
                } else if (event.getSlot() == 1) { // Share -> Sign Input
                    player.closeInventory();
                    manager.openSignInput(player, WarpManager.InputType.SHARE, stone);
                } else if (event.getSlot() == 6) { // Icon -> Selector GUI
                    menus.openIconSelector(player, stone);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.LODESTONE) {
            handleBreak(event, b.getLocation());
        } else if (b.getType() == Material.STONE_BRICK_WALL) {
            Block up = b.getRelative(0, 1, 0);
            if (up.getType() == Material.LODESTONE) handleBreak(event, up.getLocation());
        }
    }

    private void handleBreak(BlockBreakEvent event, Location stoneLoc) {
        WarpManager manager = module.getWarpManager();
        WarpStone stone = manager.getWarpStoneAt(stoneLoc);
        if (stone != null) {
            Player p = event.getPlayer();
            boolean isAdmin = p.hasPermission("rabi.warpstone.admin") && p.getGameMode() == GameMode.CREATIVE;
            boolean isOwner = stone.getOwner().equals(p.getUniqueId());

            if (!isAdmin && !isOwner) {
                event.setCancelled(true);
                manager.sendActionBar(p, ChatColor.RED + "你不能破坏这个传送石");
            } else {
                manager.getWarpStones().remove(stone.getName());
                manager.sendActionBar(p, ChatColor.GREEN + "传送石已移除");

                event.setDropItems(false);
                stoneLoc.getWorld().dropItemNaturally(stoneLoc, new ItemStack(Material.LODESTONE));
                stoneLoc.getBlock().setType(Material.AIR);

                Block down = stoneLoc.getBlock().getRelative(0, -1, 0);
                if (down.getType() == Material.STONE_BRICK_WALL) {
                    down.setType(Material.AIR);
                }
                module.getConfigManager().saveWarpStones(manager.getWarpStones());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        module.getWarpManager().getSignSessions().remove(event.getPlayer().getUniqueId());
    }
}