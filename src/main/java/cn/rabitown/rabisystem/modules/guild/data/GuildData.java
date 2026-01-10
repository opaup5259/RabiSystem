package cn.rabitown.rabisystem.modules.guild.data;

import org.bukkit.Material;

import java.util.*;

public class GuildData {
    private final UUID id;
    private String name;
    private Material icon;
    private GuildType type;
    private String password; // 仅 PASSWORD 模式有效
    private final Map<UUID, GuildMember> members = new HashMap<>();
    private final Map<UUID, Long> invites = new HashMap<>(); // 受邀玩家UUID -> 过期时间

    public GuildData(UUID id, String name, Material icon, GuildType type, UUID creator) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.type = type;
        this.members.put(creator, new GuildMember(creator, GuildRank.LEADER, System.currentTimeMillis()));
    }

    // Constructor for loading
    public GuildData(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
    public GuildType getType() { return type; }
    public void setType(GuildType type) { this.type = type; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Map<UUID, GuildMember> getMembers() { return members; }
    public Map<UUID, Long> getInvites() { return invites; }

    public GuildMember getMember(UUID uuid) { return members.get(uuid); }
    public boolean hasMember(UUID uuid) { return members.containsKey(uuid); }
    public void addMember(UUID uuid, GuildRank rank) {
        members.put(uuid, new GuildMember(uuid, rank, System.currentTimeMillis()));
        invites.remove(uuid); // 进队后移除邀请
    }
    public void removeMember(UUID uuid) { members.remove(uuid); }

    public void addInvite(UUID uuid) {
        // 48小时 = 48 * 60 * 60 * 1000
        invites.put(uuid, System.currentTimeMillis() + 172800000L);
    }
    public boolean isInvited(UUID uuid) {
        if (!invites.containsKey(uuid)) return false;
        if (System.currentTimeMillis() > invites.get(uuid)) {
            invites.remove(uuid);
            return false;
        }
        return true;
    }

    public UUID getOwner() {
        return members.entrySet().stream()
                .filter(e -> e.getValue().getRank() == GuildRank.LEADER)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
}