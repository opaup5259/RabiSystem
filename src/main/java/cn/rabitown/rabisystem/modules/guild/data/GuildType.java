package cn.rabitown.rabisystem.modules.guild.data;

import org.bukkit.Material;

public enum GuildType {
    INVITE("私密 (邀请制)", Material.IRON_DOOR),
    PASSWORD("密码 (需验证)", Material.OAK_DOOR),
    PUBLIC("公开 (自由加入)", Material.OAK_FENCE_GATE);

    private final String display;
    private final Material icon;

    GuildType(String display, Material icon) {
        this.display = display;
        this.icon = icon;
    }

    public String getDisplay() { return display; }
    public Material getIcon() { return icon; }
}