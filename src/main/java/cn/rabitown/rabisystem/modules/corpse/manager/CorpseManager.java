package cn.rabitown.rabisystem.modules.corpse.manager;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.corpse.CorpseModule;
import cn.rabitown.rabisystem.modules.corpse.data.CorpseData;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CorpseManager {

    private final CorpseModule module;
    private final Set<UUID> ghosts = new HashSet<>();
    private final Set<UUID> specialDeathPending = new HashSet<>();
    private final Set<UUID> teleportingPlayers = new HashSet<>();
    private final Map<Location, CorpseData> corpseCache = new HashMap<>();
    private final NamespacedKey holoKey;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private boolean hasTabPlugin = false;

    // Tasks
    private BukkitTask ghostTask;
    private BukkitTask hologramTask;
    private BukkitTask actionbarTask;

    public CorpseManager(CorpseModule module) {
        this.module = module;
        this.holoKey = new NamespacedKey(module.getPlugin(), "is_corpse_holo");
        if (Bukkit.getPluginManager().getPlugin("TAB") != null) {
            hasTabPlugin = true;
        }

        module.getConfigManager().loadCorpseData(corpseCache, ghosts);

        for (UUID uuid : ghosts) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) updateGhostPrefix(p, true);
        }
    }

    public void startTasks() {
        ghostTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : ghosts) {
                    if (teleportingPlayers.contains(uuid)) continue;
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, false, false));

                        // [Config] 路径更新
                        if(module.getConfigManager().getConfig().getBoolean("corpse.ghost-visuals.night-vision", true))
                            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, false, false));

                        p.setFreezeTicks(module.getConfigManager().getConfig().getBoolean("corpse.ghost-visuals.freezing-overlay", true) ? 150 : 0);
                        if(module.getConfigManager().getConfig().getBoolean("corpse.ghost-visuals.gloomy-weather", true))
                            p.setPlayerWeather(WeatherType.DOWNFALL);
                        p.setPlayerTime(18000, false);

                        p.setAllowFlight(true);
                        p.setFlying(true);
                        p.setFireTicks(0);

                        playConfigSound(p, "corpse.sounds.ghost-heartbeat", false);
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 20L);

        hologramTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                // [Config] 路径更新
                long protectTimeMillis = module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000;

                for (Map.Entry<Location, CorpseData> entry : corpseCache.entrySet()) {
                    Location loc = entry.getKey();
                    CorpseData data = entry.getValue();
                    if (data.hologramUUID == null || Bukkit.getEntity(data.hologramUUID) == null) {
                        if (loc.getChunk().isLoaded()) {
                            createHologram(loc, data);
                        }
                    } else {
                        Entity entity = Bukkit.getEntity(data.hologramUUID);
                        if (entity instanceof TextDisplay display) {
                            updateHologramText(display, data, now, protectTimeMillis);
                        }
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 20L);

        actionbarTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : ghosts) {
                    if (teleportingPlayers.contains(uuid)) continue;
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        sendGhostNavigation(p);
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 5L);
    }

    public void stopTasks() {
        if (ghostTask != null) ghostTask.cancel();
        if (hologramTask != null) hologramTask.cancel();
        if (actionbarTask != null) actionbarTask.cancel();

        module.getConfigManager().saveCorpseData(corpseCache, ghosts);
        purgeAllHolograms();

        for (UUID uuid : ghosts) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setFreezeTicks(0);
                p.resetPlayerWeather();
                p.resetPlayerTime();
                updateGhostPrefix(p, false);
                stopConfigSound(p, "corpse.sounds.ghost-bgm");
            }
        }
    }

    public void createCorpse(Player player, Location loc, List<ItemStack> main, List<ItemStack> armor, ItemStack offhand, int exp, String gm, int level, String causeStr) {
        Block headBlock = loc.getBlock();
        if (headBlock.getType().isSolid()) headBlock = loc.add(0, 1, 0).getBlock();

        boolean isWater = headBlock.getType() == Material.WATER;
        if (isWater) {
            headBlock.setType(Material.DECORATED_POT);
            if (headBlock.getBlockData() instanceof org.bukkit.block.data.Waterlogged wl) {
                wl.setWaterlogged(true);
                headBlock.setBlockData(wl);
            }
        } else {
            headBlock.setType(Material.PLAYER_HEAD);
            if (headBlock.getState() instanceof Skull skull) {
                skull.setOwningPlayer(player);
                skull.update();
            }
        }

        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        CorpseData data = new CorpseData(
                player.getUniqueId(), player.getName(), main, armor, offhand, exp,
                timeStr, causeStr, gm, System.currentTimeMillis(), level,
                loc.getX(), loc.getY(), loc.getZ()
        );

        createHologram(headBlock.getLocation(), data);
        corpseCache.put(headBlock.getLocation(), data);
        module.getConfigManager().saveCorpseData(corpseCache, ghosts);
    }

    public void retrieveCorpseAction(Player player, Location loc, CorpseData data, boolean isSelf) {
        if (ghosts.contains(player.getUniqueId()) && isSelf) {
            GameMode gm = GameMode.SURVIVAL;
            try { gm = GameMode.valueOf(data.gameMode); } catch (Exception ignored) {}
            restorePlayerStatus(player, true, gm);
        }

        if (data.armorItems == null) data.armorItems = new ArrayList<>();
        if (data.mainItems == null) data.mainItems = new ArrayList<>();

        ItemStack[] currentArmor = player.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            if (i < data.armorItems.size()) {
                ItemStack ca = data.armorItems.get(i);
                if (ca != null && ca.getType() != Material.AIR) {
                    if (currentArmor[i] == null || currentArmor[i].getType() == Material.AIR) currentArmor[i] = ca;
                    else player.getInventory().addItem(ca);
                }
            }
        }
        player.getInventory().setArmorContents(currentArmor);
        if (data.offhandItem != null && data.offhandItem.getType() != Material.AIR) {
            if (player.getInventory().getItemInOffHand().getType() == Material.AIR) player.getInventory().setItemInOffHand(data.offhandItem);
            else player.getInventory().addItem(data.offhandItem);
        }
        for (ItemStack item : data.mainItems) {
            if (item != null && item.getType() != Material.AIR) player.getInventory().addItem(item);
        }
        if (data.exp > 0) player.giveExp(data.exp);

        loc.getBlock().setType(Material.AIR);
        removeHologram(data, loc);
        corpseCache.remove(loc);
        module.getConfigManager().saveCorpseData(corpseCache, ghosts);

        playConfigSound(player, "corpse.sounds.corpse-pickup", false);
        player.sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.corpse-found")));
        if (!isSelf) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    public void adminRetrieveCorpse(Player admin, Location loc) {
        if (!corpseCache.containsKey(loc)) return;
        CorpseData data = corpseCache.get(loc);

        if (data.armorItems != null) {
            for (ItemStack is : data.armorItems) {
                if (is != null && is.getType() != Material.AIR) admin.getInventory().addItem(is);
            }
        }
        if (data.offhandItem != null && data.offhandItem.getType() != Material.AIR) {
            admin.getInventory().addItem(data.offhandItem);
        }
        if (data.mainItems != null) {
            for (ItemStack is : data.mainItems) {
                if (is != null && is.getType() != Material.AIR) admin.getInventory().addItem(is);
            }
        }

        loc.getBlock().setType(Material.AIR);
        removeHologram(data, loc);
        corpseCache.remove(loc);
        module.getConfigManager().saveCorpseData(corpseCache, ghosts);

        admin.sendMessage(Component.text("已强制拾取 " + data.ownerName + " 的尸体。", NamedTextColor.GREEN));
        admin.playSound(admin.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1f, 1f);
    }

    public void restorePlayerStatus(Player player, boolean isStrongRevive, GameMode targetMode) {
        ghosts.remove(player.getUniqueId());
        updateGhostPrefix(player, false);
        stopConfigSound(player, "corpse.sounds.ghost-bgm");
        playConfigSound(player, "corpse.sounds.revive", false);
        player.setGameMode(targetMode);
        player.setAllowFlight(false); player.setFlying(false);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        player.setSaturation(0f); player.setFreezeTicks(0);
        player.resetPlayerWeather(); player.resetPlayerTime();

        double health; int food;
        FileConfiguration cfg = module.getConfigManager().getConfig();
        // [Config] 路径更新
        if (isStrongRevive) {
            health = cfg.getDouble("corpse.revive-stats.corpse-retrieval.health", 10.0);
            food = cfg.getInt("corpse.revive-stats.corpse-retrieval.food", 8);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 7 * 20, 255, false, false));
            if (cfg.getBoolean("corpse.visuals.enable-revive-distortion", true)) { player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false)); }
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
            Title title = Title.title(Component.text("灵魂归位！", NamedTextColor.GREEN), Component.text("获得复活保护", NamedTextColor.YELLOW), times);
            player.showTitle(title);
        } else {
            health = cfg.getDouble("corpse.revive-stats.bed-revive.health", 6.0);
            food = cfg.getInt("corpse.revive-stats.bed-revive.food", 6);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120 * 20, 0, false, false));
            player.sendActionBar(Component.text("虚弱复活...", NamedTextColor.GRAY));
            if (cfg.getBoolean("corpse.visuals.enable-bed-wakeup", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, false));
                Location loc = player.getLocation(); loc.setPitch(90); player.teleport(loc);
                new BukkitRunnable() { float p = 90; @Override public void run() { if (!player.isOnline() || p <= 0) { this.cancel(); return; } p -= 10; Location l = player.getLocation(); l.setPitch(p); player.teleport(l); } }.runTaskTimer(module.getPlugin(), 0L, 1L);
            }
        }
        player.setHealth(Math.min(health, player.getMaxHealth())); player.setFoodLevel(food);
        module.getConfigManager().saveCorpseData(corpseCache, ghosts);
        boolean hasCorpse = false;
        for (CorpseData d : corpseCache.values()) { if (d.owner.equals(player.getUniqueId())) { hasCorpse = true; break; } }
        if (hasCorpse) player.sendMessage(mm.deserialize(cfg.getString("corpse.messages.revive-reminder")));
    }

    public void removeHologram(CorpseData data, Location loc) {
        if (data.hologramUUID != null) {
            Entity entity = Bukkit.getEntity(data.hologramUUID);
            if (entity != null) entity.remove();
        }
        if (loc != null && loc.getWorld() != null) {
            for (Entity e : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                if (e instanceof TextDisplay && e.getPersistentDataContainer().has(holoKey, PersistentDataType.BYTE)) {
                    e.remove();
                }
            }
        }
    }

    public void createHologram(Location baseLoc, CorpseData data) {
        Location holoLoc = baseLoc.clone().add(0.5, 1.2, 0.5);
        TextDisplay display = baseLoc.getWorld().spawn(holoLoc, TextDisplay.class);
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
        display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
        display.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(0.8f);
        display.setTransformation(transformation);
        data.hologramUUID = display.getUniqueId();
        // [Config] 路径更新
        updateHologramText(display, data, System.currentTimeMillis(), module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000);
    }

    private void updateHologramText(TextDisplay display, CorpseData data, long now, long protectTimeMillis) {
        long elapsed = now - data.timestamp;
        long remaining = protectTimeMillis - elapsed;
        Component protectionLine = remaining > 0 ? Component.text(String.format("保护剩余: %02d:%02d", remaining / 60000, (remaining % 60000) / 1000), NamedTextColor.GREEN) : Component.text("已失去保护", NamedTextColor.RED);
        display.text(Component.text().append(Component.text("[Lv." + data.level + "] ", NamedTextColor.AQUA)).append(Component.text(data.ownerName + " 的尸体", NamedTextColor.GOLD)).append(Component.newline()).append(Component.text("死因: " + data.deathCause, NamedTextColor.GRAY)).append(Component.newline()).append(protectionLine).build());
    }

    public int purgeAllHolograms() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof TextDisplay && e.getPersistentDataContainer().has(holoKey, PersistentDataType.BYTE)) {
                    e.remove(); count++;
                }
            }
        }
        return count;
    }

    public void playConfigSound(Player player, String path, boolean global) {
        String soundName = module.getConfigManager().getConfig().getString(path);
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float vol = global ? Float.MAX_VALUE : (float) module.getConfigManager().getConfig().getDouble(path + "-volume", 1.0);
            float pitch = (float) module.getConfigManager().getConfig().getDouble(path + "-pitch", 1.0);
            player.playSound(player.getLocation(), sound, vol, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    public void stopConfigSound(Player player, String path) {
        String soundName = module.getConfigManager().getConfig().getString(path);
        if (soundName == null || soundName.isEmpty()) return;
        try { player.stopSound(Sound.valueOf(soundName.toUpperCase())); } catch (IllegalArgumentException ignored) {}
    }

    public void updateGhostPrefix(Player player, boolean isGhost) {
        // [Config] 路径更新
        Component prefixComp = mm.deserialize(module.getConfigManager().getConfig().getString("corpse.ghost-prefix", "<gray>[<red>☠<gray>] "));
        if (hasTabPlugin) {
            try {
                TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
                if (tabPlayer != null) {
                    if (isGhost) {
                        String legacyPrefix = LegacyComponentSerializer.legacySection().serialize(prefixComp);
                        TabAPI.getInstance().getTabListFormatManager().setPrefix(tabPlayer, legacyPrefix);
                    } else {
                        TabAPI.getInstance().getTabListFormatManager().setPrefix(tabPlayer, null);
                    }
                }
            } catch (Exception ignored) {}
            return;
        }
        if (isGhost) player.playerListName(prefixComp.append(player.name()));
        else player.playerListName(player.name());
    }

    public void sendUnifiedSoulMessage(Player player) {
        List<CorpseData> userCorpses = new ArrayList<>();
        for (CorpseData d : corpseCache.values()) { if (d.owner.equals(player.getUniqueId())) { userCorpses.add(d); } }
        if (userCorpses.isEmpty()) { player.sendMessage(Component.text("你当前没有未拾取的尸体。", NamedTextColor.GRAY)); return; }

        FileConfiguration cfg = module.getConfigManager().getConfig();
        // [Config] 路径更新
        Component header = mm.deserialize(cfg.getString("corpse.messages.soul-status.header"));
        Component footer = mm.deserialize(cfg.getString("corpse.messages.soul-status.footer"));
        player.sendMessage(header);
        boolean isGhost = ghosts.contains(player.getUniqueId());
        if (isGhost) { player.sendMessage(mm.deserialize(cfg.getString("corpse.messages.soul-status.line1"))); }
        else { player.sendMessage(Component.text("尸体列表详情：", NamedTextColor.GOLD)); }
        String line2Template = cfg.getString("corpse.messages.soul-status.line2");
        String tpButton = cfg.getString("corpse.messages.soul-status.line2-tp", "");
        for (CorpseData data : userCorpses) {
            Component line = mm.deserialize(line2Template.replace("<x>", String.valueOf((int)data.locX)).replace("<y>", String.valueOf((int)data.locY)).replace("<z>", String.valueOf((int)data.locZ)));
            if (isGhost) line = line.append(mm.deserialize(tpButton));
            player.sendMessage(line);
        }
        if (isGhost) {
            Component line3 = (player.getRespawnLocation() != null) ? mm.deserialize(cfg.getString("corpse.messages.soul-status.line3-bed")) : mm.deserialize(cfg.getString("corpse.messages.soul-status.line3-nobed"));
            player.sendMessage(line3);
        }
        player.sendMessage(footer);
    }

    public void showDeathTitle(Player player, Location loc) {
        // [Config] 路径更新
        String subtitleRaw = module.getConfigManager().getConfig().getString("corpse.messages.death-subtitle", "<yellow>坐标: <x>, <y>, <z>");
        if (loc != null) {
            subtitleRaw = subtitleRaw.replace("<x>", String.valueOf(loc.getBlockX())).replace("<y>", String.valueOf(loc.getBlockY())).replace("<z>", String.valueOf(loc.getBlockZ()));
        } else {
            subtitleRaw = subtitleRaw.replace("<x>", "?").replace("<y>", "?").replace("<z>", "?");
        }
        Title title = Title.title(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.death-title")), mm.deserialize(subtitleRaw), Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000)));
        player.showTitle(title);
    }

    private void sendGhostNavigation(Player player) {
        CorpseData data = null;
        for (CorpseData d : corpseCache.values()) {
            if (d.owner.equals(player.getUniqueId())) {
                if (data == null || player.getLocation().distanceSquared(new Location(player.getWorld(), d.locX, d.locY, d.locZ)) < player.getLocation().distanceSquared(new Location(player.getWorld(), data.locX, data.locY, data.locZ))) {
                    data = d;
                }
            }
        }
        if (data == null) return;
        Location target = new Location(player.getWorld(), data.locX, data.locY, data.locZ);
        if (!player.getWorld().getUID().equals(target.getWorld().getUID())) return;
        double distance = player.getLocation().distance(target);
        String arrow = getDirectionArrow(player, target);
        // [Config] 路径更新
        String msg = module.getConfigManager().getConfig().getString("corpse.messages.ghost-actionbar").replace("<distance>", String.format("%.1f", distance)).replace("<arrow>", arrow);
        player.sendActionBar(mm.deserialize(msg));
    }

    private String getDirectionArrow(Player player, Location target) {
        Location pLoc = player.getLocation();
        Vector pDir = pLoc.getDirection().setY(0).normalize();
        Vector targetDir = target.toVector().subtract(pLoc.toVector()).setY(0).normalize();
        float angle = (float) Math.toDegrees(Math.atan2(targetDir.getZ(), targetDir.getX()) - Math.atan2(pDir.getZ(), pDir.getX()));
        if (angle < 0) angle += 360;
        if (angle >= 337.5 || angle < 22.5) return "⬆"; if (angle >= 22.5 && angle < 67.5) return "➡"; if (angle >= 67.5 && angle < 112.5) return "➡";
        if (angle >= 112.5 && angle < 157.5) return "⬇"; if (angle >= 157.5 && angle < 202.5) return "⬇"; if (angle >= 202.5 && angle < 247.5) return "⬇";
        if (angle >= 247.5 && angle < 292.5) return "⬅"; if (angle >= 292.5 && angle < 337.5) return "⬅"; return "⬆";
    }

    public void teleportNearCorpse(Player player) {
        // [Config] 路径更新
        if (!module.getConfigManager().getConfig().getBoolean("corpse.visuals.enable-gta-teleport", true)) { instantTeleportNearCorpse(player); return; }
        CorpseData nearest = null; double minDst = Double.MAX_VALUE;
        for (CorpseData d : corpseCache.values()) {
            if (d.owner.equals(player.getUniqueId())) {
                World w = Bukkit.getWorld(d.hologramUUID != null ? Bukkit.getEntity(d.hologramUUID).getWorld().getUID() : player.getWorld().getUID());
                if (w != null && w.getName().equals(player.getWorld().getName())) {
                    double dst = player.getLocation().distanceSquared(new Location(w, d.locX, d.locY, d.locZ));
                    if (dst < minDst) { minDst = dst; nearest = d; }
                }
            }
        }
        if (nearest == null) { player.sendMessage(Component.text("未找到可传送的尸体 (可能在其他世界)", NamedTextColor.RED)); return; }
        World world = player.getWorld();
        int radius = module.getConfigManager().getConfig().getInt("corpse.tp-radius", 350);
        int offsetX = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int offsetZ = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int targetX = (int)nearest.locX + offsetX; int targetZ = (int)nearest.locZ + offsetZ;
        int targetY = world.getHighestBlockYAt(targetX, targetZ) + 1;
        if (targetY < -64) targetY = 64;
        Location targetLoc = new Location(world, targetX, targetY, targetZ);
        startGtaAnimation(player, targetLoc);
    }

    private void instantTeleportNearCorpse(Player player) {
        CorpseData data = null;
        for (CorpseData d : corpseCache.values()) { if (d.owner.equals(player.getUniqueId())) { data = d; break; } }
        if (data == null) return;
        World world = Bukkit.getWorld(data.hologramUUID != null ? Bukkit.getEntity(data.hologramUUID).getWorld().getUID() : player.getWorld().getUID());
        if (world == null) return;
        // [Config] 路径更新
        int radius = module.getConfigManager().getConfig().getInt("corpse.tp-radius", 350);
        int offsetX = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int offsetZ = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int targetX = (int)data.locX + offsetX; int targetZ = (int)data.locZ + offsetZ;
        int targetY = world.getHighestBlockYAt(targetX, targetZ) + 1;
        if (targetY < -64) targetY = 64;
        Location tpLoc = new Location(world, targetX, targetY, targetZ);
        player.teleport(tpLoc);
        player.sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.tp-success", "<green>已传送。")));
    }

    private void startGtaAnimation(Player player, Location targetLoc) {
        teleportingPlayers.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        // [Config] 路径更新
        int liftHeight = module.getConfigManager().getConfig().getInt("corpse.visuals.gta-height", 50);
        double ascendSpeed = 1.5; double descendSpeed = 2.5; float pitchSpeed = 5.0f;
        new BukkitRunnable() {
            int phase = 0; Location current = player.getLocation();
            @Override public void run() {
                if (!player.isOnline()) { teleportingPlayers.remove(player.getUniqueId()); this.cancel(); return; }
                if (phase == 0) {
                    if (current.getY() < player.getWorld().getHighestBlockYAt(current) + liftHeight) {
                        current.add(0, ascendSpeed, 0);
                        float p = current.getPitch();
                        if (p < 90) { p = Math.min(90, p + pitchSpeed); current.setPitch(p); }
                        player.teleport(current);
                    } else { phase = 1; }
                } else if (phase == 1) {
                    current.setX(targetLoc.getX()); current.setZ(targetLoc.getZ()); current.setPitch(90);
                    player.teleport(current); phase = 2;
                } else if (phase == 2) {
                    if (current.getY() > targetLoc.getY()) {
                        current.subtract(0, descendSpeed, 0);
                        if (current.getY() < targetLoc.getY()) current.setY(targetLoc.getY());
                        current.setPitch(90);
                        player.teleport(current);
                    } else {
                        player.teleport(targetLoc); player.setGameMode(GameMode.ADVENTURE);
                        teleportingPlayers.remove(player.getUniqueId());
                        // [Config] 路径更新
                        player.sendMessage(mm.deserialize(module.getConfigManager().getConfig().getString("corpse.messages.tp-success", "<green>已传送。")));
                        new BukkitRunnable() {
                            float p = 90;
                            @Override public void run() {
                                if (!player.isOnline() || p <= 0) { this.cancel(); return; }
                                p -= 8; if (p < 0) p = 0;
                                Location l = player.getLocation(); l.setPitch(p); player.teleport(l);
                            }
                        }.runTaskTimer(module.getPlugin(), 0L, 1L);
                        this.cancel();
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 1L);
    }

    public GameMode getSavedGameMode(Player player) {
        for (CorpseData data : corpseCache.values()) { if (data.owner.equals(player.getUniqueId())) { try { return GameMode.valueOf(data.gameMode); } catch (Exception e) { return GameMode.SURVIVAL; } } }
        return GameMode.SURVIVAL;
    }

    public boolean canInventoryHold(Player player, CorpseData data) {
        int emptySlots = 0;
        for (ItemStack i : player.getInventory().getStorageContents()) if (i == null || i.getType() == Material.AIR) emptySlots++;
        int neededSlots = (data.mainItems != null) ? data.mainItems.size() : 0;
        ItemStack[] currentArmor = player.getInventory().getArmorContents();
        if (data.armorItems != null) {
            for (int i = 0; i < 4; i++) {
                if (i < data.armorItems.size()) {
                    ItemStack ca = data.armorItems.get(i);
                    if (ca != null && ca.getType() != Material.AIR && currentArmor[i] != null && currentArmor[i].getType() != Material.AIR) neededSlots++;
                }
            }
        }
        if (data.offhandItem != null && data.offhandItem.getType() != Material.AIR && player.getInventory().getItemInOffHand().getType() != Material.AIR) neededSlots++;
        return emptySlots >= neededSlots;
    }

    public Set<UUID> getGhosts() { return ghosts; }
    public Set<UUID> getSpecialDeathPending() { return specialDeathPending; }
    public Map<Location, CorpseData> getCorpseCache() { return corpseCache; }
}