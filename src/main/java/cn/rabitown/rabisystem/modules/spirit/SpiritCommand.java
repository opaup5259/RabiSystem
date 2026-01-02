package cn.rabitown.rabisystem.modules.spirit;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpiritCommand implements ISubCommand {

    private final SpiritModule module;

    public SpiritCommand(SpiritModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "lanternspirit.admin"; // 沿用旧权限
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        // 注意：这里的 args[0] 已经是原指令的第一个参数 (例如 "xp")，而不是 "spirit"

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subOp = args[0].toLowerCase();

        // 处理全局保存 (针对此模块)
        if (subOp.equals("saveall")) {
            module.getConfigManager().saveAllData();
            sender.sendMessage("§a[Spirit] 数据已保存。");
            return;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c[Spirit] 找不到玩家: " + args[1]);
            return;
        }

        // 获取 Profile
        SpiritProfile profile = module.getSpiritManager().getProfile(target.getUniqueId());

        // --- 逻辑搬运 (简化展示，请将原 CommandHandler 的 switch 逻辑复制过来) ---
        // 注意：原代码中的 plugin.getConfigManager() 需要改为 module.getConfigManager()

        switch (subOp) {
            case "xp":
                // ... 原有逻辑 (记得替换 plugin 调用) ...
                break;
            // ... 其他 case ...
            default:
                sendHelp(sender);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // ... 搬运原 onTabComplete 逻辑 ...
        // 注意参数索引可能需要调整，因为这里 args 不包含 "spirit"
        return Collections.emptyList(); // 临时占位
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§l======== §3§lRabiSystem - 灵契模块 §8§l========");
        sender.sendMessage("§7/rs spirit xp <玩家> ...   §f- 经验管理");
        sender.sendMessage("§7/rs spirit mood <玩家> ... §f- 心情管理");
        // ... 其他帮助信息
    }

    // ... 其他辅助方法 (handleXpCommand 等) 复制过来 ...
}