package cn.rabitown.rabisystem.modules.fakeplayer.command;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.fakeplayer.FakePlayerModule;
import cn.rabitown.rabisystem.modules.fakeplayer.data.FakePlayerData;
import cn.rabitown.rabisystem.modules.fakeplayer.manager.FakePlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FakePlayerCommand implements ISubCommand {

    private final FakePlayerModule module;
    private final FakePlayerManager manager;

    public FakePlayerCommand(FakePlayerModule module) {
        this.module = module;
        this.manager = module.getManager();
    }

    @Override
    public String getPermission() {
        return "rabisystem.fakeplayer.use";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        // args[0] 是子操作 (如 join)，因为 "fp" 已经被 CommandManager 截取了

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();

        // --- Join (Create/Spawn) ---
        if (sub.equals("join")) {
            // 基本权限校验已由 ISubCommand 在 CommandManager 中处理，但这里我们保留细粒度检查
            if (!(sender instanceof Player player)) {
                sender.sendMessage("只有玩家可以使用此指令");
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(Component.text("用法: /rs fp join <id>", NamedTextColor.RED));
                return;
            }
            String id = args[1];
            if (manager.exists(id)) {
                // 如果已存在，检查归属
                FakePlayerData data = manager.getData(id);
                if (!canAccess(sender, data, true)) return;
                manager.spawnFakePlayer(id);
                sender.sendMessage(Component.text("假人 " + id + " 正在上线...", NamedTextColor.GREEN));
            } else {
                // 创建新的
                manager.createData(id, player);
                manager.spawnFakePlayer(id);
                sender.sendMessage(Component.text("创建并上线假人: " + id, NamedTextColor.GREEN));
            }
            return;
        }

        // --- List ---
        if (sub.equals("list")) {
            boolean isAdmin = sender.hasPermission("rabisystem.fakeplayer.admin") || sender.isOp();
            sender.sendMessage(Component.text("=== 假人列表 ===", NamedTextColor.GOLD));

            UUID viewerUUID = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;

            manager.getAllData().values().forEach(data -> {
                boolean isOwner = viewerUUID != null && data.getOwner().equals(viewerUUID);
                // 仅显示自己或Admin查看全部
                if (isAdmin || isOwner) {
                    Component row = Component.text("- [" + data.getId() + "] ", isOwner ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                            .append(Component.text("Ow: " + data.getOwnerName() + " ", NamedTextColor.AQUA))
                            .append(Component.text(String.format("Loc: %s %.1f %.1f %.1f",
                                    data.getLocation().getWorld().getName(),
                                    data.getLocation().getX(),
                                    data.getLocation().getY(),
                                    data.getLocation().getZ()), NamedTextColor.GRAY));

                    // 传送按钮
                    Component tpBtn = Component.text("[TP]", NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.runCommand("/rs fp tp " + data.getId()));

                    sender.sendMessage(row.append(tpBtn));
                }
            });
            return;
        }

        // --- 参数检查 ---
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }
        String id = args[1];
        if (!manager.exists(id)) {
            sender.sendMessage(Component.text("不存在该ID的假人。", NamedTextColor.RED));
            return;
        }
        FakePlayerData data = manager.getData(id);

        // --- 基础权限指令 (Owner & Admin) ---

        // Left (下线)
        if (sub.equals("left")) {
            if (!canAccess(sender, data, true)) return;
            manager.despawnFakePlayer(id);
            sender.sendMessage(Component.text("假人已下线。", NamedTextColor.YELLOW));
            return;
        }

        // Remove (删除数据)
        if (sub.equals("remove")) {
            if (!canAccess(sender, data, true)) return;
            manager.removeData(id);
            sender.sendMessage(Component.text("假人数据已删除。", NamedTextColor.RED));
            return;
        }

        // TP (传送到假人)
        if (sub.equals("tp")) {
            if (!(sender instanceof Player p)) return;
            if (!canAccess(sender, data, true)) return;

            Player bot = manager.getFakePlayerEntity(id);
            if (bot != null) {
                p.teleport(bot);
            } else {
                p.teleport(data.getLocation());
                sender.sendMessage(Component.text("假人离线，已传送至最后记录位置。", NamedTextColor.YELLOW));
            }
            return;
        }

        // --- 进阶权限指令 (rabisystem.fakeplayer.main) ---

        // Move (传送假人到我)
        if (sub.equals("move")) {
            if (!canAccess(sender, data, false)) return;
            if (!(sender instanceof Player p)) return;

            Player bot = manager.getFakePlayerEntity(id);
            if (bot != null) {
                bot.teleport(p.getLocation());
                data.setLocation(p.getLocation());
                sender.sendMessage(Component.text("假人已传送至你的位置。", NamedTextColor.GREEN));
            } else {
                data.setLocation(p.getLocation());
                sender.sendMessage(Component.text("假人离线，已更新坐标。", NamedTextColor.YELLOW));
            }
            return;
        }

        // Skin
        if (sub.equals("skin")) {
            if (!canAccess(sender, data, false)) return;
            if (args.length < 3) {
                sender.sendMessage("用法: /rs fp skin <fpid> <skin_name>");
                return;
            }
            manager.setSkin(id, args[2]);
            sender.sendMessage(Component.text("皮肤已更新(需重新上线生效)。", NamedTextColor.GREEN));
            return;
        }

        // Set (God/Equip/Hands)
        if (sub.equals("set")) {
            if (!canAccess(sender, data, false)) return;
            if (args.length < 4) {
                sender.sendMessage("用法: /rs fp set <fpid> <god/equip/righthand/lefthand> <on/off/value>");
                return;
            }
            String type = args[2].toLowerCase();
            Player bot = manager.getFakePlayerEntity(id);
            if (bot == null) {
                sender.sendMessage(Component.text("假人必须在线才能操作此项。", NamedTextColor.RED));
                return;
            }

            if (type.equals("god")) {
                boolean state = args[3].equalsIgnoreCase("on");
                data.setGodMode(state);
                bot.setInvulnerable(state);
                sender.sendMessage("无敌模式: " + state);
            } else if (type.equals("equip")) {
                if (!(sender instanceof Player p)) return;
                bot.getInventory().setArmorContents(p.getInventory().getArmorContents());
                sender.sendMessage("已同步盔甲栏。");
            } else if (type.equals("righthand")) {
                if (!(sender instanceof Player p)) return;
                bot.getInventory().setItemInMainHand(p.getInventory().getItemInMainHand());
                sender.sendMessage("已设置主手物品。");
            } else if (type.equals("lefthand")) {
                if (!(sender instanceof Player p)) return;
                bot.getInventory().setItemInOffHand(p.getInventory().getItemInMainHand());
                sender.sendMessage("已设置副手物品。");
            }
            return;
        }

        // Pickup
        if (sub.equals("pickup")) {
            if (!canAccess(sender, data, false)) return;
            if (args.length < 3) {
                sender.sendMessage("用法: /rs fp <fpid> pickup on/off");
                return;
            }
            boolean pickup = args[2].equalsIgnoreCase("on");
            data.setPickup(pickup);
            Player bot = manager.getFakePlayerEntity(id);
            if (bot != null) bot.setCanPickupItems(pickup);
            sender.sendMessage("拾取物品: " + pickup);
            return;
        }

        // Inv (Inventory)
        if (sub.equals("inv")) {
            if (!canAccess(sender, data, false)) return;
            if (!(sender instanceof Player p)) return;
            Player bot = manager.getFakePlayerEntity(id);
            if (bot != null) {
                p.openInventory(bot.getInventory());
            } else {
                sender.sendMessage("假人离线无法查看背包。");
            }
            return;
        }

        // CMD
        if (sub.equals("cmd")) {
            if (!canAccess(sender, data, false)) return;
            if (args.length < 3) {
                sender.sendMessage("用法: /rs fp <fpid> cmd <指令>");
                return;
            }
            Player bot = manager.getFakePlayerEntity(id);
            if (bot != null) {
                StringBuilder cmdBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    cmdBuilder.append(args[i]).append(" ");
                }
                String cmdToRun = cmdBuilder.toString().trim();
                if (cmdToRun.startsWith("/")) cmdToRun = cmdToRun.substring(1);

                bot.performCommand(cmdToRun);
                sender.sendMessage("已让假人执行: " + cmdToRun);
            } else {
                sender.sendMessage("假人离线。");
            }
            return;
        }
    }

    private boolean canAccess(CommandSender sender, FakePlayerData data, boolean basicLevel) {
        if (sender.hasPermission("rabisystem.fakeplayer.admin")) return true;

        if (sender instanceof Player p) {
            if (!data.getOwner().equals(p.getUniqueId())) {
                sender.sendMessage(Component.text("你不是该假人的主人。", NamedTextColor.RED));
                return false;
            }
        }

        // 进阶权限检查
        if (!basicLevel && !sender.hasPermission("rabisystem.fakeplayer.main")) {
            sender.sendMessage(Component.text("你需要更多权限(rabisystem.fakeplayer.main)来操作此功能。", NamedTextColor.RED));
            return false;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- RabiFakePlayer Help ---", NamedTextColor.GOLD));
        sender.sendMessage("/rs fp join <id> - 上线假人");
        sender.sendMessage("/rs fp left <id> - 假人离线");
        sender.sendMessage("/rs fp remove <id> - 删除假人");
        sender.sendMessage("/rs fp list - 列表");
        sender.sendMessage("/rs fp tp <id> - 传送");
        sender.sendMessage("/rs fp skin <id> <name> - 换肤");
        sender.sendMessage("/rs fp set <id> god/equip... - 设置状态");
        sender.sendMessage("/rs fp <id> inv - 查看背包");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("join", "left", "remove", "list", "tp", "skin", "set", "inv", "cmd", "move", "help", "pickup").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (List.of("left", "remove", "tp", "skin", "set", "inv", "cmd", "move", "pickup").contains(args[0].toLowerCase())) {
                if (sender instanceof Player p) {
                    boolean isAdmin = p.hasPermission("rabisystem.fakeplayer.admin");
                    return manager.getAllData().values().stream()
                            .filter(d -> isAdmin || d.getOwner().equals(p.getUniqueId()))
                            .map(FakePlayerData::getId)
                            .filter(id -> id.startsWith(args[1]))
                            .collect(Collectors.toList());
                }
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return List.of("god", "equip", "righthand", "lefthand").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}