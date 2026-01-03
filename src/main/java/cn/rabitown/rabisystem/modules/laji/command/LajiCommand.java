package cn.rabitown.rabisystem.modules.laji.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.laji.LajiModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LajiCommand implements ISubCommand, CommandExecutor, TabCompleter {

    private final LajiModule module;

    public LajiCommand(LajiModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "lajimiao.admin";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lajimiao.admin")) {
            sender.sendMessage(Component.text("杂鱼没有权限指挥本喵！", NamedTextColor.RED));
            return;
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                module.getPlugin().reloadConfig();
                module.getManager().reloadData();
                sender.sendMessage(Component.text("配置已重载喵！", NamedTextColor.GREEN));
            } else if (args[0].equalsIgnoreCase("clean")) {
                module.getManager().performCleanUp();
                sender.sendMessage(Component.text("正在手动执行清理...", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("用法: /lajimiao <reload|clean>", NamedTextColor.YELLOW));
            }
        } else {
            sender.sendMessage(Component.text("用法: /lajimiao <reload|clean>", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        onCommand(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return onTabComplete(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "clean");
        }
        return Collections.emptyList();
    }
}