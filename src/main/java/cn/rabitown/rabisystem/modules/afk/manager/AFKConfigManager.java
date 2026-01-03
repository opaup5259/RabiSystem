package cn.rabitown.rabisystem.modules.afk.manager;

import cn.rabitown.rabisystem.modules.afk.AFKModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class AFKConfigManager {

    private final AFKModule module;
    private File dataFile;
    private FileConfiguration dataConfig;

    public AFKConfigManager(AFKModule module) {
        this.module = module;
        loadData();
    }

    private void loadData() {
        dataFile = new File(module.getPlugin().getDataFolder(), "afk_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getData() {
        return dataConfig;
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "保存 afk_data.yml 失败", e);
        }
    }

    public long getAutoAfkSeconds() {
        // 从主配置读取，默认600秒
        return module.getPlugin().getConfig().getLong("modules.afk.auto-afk-seconds", 600);
    }
}