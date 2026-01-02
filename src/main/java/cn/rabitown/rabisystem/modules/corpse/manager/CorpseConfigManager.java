package cn.rabitown.rabisystem.modules.corpse.manager;

import cn.rabitown.rabisystem.modules.corpse.CorpseModule;
import cn.rabitown.rabisystem.modules.corpse.data.CorpseData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class CorpseConfigManager {

    private final CorpseModule module;
    // 不再持有独立的 configFile 和 config 对象，直接使用 plugin config
    private File dataFile;
    private FileConfiguration data;

    public CorpseConfigManager(CorpseModule module) {
        this.module = module;
        // Config 已由 RabiSystem 核心加载
        loadData();
    }

    private void loadData() {
        dataFile = new File(module.getPlugin().getDataFolder(), "corpse_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * 获取主配置文件 (RabiSystem/config.yml)
     */
    public FileConfiguration getConfig() {
        return module.getPlugin().getConfig();
    }

    public void reloadConfig() {
        module.getPlugin().reloadConfig();
        // data file reload
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadCorpseData(Map<Location, CorpseData> corpseCache, Set<UUID> ghosts) {
        corpseCache.clear();
        ghosts.clear();

        if (data.contains("corpses")) {
            if (data.getConfigurationSection("corpses") == null) return;
            for (String key : data.getConfigurationSection("corpses").getKeys(false)) {
                String path = "corpses." + key;
                try {
                    UUID owner = UUID.fromString(data.getString(path + ".owner"));
                    String ownerName = data.getString(path + ".ownerName", "Unknown");
                    World world = Bukkit.getWorld(data.getString(path + ".world"));
                    if (world == null) continue;
                    Location loc = new Location(world, data.getInt(path + ".x"), data.getInt(path + ".y"), data.getInt(path + ".z"));

                    List<ItemStack> main = new ArrayList<>();
                    List<?> rawMain = data.getList(path + ".items");
                    if (rawMain != null) { for (Object o : rawMain) if (o instanceof ItemStack) main.add((ItemStack) o); }

                    List<ItemStack> armor = new ArrayList<>();
                    List<?> rawArmor = data.getList(path + ".armor");
                    if (rawArmor != null) {
                        for (Object o : rawArmor) { if (o instanceof ItemStack) armor.add((ItemStack) o); else armor.add(new ItemStack(Material.AIR)); }
                    } else { armor.addAll(Arrays.asList(new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR))); }

                    ItemStack offhand = data.getItemStack(path + ".offhand");
                    if (offhand == null) offhand = new ItemStack(Material.AIR);

                    int exp = data.getInt(path + ".exp", 0);
                    String gm = data.getString(path + ".gamemode", "SURVIVAL");
                    String time = data.getString(path + ".time");
                    String cause = data.getString(path + ".cause");
                    long timestamp = data.getLong(path + ".timestamp", System.currentTimeMillis());
                    int level = data.getInt(path + ".level", 0);

                    CorpseData cd = new CorpseData(owner, ownerName, main, armor, offhand, exp, time, cause, gm, timestamp, level, loc.getX(), loc.getY(), loc.getZ());
                    cd.hologramUUID = null;
                    corpseCache.put(loc, cd);
                } catch (Exception ignored) {}
            }
        }
        if (data.contains("ghosts")) {
            List<String> ghostStrings = data.getStringList("ghosts");
            for (String s : ghostStrings) ghosts.add(UUID.fromString(s));
        }
    }

    public void saveCorpseData(Map<Location, CorpseData> corpseCache, Set<UUID> ghosts) {
        data.set("corpses", null);
        int i = 0;
        for (Map.Entry<Location, CorpseData> entry : corpseCache.entrySet()) {
            Location loc = entry.getKey();
            CorpseData cd = entry.getValue();
            String path = "corpses.c" + i;
            data.set(path + ".owner", cd.owner.toString());
            data.set(path + ".ownerName", cd.ownerName);
            data.set(path + ".world", loc.getWorld().getName());
            data.set(path + ".x", loc.getBlockX());
            data.set(path + ".y", loc.getBlockY());
            data.set(path + ".z", loc.getBlockZ());
            data.set(path + ".items", cd.mainItems);
            data.set(path + ".armor", cd.armorItems);
            data.set(path + ".offhand", cd.offhandItem);
            data.set(path + ".exp", cd.exp);
            data.set(path + ".gamemode", cd.gameMode);
            data.set(path + ".time", cd.deathTime);
            data.set(path + ".cause", cd.deathCause);
            data.set(path + ".timestamp", cd.timestamp);
            data.set(path + ".level", cd.level);
            if (cd.hologramUUID != null) data.set(path + ".holoUuid", cd.hologramUUID.toString());
            i++;
        }
        List<String> ghostStrings = new ArrayList<>();
        for (UUID uuid : ghosts) ghostStrings.add(uuid.toString());
        data.set("ghosts", ghostStrings);
        try { data.save(dataFile); } catch (IOException e) { module.getPlugin().getLogger().log(Level.SEVERE, "保存 corpse_data.yml 失败", e); }
    }
}