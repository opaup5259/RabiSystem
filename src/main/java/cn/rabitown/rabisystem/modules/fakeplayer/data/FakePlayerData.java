package cn.rabitown.rabisystem.modules.fakeplayer.data;

import org.bukkit.Location;
import java.util.UUID;

public class FakePlayerData {
    private String id; // The custom ID (without prefix)
    private String fullId; // With _fakeplayer_ prefix
    private UUID owner; // Owner UUID
    private String ownerName; // Owner Name for display
    private Location location;
    private String skinName;
    private boolean godMode = true;
    private boolean pickup = false;

    public FakePlayerData(String id, UUID owner, String ownerName, Location location) {
        this.id = id;
        this.fullId = "_fakeplayer_" + id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.location = location;
        this.skinName = fullId;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public String getFullId() {
        return fullId;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getSkinName() {
        return skinName;
    }

    public void setSkinName(String skinName) {
        this.skinName = skinName;
    }

    public boolean isGodMode() {
        return godMode;
    }

    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    public boolean isPickup() {
        return pickup;
    }

    public void setPickup(boolean pickup) {
        this.pickup = pickup;
    }
}