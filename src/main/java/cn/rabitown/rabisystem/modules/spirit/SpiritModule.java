package cn.rabitown.rabisystem.modules.spirit;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule; // 👈 必须导入这个
import cn.rabitown.rabisystem.modules.spirit.manager.ConfigManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SpiritManager;
import cn.rabitown.rabisystem.modules.spirit.listener.*;
import cn.rabitown.rabisystem.modules.spirit.task.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class SpiritModule implements IRabiModule {

    private SpiritManager spiritManager;
    private ConfigManager configManager;
    private final RabiSystem plugin = RabiSystem.getInstance(); // 持有主插件引用

    @Override
    public String getModuleId() {
        return "spirit";
    }

    @Override
    public boolean isEnabled() {
        return true; // 或者读取配置文件
    }

    @Override
    public void onEnable() {
        // 1. 初始化管理器 (注意：ConfigManager 需要传入 plugin 实例)
        // 建议修改 ConfigManager 构造函数，或者让它接受 JavaPlugin 参数
        this.configManager = new ConfigManager(this);
        this.spiritManager = new SpiritManager(this);
//        RabiSystem.getInstance().getServer().getPluginManager().registerEvents(new PlayerStateListener(this), RabiSystem.getInstance());

        // 2. 注册监听器
        // 注意：原代码中的 new Listener(this) 需要改为 new Listener(plugin) 或调整构造函数
        // 建议：将 Listener 的构造函数改为接收 RabiSystem 或 SpiritModule
        // 这里演示传入 plugin (主类) 的方式，你需要对应修改 Listener 代码
        plugin.getServer().getPluginManager().registerEvents(new PlayerStateListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpiritInteractListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpiritCombatListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpiritDefenseListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new BreedingListener(), plugin); // 别忘了这个

        // 3. 启动任务
        new CompanionTask().runTaskTimer(plugin, 400L, 400L);
        new SpiritAppTask().runTaskTimer(plugin, 0L, 5L);
        new SpiritBehaviorTask().runTaskTimer(plugin, 0L, 5L);

        // 4. 【关键】注册指令
        RabiSystem.getCommandManager().registerSubCommand("spirit", new SpiritCommand(this));

        // 5. 恢复小精灵
        this.spiritManager.reloadSpirits();

        plugin.getLogger().info("⚡ [灵契模块] 已加载！");
    }

    @Override
    public void onDisable() {
        if (spiritManager != null) {
            spiritManager.despawnAll(true);
        }
        if (configManager != null) {
            configManager.saveAllData();
        }
        plugin.getLogger().info("⚡ [灵契模块] 已卸载！");
    }

    @Override
    public void reload() {
        // 实现重载逻辑
        onDisable();
        onEnable();
    }

    // --- Getter 供模块内部使用 ---
    public SpiritManager getSpiritManager() {
        return spiritManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RabiSystem getPlugin() {
        return plugin;
    }

    public FileConfiguration getDataConfig() {
        return this.configManager.getDataConfig();
    }
}