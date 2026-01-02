package cn.rabitown.rabisystem.api;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface ISubCommand {
    // 处理指令 (args 已去除模块名，例如输入 /rs spirit xp 10，这里 args 为 ["xp", "10"])
    void onCommand(CommandSender sender, String[] args);

    // Tab 补全
    List<String> onTabComplete(CommandSender sender, String[] args);

    // 权限节点
    String getPermission();
}