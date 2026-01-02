package cn.rabitown.rabisystem.modules.warpStone.data;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WarpStone {
    private String name;
    private final Location location;
    private final UUID owner;
    private boolean isPublic;
    private final long created;
    private final Set<UUID> whitelist = new HashSet<>();
    private Material icon = Material.LODESTONE;

    public WarpStone(String name, Location location, UUID owner, boolean isPublic, long created) {
        this.name = name;
        this.location = location;
        this.owner = owner;
        this.isPublic = isPublic;
        this.created = created;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Location getLocation() { return location; }
    public UUID getOwner() { return owner; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public long getCreated() { return created; }
    public Set<UUID> getWhitelist() { return whitelist; }
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
}