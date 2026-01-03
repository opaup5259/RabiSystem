package cn.rabitown.rabisystem.modules.afk.listener;

import cn.rabitown.rabisystem.modules.afk.AFKModule;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;

public class AFKListener implements Listener {

    private final AFKModule module;

    public AFKListener(AFKModule module) {
        this.module = module;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (e.getTo() == null) return;

        Location from = e.getFrom();
        Location to = e.getTo();

        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        double MOVE_THRESHOLD_SQUARED = 0.04;

        if (module.getManager().isAFK(p)) {
            if (distanceSquared > MOVE_THRESHOLD_SQUARED) {
                module.getManager().resetActivity(p);
            }
        } else {
            if (distanceSquared > 0) {
                module.getManager().updateLastActivity(p);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        module.getManager().resetActivity(e.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.getManager().resetActivity(e.getPlayer()));
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        if (msg.startsWith("/afk") || msg.startsWith("/moyu") || msg.startsWith("/rs afk")) return;
        module.getManager().resetActivity(e.getPlayer());
    }

    // --- 保护与限制 ---

    @EventHandler
    public void onTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player p && module.getManager().isAFK(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && module.getManager().isAFK(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && module.getManager().isAFK(p)) {
            e.setCancelled(true);
            p.sendMessage(Component.text("摸鱼模式下无法攻击！", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && module.getManager().isAFK(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onExpPickup(PlayerPickupExperienceEvent e) {
        if (module.getManager().isAFK(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        module.getManager().exitAFK(e.getPlayer());
        module.getManager().removeLastActivity(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        module.getManager().updateLastActivity(e.getPlayer());
    }
}