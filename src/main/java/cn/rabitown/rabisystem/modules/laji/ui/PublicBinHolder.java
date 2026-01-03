package cn.rabitown.rabisystem.modules.laji.ui;

import cn.rabitown.rabisystem.modules.laji.data.TrashCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class PublicBinHolder implements InventoryHolder {
    private final int page;
    private final TrashCategory currentCategory;

    public PublicBinHolder(int page, TrashCategory currentCategory) {
        this.page = page;
        this.currentCategory = currentCategory;
    }

    public int getPage() { return page; }
    public TrashCategory getCurrentCategory() { return currentCategory; }

    @Override
    public @NotNull Inventory getInventory() { return null; }
}