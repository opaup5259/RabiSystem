package cn.rabitown.rabisystem.modules.playtime;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayTimeConfigManager {

    private final PlayTimeModule module;
    private File dataFile;
    private FileConfiguration dataConfig;

    public PlayTimeConfigManager(PlayTimeModule module) {
        this.module = module;
        loadData();
    }

    private void loadData() {
        dataFile = new File(module.getPlugin().getDataFolder(), "playtime_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadPlayTimes(Map<UUID, Long> totalPlaytime) {
        totalPlaytime.clear();
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    totalPlaytime.put(UUID.fromString(uuidStr), dataConfig.getLong("players." + uuidStr));
                } catch (Exception e) {
                    // 忽略无效UUID
                }
            }
        }
    }

    public void savePlayTimes(Map<UUID, Long> totalPlaytime) {
        for (Map.Entry<UUID, Long> entry : totalPlaytime.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "保存 playtime_data.yml 失败", e);
        }
    }
}