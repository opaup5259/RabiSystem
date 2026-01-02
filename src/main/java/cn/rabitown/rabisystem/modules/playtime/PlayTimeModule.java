package cn.rabitown.rabisystem.modules.playtime;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import org.bukkit.event.HandlerList;

public class PlayTimeModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private PlayTimeManager manager;
    private PlayTimeConfigManager configManager;
    private PlayTimeListener listener;

    @Override
    public String getModuleId() {
        return "playtime";
    }

    @Override
    public String getDisplayName() {
        return "PlayTime (时长统计)";
    }

    @Override
    public void onEnable() {
        // 初始化配置与管理器
        this.configManager = new PlayTimeConfigManager(this);
        this.manager = new PlayTimeManager(this);
        this.listener = new PlayTimeListener(this);

        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 注册指令 /rabi playtime
        RabiSystem.getCommandManager().registerSubCommand("playtime", new PlayTimeCommand(this));

        // 启动任务
        manager.startTasks();
        manager.handleReload(); // 处理重载时在线玩家的数据初始化

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown(); // 保存数据
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        RabiSystem.getCommandManager().unregisterSubCommand("playtime");

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已卸载");
    }

    @Override
    public void reload() {
        onDisable();
        onEnable();
    }

    @Override
    public boolean isEnabled() {
        return RabiSystem.getModuleManager().isModuleEnabled(getModuleId());
    }

    public RabiSystem getPlugin() { return plugin; }
    public PlayTimeManager getManager() { return manager; }
    public PlayTimeConfigManager getConfigManager() { return configManager; }
}