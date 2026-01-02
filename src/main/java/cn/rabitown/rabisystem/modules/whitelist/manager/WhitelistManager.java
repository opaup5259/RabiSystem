package cn.rabitown.rabisystem.modules.whitelist.manager;

import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WhitelistManager {

    private final WhitelistModule module;
    private final Set<String> whitelistCache = new HashSet<>();
    private final Set<String> acceptedCache = new HashSet<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WhitelistManager(WhitelistModule module) {
        this.module = module;
        reload();
    }

    public void reload() {
        module.getConfigManager().loadLists(whitelistCache, acceptedCache);
    }

    public void save() {
        module.getConfigManager().saveData(whitelistCache, acceptedCache);
    }

    public Set<String> getWhitelistCache() {
        return whitelistCache;
    }

    // --- 逻辑判定 ---

    public boolean isRestricted(Player player) {
        if (player.isOp()) return false; // OP 豁免
        String name = player.getName();
        return !whitelistCache.contains(name) || !acceptedCache.contains(name);
    }

    public boolean isVisitor(Player player) {
        return !whitelistCache.contains(player.getName());
    }

    public boolean hasAccepted(Player player) {
        return acceptedCache.contains(player.getName());
    }

    // --- 业务操作 ---

    public void performAccept(Player player) {
        if (isVisitor(player)) {
            player.sendMessage(mm.deserialize("<red>你不在白名单中，无法接受规则。"));
            return;
        }
        if (hasAccepted(player)) {
            player.sendMessage(mm.deserialize(getString("whitelist.messages.already-accepted")));
            return;
        }

        acceptedCache.add(player.getName());
        save(); // 实时保存

        player.sendMessage(mm.deserialize(getString("whitelist.messages.accept-success")));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); // 增加成就音效，更喜庆

        // [优化] 不再使用 on-accept-commands (预留代码，暂不执行)
        List<String> commands = module.getPlugin().getConfig().getStringList("whitelist.on-accept-commands");
        for (String cmd : commands) {
            String finalCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }

        // [新增] 直接广播美化后的欢迎消息
        Component welcomeMsg = mm.deserialize(
                "<newline>   <gradient:#ff5555:#ffaa00><b>[RabiWL]</b></gradient> <gray>✦ 欢迎新伙伴 <aqua>" + player.getName() + "</aqua> 正式加入小兔社区！ ✦   <newline>"
        );
        Bukkit.broadcast(welcomeMsg);
    }

    public void performDeny(Player player) {
        String action = module.getPlugin().getConfig().getString("whitelist.on-deny-action", "KICK");
        if ("KICK".equalsIgnoreCase(action)) {
            player.kick(mm.deserialize(getString("whitelist.messages.deny-kick")));
        } else {
            player.sendMessage(mm.deserialize(getString("whitelist.messages.deny-message")));
        }
    }

    public void addPlayer(String name) {
        whitelistCache.add(name);
        save();

        // --- 在线通知逻辑 ---
        Player onlinePlayer = Bukkit.getPlayer(name); // 尝试获取在线玩家
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            // 1. 获取 pending-rules 消息列表
            List<String> msgList = module.getPlugin().getConfig().getStringList("whitelist.messages.pending-rules");

            // 2. 发送消息
            for (String line : msgList) {
                onlinePlayer.sendMessage(mm.deserialize(line.replace("{player}", onlinePlayer.getName())));
            }

            // 3. 播放提示音 (叮~)
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
    }

    public void removePlayer(String name) {
        whitelistCache.remove(name);
        acceptedCache.remove(name); // 同时移除同意记录
        save();
    }

    private String getString(String path) {
        return module.getPlugin().getConfig().getString(path, "<red>Config Missing: " + path);
    }
}