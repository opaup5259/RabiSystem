package cn.rabitown.rabisystem.modules.afk.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.afk.AFKModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // 1. 导入 TabCompleter
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

// 2. 在 implements 列表中添加 TabCompleter
public class AFKCommand implements ISubCommand, CommandExecutor, TabCompleter {

    private final AFKModule module;

    public AFKCommand(AFKModule module) {
        this.module = module;
    }

    // --- 作为子指令 /rs afk 调用 ---
    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c控制台无法挂机。");
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
        }
    }

    @Override
    public String getPermission() {
        return "rabisystem.use";
    }

    // --- 作为独立指令 /afk, /moyu, /afkrank 调用 ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c控制台无法挂机。");
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

    // 3. 实现 TabCompleter 接口要求的 onTabComplete 方法
    // 这个方法是 Bukkit 独立指令 (/afk) 调用的入口
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 直接复用 ISubCommand 的补全逻辑，保证 /rs afk 和 /afk 的补全一致
        return onTabComplete(sender, args);
    }

    // --- ISubCommand 接口实现 (子指令补全) ---
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("rank");
        }
        return Collections.emptyList();
    }
}