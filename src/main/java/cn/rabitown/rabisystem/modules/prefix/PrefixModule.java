package cn.rabitown.rabisystem.modules.prefix;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.prefix.listener.PrefixListener;
import cn.rabitown.rabisystem.modules.prefix.manager.PrefixManager;
import org.bukkit.event.HandlerList;

public class PrefixModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private PrefixManager manager;
    private PrefixListener listener;

    @Override
    public String getModuleId() {
        return "prefix";
    }

    @Override
    public String getDisplayName() {
        return "前缀管理 (Prefix Control)";
    }

    @Override
    public void onEnable() {
        // 初始化管理器
        this.manager = new PrefixManager(this);

        // 注册监听器
        this.listener = new PrefixListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    @Override
    public void onDisable() {
        // 清理监听器
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }

        // 清理缓存（可选，如果Manager有清理逻辑）
        if (manager != null) {
            manager.clearAll();
        }

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
    public PrefixManager getManager() { return manager; }
}