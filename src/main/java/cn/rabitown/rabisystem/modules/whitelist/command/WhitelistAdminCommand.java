package cn.rabitown.rabisystem.modules.whitelist.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhitelistAdminCommand implements ISubCommand {

    private final WhitelistModule module;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WhitelistAdminCommand(WhitelistModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "rabisystem.admin";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return;
        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            module.getPlugin().reloadConfig(); // 重载主配置
            module.getManager().reload(); // 重载数据
            sender.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.reload")));
            return;
        }

        if (sub.equals("list")) {
            sender.sendMessage(Component.text("§e白名单玩家: §f" + module.getManager().getWhitelistCache().toString()));
            return;
        }

        if (args.length < 2) return;
        String targetName = args[1];

        if (sub.equals("add")) {
            if (module.getManager().getWhitelistCache().contains(targetName)) {
                sender.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.player-exists")));
            } else {
                module.getManager().addPlayer(targetName);
                sender.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.player-added").replace("{player}", targetName)));
            }
        } else if (sub.equals("remove")) {
            if (!module.getManager().getWhitelistCache().contains(targetName)) {
                sender.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.player-not-found")));
            } else {
                module.getManager().removePlayer(targetName);
                sender.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.player-removed").replace("{player}", targetName)));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("add", "remove", "list", "reload"), completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add")) {
                List<String> online = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], online, completions);
            } else if (sub.equals("remove")) {
                StringUtil.copyPartialMatches(args[1], module.getManager().getWhitelistCache(), completions);
            }
        }
        return completions;
    }
}