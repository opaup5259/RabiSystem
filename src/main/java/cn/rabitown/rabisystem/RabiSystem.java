// File: src/main/java/cn/rabitown/rabisystem/RabiSystem.java
package cn.rabitown.rabisystem;

import cn.rabitown.rabisystem.core.command.CommandManager;
import cn.rabitown.rabisystem.core.command.ModuleCommand;
import cn.rabitown.rabisystem.core.manager.ModuleManager;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import org.bukkit.plugin.java.JavaPlugin;

public class RabiSystem extends JavaPlugin {

    private static RabiSystem instance;
    private static CommandManager commandManager;
    private static ModuleManager moduleManager;

    public static RabiSystem getInstance() {
        return instance;
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
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

        // 注册核心管理指令 (/rs module)
        commandManager.registerSubCommand("module", new ModuleCommand());

        // 3. 初始化模块管理器并加载模块
        moduleManager = new ModuleManager();
        moduleManager.loadAllModules();

        getLogger().info("RabiSystem 核心已启动！(paper 1.21.11)");
    }

    @Override
    public void onDisable() {
        // 卸载所有模块
        if (moduleManager != null) {
            moduleManager.disableAllModules();
        }

        getLogger().info("RabiSystem 核心已关闭！");
    }

    // 快捷获取 SpiritModule 的方法（为了兼容旧代码 SpiritUtils）
    public SpiritModule getSpiritModule() {
        if (moduleManager != null) {
            return (SpiritModule) moduleManager.getModule("spirit");
        }
        return null;
    }
}