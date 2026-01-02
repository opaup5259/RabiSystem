package cn.rabitown.rabisystem.modules.corpse;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.corpse.data.CorpseData;
import cn.rabitown.rabisystem.modules.corpse.manager.CorpseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CorpseCommand implements ISubCommand {

    private final CorpseModule module;
    private static final String PREFIX = "§8[§c跑尸§8] "; // 统一前缀风格

    public CorpseCommand(CorpseModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "rabisystem.use";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subOp = args[0].toLowerCase();
        CorpseManager manager = module.getCorpseManager();

        // --- 玩家指令 ---
        if (sender instanceof Player player) {
            if (subOp.equals("info")) {
                manager.sendUnifiedSoulMessage(player);
                return;
            }
            if (subOp.equals("tpnear")) {
                if (!manager.getGhosts().contains(player.getUniqueId())) {
                    sender.sendMessage(PREFIX + "§c只有幽灵状态才能使用此指令。");
                    return;
                }
                manager.teleportNearCorpse(player);
                return;
            }
            if (subOp.equals("spawnrevive")) {
                if (!manager.getGhosts().contains(player.getUniqueId())) {
                    sender.sendMessage(PREFIX + "§c只有幽灵状态才能使用此指令。");
                    return;
                }
                player.teleport(player.getWorld().getSpawnLocation());
                manager.restorePlayerStatus(player, true, manager.getSavedGameMode(player));
                player.sendMessage(PREFIX + "§a你在世界出生点重获新生！");
                return;
            }
        }

        // --- 管理员指令检查 ---
        if (!sender.hasPermission("rabisystem.admin")) {
            sender.sendMessage(PREFIX + "§c你没有权限执行此操作。");
            return;
        }

        // --- 全局管理 ---
        switch (subOp) {
            case "saveall":
                module.getConfigManager().saveCorpseData(manager.getCorpseCache(), manager.getGhosts());
                sender.sendMessage(PREFIX + "§a已强制保存所有尸体数据。");
                return;
            case "reload":
                module.getConfigManager().reloadConfig();
                module.getConfigManager().loadCorpseData(manager.getCorpseCache(), manager.getGhosts());
                sender.sendMessage(PREFIX + "§a配置与数据已重载。");
                return;
            case "purge":
                int count = manager.purgeAllHolograms();
                sender.sendMessage(PREFIX + "§a已清理 §e" + count + " §a个全息悬浮字。");
                return;
            case "list":
                handleListCommand(sender, manager);
                return;
        }

        // --- 针对特定尸体的操作 ---
        // 尝试解析 args[1] 为玩家名，或者处理 _act 内部指令
        if (subOp.equals("_act")) {
            handleInternalAction(sender, manager, args);
            return;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return;
        }

        String targetName = args[1];
        // 查找玩家最新的尸体
        Map.Entry<Location, CorpseData> entry = findLatestCorpse(targetName, manager);

        if (entry == null) {
            sender.sendMessage(PREFIX + "§c未找到玩家 " + targetName + " 的尸体数据。");
            return;
        }

        Location loc = entry.getKey();
        CorpseData data = entry.getValue();

        switch (subOp) {
            case "teleport":
            case "tp":
                if (sender instanceof Player p) {
                    p.teleport(loc.clone().add(0, 1, 0));
                    sender.sendMessage(PREFIX + "§a已传送到 " + data.ownerName + " 的尸体位置。");
                } else {
                    sender.sendMessage(PREFIX + "§c控制台无法传送。");
                }
                break;

            case "retrieve": // 管理员拿回物品
                if (sender instanceof Player p) {
                    manager.adminRetrieveCorpse(p, loc);
                    // adminRetrieveCorpse 内部已经包含提示音效和移除逻辑
                } else {
                    sender.sendMessage(PREFIX + "§c控制台无法拾取物品。");
                }
                break;

            case "return": // 归还给玩家
                Player owner = Bukkit.getPlayer(data.owner);
                if (owner != null && owner.isOnline()) {
                    manager.retrieveCorpseAction(owner, loc, data, true); // true 表示是自己拾取，会恢复状态
                    sender.sendMessage(PREFIX + "§a已将尸体物品归还给在线玩家 " + owner.getName() + "。");
                } else {
                    sender.sendMessage(PREFIX + "§c玩家不在线，无法直接归还。请使用 retrieve 取出后手动处理。");
                }
                break;

            case "delete": // 直接删除
                loc.getBlock().setType(org.bukkit.Material.AIR);
                manager.removeHologram(data, loc);
                manager.getCorpseCache().remove(loc);
                module.getConfigManager().saveCorpseData(manager.getCorpseCache(), manager.getGhosts());
                sender.sendMessage(PREFIX + "§c已彻底删除 " + data.ownerName + " 的尸体数据（物品已销毁）。");
                break;

            case "info":
                sendCorpseInfo(sender, data, loc);
                break;

            case "time": // 修改保护时间
                handleTimeCommand(sender, data, args);
                // 修改时间不需要移除尸体，只需要刷新全息图（Task会自动处理）和保存数据
                module.getConfigManager().saveCorpseData(manager.getCorpseCache(), manager.getGhosts());
                break;

            default:
                sendHelp(sender);
                break;
        }
    }

    // --- 逻辑处理方法 ---

    private void handleListCommand(CommandSender sender, CorpseManager manager) {
        sender.sendMessage("§8§m--------------------------------------------------");
        sender.sendMessage("  §6§l尸体管理列表 (共 " + manager.getCorpseCache().size() + " 个)");

        if (manager.getCorpseCache().isEmpty()) {
            sender.sendMessage("  §7(暂无数据)");
            sender.sendMessage("§8§m--------------------------------------------------");
            return;
        }

        int i = 1;
        for (Map.Entry<Location, CorpseData> entry : manager.getCorpseCache().entrySet()) {
            Location loc = entry.getKey();
            CorpseData data = entry.getValue();

            // 构造坐标字符串用于点击指令
            String posArgs = String.format("%d %d %d %s", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());

            Component line = Component.text("  " + i + ". ", NamedTextColor.GRAY)
                    .append(Component.text(data.ownerName, NamedTextColor.YELLOW))
                    .append(Component.text(" [Lv." + data.level + "]", NamedTextColor.AQUA))
                    .append(Component.space())
                    .append(Component.text("[传送]", NamedTextColor.GREEN)
                            .hoverEvent(HoverEvent.showText(Component.text("§a点击传送")))
                            .clickEvent(ClickEvent.runCommand("/rs corpse _act teleport " + posArgs)))
                    .append(Component.space())
                    .append(Component.text("[拾取]", NamedTextColor.GOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("§6点击放入你的背包")))
                            .clickEvent(ClickEvent.runCommand("/rs corpse _act retrieve " + posArgs)))
                    .append(Component.space())
                    .append(Component.text("[删除]", NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(Component.text("§c点击彻底删除")))
                            .clickEvent(ClickEvent.runCommand("/rs corpse _act delete " + posArgs)));

            sender.sendMessage(line);
            i++;
        }
        sender.sendMessage("§8§m--------------------------------------------------");
    }

    private void handleTimeCommand(CommandSender sender, CorpseData data, String[] args) {
        // args: time <player> <add|set> <minutes>
        if (args.length < 4) {
            sender.sendMessage(PREFIX + "§c用法: /rs corpse time <玩家> <add|set> <分钟>");
            return;
        }
        try {
            long minutes = Long.parseLong(args[3]);
            long millis = minutes * 60 * 1000;

            if (args[2].equalsIgnoreCase("add")) {
                // 增加剩余时间 = 增加 timestamp (让死的时间变晚？不对)
                // 剩余时间 = (protection * 60000) - (now - timestamp)
                // 要增加剩余时间，需要让 (now - timestamp) 变小，即 timestamp 变大
                data.timestamp += millis;
                sender.sendMessage(PREFIX + "§a已延长保护时间 " + minutes + " 分钟。");
            } else if (args[2].equalsIgnoreCase("set")) {
                // 设置剩余时间为 X 分钟
                // X * 60000 = total_protect - (now - timestamp)
                // now - timestamp = total_protect - X * 60000
                // timestamp = now - total_protect + X * 60000
                long protectConfig = module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000;
                data.timestamp = System.currentTimeMillis() - protectConfig + millis;
                sender.sendMessage(PREFIX + "§a已设置剩余保护时间为 " + minutes + " 分钟。");
            } else {
                sender.sendMessage(PREFIX + "§c操作类型错误，请使用 add 或 set。");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + "§c请输入有效的数字。");
        }
    }

    private void sendCorpseInfo(CommandSender sender, CorpseData data, Location loc) {
        sender.sendMessage("§8§m--------------------------------");
        sender.sendMessage(" §6§l尸体详情: §e" + data.ownerName);
        sender.sendMessage(" §7死因: §f" + data.deathCause);
        sender.sendMessage(" §7死亡时间: §f" + data.deathTime);
        sender.sendMessage(" §7位置: §f" + loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        sender.sendMessage(" §7经验: §a" + data.exp);
        sender.sendMessage(" §7等级: §a" + data.level);

        long protectTotal = module.getConfigManager().getConfig().getLong("corpse.protection-minutes", 60) * 60 * 1000;
        long elapsed = System.currentTimeMillis() - data.timestamp;
        long remaining = protectTotal - elapsed;

        if (remaining > 0) {
            sender.sendMessage(" §7保护剩余: §a" + (remaining / 60000) + "分钟");
        } else {
            sender.sendMessage(" §7保护状态: §c已过期");
        }
        sender.sendMessage("§8§m--------------------------------");
    }

    // 处理内部点击指令: /rs corpse _act <action> <x> <y> <z> <world>
    private void handleInternalAction(CommandSender sender, CorpseManager manager, String[] args) {
        if (args.length < 6) return;
        try {
            String act = args[1];
            int x = Integer.parseInt(args[2]);
            int y = Integer.parseInt(args[3]);
            int z = Integer.parseInt(args[4]);
            String worldName = args[5];
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                sender.sendMessage(PREFIX + "§c世界 " + worldName + " 不存在。");
                return;
            }

            Location targetLoc = null;
            // 精确匹配
            for (Location key : manager.getCorpseCache().keySet()) {
                if (key.getWorld().getName().equals(w.getName()) &&
                        key.getBlockX() == x && key.getBlockY() == y && key.getBlockZ() == z) {
                    targetLoc = key;
                    break;
                }
            }

            if (targetLoc == null) {
                sender.sendMessage(PREFIX + "§c该位置的尸体已不存在。");
                return;
            }

            CorpseData data = manager.getCorpseCache().get(targetLoc);

            switch (act) {
                case "teleport":
                    if (sender instanceof Player p) {
                        p.teleport(targetLoc.clone().add(0, 1, 0));
                        sender.sendMessage(PREFIX + "§a已传送。");
                    }
                    break;
                case "retrieve":
                    if (sender instanceof Player p) {
                        manager.adminRetrieveCorpse(p, targetLoc);
                    }
                    break;
                case "delete":
                    targetLoc.getBlock().setType(org.bukkit.Material.AIR);
                    manager.removeHologram(data, targetLoc);
                    manager.getCorpseCache().remove(targetLoc);
                    module.getConfigManager().saveCorpseData(manager.getCorpseCache(), manager.getGhosts());
                    sender.sendMessage(PREFIX + "§c已删除尸体。");
                    break;
            }

        } catch (Exception e) {
            sender.sendMessage(PREFIX + "§c操作执行失败。");
        }
    }

    // 辅助：查找玩家最新尸体
    private Map.Entry<Location, CorpseData> findLatestCorpse(String playerName, CorpseManager manager) {
        Map.Entry<Location, CorpseData> latest = null;
        for (Map.Entry<Location, CorpseData> entry : manager.getCorpseCache().entrySet()) {
            if (entry.getValue().ownerName.equalsIgnoreCase(playerName)) {
                if (latest == null || entry.getValue().timestamp > latest.getValue().timestamp) {
                    latest = entry;
                }
            }
        }
        return latest;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §c§l跑尸系统 管理指令 §8§l========");
        if (sender.hasPermission("rabisystem.admin")) {
            sender.sendMessage("§7/rs corpse list                         §f- 列出所有尸体(可点击操作)");
            sender.sendMessage("§7/rs corpse teleport <玩家>              §f- 传送到玩家最新尸体");
            sender.sendMessage("§7/rs corpse retrieve <玩家>              §f- 强制拾取(到自己背包)");
            sender.sendMessage("§7/rs corpse return <玩家>                §f- 归还尸体(给在线玩家)");
            sender.sendMessage("§7/rs corpse info <玩家>                  §f- 查看详细数据");
            sender.sendMessage("§7/rs corpse time <玩家> <add|set> <分>   §f- 修改保护时间");
            sender.sendMessage("§7/rs corpse delete <玩家>                §f- 删除尸体(不掉落)");
            sender.sendMessage("§7/rs corpse saveall                      §f- 强制保存数据");
            sender.sendMessage("§7/rs corpse reload                       §f- 重载配置");
            sender.sendMessage("§7/rs corpse purge                        §f- 清理全息文本");
        }
        sender.sendMessage("§7/rs corpse info                         §f- 查看自己的尸体信息");
        sender.sendMessage("§7/rs corpse tpnear                       §f- (幽灵) 传送到尸体附近");
        sender.sendMessage("§7/rs corpse spawnrevive                  §f- (幽灵) 在出生点复活");
        sender.sendMessage(" ");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("info");
            list.add("tpnear");
            list.add("spawnrevive");
            if (sender.hasPermission("rabisystem.admin")) {
                list.addAll(Arrays.asList("list", "teleport", "retrieve", "return", "delete", "time", "saveall", "reload", "purge"));
            }
            return list;
        }

        if (args.length == 2) {
            if (Arrays.asList("teleport", "retrieve", "return", "delete", "time", "info", "tp").contains(args[0].toLowerCase())
                    && sender.hasPermission("rabisystem.admin")) {
                // 补全所有有尸体的玩家名字
                return module.getCorpseManager().getCorpseCache().values().stream()
                        .map(data -> data.ownerName)
                        .distinct()
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("time") && sender.hasPermission("rabisystem.admin")) {
            return Arrays.asList("add", "set");
        }

        return Collections.emptyList();
    }
}