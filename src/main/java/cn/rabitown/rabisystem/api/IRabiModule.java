package cn.rabitown.rabisystem.api;

public interface IRabiModule {
    // 模块的唯一标识符 (例如 "spirit", "corpse")
    String getModuleId();

    // 启用模块时执行
    void onEnable();

    // 禁用模块时执行
    void onDisable();

    // 重载模块配置
    void reload();

    // 模块是否启用 (可以从 config.yml 读取)
    boolean isEnabled();
}