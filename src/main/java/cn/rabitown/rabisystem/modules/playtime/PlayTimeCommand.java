package cn.rabitown.rabisystem.modules.playtime;

import cn.rabitown.rabisystem.api.ISubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class PlayTimeCommand implements ISubCommand {

    private final PlayTimeModule module;

    public PlayTimeCommand(PlayTimeModule module) {
        this.module = module;
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            module.getManager().openLeaderboard(player, 1);
        } else {
            sender.sendMessage("§c只有玩家可以使用此指令。");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "rabisystem.use"; // 默认玩家权限
    }
}