package cn.rabitown.rabisystem.modules.warpStone;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.warpStone.data.WarpStone;
import cn.rabitown.rabisystem.modules.warpStone.ui.WarpMenus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class WarpCommand implements ISubCommand {

    private final WarpStoneModule module;

    public WarpCommand(WarpStoneModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        // 允许普通玩家使用基础菜单
        return "rabi.warpstone.use";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此指令只能由玩家执行。");
            return;
        }

        // 默认打开菜单
        WarpMenus menus = new WarpMenus(module.getWarpManager(), module.getPlugin());
        menus.openWarpMenu(player, 1);

        // 如果有额外参数（例如管理员指令），可以在这里扩展
        if (args.length > 0 && sender.hasPermission("rabi.warpstone.admin")) {
            if (args[0].equalsIgnoreCase("save")) {
                module.getConfigManager().saveWarpStones(module.getWarpManager().getWarpStones());
                player.sendMessage("§a[RabiWarp] 数据已保存。");
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