package cn.rabitown.rabisystem.modules.whitelist.manager;

import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class WhitelistConfigManager {

    private final WhitelistModule module;
    private File dataFile;
    private FileConfiguration dataConfig;

    public WhitelistConfigManager(WhitelistModule module) {
        this.module = module;
        loadData();
    }

    private void loadData() {
        dataFile = new File(module.getPlugin().getDataFolder(), "whitelist_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadLists(Set<String> whitelist, Set<String> accepted) {
        whitelist.clear();
        accepted.clear();

        if (dataConfig.contains("whitelisted")) {
            whitelist.addAll(dataConfig.getStringList("whitelisted"));
        }
        if (dataConfig.contains("accepted-rules")) {
            accepted.addAll(dataConfig.getStringList("accepted-rules"));
        }
    }

    public void saveData(Set<String> whitelist, Set<String> accepted) {
        dataConfig.set("whitelisted", List.copyOf(whitelist));
        dataConfig.set("accepted-rules", List.copyOf(accepted));
        saveData();
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "无法保存 whitelist_data.yml", e);
        }
    }

    // 获取主配置
    public FileConfiguration getConfig() {
        return module.getPlugin().getConfig();
    }
}