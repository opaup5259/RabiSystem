package cn.rabitown.rabisystem.modules.whitelist;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.whitelist.command.WhitelistAdminCommand;
import cn.rabitown.rabisystem.modules.whitelist.command.WhitelistPlayerCommand;
import cn.rabitown.rabisystem.modules.whitelist.listener.WhitelistListener;
import cn.rabitown.rabisystem.modules.whitelist.manager.WhitelistConfigManager;
import cn.rabitown.rabisystem.modules.whitelist.manager.WhitelistManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;

public class WhitelistModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private WhitelistManager manager;
    private WhitelistConfigManager configManager;
    private WhitelistListener listener;

    @Override
    public String getModuleId() {
        return "whitelist";
    }

    @Override
    public String getDisplayName() {
        return "RabiWhitelist (白名单)";
    }

    @Override
    public void onEnable() {
        this.configManager = new WhitelistConfigManager(this);
        this.manager = new WhitelistManager(this);
        this.listener = new WhitelistListener(this);

        // 注册事件
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 注册管理指令 /rabi whitelist
        RabiSystem.getCommandManager().registerSubCommand("whitelist", new WhitelistAdminCommand(this));

        // 注册玩家指令 /wl 和 /whitelist (需要在 plugin.yml 中定义)
        WhitelistPlayerCommand playerCmd = new WhitelistPlayerCommand(this);
        registerStandaloneCommand("wl", playerCmd);
        registerStandaloneCommand("whitelist_player", playerCmd); // 避免冲突，plugin.yml 里可以叫 whitelist

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    private void registerStandaloneCommand(String name, WhitelistPlayerCommand executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (configManager != null) {
            configManager.saveData();
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        RabiSystem.getCommandManager().unregisterSubCommand("whitelist");

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
    public WhitelistManager getManager() { return manager; }
    public WhitelistConfigManager getConfigManager() { return configManager; }
}