package cn.rabitown.rabisystem.modules.afk;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.afk.command.AFKCommand;
import cn.rabitown.rabisystem.modules.afk.listener.AFKListener;
import cn.rabitown.rabisystem.modules.afk.manager.AFKConfigManager;
import cn.rabitown.rabisystem.modules.afk.manager.AFKManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;

public class AFKModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private AFKConfigManager configManager;
    private AFKManager manager;
    private AFKListener listener;

    @Override
    public String getModuleId() {
        return "afk";
    }

    @Override
    public String getDisplayName() {
        return "摸鱼挂机 (RabiAFK)";
    }

    @Override
    public void onEnable() {
        this.configManager = new AFKConfigManager(this);
        this.manager = new AFKManager(this);
        this.listener = new AFKListener(this);

        // 注册事件
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 注册管理指令 /rs afk (作为子命令)
        AFKCommand cmdExecutor = new AFKCommand(this);
        RabiSystem.getCommandManager().registerSubCommand("afk", cmdExecutor);

        // 注册玩家独立指令 /afk, /moyu, /afkrank
        registerStandaloneCommand("afk", cmdExecutor);
        registerStandaloneCommand("moyu", cmdExecutor);
        registerStandaloneCommand("afkrank", cmdExecutor);

        // 启动任务
        manager.startTasks();

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    private void registerStandaloneCommand(String name, AFKCommand executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        RabiSystem.getCommandManager().unregisterSubCommand("afk");

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
    public AFKManager getManager() { return manager; }
    public AFKConfigManager getConfigManager() { return configManager; }
}