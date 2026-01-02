package cn.rabitown.rabisystem.modules.warpStone.manager;

import cn.rabitown.rabisystem.modules.warpStone.WarpStoneModule;
import cn.rabitown.rabisystem.modules.warpStone.data.WarpStone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class WarpConfigManager {

    private final WarpStoneModule module;
    private File dataFile;
    private FileConfiguration dataConfig;

    public WarpConfigManager(WarpStoneModule module) {
        this.module = module;
        loadDataFile();
    }

    private void loadDataFile() {
        dataFile = new File(module.getPlugin().getDataFolder(), "warp_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadWarpStones(Map<String, WarpStone> warpStones) {
        warpStones.clear();
        if (dataConfig.contains("stones")) {
            for (String key : dataConfig.getConfigurationSection("stones").getKeys(false)) {
                ConfigurationSection sec = dataConfig.getConfigurationSection("stones." + key);
                if (sec == null) continue;
                World world = Bukkit.getWorld(sec.getString("world", "world"));
                if (world == null) continue;
                Location loc = new Location(world, sec.getInt("x"), sec.getInt("y"), sec.getInt("z"));
                UUID owner = UUID.fromString(sec.getString("owner"));
                boolean isPublic = sec.getBoolean("public");
                long created = sec.getLong("created");
                List<String> whitelist = sec.getStringList("whitelist");
                String iconMatName = sec.getString("icon", "LODESTONE");
                Material icon = Material.getMaterial(iconMatName);
                if (icon == null) icon = Material.LODESTONE;

                WarpStone stone = new WarpStone(key, loc, owner, isPublic, created);
                stone.getWhitelist().addAll(whitelist.stream().map(UUID::fromString).toList());
                stone.setIcon(icon);
                warpStones.put(key, stone);
            }
        }
        module.getPlugin().getLogger().info("已加载 " + warpStones.size() + " 个传送石数据。");
    }

    public void saveWarpStones(Map<String, WarpStone> warpStones) {
        dataConfig.set("stones", null);
        for (WarpStone stone : warpStones.values()) {
            String path = "stones." + stone.getName();
            dataConfig.set(path + ".world", stone.getLocation().getWorld().getName());
            dataConfig.set(path + ".x", stone.getLocation().getBlockX());
            dataConfig.set(path + ".y", stone.getLocation().getBlockY());
            dataConfig.set(path + ".z", stone.getLocation().getBlockZ());
            dataConfig.set(path + ".owner", stone.getOwner().toString());
            dataConfig.set(path + ".public", stone.isPublic());
            dataConfig.set(path + ".created", stone.getCreated());
            dataConfig.set(path + ".whitelist", stone.getWhitelist().stream().map(UUID::toString).toList());
            dataConfig.set(path + ".icon", stone.getIcon().name());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "保存 warp_data.yml 失败", e);
        }
    }
}