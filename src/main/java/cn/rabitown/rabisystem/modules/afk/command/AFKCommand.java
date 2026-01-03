// File: src/main/java/cn/rabitown/rabisystem/modules/afk/command/AFKCommand.java
package cn.rabitown.rabisystem.modules.afk.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.afk.AFKModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AFKCommand implements ISubCommand, CommandExecutor, TabCompleter {

    private final AFKModule module;
    private static final String PREFIX = "§8[§e摸鱼§8] ";

    public AFKCommand(AFKModule module) {
        this.module = module;
    }

    // --- 作为子指令 /rs afk 调用 ---
    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(PREFIX + "§c控制台无法挂机。");
            return;
        }

        // /rs afk -> 切换状态
        if (args.length == 0) {
            toggleAFK(p);
            return;
        }

        // /rs afk rank -> 排行榜
        if (args[0].equalsIgnoreCase("rank")) {
            module.getManager().sendRankMessage(p);
        } else {
            sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §e§l摸鱼挂机 §8§l========");
        sender.sendMessage("§7/afk 或 /moyu       §f- 切换挂机状态");
        sender.sendMessage("§7/afkrank            §f- 查看摸鱼时长榜");
        sender.sendMessage(" ");
    }

    @Override
    public String getPermission() {
        return "rabisystem.use";
    }

    // --- 作为独立指令 /afk, /moyu, /afkrank 调用 ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(PREFIX + "§c控制台无法挂机。");
            return true;
        }

        String cmdName = label.toLowerCase();

        if (cmdName.equals("afkrank")) {
            module.getManager().sendRankMessage(p);
            return true;
        }

        if (cmdName.equals("afk") || cmdName.equals("moyu")) {
            toggleAFK(p);
            return true;
        }

        return false;
    }

    private void toggleAFK(Player p) {
        if (module.getManager().isAFK(p)) {
            module.getManager().exitAFK(p);
        } else {
            module.getManager().enterAFK(p);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return onTabComplete(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("rank");
        }
        return Collections.emptyList();
    }
}