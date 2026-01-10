package cn.rabitown.rabisystem.modules.guild.manager;

import cn.rabitown.rabisystem.modules.guild.GuildModule;
import cn.rabitown.rabisystem.modules.guild.data.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildManager {

    private final GuildModule module;
    // GuildID -> Data
    private final Map<UUID, GuildData> guilds = new ConcurrentHashMap<>();
    // PlayerUUID -> GuildID (缓存)
    private final Map<UUID, UUID> playerGuildMap = new ConcurrentHashMap<>();

    // 正在进行创建/改名等操作的玩家缓存
    private final Map<UUID, InputSession> inputSessions = new ConcurrentHashMap<>();

    public GuildManager(GuildModule module) {
        this.module = module;
    }

    public void load() {
        module.getConfigManager().loadGuilds(guilds, playerGuildMap);
    }

    public void save() {
        module.getConfigManager().saveGuilds(guilds);
    }

    public GuildData getPlayerGuild(UUID playerUUID) {
        UUID guildId = playerGuildMap.get(playerUUID);
        return guildId == null ? null : guilds.get(guildId);
    }

    public GuildData getGuild(UUID guildId) {
        return guilds.get(guildId);
    }

    public Map<UUID, GuildData> getAllGuilds() {
        return guilds;
    }

    public void createGuild(Player creator, String name) {
        if (getPlayerGuild(creator.getUniqueId()) != null) {
            creator.sendMessage("§c你已经加入了一个公会！");
            return;
        }

        UUID id = UUID.randomUUID();
        GuildData guild = new GuildData(id, name, Material.GRASS_BLOCK, GuildType.INVITE, creator.getUniqueId());

        guilds.put(id, guild);
        playerGuildMap.put(creator.getUniqueId(), id);
        save();

        creator.sendMessage("§a[Guild] 成功创建公会: " + name);
    }

    public void disbandGuild(Player leader) {
        GuildData guild = getPlayerGuild(leader.getUniqueId());
        if (guild == null) return;

        // 移除所有成员映射
        for (UUID memberId : guild.getMembers().keySet()) {
            playerGuildMap.remove(memberId);
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) p.sendMessage("§c[Guild] 公会 " + guild.getName() + " 已解散。");
        }

        guilds.remove(guild.getId());
        save();
    }

    public void invitePlayer(Player inviter, Player target) {
        GuildData guild = getPlayerGuild(inviter.getUniqueId());
        if (guild == null) return;

        if (getPlayerGuild(target.getUniqueId()) != null) {
            inviter.sendMessage("§c对方已经加入了公会。");
            return;
        }

        guild.addInvite(target.getUniqueId());
        save();

        inviter.sendMessage("§a已向 " + target.getName() + " 发送邀请。");

        Component msg = Component.text("§e[Guild] §f收到来自 §b" + guild.getName() + " §f的邀请。\n")
                .append(Component.text("§a[点击接受] ").clickEvent(ClickEvent.runCommand("/team accept " + guild.getId().toString())).color(NamedTextColor.GREEN))
                .append(Component.text("  "))
                .append(Component.text("§c[点击拒绝] ").clickEvent(ClickEvent.runCommand("/team deny " + guild.getId().toString())).color(NamedTextColor.RED));

        target.sendMessage(msg);
    }

    public void acceptInvite(Player player, UUID guildId) {
        GuildData guild = guilds.get(guildId);
        if (guild == null) {
            player.sendMessage("§c该公会已不存在。");
            return;
        }
        if (!guild.isInvited(player.getUniqueId())) {
            player.sendMessage("§c邀请已过期或不存在。");
            return;
        }
        if (getPlayerGuild(player.getUniqueId()) != null) {
            player.sendMessage("§c你已经有一个公会了。");
            return;
        }

        guild.addMember(player.getUniqueId(), GuildRank.MEMBER);
        playerGuildMap.put(player.getUniqueId(), guild.getId());
        save();

        player.sendMessage("§a成功加入公会 " + guild.getName() + "！");
        broadcastToGuild(guild, "§e欢迎新成员 " + player.getName() + " 加入公会！");
    }

    public void joinPublicGuild(Player player, GuildData guild) {
        if (guild.getType() != GuildType.PUBLIC) return;

        guild.addMember(player.getUniqueId(), GuildRank.MEMBER);
        playerGuildMap.put(player.getUniqueId(), guild.getId());
        save();

        player.sendMessage("§a成功加入公会 " + guild.getName() + "！");
        broadcastToGuild(guild, "§e新成员 " + player.getName() + " 自由加入了公会。");
    }

    public void broadcastToGuild(GuildData guild, String msg) {
        for (UUID uuid : guild.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    // --- 输入会话 ---
    public void startInput(Player player, InputType type, Object data) {
        inputSessions.put(player.getUniqueId(), new InputSession(type, data));
    }

    public InputSession getInput(Player player) {
        return inputSessions.get(player.getUniqueId());
    }

    public void endInput(Player player) {
        inputSessions.remove(player.getUniqueId());
    }

    public enum InputType { CREATE_NAME, RENAME, PASSWORD }
    public record InputSession(InputType type, Object data) {}
}