package cn.rabitown.rabisystem.modules.guild.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.guild.GuildModule;
import cn.rabitown.rabisystem.modules.guild.data.GuildData;
import cn.rabitown.rabisystem.modules.guild.ui.GuildMenus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GuildCommand implements ISubCommand, CommandExecutor, TabCompleter {

    private final GuildModule module;

    public GuildCommand(GuildModule module) {
        this.module = module;
    }

    // --- ISubCommand 接口实现 (作为子命令 /rs guild 调用) ---
    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player p) {
            handleCommand(p, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    // --- CommandExecutor / TabCompleter 接口实现 (作为独立指令 /team 调用) ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            handleCommand(p, args);
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }

    // --- 统一逻辑 ---
    private void handleCommand(Player player, String[] args) {
        if (args.length > 0) {
            // 处理聊天点击事件
            if (args[0].equalsIgnoreCase("accept") && args.length > 1) {
                try {
                    module.getManager().acceptInvite(player, UUID.fromString(args[1]));
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§c无效的公会ID。");
                }
                return;
            }
            if (args[0].equalsIgnoreCase("deny") && args.length > 1) {
                player.sendMessage("§c已拒绝邀请。");
                return;
            }
        }

        // 默认打开菜单
        GuildData guild = module.getManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            GuildMenus.openNoGuildMenu(player);
        } else {
            GuildMenus.openGuildDetail(player, guild);
        }
    }

    @Override
    public String getPermission() {
        return "rabisystem.use";
    }
}