package cn.rabitown.rabisystem.modules.whitelist.command;

import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WhitelistPlayerCommand implements CommandExecutor, TabCompleter {

    private final WhitelistModule module;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WhitelistPlayerCommand(WhitelistModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("仅玩家可用");
            return true;
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("accept")) {
                module.getManager().performAccept(player);
                return true;
            } else if (args[0].equalsIgnoreCase("deny")) {
                module.getManager().performDeny(player);
                return true;
            }
        }
        sender.sendMessage(mm.deserialize("<yellow>用法: /wl <accept|deny> 或输入 '同意'/'拒绝'"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("accept", "deny"), completions);
        }
        return completions;
    }
}