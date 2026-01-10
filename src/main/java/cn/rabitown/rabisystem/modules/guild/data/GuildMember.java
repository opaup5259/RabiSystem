package cn.rabitown.rabisystem.modules.guild.data;

import java.util.UUID;

public class GuildMember {
    private final UUID uuid;
    private GuildRank rank;
    private final long joinTime;

    public GuildMember(UUID uuid, GuildRank rank, long joinTime) {
        this.uuid = uuid;
        this.rank = rank;
        this.joinTime = joinTime;
    }

    public UUID getUuid() { return uuid; }
    public GuildRank getRank() { return rank; }
    public void setRank(GuildRank rank) { this.rank = rank; }
    public long getJoinTime() { return joinTime; }
}