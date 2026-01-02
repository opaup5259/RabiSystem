package cn.rabitown.rabisystem.modules.spirit.utils;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.manager.ConfigManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SpiritManager;

public class SpiritUtils {

    public static RabiSystem getPlugin(){
        return getModule().getPlugin();
    }
    /**
     * 快速获取灵契模块实例
     */
    public static SpiritModule getModule() {
        return RabiSystem.getInstance().getSpiritModule();
    }

    /**
     * 快速获取 ConfigManager (存取数据用)
     */
    public static ConfigManager getConfigManager() {
        return getModule().getConfigManager();
    }

    /**
     * 快速获取 SpiritManager (操作小精灵用)
     */
    public static SpiritManager getSpiritManager() {
        return getModule().getSpiritManager();
    }
}