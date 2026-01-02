// File: src/main/java/cn/rabitown/rabisystem/core/manager/ModuleManager.java
package cn.rabitown.rabisystem.core.manager;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.modules.corpse.CorpseModule;
import cn.rabitown.rabisystem.modules.playtime.PlayTimeModule;
import cn.rabitown.rabisystem.modules.prefix.PrefixModule;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.warpStone.WarpStoneModule;
import cn.rabitown.rabisystem.modules.warpStone.data.WarpStone;
import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ModuleManager {

    private final Map<String, IRabiModule> modules = new HashMap<>();
    private final Map<String, Boolean> moduleStatus = new HashMap<>();

    public ModuleManager() {
        // 在这里注册所有可用的模块实例
        registerModule(new WhitelistModule()); // ✅ 注册白名单模块
        registerModule(new PrefixModule()); // ✅ 注册玩家前缀控制模块
        registerModule(new SpiritModule()); // ✅ 注册小精灵模块
        registerModule(new CorpseModule()); // ✅ 注册跑尸模块
        registerModule(new WarpStoneModule()); // ✅ 注册传送石模块
        registerModule(new PlayTimeModule()); // ✅ 注册游戏时间统计模块
    }

    private void registerModule(IRabiModule module) {
        modules.put(module.getModuleId(), module);
    }

    /**
     * 根据配置文件加载所有模块
     */
    public void loadAllModules() {
        RabiSystem.getInstance().getLogger().info("正在初始化模块系统...");
        for (IRabiModule module : modules.values()) {
            // 读取配置 modules.<id>.enabled，默认为 true
            boolean shouldEnable = RabiSystem.getInstance().getConfig().getBoolean("modules." + module.getModuleId() + ".enabled", true);
            if (shouldEnable) {
                // 启动时无需重复保存 "true" 到配置
                enableModule(module.getModuleId(), false);
            }
        }
    }

    /**
     * 卸载所有模块 (关服/重载时使用)
     * 注意：这里传入 false，表示不保存状态到配置文件
     */
    public void disableAllModules() {
        for (String id : modules.keySet()) {
            if (isModuleEnabled(id)) {
                disableModule(id, false);
            }
        }
    }

    // --- 重载方法：默认保存状态 (用于指令) ---
    public boolean enableModule(String id) {
        return enableModule(id, true);
    }

    public boolean disableModule(String id) {
        return disableModule(id, true);
    }

    /**
     * 启用模块核心逻辑
     * @param saveConfig 是否将状态持久化保存到 config.yml
     */
    public boolean enableModule(String id, boolean saveConfig) {
        IRabiModule module = modules.get(id);
        if (module == null) return false;
        if (moduleStatus.getOrDefault(id, false)) return true; // 已启用

        try {
            module.onEnable();
            moduleStatus.put(id, true);

            if (saveConfig) {
                RabiSystem.getInstance().getConfig().set("modules." + id + ".enabled", true);
                RabiSystem.getInstance().saveConfig();
            }

            RabiSystem.getInstance().getLogger().info("✅ 模块 [" + module.getDisplayName() + "] 已启用。");
            return true;
        } catch (Exception e) {
            RabiSystem.getInstance().getLogger().log(Level.SEVERE, "❌ 模块 [" + id + "] 启动失败!", e);
            return false;
        }
    }

    /**
     * 禁用模块核心逻辑
     * @param saveConfig 是否将状态持久化保存到 config.yml
     */
    public boolean disableModule(String id, boolean saveConfig) {
        IRabiModule module = modules.get(id);
        if (module == null) return false;
        if (!moduleStatus.getOrDefault(id, false)) return true; // 已禁用

        try {
            module.onDisable();
            moduleStatus.put(id, false);

            if (saveConfig) {
                RabiSystem.getInstance().getConfig().set("modules." + id + ".enabled", false);
                RabiSystem.getInstance().saveConfig();
            }

            RabiSystem.getInstance().getLogger().info("🛑 模块 [" + module.getDisplayName() + "] 已卸载。");
            return true;
        } catch (Exception e) {
            RabiSystem.getInstance().getLogger().log(Level.SEVERE, "❌ 模块 [" + id + "] 卸载失败!", e);
            return false;
        }
    }

    public void reloadModule(String id) {
        // 重载时暂时不需要保存状态变更，因为这只是刷新
        if (disableModule(id, false)) {
            enableModule(id, false);
        }
    }

    public boolean isModuleEnabled(String id) {
        return moduleStatus.getOrDefault(id, false);
    }

    public IRabiModule getModule(String id) {
        return modules.get(id);
    }

    public Map<String, IRabiModule> getAllModules() {
        return modules;
    }
}