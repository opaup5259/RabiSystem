// File: src/main/java/cn/rabitown/rabisystem/modules/spirit/SpiritCommand.java
package cn.rabitown.rabisystem.modules.spirit;

import cn.rabitown.rabisystem.api.ISubCommand;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpiritCommand implements ISubCommand {

    private final SpiritModule module;
    private static final String PREFIX = "§8[§d灵契§8] "; // 统一前缀

    public SpiritCommand(SpiritModule module) {
        this.module = module;
    }

    @Override
    public String getPermission() {
        return "lanternspirit.admin";
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        // args[0] 是子命令 (如 xp, mood)，因为 CommandManager 已经去掉了 "spirit"

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subOp = args[0].toLowerCase();

        // --- 全局保存指令 ---
        if (subOp.equals("saveall")) {
            module.getConfigManager().saveAllData();
            sender.sendMessage(PREFIX + "§a已强制保存所有在线玩家数据到磁盘。");
            return;
        }

        // --- 玩家相关指令 ---
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "§c找不到在线玩家: " + args[1]);
            return;
        }

        // 获取档案
        SpiritProfile profile = module.getSpiritManager().getProfile(target.getUniqueId());

        switch (subOp) {
            case "xp":
                handleXpCommand(sender, target, profile, args);
                break;
            case "mood":
                handleMoodCommand(sender, target, profile, args);
                break;
            case "health":
                handleHealthCommand(sender, target, profile, args);
                break;
            case "name":
                handleNameCommand(sender, target, profile, args);
                break;
            case "bonus":
                handleBonusCommand(sender, target, profile, args);
                break;
            case "checkin":
                handleCheckInCommand(sender, target, profile, args);
                break;
            case "effect":
                handleEffectCommand(sender, target, profile, args);
                break;
            case "dead":
                if (args.length >= 3 && args[2].equalsIgnoreCase("clear")) {
                    profile.setReunionExpireTime(0);
                    sender.sendMessage(PREFIX + "§a已清除玩家 " + target.getName() + " 的重聚冷却。");
                    target.sendMessage("§b✨ 神秘力量拂过，你的小精灵已重聚完成！");
                    module.getConfigManager().saveProfile(profile);
                } else {
                    sender.sendMessage("§c用法: /rs spirit dead clear <玩家>");
                }
                break;
            case "kill":
                module.getSpiritManager().killSpirit(target.getUniqueId());
                sender.sendMessage(PREFIX + "§a已强制击碎玩家 " + target.getName() + " 的小精灵。");
                break;
            case "save":
                module.getConfigManager().saveProfile(profile);
                sender.sendMessage(PREFIX + "§a已保存玩家 " + target.getName() + " 的数据。");
                break;
            default:
                sendHelp(sender);
                break;
        }
    }

    // --- 各个子指令的处理逻辑 (移植版) ---

    private void handleXpCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c用法: /rs spirit xp <玩家> <add|set> <数值>");
            return;
        }
        try {
            double amount = Double.parseDouble(args[3]);
            if (args[2].equalsIgnoreCase("add")) {
                profile.addExp(amount);
                sender.sendMessage(PREFIX + "§a已增加经验，当前等级: Lv." + profile.getLevel());
            } else if (args[2].equalsIgnoreCase("set")) {
                profile.setExp(amount);
                sender.sendMessage(PREFIX + "§a已设置经验，当前等级: Lv." + profile.getLevel());
            }
            module.getConfigManager().saveProfile(profile);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值无效。");
        }
    }

    private void handleMoodCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c用法: /rs spirit mood <玩家> <add|set> <数值>");
            return;
        }
        try {
            int amount = Integer.parseInt(args[3]);
            if (args[2].equalsIgnoreCase("add")) {
                profile.addMood(amount);
                sender.sendMessage(PREFIX + "§a心情已增加，当前: " + profile.getMood());
            } else if (args[2].equalsIgnoreCase("set")) {
                profile.setMood(amount);
                sender.sendMessage(PREFIX + "§a心情已设置，当前: " + profile.getMood());
            }
            module.getConfigManager().saveProfile(profile);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值无效。");
        }
    }

    private void handleHealthCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("set")) {
            sender.sendMessage("§c用法: /rs spirit health <玩家> set <数值>");
            return;
        }
        try {
            double val = Double.parseDouble(args[3]);
            profile.setHealth(val);
            Allay spirit = module.getSpiritManager().getSpiritEntity(target.getUniqueId());
            if (spirit != null && spirit.isValid()) {
                spirit.setHealth(Math.min(val, profile.getMaxHealth()));
            }
            sender.sendMessage(PREFIX + "§a血量已设置。");
            module.getConfigManager().saveProfile(profile);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值无效。");
        }
    }

    private void handleNameCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /rs spirit name <玩家> <新名字>");
            return;
        }
        String newName = args[2].replace("&", "§");
        profile.setName(newName);
        Allay spirit = module.getSpiritManager().getSpiritEntity(target.getUniqueId());
        if (spirit != null && spirit.isValid()) {
            spirit.customName(Component.text(newName));
        }
        sender.sendMessage(PREFIX + "§a已将小精灵改名为: " + newName);
        module.getConfigManager().saveProfile(profile);
    }

    private void handleBonusCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("set")) {
            sender.sendMessage("§c用法: /rs spirit bonus <玩家> set <数值>");
            return;
        }
        try {
            double val = Double.parseDouble(args[3]);
            profile.setExtraExpBonus(val);
            sender.sendMessage(PREFIX + "§a额外经验加成已设置为: " + val);
            module.getConfigManager().saveProfile(profile);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值无效。");
        }
    }

    private void handleCheckInCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 3) {
            sendCheckInHelp(sender);
            return;
        }
        String action = args[2].toLowerCase();

        // Clear 操作
        if (action.equals("clear")) {
            profile.getCheckInHistory().clear();
            profile.setTotalCheckIns(0);
            profile.setConsecutiveDays(0);
            profile.setLastCheckInMillis(0);
            profile.getReceivedHolidayCards().clear();
            sender.sendMessage(PREFIX + "§a已清空玩家 " + target.getName() + " 的所有签到历史与统计。");
            module.getConfigManager().saveProfile(profile);
            return;
        }

        // 其他操作需要 5 个参数
        if (args.length < 5) {
            sendCheckInHelp(sender);
            return;
        }
        try {
            int value = Integer.parseInt(args[4]);
            String subOp = args[3].toLowerCase();
            switch (action) {
                case "lottery":
                    if (subOp.equals("add")) profile.setLotteryChances(profile.getLotteryChances() + value);
                    else if (subOp.equals("set")) profile.setLotteryChances(value);
                    sender.sendMessage(PREFIX + "§a抽奖次数已更新: " + profile.getLotteryChances());
                    break;
                case "card":
                    if (subOp.equals("add")) profile.addReplacementCards(value);
                    else if (subOp.equals("set")) profile.setReplacementCards(value);
                    sender.sendMessage(PREFIX + "§a补签卡数量已更新: " + profile.getReplacementCards());
                    break;
                case "total":
                    if (subOp.equals("add")) profile.setTotalCheckIns(profile.getTotalCheckIns() + value);
                    else if (subOp.equals("set")) profile.setTotalCheckIns(value);
                    sender.sendMessage(PREFIX + "§a总签到次数已更新: " + profile.getTotalCheckIns());
                    break;
                case "streak":
                    if (subOp.equals("set")) profile.setConsecutiveDays(value);
                    sender.sendMessage(PREFIX + "§a连续签到天数已更新: " + profile.getConsecutiveDays());
                    break;
                default:
                    sendCheckInHelp(sender);
                    return;
            }
            module.getConfigManager().saveProfile(profile);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值无效。");
        }
    }

    private void handleEffectCommand(CommandSender sender, Player target, SpiritProfile profile, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /rs spirit effect <玩家> <unlock|set|clear> [特效ID]");
            return;
        }

        String action = args[2].toLowerCase();

        if (action.equals("clear")) {
            profile.getUnlockedEffects().clear();
            sender.sendMessage(PREFIX + "§a已清空玩家 " + target.getName() + " 的所有额外解锁特效。");
            module.getConfigManager().saveProfile(profile);
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§c请指定特效ID (按Tab可补全)。");
            return;
        }

        String effectId = args[3];
        SpiritEffectType type = SpiritEffectType.fromId(effectId);

        if (type == SpiritEffectType.NONE && !effectId.equals("0")) { // 简单的校验
            // fromId 默认返回 NONE，如果输入的不是 0 且返回了 NONE，说明找不到
            // 但其实 NONE 本身也是一个有效类型，这里宽容处理，或者你可以在 fromId 中增加 null 返回值
        }

        if (action.equals("unlock")) {
            profile.addUnlockedEffect(effectId);
            sender.sendMessage(PREFIX + "§a已为 " + target.getName() + " 强制解锁特效: " + type.getName());
            target.sendMessage("§d✨ 管理员为你解锁了新特效: " + type.getName());
        } else if (action.equals("set")) {
            profile.setActiveEffect(type);
            sender.sendMessage(PREFIX + "§a已将 " + target.getName() + " 的特效设置为: " + type.getName());
        } else {
            sender.sendMessage("§c未知操作: " + action);
        }
        module.getConfigManager().saveProfile(profile);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage("§8§l======== §d§l灯火灵契 管理指令 §8§l========");
        sender.sendMessage("§7/rs spirit xp <玩家> <add|set> <数值>   §f- 修改经验");
        sender.sendMessage("§7/rs spirit mood <玩家> <add|set> <数值> §f- 修改心情");
        sender.sendMessage("§7/rs spirit health <玩家> set <数值>     §f- 设置血量");
        sender.sendMessage("§7/rs spirit effect <玩家> ...            §f- 特效管理");
        sender.sendMessage("§7/rs spirit name <玩家> <新名字>         §f- 强制改名");
        sender.sendMessage("§7/rs spirit bonus <玩家> set <数值>      §f- 设置经验加成池");
        sender.sendMessage("§7/rs spirit checkin <玩家> ...           §f- 签到数据管理");
        sender.sendMessage("§7/rs spirit dead clear <玩家>            §f- 清除重聚CD");
        sender.sendMessage("§7/rs spirit kill <玩家>                  §f- 强制击碎小精灵");
        sender.sendMessage("§7/rs spirit save <玩家>                  §f- 保存指定数据");
        sender.sendMessage("§7/rs spirit saveall                      §f- 保存所有数据");
        sender.sendMessage(" ");
    }

    private void sendCheckInHelp(CommandSender sender) {
        sender.sendMessage("§e--- 签到数据管理 ---");
        sender.sendMessage("§7/rs spirit checkin <玩家> clear               §f- 清空历史与统计");
        sender.sendMessage("§7/rs spirit checkin <玩家> card <add|set> <数> §f- 修改补签卡");
        sender.sendMessage("§7/rs spirit checkin <玩家> total set <数>      §f- 修改总签到数");
        sender.sendMessage("§7/rs spirit checkin <玩家> streak set <数>     §f- 修改连签天数");
        sender.sendMessage("§7/rs spirit checkin <玩家> lottery <add|set> <数> §f- 修改抽奖次数");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // args[0] = subOp
        if (args.length == 1) {
            return Arrays.asList("xp", "mood", "health", "name", "effect", "bonus", "checkin", "dead", "kill", "save", "saveall", "help");
        }

        // args[1] = player
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("saveall") || args[0].equalsIgnoreCase("help")) return Collections.emptyList();
            return null; // 默认补全在线玩家
        }

        String sub = args[0].toLowerCase();
        // args[2] = sub-action
        if (args.length == 3) {
            if (sub.equals("xp") || sub.equals("mood") || sub.equals("checkin")) {
                if (sub.equals("checkin")) return Arrays.asList("clear", "card", "total", "streak", "lottery");
                return Arrays.asList("add", "set");
            }
            if (sub.equals("health") || sub.equals("bonus")) return Collections.singletonList("set");
            if (sub.equals("dead")) return Collections.singletonList("clear");
            if (sub.equals("effect")) return Arrays.asList("unlock", "set", "clear");
        }

        // args[3] = value or id
        if (args.length == 4) {
            if (sub.equals("checkin")) {
                String checkAction = args[2].toLowerCase();
                if (checkAction.equals("card") || checkAction.equals("lottery")) return Arrays.asList("add", "set");
                if (checkAction.equals("total") || checkAction.equals("streak")) return Collections.singletonList("set");
            }
            if (sub.equals("effect")) {
                String effectAction = args[2].toLowerCase();
                if (effectAction.equals("unlock") || effectAction.equals("set")) {
                    return Arrays.stream(SpiritEffectType.values())
                            .map(SpiritEffectType::getId)
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}