package cn.rabitown.rabisystem.core.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, ISubCommand> subCommands = new HashMap<>();

    /**
     * 注册子命令
     * @param label 子命令名称 (如 "spirit")
     * @param executor 执行器
     */
    public void registerSubCommand(String label, ISubCommand executor) {
        subCommands.put(label.toLowerCase(), executor);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§8§l======== §6§lRabiSystem Core §8§l========");
            sender.sendMessage("§7/rs spirit ... §f- 灵契系统");
            sender.sendMessage("§7/rs help ...   §f- 查看帮助");
            return true;
        }

        String subLabel = args[0].toLowerCase();

        if (subCommands.containsKey(subLabel)) {
            ISubCommand executor = subCommands.get(subLabel);

            // 权限检查
            if (!sender.hasPermission(executor.getPermission())) {
                sender.sendMessage("§c[RabiSystem] 你没有权限执行此模块指令。");
                return true;
            }

            // 移除第一个参数 (模块名)，传递剩余参数
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);

            executor.onCommand(sender, newArgs);
        } else {
            sender.sendMessage("§c[RabiSystem] 未知的模块: " + subLabel);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // 补全模块名
            return new ArrayList<>(subCommands.keySet());
        }

        String subLabel = args[0].toLowerCase();
        if (subCommands.containsKey(subLabel)) {
            ISubCommand executor = subCommands.get(subLabel);
            if (sender.hasPermission(executor.getPermission())) {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                return executor.onTabComplete(sender, newArgs);
            }
        }
        return null;
    }
}