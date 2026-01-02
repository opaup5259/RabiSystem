package cn.rabitown.rabisystem.modules.warpStone;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.warpStone.listener.WarpListener;
import cn.rabitown.rabisystem.modules.warpStone.manager.WarpConfigManager;
import cn.rabitown.rabisystem.modules.warpStone.manager.WarpManager;
import org.bukkit.event.HandlerList;

public class WarpStoneModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private WarpConfigManager configManager;
    private WarpManager warpManager;
    private WarpListener warpListener;

    @Override
    public String getModuleId() {
        return "warp";
    }

    @Override
    public String getDisplayName() {
        return "RabiWarp (传送石)";
    }

    @Override
    public void onEnable() {
        this.configManager = new WarpConfigManager(this);
        this.warpManager = new WarpManager(this);
        this.warpListener = new WarpListener(this);

        plugin.getServer().getPluginManager().registerEvents(warpListener, plugin);

        // 注册子命令 /rs warp
        RabiSystem.getCommandManager().registerSubCommand("warp", new WarpCommand(this));

        // 启动粒子任务等
        warpManager.startTasks();

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    @Override
    public void onDisable() {
        if (warpManager != null) {
            warpManager.stopTasks();
        }
        if (warpListener != null) {
            HandlerList.unregisterAll(warpListener);
        }

        RabiSystem.getCommandManager().unregisterSubCommand("warp");

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
    public WarpConfigManager getConfigManager() { return configManager; }
    public WarpManager getWarpManager() { return warpManager; }
}