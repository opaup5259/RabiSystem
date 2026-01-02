package cn.rabitown.rabisystem.modules.corpse.listener;

import cn.rabitown.rabisystem.modules.corpse.CorpseModule;
import cn.rabitown.rabisystem.modules.corpse.data.CorpseData;
import cn.rabitown.rabisystem.modules.corpse.manager.CorpseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class CorpseListener implements Listener {

    private final CorpseModule module;
    private final CorpseManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CorpseListener(CorpseModule module) {
        this.module = module;
        this.manager = module.getCorpseManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // [Config] 路径更新
        if (module.getConfigManager().getConfig().getBoolean("corpse.creative-bypass", true) && player.getGameMode() == GameMode.CREATIVE) return;

        Location loc = player.getLocation();
        boolean isVoidOrLava = loc.getY() < -64 || loc.getBlock().getType() == Material.LAVA || loc.getBlock().getType() == Material.LAVA_CAULDRON;

        if (isVoidOrLava) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            manager.getSpecialDeathPending().add(player.getUniqueId());
            // [Config] 路径更新
            player.sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.prefix") + module.getConfigManager().getConfig().getString("corpse.messages.special-death-bypass")));
            new BukkitRunnable() {
                // [Fix] 增加到 5 tick 延迟，防止客户端未处理完死亡包就强制复活导致卡死
                @Override public void run() { if (player.isOnline() && player.isDead()) player.spigot().respawn(); }
            }.runTaskLater(module.getPlugin(), 5L);
            return;
        }

        String originalGameMode = player.getGameMode().name();
        boolean isKeepInventory = event.getKeepInventory();

        List<ItemStack> mainItems = new ArrayList<>();
        List<ItemStack> armorItems = new ArrayList<>();
        ItemStack offhandItem = null;

        int collectedExp = calculateTotalExperience(player);
        int playerLevel = player.getLevel();

        if (isKeepInventory) {
            for (ItemStack i : player.getInventory().getStorageContents()) {
                if (i != null && i.getType() != Material.AIR) mainItems.add(i.clone());
            }
            for (ItemStack i : player.getInventory().getArmorContents()) {
                if (i != null && i.getType() != Material.AIR) armorItems.add(i.clone());
                else armorItems.add(new ItemStack(Material.AIR));
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.getType() != Material.AIR) offhandItem = off.clone();
            else offhandItem = new ItemStack(Material.AIR);

            player.getInventory().clear();
            player.setTotalExperience(0); player.setLevel(0); player.setExp(0);
            event.setKeepInventory(false); event.setKeepLevel(false); event.setDroppedExp(0);
        } else {
            for (ItemStack i : player.getInventory().getStorageContents()) {
                if (i != null && i.getType() != Material.AIR) mainItems.add(i.clone());
            }
            for (ItemStack i : player.getInventory().getArmorContents()) {
                if (i != null && i.getType() != Material.AIR) armorItems.add(i.clone());
                else armorItems.add(new ItemStack(Material.AIR));
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.getType() != Material.AIR) offhandItem = off.clone();
            else offhandItem = new ItemStack(Material.AIR);
            event.getDrops().clear(); event.setDroppedExp(0);
        }

        String causeStr = resolveDeathCause(player);
        manager.createCorpse(player, loc, mainItems, armorItems, offhandItem, collectedExp, originalGameMode, playerLevel, causeStr);

        manager.getGhosts().add(player.getUniqueId());
        new BukkitRunnable() {
            // [Fix] 增加到 5 tick 延迟，防止客户端未处理完死亡包就强制复活导致卡死
            @Override public void run() { if (player.isOnline() && player.isDead()) player.spigot().respawn(); }
        }.runTaskLater(module.getPlugin(), 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (manager.getSpecialDeathPending().contains(player.getUniqueId())) {
            manager.getSpecialDeathPending().remove(player.getUniqueId());
            new BukkitRunnable() { @Override public void run() { manager.restorePlayerStatus(player, false, player.getGameMode()); } }.runTaskLater(module.getPlugin(), 1L);
            return;
        }
        if (manager.getGhosts().contains(player.getUniqueId())) {
            player.setGameMode(GameMode.ADVENTURE); player.getInventory().clear();
            player.setAllowFlight(true); player.setFlying(true);
            manager.updateGhostPrefix(player, true);
            manager.playConfigSound(player, "corpse.sounds.ghost-bgm", true);
            Location corpseLoc = null;
            for (CorpseData d : manager.getCorpseCache().values()) {
                if (d.owner.equals(player.getUniqueId())) {
                    corpseLoc = new Location(player.getWorld(), d.locX, d.locY, d.locZ);
                    break;
                }
            }
            manager.showDeathTitle(player, corpseLoc);
            manager.sendUnifiedSoulMessage(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (manager.getGhosts().contains(event.getPlayer().getUniqueId())) {
            manager.updateGhostPrefix(event.getPlayer(), true);
            manager.playConfigSound(event.getPlayer(), "corpse.sounds.ghost-bgm", true);
        }
    }

    @EventHandler
    public void onChat(io.papermc.paper.event.player.AsyncChatEvent event) {
        if (manager.getGhosts().contains(event.getPlayer().getUniqueId())) {
            // [Config] 路径更新
            Component prefix = mm.deserialize(module.getConfigManager().getConfig().getString("corpse.ghost-prefix", "<gray>[<red>☠<gray>] "));
            event.renderer((source, sourceDisplayName, message, viewer) ->
                    prefix.append(sourceDisplayName).append(Component.text(": ")).append(message)
            );
        }
    }

    @EventHandler public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        if (manager.getGhosts().contains(player.getUniqueId())) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock.getType().name().contains("BED")) {
                event.setCancelled(true); manager.restorePlayerStatus(player, false, manager.getSavedGameMode(player)); return;
            }
        }

        Material type = clickedBlock.getType();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (type == Material.PLAYER_HEAD || type == Material.DECORATED_POT)) {
            Location loc = clickedBlock.getLocation();
            if (manager.getCorpseCache().containsKey(loc)) {
                event.setCancelled(true);

                CorpseData data = manager.getCorpseCache().get(loc);
                boolean isOwner = data.owner.equals(player.getUniqueId());
                long elapsed = System.currentTimeMillis() - data.timestamp;
                // [Config] 路径更新
                boolean isExpired = elapsed > (module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000);

                if (isOwner || isExpired || player.hasPermission("wowcorpse.admin")) {
                    if (!manager.canInventoryHold(player, data)) {
                        player.sendActionBar(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.inventory-full")));
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        return;
                    }
                    if (!isOwner) player.sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.protection-expired")));
                    manager.retrieveCorpseAction(player, loc, data, isOwner);
                } else {
                    long remaining = (module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000) - elapsed;
                    player.sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.not-owner").replace("<time>", (remaining/60000) + "分")));
                }
            }
        }
    }

    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        Location headLoc = event.getBlock().getLocation();
        if (manager.getCorpseCache().containsKey(headLoc)) {
            CorpseData data = manager.getCorpseCache().get(headLoc);
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                event.setCancelled(false);
                dropCorpseItemsNaturally(headLoc, data);
                if (data.exp > 0) ((ExperienceOrb)headLoc.getWorld().spawnEntity(headLoc, EntityType.EXPERIENCE_ORB)).setExperience(data.exp);
                manager.removeHologram(data, headLoc);
                manager.getCorpseCache().remove(headLoc);
                module.getConfigManager().saveCorpseData(manager.getCorpseCache(), manager.getGhosts());
                // [Config] 路径更新
                event.getPlayer().sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.admin-break"))); return;
            }
            boolean isOwner = data.owner.equals(event.getPlayer().getUniqueId());
            // [Config] 路径更新
            boolean isExpired = (System.currentTimeMillis() - data.timestamp) > (module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000);
            if (isOwner || isExpired) {
                event.setCancelled(true);
                if (!manager.canInventoryHold(event.getPlayer(), data)) { event.getPlayer().sendActionBar(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.inventory-full"))); return; }
                manager.retrieveCorpseAction(event.getPlayer(), headLoc, data, isOwner);
            } else { event.setCancelled(true); event.getPlayer().sendMessage(mm.deserialize("<red>受保护的尸体。")); }
        }
        if (manager.getGhosts().contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler public void onExplode(EntityExplodeEvent event) { event.blockList().removeIf(b -> manager.getCorpseCache().containsKey(b.getLocation())); }
    @EventHandler public void onBlockExplode(BlockExplodeEvent event) { event.blockList().removeIf(b -> manager.getCorpseCache().containsKey(b.getLocation())); }
    @EventHandler public void onBurn(BlockBurnEvent event) { if (manager.getCorpseCache().containsKey(event.getBlock().getLocation())) event.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageByEntityEvent event) { if (event.getDamager() instanceof Player p && manager.getGhosts().contains(p.getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onReceiveDamage(EntityDamageEvent event) { if (event.getEntity() instanceof Player p && manager.getGhosts().contains(p.getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onPickup(EntityPickupItemEvent event) { if (event.getEntity() instanceof Player p && manager.getGhosts().contains(p.getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onTarget(EntityTargetEvent event) { if (event.getTarget() instanceof Player p && manager.getGhosts().contains(p.getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onInventoryOpen(InventoryOpenEvent event) { if (manager.getGhosts().contains(event.getPlayer().getUniqueId())) event.setCancelled(true); }

    private String resolveDeathCause(Player player) {
        EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent == null) return "未知原因";
        EntityDamageEvent.DamageCause cause = damageEvent.getCause();
        if (damageEvent instanceof EntityDamageByEntityEvent entityEvent) {
            Entity killer = entityEvent.getDamager();
            if (killer instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) killer = shooter;
            String killerName;
            if (killer instanceof Player pKiller) killerName = pKiller.getName();
            else {
                Component entityNameComp = killer.customName();
                if (entityNameComp == null) entityNameComp = Component.translatable(killer.getType().translationKey());
                killerName = PlainTextComponentSerializer.plainText().serialize(entityNameComp);
            }
            return "被 " + killerName + " 杀死";
        }
        return switch (cause) {
            case KILL, SUICIDE -> "紫砂";
            case FALL -> "摔死"; case DROWNING -> "淹死"; case FIRE, FIRE_TICK -> "被烧死";
            case LAVA -> "试图在岩浆游泳"; case VOID -> "掉入虚空"; case STARVATION -> "饿死";
            case SUFFOCATION -> "窒息"; case MAGIC -> "被魔法杀死"; case POISON -> "中毒身亡";
            case WITHER -> "凋零致死"; case FALLING_BLOCK -> "被方块砸死"; case THORNS -> "被反伤刺死";
            case DRAGON_BREATH -> "被龙息烧死"; case FLY_INTO_WALL -> "撞墙而死"; case HOT_FLOOR -> "烫脚致死";
            case FREEZE -> "冻死"; case SONIC_BOOM -> "被监守者吼死";
            default -> "意外死亡 (" + cause.name() + ")";
        };
    }

    private int calculateTotalExperience(Player player) {
        int level = player.getLevel(); float progress = player.getExp(); int total = 0;
        for (int i = 0; i < level; i++) total += getExpAtLevel(i);
        total += Math.round(getExpAtLevel(level) * progress); return total;
    }
    private int getExpAtLevel(int level) {
        if (level <= 15) return 2 * level + 7; if (level <= 30) return 5 * level - 38; return 9 * level - 158;
    }
    private void dropCorpseItemsNaturally(Location loc, CorpseData data) {
        if (data.mainItems != null) for (ItemStack item : data.mainItems) if (item != null) loc.getWorld().dropItemNaturally(loc, item);
        if (data.armorItems != null) for (ItemStack item : data.armorItems) if (item != null) loc.getWorld().dropItemNaturally(loc, item);
        if (data.offhandItem != null) loc.getWorld().dropItemNaturally(loc, data.offhandItem);
    }
}