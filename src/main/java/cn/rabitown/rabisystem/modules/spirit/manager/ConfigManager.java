package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    private final SpiritModule module;
    private File dataFile;
    private FileConfiguration dataConfig;

    public ConfigManager(SpiritModule module) {
        this.module = module;
        loadDataFile();
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    private void loadDataFile() {
        dataFile = new File(module.getPlugin().getDataFolder(), "spirit_data.yml");
        if (!dataFile.exists()) {
            module.getPlugin().saveResource("spirit_data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveAllData() {
        Map<UUID, SpiritProfile> activeProfiles = module.getSpiritManager().getLoadedProfiles();
        for (SpiritProfile profile : activeProfiles.values()) {
            saveProfileToConfig(profile);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "无法保存 data.yml", e);
        }
    }

    public void saveProfile(SpiritProfile profile) {
        saveProfileToConfig(profile);
    }

    public void saveProfileToConfig(SpiritProfile profile) {
        String path = "spirits." + profile.getOwnerId().toString();

        dataConfig.set(path + ".totalSummonTime", profile.getTotalSummonTime());
        dataConfig.set(path + ".receivedFirstLantern", profile.hasReceivedFirstLantern());
        dataConfig.set(path + ".name", profile.getName());
        dataConfig.set(path + ".level", profile.getLevel());
        dataConfig.set(path + ".exp", profile.getExp());
        dataConfig.set(path + ".health", profile.getHealth());
        dataConfig.set(path + ".mood", profile.getMood());
        dataConfig.set(path + ".isSummoned", profile.isSummoned());
        dataConfig.set(path + ".autoEat", profile.isAutoEat());
        dataConfig.set(path + ".lastLoginDate", profile.getLastLoginDate());

        dataConfig.set(path + ".daily.pet", profile.getDailyPetExp());
        dataConfig.set(path + ".daily.companion", profile.getDailyCompanionExp());
        dataConfig.set(path + ".daily.social", profile.getDailySocialExp());
        dataConfig.set(path + ".daily.feed", profile.getDailyFeedExp());
        dataConfig.set(path + ".daily.heal", profile.getDailyHealExp());
        dataConfig.set(path + ".daily.buff", profile.getDailyBuffExp());
        dataConfig.set(path + ".daily.damage", profile.getDailyDamageExp());
        dataConfig.set(path + ".daily.signin", profile.getDailySignInExp()); // 现在这个方法存在了

        dataConfig.set(path + ".bonus.value", profile.getExtraExpBonus());
        dataConfig.set(path + ".bonus.card", profile.getExpBonusCard());

        dataConfig.set(path + ".backpack", Arrays.asList(profile.getBackpack()));
        dataConfig.set(path + ".foodBag", Arrays.asList(profile.getFoodBag()));

        dataConfig.set(path + ".gravity.enabled", profile.isVoidGravityEnabled());
        dataConfig.set(path + ".gravity.filterEnabled", profile.isFilterEnabled());
        dataConfig.set(path + ".gravity.whitelistMode", profile.isWhitelistMode());
        List<String> materialNames = profile.getFilterList().stream().map(Enum::name).toList();
        dataConfig.set(path + ".gravity.filterList", materialNames);

        dataConfig.set(path + ".effects.enabled", profile.isEffectsEnabled());
        dataConfig.set(path + ".effects.active-id", profile.getActiveEffect().getId());

        dataConfig.set(path + ".checkin.cards", profile.getReplacementCards());
        dataConfig.set(path + ".checkin.total", profile.getTotalCheckIns());
        dataConfig.set(path + ".checkin.consecutive", profile.getConsecutiveDays());
        dataConfig.set(path + ".checkin.last_millis", profile.getLastCheckInMillis());
        dataConfig.set(path + ".checkin.lottery", profile.getLotteryChances());
        dataConfig.set(path + ".checkin.holiday_cards", new ArrayList<>(profile.getReceivedHolidayCards()));

        List<String> historyStrings = new ArrayList<>();
        profile.getCheckInHistory().forEach((k, v) -> {
            historyStrings.add(k + ":" + v.toString().replace("[", "").replace("]", "").replace(" ", ""));
        });
        dataConfig.set(path + ".checkin.history", historyStrings);

        dataConfig.set(path + ".achievement.unlocked", new ArrayList<>(profile.getUnlockedAchievements()));
        dataConfig.set(path + ".achievement.claimed", new ArrayList<>(profile.getClaimedAchievements()));

        dataConfig.set(path + ".achievement.stats", null);
        ConfigurationSection statsSection = dataConfig.createSection(path + ".achievement.stats");
        Map<String, Integer> stats = profile.getStat();
        if (stats != null && !stats.isEmpty()) {
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                statsSection.set(entry.getKey(), entry.getValue());
            }
        }

        dataConfig.set(path + ".skins.unlocked", new ArrayList<>(profile.getUnlockedSkins()));
        dataConfig.set(path + ".skins.current", profile.getCurrentSkin());
        dataConfig.set(path + ".settings.hideOthers", profile.isHideOthers());
        dataConfig.set(path + ".effects.unlocked_list", new ArrayList<>(profile.getUnlockedEffects()));

        dataConfig.set(path + ".skills.extra_points", profile.getExtraSkillPoints());
        dataConfig.set(path + ".skills.unlocked", new ArrayList<>(profile.getUnlockedSkills()));
        dataConfig.set(path + ".skills.active", profile.getActiveSkillId());
        dataConfig.set(path + ".skills.quick", profile.getQuickSkillIds());

        dataConfig.set(path + ".virtual.furnace.items", profile.getVirtualFurnaceItems());
        dataConfig.set(path + ".virtual.furnace.cook", profile.getvFurnaceCookTime());
        dataConfig.set(path + ".virtual.furnace.fuel", profile.getvFurnaceFuelTime());
        dataConfig.set(path + ".virtual.furnace.max", profile.getvFurnaceMaxFuel());

        dataConfig.set(path + ".virtual.smoker.items", profile.getVirtualSmokerItems());
        dataConfig.set(path + ".virtual.smoker.cook", profile.getvSmokerCookTime());
        dataConfig.set(path + ".virtual.smoker.fuel", profile.getvSmokerFuelTime());
        dataConfig.set(path + ".virtual.smoker.max", profile.getvSmokerMaxFuel());

        dataConfig.set(path + ".virtual.blast.items", profile.getVirtualBlastItems());
        dataConfig.set(path + ".virtual.blast.cook", profile.getvBlastCookTime());
        dataConfig.set(path + ".virtual.blast.fuel", profile.getvBlastFuelTime());
        dataConfig.set(path + ".virtual.blast.max", profile.getvBlastMaxFuel());
    }

    public SpiritProfile loadProfile(UUID uuid) {
        String path = "spirits." + uuid.toString();
        SpiritProfile profile = new SpiritProfile(uuid, Bukkit.getOfflinePlayer(uuid).getName());

        if (!dataConfig.contains(path)) return profile;

        ConfigurationSection section = dataConfig.getConfigurationSection(path);
        if (section != null) {
            // 基础加载...
            profile.setTotalSummonTime(section.getLong("totalSummonTime", 0));
            profile.setReceivedFirstLantern(section.getBoolean("receivedFirstLantern", false));
            profile.setName(section.getString("name", "小精灵"));
            profile.setExp(section.getDouble("exp", 0.0));
            profile.setHealth(section.getDouble("health", 10.0));
            profile.setMood(section.getInt("mood", 70));
            profile.setSummoned(section.getBoolean("isSummoned", false));
            profile.setAutoEat(section.getBoolean("autoEat", true));
            profile.setLastLoginDate(section.getInt("lastLoginDate", 0));

            // 今日经验加载...
            profile.addDailyPetExp(section.getInt("daily.pet", 0));
            profile.addDailyCompanionExp(section.getInt("daily.companion", 0));
            profile.addDailySocialExp(section.getInt("daily.social", 0));
            profile.addDailyFeedExp(section.getInt("daily.feed", 0));
            profile.addDailyHealExp(section.getInt("daily.heal", 0));
            profile.addDailyBuffExp(section.getInt("daily.buff", 0));
            profile.addDailyDamageExp(section.getInt("daily.damage", 0));

            // [关键点] 这个调用现在安全了
            profile.addDailySignInExp(section.getInt("daily.signin", 0));

            profile.setExtraExpBonus(section.getDouble("bonus.value", 0.0));
            profile.setExpBonusCard(section.getString("bonus.card", "无"));

            // [关键点] 背包加载修复：使用修复后的 loadInventory，防止数组为空
            profile.setBackpack(loadInventory(section, "backpack", 54));
            profile.setFoodBag(loadInventory(section, "foodBag", 9));

            profile.setVoidGravityEnabled(section.getBoolean("gravity.enabled", true));
            profile.setFilterEnabled(section.getBoolean("gravity.filterEnabled", false));
            profile.setWhitelistMode(section.getBoolean("gravity.whitelistMode", false));
            List<String> names = section.getStringList("gravity.filterList");
            List<Material> materials = new ArrayList<>();
            for (String name : names) {
                try { materials.add(Material.valueOf(name)); } catch (IllegalArgumentException ignored) {}
            }
            profile.setFilterList(materials);

            migrateOldData(profile, section);

            profile.setReplacementCards(section.getInt("checkin.cards", 0));
            profile.setTotalCheckIns(section.getInt("checkin.total", 0));
            profile.setConsecutiveDays(section.getInt("checkin.consecutive", 0));
            profile.setLastCheckInMillis(section.getLong("checkin.last_millis", 0));
            profile.setLotteryChances(section.getInt("checkin.lottery", 0));
            List<String> holidayList = section.getStringList("checkin.holiday_cards");
            profile.setReceivedHolidayCards(new HashSet<>(holidayList));

            List<String> historyList = section.getStringList("checkin.history");
            for (String entry : historyList) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String monthKey = parts[0];
                    Set<Integer> days = new HashSet<>();
                    for (String dayStr : parts[1].split(",")) {
                        try {
                            if (!dayStr.isEmpty()) days.add(Integer.parseInt(dayStr));
                        } catch (NumberFormatException ignored) {}
                    }
                    profile.getCheckInHistory().put(monthKey, days);
                }
            }
            cleanupOldHistory(profile);

            List<String> unlockedList = section.getStringList("achievement.unlocked");
            if (unlockedList != null) {
                for(String s : unlockedList) profile.addUnlockedAchievement(s);
            }

            List<String> claimedList = section.getStringList("achievement.claimed");
            if (claimedList != null) {
                for(String s : claimedList) profile.setClaimed(s);
            }

            if (section.isConfigurationSection("achievement.stats")) {
                ConfigurationSection statsSection = section.getConfigurationSection("achievement.stats");
                for (String key : statsSection.getKeys(false)) {
                    int value = statsSection.getInt(key);
                    profile.getStat().put(key, value);
                }
            }

            List<String> skinList = section.getStringList("skins.unlocked");
            if (skinList != null) profile.setUnlockedSkins(new HashSet<>(skinList));
            profile.setCurrentSkin(section.getString("skins.current", "DEFAULT"));
            List<String> unlockedFx = section.getStringList("effects.unlocked_list");
            profile.setUnlockedEffects(new HashSet<>(unlockedFx));

            profile.setHideOthers(section.getBoolean("settings.hideOthers", false));

            profile.setExtraSkillPoints(section.getInt("skills.extra_points", 0));
            List<String> unlockedSkills = section.getStringList("skills.unlocked");
            if (unlockedSkills != null) profile.setUnlockedSkills(new HashSet<>(unlockedSkills));
            profile.setActiveSkillId(section.getString("skills.active", null));
            profile.setQuickSkillIds(section.getStringList("skills.quick"));

            // 使用辅助方法 loadInventory 读取固定长度3的数组
            profile.setVirtualFurnaceItems(loadInventory(section, "virtual.furnace.items", 3));
            profile.setvFurnaceCookTime(section.getInt("virtual.furnace.cook", 0));
            profile.setvFurnaceFuelTime(section.getInt("virtual.furnace.fuel", 0));
            profile.setvFurnaceMaxFuel(section.getInt("virtual.furnace.max", 0));

            profile.setVirtualSmokerItems(loadInventory(section, "virtual.smoker.items", 3));
            profile.setvSmokerCookTime(section.getInt("virtual.smoker.cook", 0));
            profile.setvSmokerFuelTime(section.getInt("virtual.smoker.fuel", 0));
            profile.setvSmokerMaxFuel(section.getInt("virtual.smoker.max", 0));

            profile.setVirtualBlastItems(loadInventory(section, "virtual.blast.items", 3));
            profile.setvBlastCookTime(section.getInt("virtual.blast.cook", 0));
            profile.setvBlastFuelTime(section.getInt("virtual.blast.fuel", 0));
            profile.setvBlastMaxFuel(section.getInt("virtual.blast.max", 0));
        }
        return profile;
    }

    private void cleanupOldHistory(SpiritProfile profile) {
        Calendar now = Calendar.getInstance();
        Set<String> keepKeys = new HashSet<>();
        for (int i = -3; i <= 3; i++) {
            Calendar temp = (Calendar) now.clone();
            temp.add(Calendar.MONTH, i);
            keepKeys.add(temp.get(Calendar.YEAR) + "-" + (temp.get(Calendar.MONTH) + 1));
        }
        profile.getCheckInHistory().keySet().removeIf(key -> !keepKeys.contains(key));
    }

    private void migrateOldData(SpiritProfile profile, ConfigurationSection userSection) {
        if (userSection.contains("effects")) {
            profile.setEffectsEnabled(userSection.getBoolean("effects.enabled", true));
            String effectId = userSection.getString("effects.active-id", "1");
            profile.setActiveEffect(SpiritEffectType.fromId(effectId));
        } else if (dataConfig.contains("effects")) {
            module.getPlugin().getLogger().info("检测到玩家 " + profile.getName() + " 的旧版特效数据，正在进行结构迁移...");
            boolean oldEnabled = dataConfig.getBoolean("effects.enabled", true);
            String oldId = dataConfig.getString("effects.active-id", "1");
            profile.setEffectsEnabled(oldEnabled);
            profile.setActiveEffect(SpiritEffectType.fromId(oldId));
        }
    }

    // --- 强制返回指定长度的数组 ---
    private ItemStack[] loadInventory(ConfigurationSection section, String key, int maxSize) {
        ItemStack[] items = new ItemStack[maxSize];
        if (!section.contains(key)) return items;

        List<?> list = section.getList(key);
        if (list == null) return items;

        for (int i = 0; i < Math.min(list.size(), maxSize); i++) {
            Object obj = list.get(i);
            if (obj instanceof ItemStack) {
                items[i] = (ItemStack) obj;
            } else {
                items[i] = null;
            }
        }
        return items;
    }
}