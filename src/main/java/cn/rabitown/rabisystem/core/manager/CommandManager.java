// File: src/main/java/cn/rabitown/rabisystem/core/manager/CommandManager.java
package cn.rabitown.rabisystem.core.command; // 注意你的包名是 core.command 还是 core.manager，根据你之前的文件结构，这里应该是 core.command

import cn.rabitown.rabisystem.api.ISubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, ISubCommand> subCommands = new HashMap<>();

    /**
     * 注册子命令
     */
    public void registerSubCommand(String label, ISubCommand executor) {
        subCommands.put(label.toLowerCase(), executor);
    }

    /**
     * 注销子命令 (热重载必须)
     */
    public void unregisterSubCommand(String label) {
        subCommands.remove(label.toLowerCase());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subLabel = args[0].toLowerCase();

        if (subCommands.containsKey(subLabel)) {
            ISubCommand executor = subCommands.get(subLabel);

            if (!sender.hasPermission(executor.getPermission())) {
                sender.sendMessage("§c[RabiSystem] 你没有权限执行此模块指令。");
                return true;
            }

            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);

            executor.onCommand(sender, newArgs);
        } else {
            sendHelp(sender); // 未知指令显示帮助
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §6§lRabiSystem Core §8§l========");
        sender.sendMessage("§7当前加载的模块指令:");

        // 动态生成帮助
        if (subCommands.isEmpty()) {
            sender.sendMessage("§7  (暂无已启用的模块)");
        } else {
            subCommands.forEach((key, cmd) -> {
                // 使用 Component 构建可点击的帮助
                Component msg = Component.text("  ")
                        .append(Component.text("/rs " + key).color(NamedTextColor.YELLOW))
                        .append(Component.text(" - " + getModuleDesc(key)).color(NamedTextColor.GRAY))
                        .hoverEvent(HoverEvent.showText(Component.text("点击执行")))
                        .clickEvent(ClickEvent.suggestCommand("/rs " + key + " "));
                sender.sendMessage(msg);
            });
        }

        // 管理员额外的指令
        if (sender.hasPermission("rabisystem.admin")) {
            sender.sendMessage(" ");
            Component adminMsg = Component.text("  ")
                    .append(Component.text("/rs module").color(NamedTextColor.RED))
                    .append(Component.text(" - 模块管理(热开关)").color(NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.suggestCommand("/rs module "));
            sender.sendMessage(adminMsg);
        }
        sender.sendMessage("§8§l==================================");
        sender.sendMessage(" ");
    }

    // 简单的描述映射，为了美观
    private String getModuleDesc(String key) {
        return switch (key) {
            case "spirit" -> "灵契系统";
            case "module" -> "模块管理";
            default -> "子模块";
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(subCommands.keySet());
            if (sender.hasPermission("rabisystem.admin")) {
                list.add("module"); // 补全管理指令
            }
            return list;
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