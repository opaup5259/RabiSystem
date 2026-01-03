package cn.rabitown.rabisystem.modules.fakeplayer.manager;

import cn.rabitown.rabisystem.modules.fakeplayer.FakePlayerModule;
import cn.rabitown.rabisystem.modules.fakeplayer.data.FakePlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class FakePlayerConfigManager {
    private final FakePlayerModule module;
    private File file;
    private FileConfiguration data;

    public FakePlayerConfigManager(FakePlayerModule module) {
        this.module = module;
        this.file = new File(module.getPlugin().getDataFolder(), "fakeplayer_data.yml");
        if (!file.exists()) {
            try {
                if (file.getParentFile().mkdirs()) {
                    // ignore
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void saveData(Map<String, FakePlayerData> bots) {
        data.set("bots", null); // Clear old data
        for (FakePlayerData bot : bots.values()) {
            String path = "bots." + bot.getId();
            data.set(path + ".owner", bot.getOwner().toString());
            data.set(path + ".ownerName", bot.getOwnerName());
            data.set(path + ".world", bot.getLocation().getWorld().getName());
            data.set(path + ".x", bot.getLocation().getX());
            data.set(path + ".y", bot.getLocation().getY());
            data.set(path + ".z", bot.getLocation().getZ());
            data.set(path + ".yaw", bot.getLocation().getYaw());
            data.set(path + ".pitch", bot.getLocation().getPitch());
            data.set(path + ".skin", bot.getSkinName());
            data.set(path + ".god", bot.isGodMode());
            data.set(path + ".pickup", bot.isPickup());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "保存 fakeplayer_data.yml 失败", e);
        }
    }

    public Map<String, FakePlayerData> loadData() {
        Map<String, FakePlayerData> bots = new HashMap<>();
        if (data.getConfigurationSection("bots") == null) return bots;

        for (String key : data.getConfigurationSection("bots").getKeys(false)) {
            String path = "bots." + key;
            try {
                UUID owner = UUID.fromString(data.getString(path + ".owner"));
                String ownerName = data.getString(path + ".ownerName");
                String worldName = data.getString(path + ".world");
                if (Bukkit.getWorld(worldName) == null) continue; // Skip if world missing

                Location loc = new Location(
                        Bukkit.getWorld(worldName),
                        data.getDouble(path + ".x"),
                        data.getDouble(path + ".y"),
                        data.getDouble(path + ".z"),
                        (float) data.getDouble(path + ".yaw"),
                        (float) data.getDouble(path + ".pitch")
                );

                FakePlayerData bot = new FakePlayerData(key, owner, ownerName, loc);
                bot.setSkinName(data.getString(path + ".skin"));
                bot.setGodMode(data.getBoolean(path + ".god"));
                bot.setPickup(data.getBoolean(path + ".pickup"));
                bots.put(key, bot);
            } catch (Exception e) {
                module.getPlugin().getLogger().warning("加载假人数据 " + key + " 失败: " + e.getMessage());
            }
        }
        return bots;
    }
}