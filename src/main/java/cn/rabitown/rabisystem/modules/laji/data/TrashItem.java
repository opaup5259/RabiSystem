package cn.rabitown.rabisystem.modules.laji.data;

import org.bukkit.inventory.ItemStack;

public class TrashItem {
    private final ItemStack item;
    private final long timestamp;
    private final String ownerName;

    public TrashItem(ItemStack item, long timestamp, String ownerName) {
        this.item = item;
        this.timestamp = timestamp;
        this.ownerName = ownerName;
    }

    public ItemStack getItem() { return item; }
    public long getTimestamp() { return timestamp; }
    public String getOwnerName() { return ownerName; }
}