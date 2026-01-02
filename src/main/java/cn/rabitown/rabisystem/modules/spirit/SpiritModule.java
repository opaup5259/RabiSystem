// File: src/main/java/cn/rabitown/rabisystem/modules/spirit/SpiritModule.java
package cn.rabitown.rabisystem.modules.spirit;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.spirit.manager.ConfigManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SpiritManager;
import cn.rabitown.rabisystem.modules.spirit.listener.*;
import cn.rabitown.rabisystem.modules.spirit.task.*;
import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import org.bukkit.event.HandlerList;

public class SpiritModule implements IRabiModule {

    private SpiritManager spiritManager;
    private ConfigManager configManager;
    private final RabiSystem plugin = RabiSystem.getInstance();

    // 缓存引用以便注销
    private PlayerStateListener playerStateListener;
    private SpiritInteractListener spiritInteractListener;
    private MenuListener menuListener;
    private SpiritCombatListener spiritCombatListener;
    private SpiritDefenseListener spiritDefenseListener;
    private BreedingListener breedingListener;

    private CompanionTask companionTask;
    private SpiritAppTask spiritAppTask;
    private SpiritBehaviorTask spiritBehaviorTask;

    @Override
    public String getModuleId() {
        return "spirit";
    }

    @Override
    public String getDisplayName() {
        return "灯火灵契 (Lantern Spirit Covenant)";
    }

    @Override
    public boolean isEnabled() {
        // 交给 ModuleManager 判断
        return RabiSystem.getModuleManager().isModuleEnabled(getModuleId());
    }

    @Override
    public void onEnable() {
        // 1. 初始化管理器
        this.configManager = new ConfigManager(this);
        this.spiritManager = new SpiritManager(this);

        // 2. 注册监听器
        this.playerStateListener = new PlayerStateListener();
        this.spiritInteractListener = new SpiritInteractListener();
        this.menuListener = new MenuListener();
        this.spiritCombatListener = new SpiritCombatListener();
        this.spiritDefenseListener = new SpiritDefenseListener();
        this.breedingListener = new BreedingListener();

        plugin.getServer().getPluginManager().registerEvents(playerStateListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(spiritInteractListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(menuListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(spiritCombatListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(spiritDefenseListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(breedingListener, plugin);

        // 3. 启动任务
        this.companionTask = new CompanionTask();
        this.companionTask.runTaskTimer(plugin, 400L, 400L);
        this.spiritAppTask = new SpiritAppTask();
        this.spiritAppTask.runTaskTimer(plugin, 0L, 5L);
        this.spiritBehaviorTask = new SpiritBehaviorTask();
        this.spiritBehaviorTask.runTaskTimer(plugin, 0L, 5L);

        // 启动 GUI 刷新器
        SpiritMenus.startMenuUpdater(this);

        // 4. 【关键】动态注册指令
        RabiSystem.getCommandManager().registerSubCommand("spirit", new SpiritCommand(this));

        // 5. 恢复小精灵
        this.spiritManager.reloadSpirits();

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    @Override
    public void onDisable() {
        // 1. 清理实体
        if (spiritManager != null) {
            spiritManager.despawnAll(true);
        }

        // 2. 保存数据
        if (configManager != null) {
            configManager.saveAllData();
        }

        // 3. 取消任务
        if (companionTask != null) companionTask.cancel();
        if (spiritAppTask != null) spiritAppTask.cancel();
        if (spiritBehaviorTask != null) spiritBehaviorTask.cancel();

        // 4. 注销监听器
        HandlerList.unregisterAll(playerStateListener);
        HandlerList.unregisterAll(spiritInteractListener);
        HandlerList.unregisterAll(menuListener);
        HandlerList.unregisterAll(spiritCombatListener);
        HandlerList.unregisterAll(spiritDefenseListener);
        HandlerList.unregisterAll(breedingListener);

        // 5. 【关键】注销指令
        RabiSystem.getCommandManager().unregisterSubCommand("spirit");

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已卸载");
    }

    @Override
    public void reload() {
        onDisable();
        onEnable();
    }

    public SpiritManager getSpiritManager() { return spiritManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public RabiSystem getPlugin() { return plugin; }
    public org.bukkit.configuration.file.FileConfiguration getDataConfig() { return this.configManager.getDataConfig(); }
}