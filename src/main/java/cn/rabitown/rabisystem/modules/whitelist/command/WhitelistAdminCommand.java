package cn.rabitown.rabisystem.modules.whitelist.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistAdminCommand implements ISubCommand {

    private final WhitelistModule module;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final String PREFIX = "§8[§f白名单§8] ";

    public WhitelistAdminCommand(WhitelistModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "rabisystem.admin"; // 基础管理权限，具体细分在逻辑中也可以检查
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }
        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            module.getPlugin().reloadConfig();
            module.getManager().reload();
            sender.sendMessage(PREFIX + "§a配置与数据已重载。");
            return;
        }

        if (sub.equals("list")) {
            Set<String> list = module.getManager().getWhitelistCache();
            int size = list.size();
            sender.sendMessage("§8§l======== §f§l白名单列表 §8§l========");
            sender.sendMessage("§7共计 §f" + size + " §7人");
            sender.sendMessage("§7" + String.join(", ", list));
            return;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return;
        }
        String targetName = args[1];

        if (sub.equals("add")) {
            if (module.getManager().getWhitelistCache().contains(targetName)) {
                sender.sendMessage(PREFIX + "§e玩家 " + targetName + " 已在白名单中。");
            } else {
                module.getManager().addPlayer(targetName);
                sender.sendMessage(PREFIX + "§a已添加玩家 " + targetName + " 到白名单。");
            }
        } else if (sub.equals("remove")) {
            if (!module.getManager().getWhitelistCache().contains(targetName)) {
                sender.sendMessage(PREFIX + "§c玩家 " + targetName + " 不在白名单中。");
            } else {
                module.getManager().removePlayer(targetName);
                sender.sendMessage(PREFIX + "§c已将玩家 " + targetName + " 移出白名单。");
            }
        } else {
            sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §f§l白名单管理 §8§l========");
        sender.sendMessage("§7/rs whitelist add <玩家>    §f- 添加白名单");
        sender.sendMessage("§7/rs whitelist remove <玩家> §f- 移除白名单");
        sender.sendMessage("§7/rs whitelist list          §f- 查看列表");
        sender.sendMessage("§7/rs whitelist reload        §f- 重载配置");
        sender.sendMessage(" ");
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