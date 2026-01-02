package cn.rabitown.rabisystem.modules.spirit.data;

import cn.rabitown.rabisystem.modules.spirit.skill.SkillType;
import cn.rabitown.rabisystem.modules.spirit.utils.LevelSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SpiritProfile {
    // 是否已领取首次羁绊提灯
    private boolean receivedFirstLantern = false;
    private final UUID ownerId;
    private String name;
    private int level;
    private double currentExp;
    private double currentHealth;
    private int mood;
    private ItemStack[] backpack;
    private ItemStack[] foodBag;
    private final Map<Integer, ItemStack> equipMap = new HashMap<>();

    // --- 瞬时变量 ---
    private transient long pauseUntil = 0;
    private boolean isSummoned;
    private transient long lastPetActionTime = 0;
    private transient long lastPetMoodTime = 0;
    private transient long lastNaturalMoodTime = System.currentTimeMillis();
    private transient long lastHealTime = 0;
    private boolean autoEat = true;
    private transient long lastNaturalRegenTime = System.currentTimeMillis();
    private transient long currentFeedCycleStart = 0;
    private transient double cycleHealAmount = 0;

    // --- 今日经验获取进度 ---
    private int dailyPetExp = 0;
    private int dailyCompanionExp = 0;
    private int dailySocialExp = 0;
    private int dailyFeedExp = 0;
    private int dailyHealExp = 0;
    private int dailyBuffExp = 0;
    private int dailyDamageExp = 0;
    private int dailySignInExp = 0; // [修复] 之前缺少的字段

    private int lastLoginDate = 0;
    private boolean voidGravityEnabled = true;
    private boolean filterEnabled = false;
    private boolean whitelistMode = false;
    private List<Material> filterList = new ArrayList<>();

    // --- 经验加成 ---
    private double extraExpBonus = 0;
    private String expBonusCard = "无";

    // --- 技能状态 ---
    private transient UUID targetItemId = null;
    private transient long lastResonanceTime = 0;
    private boolean resonanceEnabled = true;
    private boolean healBackEnabled = true;
    private boolean burstEnabled = true;
    private transient long lastBurstTime = 0;
    private transient boolean bursting = false;
    private UUID burstingTarget;
    private transient int companionTickCounter = 0;
    private long reunionExpireTime = 0;
    private boolean effectsEnabled = true;
    private SpiritEffectType activeEffect = SpiritEffectType.BOND;

    // --- 签到数据 ---
    private Map<String, Set<Integer>> checkInHistory = new HashMap<>();
    private int replacementCards = 0;
    private int totalCheckIns = 0;
    private int consecutiveDays = 0;
    private long lastCheckInMillis = 0;
    private int lotteryChances = 0;
    private Set<String> receivedHolidayCards = new HashSet<>();

    private Set<String> unlockedEffects = new HashSet<>();
    // --- 成就数据 ---
    private Set<String> unlockedAchievements = new HashSet<>();
    private final Set<String> claimedAchievements = new HashSet<>();
    private Map<String, Integer> statistics = new HashMap<>();
    private long totalSummonTime = 0; // 累计召唤时长 (秒)
    private boolean hideOthers = false; // 默认不屏蔽其他玩家的小精灵

    private boolean soulCompensateEnabled = true;

    // --- 🦋 幻化外观数据 ---
    private Set<String> unlockedSkins = new HashSet<>();
    private String currentSkin = "DEFAULT";

    // --- 技能树系统 ---
    private int extraSkillPoints = 0;      // 当前可用点数
    private double currentMana = 100.0;
    private double maxMana = 100.0;

    // 存储已解锁技能ID
    private Set<String> unlockedSkills = new HashSet<>();

    // 装备的技能
    private String activeSkillId = null; // 主动技能 (Slot 33)
    private List<String> quickSkillIds = new ArrayList<>(); // 快捷技能 (Slot 34, 35)

    // --- 虚拟容器数据 (Virtual Inventory Data) ---
    // 存储格式: ItemStack[]
    private ItemStack[] virtualFurnaceItems = new ItemStack[3]; // 0:Input, 1:Fuel, 2:Output
    private int vFurnaceCookTime = 0;
    private int vFurnaceFuelTime = 0;
    private int vFurnaceMaxFuel = 0;

    private ItemStack[] virtualSmokerItems = new ItemStack[3];
    private int vSmokerCookTime = 0;
    private int vSmokerFuelTime = 0;
    private int vSmokerMaxFuel = 0;

    private ItemStack[] virtualBlastItems = new ItemStack[3];
    private int vBlastCookTime = 0;
    private int vBlastFuelTime = 0;
    private int vBlastMaxFuel = 0;
    // --- 瞬时状态 (Transient) ---
    private transient boolean spiritWalking = false;
    // 记录小精灵上一次进入“忙碌/施法/硬直”状态的时间戳
    private long lastBusyTime = 0;

    public SpiritProfile(UUID ownerId, String playerName) {
        this.ownerId = ownerId;
        this.name = playerName + "的小精灵";
        this.level = 1;
        this.currentExp = 0;
        this.currentHealth = 10.0;
        this.mood = 70;
        this.backpack = new ItemStack[9];
        this.foodBag = new ItemStack[1];
        this.isSummoned = false;
        this.activeEffect = SpiritEffectType.NONE;
    }

    // --- 基础 Getter / Setter ---
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public double getExp() { return currentExp; }
    public void setExp(double exp) {
        this.currentExp = exp;
        this.level = LevelSystem.calculateLevel(this.currentExp);
    }
    /**
     * 增加经验并自动计算等级
     * 增加升级时的成就检查逻辑
     */
    public void addExp(double amount) {
        // 1. 记录增加前的状态
        int oldLevel = this.level;
        double oldExp = this.currentExp;

        // 获取达到 100 级所需的总经验基准线
        double expAt100 = LevelSystem.getTotalExpToReachLevel(100);

        // 2. 执行经验增加
        this.currentExp += amount;
        this.level = LevelSystem.calculateLevel(this.currentExp);

        // 3. 逻辑一：处理等级提升带来的奖励 (100级及以下)
        // 只有当新等级比旧等级高时才计算
        if (this.level > oldLevel) {
            // 计算有效升级层数（最高只算到100级）
            int effectiveNewLevel = Math.min(this.level, 100);
            int effectiveOldLevel = Math.min(oldLevel, 100);

            int gainedLevels = effectiveNewLevel - effectiveOldLevel;
            if (gainedLevels > 0) {
                // 每升一级获得 1 次抽奖机会
                this.lotteryChances += gainedLevels;
            }
        }

        // 4. 逻辑二：处理 100 级后的溢出经验奖励 (每 2500 经验)
        // 只有当当前经验超过 100 级基准线时才计算
        if (this.currentExp > expAt100) {
            // 计算旧经验在 100 级基准线之上的部分（如果是负数则视为0）
            double effectiveOldExp = Math.max(oldExp, expAt100);

            // 计算之前拥有多少个 2500 块
            int oldChunks = (int) ((effectiveOldExp - expAt100) / 2500.0);

            // 计算现在拥有多少个 2500 块
            int newChunks = (int) ((this.currentExp - expAt100) / 2500.0);

            int gainedChunks = newChunks - oldChunks;
            if (gainedChunks > 0) {
                this.lotteryChances += gainedChunks;
            }
        }
    }

    public double getHealth() { return Math.min(currentHealth, getMaxHealth()); }
    public void setHealth(double health) { this.currentHealth = health; }

    public int getMood() { return mood; }
    public void setMood(int mood) { this.mood = Math.max(0, Math.min(100, mood)); }
    public void addMood(int amount) { setMood(this.mood + amount); }

    public ItemStack[] getBackpack() { return backpack; }
    public void setBackpack(ItemStack[] backpack) { this.backpack = backpack; }

    public ItemStack[] getFoodBag() { return foodBag; }
    public void setFoodBag(ItemStack[] foodBag) { this.foodBag = foodBag; }

    public boolean isSummoned() { return isSummoned; }
    public void setSummoned(boolean summoned) { isSummoned = summoned; }

    public ItemStack getEquipItem(int slotId) { return equipMap.get(slotId); }
    public void setEquipItem(int slotId, ItemStack item) {
        if (item == null) equipMap.remove(slotId);
        else equipMap.put(slotId, item);
    }

    // --- 计时器与状态 ---
    public long getPauseUntil() { return pauseUntil; }
    public void setPauseUntil(long timestamp) { this.pauseUntil = timestamp; }
    public long getLastPetActionTime() { return lastPetActionTime; }
    public void setLastPetActionTime(long time) { this.lastPetActionTime = time; }
    public long getLastPetMoodTime() { return lastPetMoodTime; }
    public void setLastPetMoodTime(long time) { this.lastPetMoodTime = time; }
    public long getLastNaturalMoodTime() { return lastNaturalMoodTime; }
    public void setLastNaturalMoodTime(long time) { this.lastNaturalMoodTime = time; }
    public long getLastHealTime() { return lastHealTime; }
    public void setLastHealTime(long lastHealTime) { this.lastHealTime = lastHealTime; }
    public boolean isAutoEat() { return autoEat; }
    public void setAutoEat(boolean autoEat) { this.autoEat = autoEat; }
    public long getLastNaturalRegenTime() { return lastNaturalRegenTime; }
    public void setLastNaturalRegenTime(long now) { this.lastNaturalRegenTime = now; }
    public long getCurrentFeedCycleStart() { return currentFeedCycleStart; }
    public void setCurrentFeedCycleStart(long time) { this.currentFeedCycleStart = time; }
    public double getCycleHealAmount() { return cycleHealAmount; }
    public void setCycleHealAmount(double amount) { this.cycleHealAmount = amount; }
    public void addCycleHealAmount(double amount) { this.cycleHealAmount += amount; }

    // --- 今日经验 (Daily Exp) ---
    public int getDailyPetExp() { return dailyPetExp; }
    public void addDailyPetExp(int val) { this.dailyPetExp += val; }
    public int getDailyCompanionExp() { return dailyCompanionExp; }
    public void addDailyCompanionExp(int val) { this.dailyCompanionExp += val; }
    public int getDailySocialExp() { return dailySocialExp; }
    public void addDailySocialExp(int val) { this.dailySocialExp += val; }
    public int getDailyFeedExp() { return dailyFeedExp; }
    public void addDailyFeedExp(int val) { this.dailyFeedExp += val; }
    public int getDailyHealExp() { return dailyHealExp; }
    public void addDailyHealExp(int val) { this.dailyHealExp += val; }
    public void setDailyHealExp(int val) { this.dailyHealExp = val; }
    public int getDailyBuffExp() { return dailyBuffExp; }
    public void addDailyBuffExp(int val) { this.dailyBuffExp += val; }
    public int getDailyDamageExp() { return dailyDamageExp; }
    public void addDailyDamageExp(int val) { this.dailyDamageExp += val; }

    // [核心修复] 之前缺少的签到经验方法，导致读取中断
    public int getDailySignInExp() { return dailySignInExp; }
    public void addDailySignInExp(int val) { this.dailySignInExp += val; }

    public void resetDailyProgress() {
        this.dailyPetExp = 0;
        this.dailyCompanionExp = 0;
        this.dailySocialExp = 0;
        this.dailyFeedExp = 0;
        this.dailyHealExp = 0;
        this.dailyBuffExp = 0;
        this.dailyDamageExp = 0;
        this.dailySignInExp = 0;
    }

    // --- 加成与杂项 ---
    public double getExtraExpBonus() { return extraExpBonus; }
    public void setExtraExpBonus(double val) { this.extraExpBonus = val; }
    public String getExpBonusCard() { return expBonusCard; }
    public void setExpBonusCard(String card) { this.expBonusCard = card; }
    public int getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(int date) { this.lastLoginDate = date; }

    public double getMaxHealth() {
        return LevelSystem.getMaxHealth(this.level);
    }
    public int getBackpackSize() {
        return LevelSystem.getBackpackSize(this.level);
    }
    public long getRegenCooldownMillis() {
        return LevelSystem.getNaturalRegenInterval(this.level);
    }
    // --- 虚空引力 ---
    public double getPickupRange() {
        return LevelSystem.getPickupRange(this.level);
    }
    public boolean isVoidGravityEnabled() { return voidGravityEnabled; }
    public void setVoidGravityEnabled(boolean enabled) { this.voidGravityEnabled = enabled; }
    public boolean isFilterEnabled() { return filterEnabled; }
    public void setFilterEnabled(boolean enabled) { this.filterEnabled = enabled; }
    public boolean isWhitelistMode() { return whitelistMode; }
    public void setWhitelistMode(boolean whitelistMode) { this.whitelistMode = whitelistMode; }
    public List<Material> getFilterList() { return filterList; }
    public void setFilterList(List<Material> list) { this.filterList = list; }
    public void addFilterItem(Material mat) { if (!filterList.contains(mat)) filterList.add(mat); }
    public void removeFilterItem(int index) { if (index >= 0 && index < filterList.size()) filterList.remove(index); }
    public UUID getTargetItemId() { return targetItemId; }
    public void setTargetItemId(UUID id) { this.targetItemId = id; }

    // --- 技能与战斗 ---
    public long getLastResonanceTime() { return lastResonanceTime; }
    public void setLastResonanceTime(long time) { this.lastResonanceTime = time; }
    public boolean isResonanceEnabled() { return resonanceEnabled; }
    public void setResonanceEnabled(boolean enabled) { this.resonanceEnabled = enabled; }
    public boolean isHealBackEnabled() { return healBackEnabled; }
    public void setHealBackEnabled(boolean enabled) { this.healBackEnabled = enabled; }
    public boolean isBurstEnabled() { return burstEnabled; }
    public void setBurstEnabled(boolean burstEnabled) { this.burstEnabled = burstEnabled; }
    public long getLastBurstTime() { return lastBurstTime; }
    public void setLastBurstTime(long lastBurstTime) { this.lastBurstTime = lastBurstTime; }
    public boolean isBursting() { return bursting; }
    public void setBursting(boolean bursting) { this.bursting = bursting; }
    public UUID getBurstingTarget() { return burstingTarget; }
    public void setBurstingTarget(UUID burstingTarget) { this.burstingTarget = burstingTarget; }
    public boolean isSoulCompensateEnabled() { return soulCompensateEnabled; }
    public void setSoulCompensateEnabled(boolean b) { this.soulCompensateEnabled = b; }

    public int getCompanionTickCounter() { return companionTickCounter; }
    public void setCompanionTickCounter(int count) { this.companionTickCounter = count; }
    public long getReunionExpireTime() { return reunionExpireTime; }
    public void setReunionExpireTime(long time) { this.reunionExpireTime = time; }
    public boolean isReuniting() { return System.currentTimeMillis() < reunionExpireTime; }

    // --- 特效 ---
    public boolean isEffectsEnabled() { return effectsEnabled; }
    public void setEffectsEnabled(boolean effectsEnabled) { this.effectsEnabled = effectsEnabled; }
    public SpiritEffectType getActiveEffect() { return activeEffect; }
    public void setActiveEffect(SpiritEffectType activeEffect) { this.activeEffect = activeEffect; }

    // --- 签到系统 ---
    public Map<String, Set<Integer>> getCheckInHistory() { return checkInHistory; }
    public int getReplacementCards() { return replacementCards; }
    public void setReplacementCards(int val) { this.replacementCards = val; }
    public void addReplacementCards(int amount) { this.replacementCards += amount; }
    public int getTotalCheckIns() { return totalCheckIns; }
    public void setTotalCheckIns(int val) { this.totalCheckIns = val; }
    public int getConsecutiveDays() { return consecutiveDays; }
    public void setConsecutiveDays(int consecutiveDays) { this.consecutiveDays = consecutiveDays; }
    public long getLastCheckInMillis() { return lastCheckInMillis; }
    public void setLastCheckInMillis(long lastCheckInMillis) { this.lastCheckInMillis = lastCheckInMillis; }
    public int getLotteryChances() { return lotteryChances; }
    public void setLotteryChances(int lotteryChances) { this.lotteryChances = lotteryChances; }
    public void addLotteryChances(int amount) { this.lotteryChances += amount; }
    public Set<String> getReceivedHolidayCards() {
        if (receivedHolidayCards == null) receivedHolidayCards = new HashSet<>();
        return receivedHolidayCards;
    }
    public void setReceivedHolidayCards(Set<String> receivedHolidayCards) { this.receivedHolidayCards = receivedHolidayCards; }

    // --- 成就系统 (核心修复：补全空逻辑) ---
    public Set<String> getUnlockedAchievements() { return unlockedAchievements; }

    // [修复] 原代码此处为空，导致成就加载后无法写入对象
    public void addUnlockedAchievement(String unlockedAchievement) {
        this.unlockedAchievements.add(unlockedAchievement);
    }
    public void setUnlockedAchievements(Set<String> unlockedAchievements) {
        this.unlockedAchievements = unlockedAchievements;
    }

    public Set<String> getClaimedAchievements() { return claimedAchievements; }
    public boolean isClaimed(String achievementId) { return claimedAchievements.contains(achievementId); }
    public void setClaimed(String achievementId) { claimedAchievements.add(achievementId); }

    public void addStat(String key, int amount) {
        statistics.put(key, statistics.getOrDefault(key, 0) + amount);
    }
    public void setStat(String key, Integer amount) { statistics.put(key, amount); }
    public Map<String, Integer> getStat() { return statistics; }
    public int getStat(String key) { return statistics.getOrDefault(key, 0); }
    public boolean hasReceivedFirstLantern() { return receivedFirstLantern; }
    public void setReceivedFirstLantern(boolean received) { this.receivedFirstLantern = received; }
    public long getTotalSummonTime() { return totalSummonTime; }
    public void setTotalSummonTime(long time) { this.totalSummonTime = time; }
    public void addSummonTime(long seconds) { this.totalSummonTime += seconds; }
    public Set<String> getUnlockedSkins() { return unlockedSkins; }
    public void setUnlockedSkins(Set<String> s) { this.unlockedSkins = s; }
    public void addUnlockedSkin(String s) { this.unlockedSkins.add(s); }
    public boolean hasAnySkin() { return !unlockedSkins.isEmpty(); }

    public String getCurrentSkin() { return currentSkin; }
    public void setCurrentSkin(String s) { this.currentSkin = s; }

    /**
     * 智能判断某个皮肤是否已解锁
     * 逻辑：默认皮肤 || 在额外列表中 || 关联的成就已达成
     */
    public boolean isSkinUnlocked(String skinId) {
        // 1. 默认皮肤永远解锁
        if ("DEFAULT".equals(skinId)) return true;

        // 2. 检查是否在额外解锁列表中 (兼容指令给予)
        if (unlockedSkins.contains(skinId)) return true;

        // 3. 【重点】检查关联成就是否已达成
        // 从枚举中查找对应的皮肤定义
        SpiritSkin skin = SpiritSkin.fromId(skinId);
        if (skin != SpiritSkin.DEFAULT && skin.getRequiredAchievement() != null) {
            return this.getUnlockedAchievements().contains(skin.getRequiredAchievement().getId());
        }

        return false;
    }

    /**
     * 判断是否拥有任意一个非默认的皮肤 (用于主菜单入口显示)
     */
    public boolean hasAnyUnlockedSkin() {
        // 只要有任意一个非默认皮肤解锁了，就返回 true
        for (SpiritSkin skin : SpiritSkin.values()) {
            if (skin == SpiritSkin.DEFAULT) continue;
            if (isSkinUnlocked(skin.getId())) return true;
        }
        return false;
    }

    public boolean isHideOthers() { return hideOthers; }
    public void setHideOthers(boolean hideOthers) { this.hideOthers = hideOthers; }

    // 判定特效是否解锁 (等级达标 OR 额外解锁)
    public boolean isEffectUnlocked(SpiritEffectType type) {
        // 1. 如果是抽奖限定特效，必须在已解锁列表中才算拥有
        if (type.getSource() == SpiritEffectType.EffectSource.LOTTERY) {
            return unlockedEffects.contains(type.getId());
        }

        // 2. 如果是默认特效，永远解锁
        if (type.getSource() == SpiritEffectType.EffectSource.DEFAULT) {
            return true;
        }

        // 3. 如果是等级特效，等级达标 OR 额外解锁（双重判定）
        return this.level >= type.getRequiredLevel() || unlockedEffects.contains(type.getId());
    }

    public Set<String> getUnlockedEffects() {
        return unlockedEffects;
    }

    public void setUnlockedEffects(Set<String> unlockedEffects) {
        this.unlockedEffects = unlockedEffects;
    }

    public void addUnlockedEffect(String effectId) {
        this.unlockedEffects.add(effectId);
    }
    // 获取总技能点数 (等级 + 额外)
    public int getTotalSkillPoints() {
        return LevelSystem.getLevelSkillPoints(this.level) + this.extraSkillPoints;
    }
    // 获取已使用的技能点数 (已解锁技能数量)
    public int getSpentSkillPoints() {
        return unlockedSkills.size();
    }
    // 获取当前剩余可用技能点
    public int getAvailableSkillPoints() {
        return getTotalSkillPoints() - getSpentSkillPoints();
    }

    // 获取特定系别的已使用点数
    public int getSpentPointsByTree(String treeType) {
        int count = 0;
        for (String id : unlockedSkills) {
            SkillType skill = SkillType.fromId(id);
            if (skill != null && skill.getTreeType().equals(treeType)) {
                count++;
            }
        }
        return count;
    }

    public double getMana() { return currentMana; }
    public void setMana(double mana) { this.currentMana = Math.min(mana, getMaxMana()); }
    public void addMana(double amount) { setMana(this.currentMana + amount); }
    public double getMaxMana() { return maxMana; } // 后续可根据被动提升

    public Set<String> getUnlockedSkills() { return unlockedSkills; }
    public void setUnlockedSkills(Set<String> skills) { this.unlockedSkills = skills; }
    public void unlockSkill(String skillId) { this.unlockedSkills.add(skillId); }
    public boolean isSkillUnlocked(String skillId) { return unlockedSkills.contains(skillId); }

    public String getActiveSkillId() { return activeSkillId; }
    public void setActiveSkillId(String id) { this.activeSkillId = id; }

    public List<String> getQuickSkillIds() { return quickSkillIds; }
    public void setQuickSkillIds(List<String> ids) { this.quickSkillIds = ids; }

    public int getExtraSkillPoints() { return extraSkillPoints; }
    public void setExtraSkillPoints(int points) { this.extraSkillPoints = points; }
    public void addExtraSkillPoints(int amount) { this.extraSkillPoints += amount; }
    // 重置技能树
    public void resetSkills() {
        this.unlockedSkills.clear();
        this.activeSkillId = null;
        this.quickSkillIds.clear();
    }

    public ItemStack[] getVirtualFurnaceItems() { return virtualFurnaceItems; }
    public void setVirtualFurnaceItems(ItemStack[] items) { this.virtualFurnaceItems = items; }
    public int getvFurnaceCookTime() { return vFurnaceCookTime; }
    public void setvFurnaceCookTime(int t) { this.vFurnaceCookTime = t; }
    public int getvFurnaceFuelTime() { return vFurnaceFuelTime; }
    public void setvFurnaceFuelTime(int t) { this.vFurnaceFuelTime = t; }
    public int getvFurnaceMaxFuel() { return vFurnaceMaxFuel; }
    public void setvFurnaceMaxFuel(int t) { this.vFurnaceMaxFuel = t; }

    public ItemStack[] getVirtualSmokerItems() { return virtualSmokerItems; }
    public void setVirtualSmokerItems(ItemStack[] items) { this.virtualSmokerItems = items; }
    public int getvSmokerCookTime() { return vSmokerCookTime; }
    public void setvSmokerCookTime(int t) { this.vSmokerCookTime = t; }
    public int getvSmokerFuelTime() { return vSmokerFuelTime; }
    public void setvSmokerFuelTime(int t) { this.vSmokerFuelTime = t; }
    public int getvSmokerMaxFuel() { return vSmokerMaxFuel; }
    public void setvSmokerMaxFuel(int t) { this.vSmokerMaxFuel = t; }

    public ItemStack[] getVirtualBlastItems() { return virtualBlastItems; }
    public void setVirtualBlastItems(ItemStack[] items) { this.virtualBlastItems = items; }
    public int getvBlastCookTime() { return vBlastCookTime; }
    public void setvBlastCookTime(int t) { this.vBlastCookTime = t; }
    public int getvBlastFuelTime() { return vBlastFuelTime; }
    public void setvBlastFuelTime(int t) { this.vBlastFuelTime = t; }
    public int getvBlastMaxFuel() { return vBlastMaxFuel; }
    public void setvBlastMaxFuel(int t) { this.vBlastMaxFuel = t; }

    public boolean isSpiritWalking() { return spiritWalking; }
    public void setSpiritWalking(boolean spiritWalking) { this.spiritWalking = spiritWalking; }

    // 辅助判断是否装备了某个技能 (用于Task判断是否运行熔炉)
    public boolean isSkillEquipped(String skillId) {
        return (activeSkillId != null && activeSkillId.equals(skillId)) || quickSkillIds.contains(skillId);
    }

    public void markBusy() {
        this.lastBusyTime = System.currentTimeMillis();
    }

    public long getLastBusyTime() {
        return lastBusyTime;
    }
}