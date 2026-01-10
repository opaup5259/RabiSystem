package cn.rabitown.rabisystem.modules.guild.data;

public enum GuildRank {
    LEADER("§6⭐ 队长", 3),
    ADMIN("§b🛡 管理", 2),
    MEMBER("§f☺ 成员", 1);

    private final String display;
    private final int level;

    GuildRank(String display, int level) {
        this.display = display;
        this.level = level;
    }

    public String getDisplay() { return display; }
    public int getLevel() { return level; }

    public boolean isAtLeast(GuildRank other) {
        return this.level >= other.level;
    }
}