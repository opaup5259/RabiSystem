package cn.rabitown.rabisystem.modules.fakeplayer;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.fakeplayer.command.FakePlayerCommand;
import cn.rabitown.rabisystem.modules.fakeplayer.listener.FakePlayerListener;
import cn.rabitown.rabisystem.modules.fakeplayer.manager.FakePlayerConfigManager;
import cn.rabitown.rabisystem.modules.fakeplayer.manager.FakePlayerManager;
import cn.rabitown.rabisystem.modules.fakeplayer.utils.NMSHelper;
import org.bukkit.event.HandlerList;

public class FakePlayerModule implements IRabiModule {

    private final RabiSystem plugin = RabiSystem.getInstance();
    private FakePlayerConfigManager configManager;
    private FakePlayerManager manager;
    private FakePlayerListener listener;

    @Override
    public String getModuleId() {
        return "fakeplayer";
    }

    @Override
    public String getDisplayName() {
        return "RabiFakePlayer (假人系统)";
    }

    @Override
    public void onEnable() {
        // 1. 初始化 NMS
        try {
            NMSHelper.init();
        } catch (Exception e) {
            plugin.getLogger().severe("假人模块 NMS 初始化失败，请检查服务器版本！");
            e.printStackTrace();
            return;
        }

        // 2. 初始化管理器
        this.configManager = new FakePlayerConfigManager(this);
        this.manager = new FakePlayerManager(this);
        this.manager.load(); // 加载数据

        // 3. 注册监听器
        this.listener = new FakePlayerListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 4. 注册指令 /rs fp
        RabiSystem.getCommandManager().registerSubCommand("fp", new FakePlayerCommand(this));

        plugin.getLogger().info("⚡ [" + getDisplayName() + "] 模块已加载");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.cleanup(); // 下线所有假人
            manager.save();    // 保存数据
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }

        RabiSystem.getCommandManager().unregisterSubCommand("fp");

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
    public FakePlayerConfigManager getConfigManager() { return configManager; }
    public FakePlayerManager getManager() { return manager; }
}