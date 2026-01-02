package cn.rabitown.rabisystem;

import cn.rabitown.rabisystem.core.command.CommandManager;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import org.bukkit.plugin.java.JavaPlugin;

public class RabiSystem extends JavaPlugin {

    private static RabiSystem instance;
    private static CommandManager commandManager;

    // 这里可以放一个 List<IRabiModule> 来管理所有模块
    private SpiritModule spiritModule;

    public static RabiSystem getInstance() {
        return instance;
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. 初始化核心配置
        saveDefaultConfig();

        // 2. 初始化指令系统
        commandManager = new CommandManager();
        if (getCommand("rabi") != null) {
            getCommand("rabi").setExecutor(commandManager);
            getCommand("rabi").setTabCompleter(commandManager);
        }

        // 3. 加载灵契模块 (Spirit)
        // 注意：这里手动加载，未来可以写一个 ModuleManager 自动扫描
        if (getConfig().getBoolean("modules.spirit.enabled", true)) {
            spiritModule = new SpiritModule();
            spiritModule.onEnable();
        }

        getLogger().info("RabiSystem 核心已启动！");
    }

    @Override
    public void onDisable() {
        // 卸载模块
        if (spiritModule != null) {
            spiritModule.onDisable();
        }

        // 保存数据逻辑可以放在各模块的 onDisable 中
    }

    // 提供给模块获取 SpiritModule 的方法 (如果需要跨模块调用)
    public SpiritModule getSpiritModule() {
        return spiritModule;
    }
}