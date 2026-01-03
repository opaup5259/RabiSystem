package cn.rabitown.rabisystem.modules.corpse;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.corpse.listener.CorpseListener;
import cn.rabitown.rabisystem.modules.corpse.manager.CorpseConfigManager;
import cn.rabitown.rabisystem.modules.corpse.manager.CorpseManager;
import org.bukkit.event.HandlerList;

public class CorpseModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private CorpseConfigManager configManager;
    private CorpseManager corpseManager;
    private CorpseListener corpseListener;

    @Override
    public String getModuleId() {
        return "corpse";
    }

    @Override
    public String getDisplayName() {
        return "跑尸模块 (WowCorpse)";
    }

    @Override
    public void onEnable() {
        this.configManager = new CorpseConfigManager(this);
        this.corpseManager = new CorpseManager(this);
        this.corpseListener = new CorpseListener(this);

        plugin.getServer().getPluginManager().registerEvents(corpseListener, plugin);

        // 注册子命令 /rs corpse
        RabiSystem.getCommandManager().registerSubCommand("corpse", new CorpseCommand(this));

        // 启动任务
        corpseManager.startTasks();

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    @Override
    public void onDisable() {
        if (corpseManager != null) {
            corpseManager.stopTasks();
        }
        if (corpseListener != null) {
            HandlerList.unregisterAll(corpseListener);
        }

        RabiSystem.getCommandManager().unregisterSubCommand("corpse");

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
    public CorpseConfigManager getConfigManager() { return configManager; }
    public CorpseManager getCorpseManager() { return corpseManager; }
}