package cn.rabitown.rabisystem.modules.warpStone;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.warpStone.ui.WarpMenus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class WarpCommand implements ISubCommand {

    private final WarpStoneModule module;
    private static final String PREFIX = "§8[§d传送石§8] ";

    public WarpCommand(WarpStoneModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "rabi.warpstone.use";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§c此指令只能由玩家执行。");
            return;
        }

        // 默认打开菜单
        WarpMenus menus = new WarpMenus(module.getWarpManager(), module.getPlugin());
        menus.openWarpMenu(player, 1);

        // 管理员指令
        if (args.length > 0 && sender.hasPermission("rabi.warpstone.admin")) {
            if (args[0].equalsIgnoreCase("save")) {
                module.getConfigManager().saveWarpStones(module.getWarpManager().getWarpStones());
                player.sendMessage(PREFIX + "§a传送石数据已保存。");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (sender.hasPermission("rabi.warpstone.admin") && args.length == 1) {
            return Collections.singletonList("save");
        }
        return Collections.emptyList();
    }
}