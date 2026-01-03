package cn.rabitown.rabisystem.modules.laji.manager;

import cn.rabitown.rabisystem.modules.laji.LajiModule;
import cn.rabitown.rabisystem.modules.laji.data.TrashItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class LajiConfigManager {

    private final LajiModule module;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final long EXPIRE_MS = 7L * 24 * 60 * 60 * 1000; // 7天

    public LajiConfigManager(LajiModule module) {
        this.module = module;
        loadDataFile();
    }

    private void loadDataFile() {
        dataFile = new File(module.getPlugin().getDataFolder(), "laji_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getMainConfig() {
        return module.getPlugin().getConfig();
    }

    public void loadTrashItems(List<TrashItem> trashItems) {
        trashItems.clear();
        if (dataConfig.contains("items")) {
            List<?> list = dataConfig.getList("items");
            if (list != null) {
                long now = System.currentTimeMillis();
                for (Object obj : list) {
                    if (obj instanceof ItemStack item) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            Long timestamp = meta.getPersistentDataContainer().get(new NamespacedKey(module.getPlugin(), "drop_time"), PersistentDataType.LONG);
                            String owner = meta.getPersistentDataContainer().get(new NamespacedKey(module.getPlugin(), "owner_name"), PersistentDataType.STRING);
                            if (timestamp != null && owner != null) {
                                if (now - timestamp < EXPIRE_MS) {
                                    trashItems.add(new TrashItem(item, timestamp, owner));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void saveTrashItems(List<TrashItem> trashItems) {
        List<ItemStack> saveList = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (TrashItem trash : trashItems) {
            if (now - trash.getTimestamp() < EXPIRE_MS) {
                saveList.add(trash.getItem());
            }
        }
        dataConfig.set("items", saveList);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "无法保存 laji_data.yml", e);
        }
    }

    public void loadFilters(Set<Material> filterMaterials) {
        filterMaterials.clear();
        List<String> filters = getMainConfig().getStringList("modules.laji.filtered-items");
        for (String name : filters) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                filterMaterials.add(mat);
            } catch (IllegalArgumentException e) {
                // 忽略无效材质
            }
        }
    }
}
