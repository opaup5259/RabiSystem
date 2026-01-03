package cn.rabitown.rabisystem.modules.playtime;

import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayTimeListener implements Listener {

    private final PlayTimeModule module;

    public PlayTimeListener(PlayTimeModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        module.getManager().onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.getManager().onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("游戏时长排行榜")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        // Slot 4: 如果是铁门，则是返回按钮
        if (event.getSlot() == 4 && event.getCurrentItem().getType() == Material.IRON_DOOR) {
            // 返回小精灵 P2 (因为入口在 P2)
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
            SpiritMenus.openMainMenu(player, SpiritUtils.getSpiritManager().getProfile(player.getUniqueId()), 2);
            return;
        }
        try {
            String numStr = event.getView().getTitle().split(" ")[3];
            int currentPage = Integer.parseInt(numStr);

            if (event.getSlot() == 0 && event.getCurrentItem().getType() == Material.PAPER) {
                module.getManager().openLeaderboard(player, currentPage - 1);
            } else if (event.getSlot() == 8 && event.getCurrentItem().getType() == Material.PAPER) {
                module.getManager().openLeaderboard(player, currentPage + 1);
            }
        } catch (Exception ignored) {}
    }
}