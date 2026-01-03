package cn.rabitown.rabisystem.modules.fakeplayer.listener;

import cn.rabitown.rabisystem.modules.fakeplayer.FakePlayerModule;
import cn.rabitown.rabisystem.modules.fakeplayer.data.FakePlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class FakePlayerListener implements Listener {

    private final FakePlayerModule module;

    public FakePlayerListener(FakePlayerModule module) {
        this.module = module;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            String name = p.getName();
            if (name.startsWith("_fakeplayer_")) {
                String id = name.replace("_fakeplayer_", "");
                if (module.getManager().exists(id)) {
                    if (module.getManager().getData(id).isGodMode()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (p.getName().startsWith("_fakeplayer_")) {
                event.setCancelled(true); // 永不掉饥饿
                p.setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (p.getName().startsWith("_fakeplayer_")) {
                String id = p.getName().replace("_fakeplayer_", "");
                if (module.getManager().exists(id)) {
                    if (!module.getManager().getData(id).isPickup()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRightClickBot(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player bot) {
            if (bot.getName().startsWith("_fakeplayer_")) {
                String id = bot.getName().replace("_fakeplayer_", "");
                if (module.getManager().exists(id)) {
                    Player p = event.getPlayer();
                    FakePlayerData data = module.getManager().getData(id);
                    // 权限检查调整为 rabisystem.fakeplayer.admin
                    if (p.getUniqueId().equals(data.getOwner()) || p.hasPermission("rabisystem.fakeplayer.admin")) {
                        p.performCommand("rs fp " + id + " inv");
                    }
                }
            }
        }
    }
}