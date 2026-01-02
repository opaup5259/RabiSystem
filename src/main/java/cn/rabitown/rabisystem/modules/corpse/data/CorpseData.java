package cn.rabitown.rabisystem.modules.corpse.data;

import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

public class CorpseData {
    public UUID owner;
    public String ownerName;
    public List<ItemStack> mainItems;
    public List<ItemStack> armorItems;
    public ItemStack offhandItem;
    public int exp;
    public String gameMode;
    public String deathTime;
    public String deathCause;
    public long timestamp;
    public int level;
    public UUID hologramUUID;
    public double locX, locY, locZ;

    public CorpseData(UUID owner, String ownerName, List<ItemStack> main, List<ItemStack> armor, ItemStack offhand, int exp, String time, String cause, String gm, long ts, int lvl, double x, double y, double z) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.mainItems = main;
        this.armorItems = armor;
        this.offhandItem = offhand;
        this.exp = exp;
        this.deathTime = time;
        this.deathCause = cause;
        this.gameMode = gm;
        this.timestamp = ts;
        this.level = lvl;
        this.locX = x;
        this.locY = y;
        this.locZ = z;
    }
}