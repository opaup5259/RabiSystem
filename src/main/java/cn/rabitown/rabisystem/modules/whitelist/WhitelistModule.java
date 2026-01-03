// File: src/main/java/cn/rabitown/rabisystem/modules/whitelist/WhitelistModule.java
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
        return "白名单 (RabiWhitelist)";
    }

    @Override
    public void onEnable() {
        this.configManager = new WhitelistConfigManager(this);
        this.manager = new WhitelistManager(this);
        this.listener = new WhitelistListener(this);

        // 注册事件
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 创建管理员指令实例
        WhitelistAdminCommand adminCmd = new WhitelistAdminCommand(this);

        // 注册管理指令 /rabi whitelist (作为子命令)
        RabiSystem.getCommandManager().registerSubCommand("whitelist", adminCmd);

        // 注册玩家独立指令 /wl 和 /whitelist (传入 adminCmd 以支持管理员快捷操作)
        WhitelistPlayerCommand playerCmd = new WhitelistPlayerCommand(this, adminCmd);
        registerStandaloneCommand("wl", playerCmd);
        registerStandaloneCommand("whitelist", playerCmd);

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