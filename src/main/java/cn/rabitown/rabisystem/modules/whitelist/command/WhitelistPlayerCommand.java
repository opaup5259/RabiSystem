package cn.rabitown.rabisystem.modules.whitelist.command;

import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WhitelistPlayerCommand implements CommandExecutor, TabCompleter {

    private final WhitelistModule module;
    private final WhitelistAdminCommand adminCommand; // 持有管理员指令实例
    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final String PREFIX = "§8[§f白名单§8] ";

    public WhitelistPlayerCommand(WhitelistModule module, WhitelistAdminCommand adminCommand) {
        this.module = module;
        this.adminCommand = adminCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 1. 检查是否为管理员操作 (add/remove/list/reload)
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (List.of("add", "remove", "list", "reload").contains(sub)) {
                // 检查权限：rabisystem.whitelist.admin 或 rabisystem.admin 或 OP
                if (sender.hasPermission("rabisystem.whitelist.admin") || sender.hasPermission("rabisystem.admin")) {
                    // 转发给管理员指令处理
                    adminCommand.onCommand(sender, args);
                    return true;
                } else {
                    sender.sendMessage(PREFIX + "§c你没有权限执行此操作。");
                    return true;
                }
            }
        }

        // 2. 玩家普通操作
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c控制台请使用 /rs whitelist <args> 进行管理。");
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

        // 发送帮助
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §f§l白名单系统 §8§l========");
        sender.sendMessage("§7/wl accept                §f- 同意服务器规则");
        sender.sendMessage("§7/wl deny                  §f- 拒绝服务器规则");
        if (sender.hasPermission("rabisystem.whitelist.admin") || sender.hasPermission("rabisystem.admin")) {
            sender.sendMessage("§7/wl add <玩家>            §f- 添加白名单");
            sender.sendMessage("§7/wl remove <玩家>         §f- 移除白名单");
            sender.sendMessage("§7/wl list                  §f- 查看名单列表");
            sender.sendMessage("§7/wl reload                §f- 重载配置");
        }
        sender.sendMessage(" ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 复用管理员指令的补全逻辑
        if (sender.hasPermission("rabisystem.whitelist.admin") || sender.hasPermission("rabisystem.admin")) {
            return adminCommand.onTabComplete(sender, args);
        }
        return new ArrayList<>();
    }
}