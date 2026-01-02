package cn.rabitown.rabisystem.modules.prefix.utils;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.prefix.PrefixModule;
import cn.rabitown.rabisystem.modules.prefix.manager.PrefixManager;

public class PrefixUtils {

    /**
     * 获取 Prefix 模块实例
     */
    public static PrefixModule getModule() {
        return (PrefixModule) RabiSystem.getModuleManager().getModule("prefix");
    }

    /**
     * 获取管理器 (用于操作前缀)
     */
    public static PrefixManager getManager() {
        PrefixModule module = getModule();
        return (module != null) ? module.getManager() : null;
    }
}