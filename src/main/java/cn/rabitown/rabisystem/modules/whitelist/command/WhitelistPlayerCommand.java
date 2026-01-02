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
    // [新增] 持有管理员指令的实例，用于转发逻辑
    private final WhitelistAdminCommand adminExecutor;

    public WhitelistPlayerCommand(WhitelistModule module) {
        this.module = module;
        this.adminExecutor = new WhitelistAdminCommand(module);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // [新增] 优先处理管理员快捷指令 (add/remove/list/reload)
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (List.of("add", "remove", "list", "reload").contains(sub)) {
                if (!sender.hasPermission("rabisystem.admin")) {
                    sender.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.no-permission", "<red>你没有权限执行此操作。")));
                    return true;
                }
                // 转发给 WhitelistAdminCommand 处理 (args 结构完全一致)
                adminExecutor.onCommand(sender, args);
                return true;
            }
        }

        // --- 原有玩家指令逻辑 ---
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
        // 美化帮助信息
        sender.sendMessage(mm.deserialize("<newline>   <gradient:#55ffff:#55aaff><b>[RabiWL]</b></gradient> <gray>白名单系统指令："));
        sender.sendMessage(mm.deserialize("   <gray>▶ <yellow>/wl accept</yellow> <dark_gray>-</dark_gray> <white>同意服务器规则"));
        sender.sendMessage(mm.deserialize("   <gray>▶ <yellow>/wl deny</yellow>   <dark_gray>-</dark_gray> <white>拒绝服务器规则"));
        if (sender.hasPermission("rabisystem.admin")) {
            sender.sendMessage(mm.deserialize("   <gray>▶ <yellow>/wl add <玩家></yellow> <dark_gray>-</dark_gray> <white>添加白名单"));
            sender.sendMessage(mm.deserialize("   <gray>▶ <yellow>/wl remove <玩家></yellow> <dark_gray>-</dark_gray> <white>移除白名单"));
            sender.sendMessage(mm.deserialize("   <gray>▶ <yellow>/wl list</yellow>    <dark_gray>-</dark_gray> <white>查看名单列表"));
        }
        sender.sendMessage(mm.deserialize(""));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> suggestions = new ArrayList<>(List.of("accept", "deny"));

        // [新增] 如果是管理员，添加管理补全
        if (sender.hasPermission("rabisystem.admin")) {
            suggestions.addAll(List.of("add", "remove", "list", "reload"));
        }

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
        } else if (args.length == 2 && sender.hasPermission("rabisystem.admin")) {
            // 转发管理员参数补全 (例如 add 后的玩家名)
            return adminExecutor.onTabComplete(sender, args);
        }
        return completions;
    }
}