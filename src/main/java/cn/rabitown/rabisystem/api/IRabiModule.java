package cn.rabitown.rabisystem.api;

public interface IRabiModule {
    // 模块的唯一标识符 (例如 "spirit", "corpse")
    String getModuleId();

    // [新增] 模块的显示名称 (例如 "灯火灵契")
    // 默认返回 ID，子类可以重写
    default String getDisplayName() {
        return getModuleId();
    }

    // 启用模块时执行
    void onEnable();

    // 禁用模块时执行
    void onDisable();

    // 重载模块配置
    void reload();

    // 模块是否启用
    boolean isEnabled();
}