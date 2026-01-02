package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpiritManager {
    private final SpiritModule module;
    private final Map<UUID, Allay> activeEntities = new ConcurrentHashMap<>();
    private final Map<UUID, SpiritProfile> profiles = new ConcurrentHashMap<>();
    private final NamespacedKey spiritKey;
    private static final String TEAM_NAME = "ls_no_collision";

    public SpiritManager(SpiritModule module) {
        this.module = module;
        this.spiritKey = new NamespacedKey(module.getPlugin(), "lantern_spirit_entity");
        setupTeam();
    }

    public SpiritProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, k -> module.getConfigManager().loadProfile(k));
    }

    // 暴露缓存供 ConfigManager 遍历保存
    public Map<UUID, SpiritProfile> getLoadedProfiles() {
        return profiles;
    }

    public void killSpirit(UUID playerUUID) {
        Map<UUID, Allay> activeSpirits = getActiveSpirits();
        Allay spirit = activeSpirits.get(playerUUID);
        Player player = Bukkit.getPlayer(playerUUID);
        SpiritProfile profile = getProfile(playerUUID);

        // 1. 物理移除 (最简逻辑，杜绝粒子报错)
        if (spirit != null) {
            try {
                // 仅仅保留音效，音效绝不会报 Color 错误
                spirit.getWorld().playSound(spirit.getLocation(), Sound.ENTITY_ALLAY_DEATH, 1f, 1f);
                spirit.getWorld().playSound(spirit.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);

                // 物理移除
                spirit.remove();
            } catch (Exception e) {
                // 即使报错也继续执行
            }
            activeSpirits.remove(playerUUID);
        }

        // 2. 状态重置 (移出实体判定块，确保逻辑必定到达)
        if (profile != null) {
            // 设置重聚CD (10分钟)
            long cooldown = 600000L;
            profile.setReunionExpireTime(System.currentTimeMillis() + cooldown);
            profile.setSummoned(false);

            // 强制保存档案
            module.getConfigManager().saveProfile(profile);

            if (player != null && player.isOnline()) {
                player.sendMessage("§c[!] 灵灯的光芒黯淡了，它正在重聚灵魂…… (10分钟)。");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
            }
        }
    }

    public void summonSpirit(Player player) {
        if (activeEntities.containsKey(player.getUniqueId())) return;

        SpiritProfile profile = getProfile(player.getUniqueId());
        Location loc = player.getLocation().add(0, 1, 0);

        Allay spirit = player.getWorld().spawn(loc, Allay.class, s -> {
            // 更改默认昵称逻辑 --- 如果当前存储的名字是默认的“小精灵”，则显示为“玩家名的小精灵”
            String displayName = profile.getName();
            if ("小精灵".equals(displayName)) {
                displayName = player.getName() + "的小精灵";
            }
            s.setInvulnerable(true);
            s.setCollidable(true);
            s.setCanPickupItems(false); // 50级前关闭，逻辑控制
            s.setSilent(true);
            s.customName(Component.text(displayName, NamedTextColor.AQUA));
            s.setCustomNameVisible(true);
            s.getPersistentDataContainer().set(spiritKey, PersistentDataType.BYTE, (byte) 1);
            s.getInventory().clear();


            // 属性应用
            if (s.getAttribute(Attribute.MOVEMENT_SPEED) != null)
                s.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.15);
            s.getAttribute(Attribute.MAX_HEALTH).setBaseValue(profile.getMaxHealth());
            s.setHealth(Math.min(profile.getMaxHealth(), profile.getHealth()));
        });

        addEntityToTeam(spirit);
        activeEntities.put(player.getUniqueId(), spirit);

        // 应用幻化伪装 (如果安装了插件)
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
            try {
                SpiritDisguiseManager.updateDisguise(spirit, profile);
            } catch (Throwable t) {
                // 忽略可能的版本不兼容错误
            }
        }

        profile.setSummoned(true); // 标记为已召唤

        // 触发成就检查：初次召唤
        AchievementManager.check(player, profile, "first_summon");

        // 调整其他玩家的可见性
        handleSpiritSummonVisibility(spirit, player.getUniqueId());
    }

    public void despawnSpirit(UUID uuid) {
        Allay spirit = activeEntities.remove(uuid);
        if (spirit != null && spirit.isValid()) {
            SpiritProfile profile = getProfile(uuid);
            profile.setHealth(spirit.getHealth()); // 保存血量
            spirit.remove();
        }
        if (activeEntities.containsKey(uuid)) {
            SpiritProfile profile = getProfile(uuid);
            profile.setSummoned(false); // 标记为已收回（若是正常收回）
            // 注意：如果是服务器关闭导致的 despawn，不应设为 false。
            // 这需要在 onDisable 中特殊处理（只调用 despawnAll(true) 而不更改状态）
        }
    }

    public void reloadSpirits() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            restoreSpirit(player);
        }
    }

    public void restoreSpirit(Player player) {
        SpiritProfile profile = getProfile(player.getUniqueId());

        // 1. 检查是否处于激活状态 [cite: 4, 114]
        if (!profile.isSummoned()) return;

        // 2. 检查是否处于“灵魂重聚”冷却期 (针对 Lv.100 替死后的状态)
        // 假设你在 Profile 中存储了 lastDeathTime
//        long cooldownMillis = 30 * 60 * 1000; // 30分钟
//        if (System.currentTimeMillis() - profile.getLastDeathTime() < cooldownMillis) {
//            return;
//        }

        // 3. 执行召唤或传送
        Allay existing = activeEntities.get(player.getUniqueId());
        if (existing != null && existing.isValid()) {
            // 如果已存在，直接传送到身边
            existing.teleport(player.getLocation().add(0, 1, 0));
        } else {
            // 如果不存在（如刚登录或插件重载），重新生成一个
            summonSpirit(player);
        }
    }
    // 专门用于服务器关闭时的清理，不改变 summoned 状态，以便重启后自动召唤
    public void shutdown() {
        for (UUID uuid : activeEntities.keySet()) {
            Allay spirit = activeEntities.remove(uuid);
            if (spirit != null) {
                SpiritProfile profile = getProfile(uuid);
                profile.setHealth(spirit.getHealth());
                spirit.remove();
                // 这里故意不调用 profile.setSummoned(false)
            }
        }
    }

    public void despawnAll(boolean save) {
        for (UUID uuid : activeEntities.keySet()) {
            despawnSpirit(uuid);
        }
    }

    public Allay getSpiritEntity(UUID uuid) {
        return activeEntities.get(uuid);
    }

    public Map<UUID, Allay> getActiveSpirits() {
        return activeEntities;
    }

    public boolean isSpirit(org.bukkit.entity.Entity entity) {
        return entity instanceof Allay && entity.getPersistentDataContainer().has(spiritKey, PersistentDataType.BYTE);
    }

    private void setupTeam() {
        Scoreboard b = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = b.getTeam(TEAM_NAME);
        if (t == null) t = b.registerNewTeam(TEAM_NAME);
        t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    private void addEntityToTeam(Allay a) {
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM_NAME);
        if (t != null) t.addEntry(a.getUniqueId().toString());
    }

    /**
     * 更新小精灵的装备显示
     * 读取 Profile 中的 equipMap 并应用到实体装备栏
     */
    public void updateSpiritEquip(UUID ownerId) {
        Allay spirit = activeEntities.get(ownerId);

        // 确保实体存在且有效
        if (spirit != null && spirit.isValid()) {
            SpiritProfile profile = getProfile(ownerId);

            // 获取装备槽 (Slot 0) 的物品
            ItemStack handItem = profile.getEquipItem(0);

            // 更新实体主手 (如果为null则会自动清空)
            spirit.getEquipment().setItemInMainHand(handItem);

            // 如果有副手需求 (Slot 1) 也可以在这里扩展
            // spirit.getEquipment().setItemInOffHand(profile.getEquipItem(1));
        }
    }

    /**
     * 检查名字是否已被使用 (包括在线和离线数据)
     */
    public boolean isNameTaken(String name) {
        // 1. 检查在线玩家缓存
        for (SpiritProfile p : profiles.values()) {
            if (p.getName().equalsIgnoreCase(name)) return true;
        }

        // 2. 检查硬盘数据 (data.yml)
        org.bukkit.configuration.file.FileConfiguration data = module.getConfigManager().getDataConfig();
        if (data.contains("spirits")) {
            for (String key : data.getConfigurationSection("spirits").getKeys(false)) {
                String savedName = data.getString("spirits." + key + ".name");
                if (savedName != null && savedName.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }

    // 在 SpiritManager 类中添加以下方法

    /**
     * 刷新指定玩家对所有现有小精灵的可见性
     * (用于：玩家切换设置时、玩家刚上线时)
     * @param viewer 观察者玩家
     */
    public void refreshVisibilityForPlayer(Player viewer) {
        SpiritProfile viewerProfile = getProfile(viewer.getUniqueId());
        boolean hideOthers = viewerProfile.isHideOthers();

        for (Map.Entry<UUID, Allay> entry : activeEntities.entrySet()) {
            UUID ownerId = entry.getKey();
            Allay spirit = entry.getValue();

            // 永远能看到自己的
            if (ownerId.equals(viewer.getUniqueId())) {
                // 确保自己的可见 (以防万一)
                // 这里的 plugin 需要是主类实例
                // 如果 viewer.canSee(spirit) 返回 false 才操作，节省带宽
                viewer.showEntity(SpiritUtils.getPlugin(), spirit);
                continue;
            }

            // 处理其他人的小精灵
            if (hideOthers) {
                viewer.hideEntity(SpiritUtils.getPlugin(), spirit);
            } else {
                viewer.showEntity(SpiritUtils.getPlugin(), spirit);
            }
        }
    }

    /**
     * 当一个新的小精灵被召唤时，处理所有在线玩家对它的可见性
     * (用于：某人召唤了小精灵，需要判断谁应该看得到它)
     * @param spirit 新召唤的小精灵
     * @param ownerId 小精灵的主人ID
     */
    public void handleSpiritSummonVisibility(Allay spirit, UUID ownerId) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            // 主人自己一定看得到
            if (viewer.getUniqueId().equals(ownerId)) continue;

            SpiritProfile viewerProfile = getProfile(viewer.getUniqueId());
            // 如果旁观者开启了屏蔽他人，则对旁观者隐藏这个新实体
            if (viewerProfile.isHideOthers()) {
                viewer.hideEntity(SpiritUtils.getPlugin(), spirit);
            }
        }
    }
}