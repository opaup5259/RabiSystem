package cn.rabitown.rabisystem.modules.laji;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.laji.command.LajiCommand;
import cn.rabitown.rabisystem.modules.laji.listener.LajiListener;
import cn.rabitown.rabisystem.modules.laji.manager.LajiConfigManager;
import cn.rabitown.rabisystem.modules.laji.manager.LajiManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;

public class LajiModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private LajiConfigManager configManager;
    private LajiManager manager;
    private LajiListener listener;

    @Override
    public String getModuleId() {
        return "laji";
    }

    @Override
    public String getDisplayName() {
        return "垃圾喵 (LajiMiao)";
    }

    @Override
    public void onEnable() {
        this.configManager = new LajiConfigManager(this);
        this.manager = new LajiManager(this);
        this.listener = new LajiListener(this);

        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 注册指令
        LajiCommand cmd = new LajiCommand(this);
        // 子指令 /rs laji
        RabiSystem.getCommandManager().registerSubCommand("laji", cmd);
        // 独立指令 /lajimiao
        registerStandaloneCommand("lajimiao", cmd);

        // 启动任务
        manager.startTasks();

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    private void registerStandaloneCommand(String name, LajiCommand executor) {
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
        RabiSystem.getCommandManager().unregisterSubCommand("laji");

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
    public LajiConfigManager getConfigManager() { return configManager; }
    public LajiManager getManager() { return manager; }
}