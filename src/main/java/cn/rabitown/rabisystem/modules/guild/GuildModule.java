package cn.rabitown.rabisystem.modules.guild;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.guild.command.GuildCommand;
import cn.rabitown.rabisystem.modules.guild.listener.GuildListener;
import cn.rabitown.rabisystem.modules.guild.manager.GuildConfigManager;
import cn.rabitown.rabisystem.modules.guild.manager.GuildManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;

public class GuildModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private GuildConfigManager configManager;
    private GuildManager manager;
    private GuildListener listener;

    @Override
    public String getModuleId() {
        return "guild";
    }

    @Override
    public String getDisplayName() {
        return "公会系统 (RabiGuild)";
    }

    @Override
    public void onEnable() {
        this.configManager = new GuildConfigManager(this);
        this.manager = new GuildManager(this);
        this.listener = new GuildListener(this);

        // 加载数据
        this.manager.load();

        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 注册指令
        GuildCommand cmd = new GuildCommand(this);
        // 子指令 /rs guild
        RabiSystem.getCommandManager().registerSubCommand("guild", cmd);
        // 独立指令 /guild
        registerStandaloneCommand("guild", cmd);

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    private void registerStandaloneCommand(String name, GuildCommand executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        RabiSystem.getCommandManager().unregisterSubCommand("guild");

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
    public GuildConfigManager getConfigManager() { return configManager; }
    public GuildManager getManager() { return manager; }
}
