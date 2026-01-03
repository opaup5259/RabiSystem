package cn.rabitown.rabisystem.core.command;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.api.IRabiModule;
import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.core.manager.ModuleManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleCommand implements ISubCommand {

    private static final String PREFIX = "§8[§b拉比§8] ";

    @Override
    public String getPermission() {
        return "rabisystem.admin";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        // Usage: /rs module <list|enable|disable|reload> [name]
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        ModuleManager manager = RabiSystem.getModuleManager();
        String op = args[0].toLowerCase();

        // --- 查看列表 ---
        if (op.equals("list")) {
            sender.sendMessage("§8§l======== §b§l系统模块状态 §8§l========");
            manager.getAllModules().forEach((id, module) -> {
                boolean enabled = manager.isModuleEnabled(id);
                String status = enabled ? "§a[运行中]" : "§c[已停止]";
                sender.sendMessage("§7- §f" + module.getDisplayName() + " §8(" + id + ") " + status);
            });
            return;
        }

        // --- 其他操作需要模块名 ---
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§c请输入模块ID (如 spirit)。");
            return;
        }

        String moduleId = args[1].toLowerCase();
        IRabiModule module = manager.getModule(moduleId);

        if (module == null) {
            sender.sendMessage(PREFIX + "§c未找到ID为 " + moduleId + " 的模块。");
            return;
        }

        switch (op) {
            case "enable":
                if (manager.enableModule(moduleId)) {
                    sender.sendMessage(PREFIX + "§a模块 " + module.getDisplayName() + " 已启用。");
                } else {
                    sender.sendMessage(PREFIX + "§c启用失败或该模块已在运行中。");
                }
                break;
            case "disable":
                if (manager.disableModule(moduleId)) {
                    sender.sendMessage(PREFIX + "§c模块 " + module.getDisplayName() + " 已禁用。");
                } else {
                    sender.sendMessage(PREFIX + "§c禁用失败或该模块已停止。");
                }
                break;
            case "reload":
                manager.reloadModule(moduleId);
                sender.sendMessage(PREFIX + "§e模块 " + module.getDisplayName() + " 已重载。");
                break;
            default:
                sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §b§lRabiSystem 核心管理 §8§l========");
        sender.sendMessage("§7/rs module list             §f- 查看所有模块状态");
        sender.sendMessage("§7/rs module reload <模块名>  §f- 热重载模块");
        sender.sendMessage("§7/rs module enable <模块名>  §f- 启用模块");
        sender.sendMessage("§7/rs module disable <模块名> §f- 禁用模块");
        sender.sendMessage(" ");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "enable", "disable", "reload");
        }
        if (args.length == 2) {
            return new ArrayList<>(RabiSystem.getModuleManager().getAllModules().keySet());
        }
        return null;
    }
}