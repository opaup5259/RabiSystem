package cn.rabitown.rabisystem.modules.laji.manager;

import cn.rabitown.rabisystem.modules.laji.LajiModule;
import cn.rabitown.rabisystem.modules.laji.data.TrashCategory;
import cn.rabitown.rabisystem.modules.laji.data.TrashItem;
import cn.rabitown.rabisystem.modules.laji.ui.DropGuiHolder;
import cn.rabitown.rabisystem.modules.laji.ui.PublicBinHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class LajiManager {

    private final LajiModule module;
    private final List<TrashItem> trashItems = new ArrayList<>();
    private final Set<Material> filterMaterials = new HashSet<>();
    private final Set<UUID> switchingMenus = new HashSet<>();
    private BukkitTask sweepTask; // 这里定义的是 BukkitTask
    private int secondsRetaining = 0;
    private static final long EXPIRE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LajiManager(LajiModule module) {
        this.module = module;
        reloadData();
    }

    public void reloadData() {
        module.getConfigManager().loadTrashItems(trashItems);
        module.getConfigManager().loadFilters(filterMaterials);
        trashItems.sort(Comparator.comparingLong(TrashItem::getTimestamp).reversed());
        startTasks(); // 这里也同步修改调用
    }

    public void shutdown() {
        if (sweepTask != null) sweepTask.cancel();
        module.getConfigManager().saveTrashItems(trashItems);
    }

    // 1. 修改方法名为 startTasks 以匹配 LajiModule 中的调用
    public void startTasks() {
        if (sweepTask != null) sweepTask.cancel();
        // [修复] 路径修改为 laji.auto-clean-interval
        int cleanInterval = module.getConfigManager().getMainConfig().getInt("laji.auto-clean-interval", 10);
        this.secondsRetaining = cleanInterval * 60;

        // 2. 修复类型不匹配：使用链式调用，将 runTaskTimer 的返回值赋值给 sweepTask
        sweepTask = new BukkitRunnable() {
            @Override
            public void run() {
                secondsRetaining--;
                // 倒计时提醒
                if (secondsRetaining == 60 || secondsRetaining == 30 || secondsRetaining == 10 || secondsRetaining == 3) {
                    // [修复] 路径修改为 laji.msg-countdown
                    String msg = module.getConfigManager().getMainConfig().getString("laji.msg-countdown", "&7[&b垃圾喵&7] &e注意啦！地上的垃圾在 &c{time} &e秒后就要被本喵扫走了喵！");
                    msg = msg.replace("{time}", String.valueOf(secondsRetaining));
                    broadcastLegacy(msg);
                }
                // 执行清理
                if (secondsRetaining <= 0) {
                    performCleanUp();
                    cleanExpiredTrash();
                    secondsRetaining = cleanInterval * 60;
                }
            }
        }.runTaskTimer(module.getPlugin(), 20L, 20L);
    }

    public void cleanExpiredTrash() {
        long now = System.currentTimeMillis();
        boolean removed = trashItems.removeIf(t -> (now - t.getTimestamp()) >= EXPIRE_MS);
        if (removed) {
            module.getConfigManager().saveTrashItems(trashItems);
        }
    }

    public void performCleanUp() {
        int count = 0;
        long now = System.currentTimeMillis();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item itemEntity) {
                    ItemStack item = itemEntity.getItemStack();
                    if (item.getType() != Material.AIR) {
                        if (filterMaterials.contains(item.getType())) {
                            itemEntity.remove();
                            continue;
                        }
                        addTrashInfo(item, "无主之物", now);
                        trashItems.add(0, new TrashItem(item, now, "无主之物"));
                        itemEntity.remove();
                        count++;
                    }
                }
            }
        }
        trashItems.sort(Comparator.comparingLong(TrashItem::getTimestamp).reversed());
        module.getConfigManager().saveTrashItems(trashItems);

        if (count > 0) {
            // [修复] 路径修改为 laji.msg-cleaned
            String msg = module.getConfigManager().getMainConfig().getString("laji.msg-cleaned", "&7[&b垃圾喵&7] &a扫除完毕！本喵一共丢掉了 &e{count} &a个垃圾，如果有不小心丢失的物品，请去公共垃圾桶自己掏喵！~");
            msg = msg.replace("{count}", String.valueOf(count));
            broadcastLegacy(msg);
        }
    }

    public void broadcastLegacy(String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        Bukkit.broadcast(component);
    }

    // --- 物品处理 ---

    public void addTrashInfo(ItemStack item, String ownerName, long now) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(new NamespacedKey(module.getPlugin(), "drop_time"), PersistentDataType.LONG, now);
        meta.getPersistentDataContainer().set(new NamespacedKey(module.getPlugin(), "owner_name"), PersistentDataType.STRING, ownerName);

        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if ("无主之物".equals(ownerName)) {
            lore.add(Component.text("原主人: ", NamedTextColor.GRAY).append(Component.text(ownerName, NamedTextColor.LIGHT_PURPLE)));
        } else {
            lore.add(Component.text("原主人: " + ownerName, NamedTextColor.GRAY));
        }
        lore.add(Component.text("分类: " + getCategory(item.getType()).getName(), NamedTextColor.AQUA));
        lore.add(Component.text("保鲜期: 计算中...", NamedTextColor.GREEN));
        lore.add(Component.text("丢弃时间: " + DATE_FORMAT.format(new Date(now)), NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public void cleanTrashInfo(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(new NamespacedKey(module.getPlugin(), "drop_time"));
        meta.getPersistentDataContainer().remove(new NamespacedKey(module.getPlugin(), "owner_name"));
        if (meta.hasLore()) {
            List<Component> lore = meta.lore();
            if (lore != null && lore.size() >= 5) {
                for (int i = 0; i < 5; i++) lore.remove(lore.size() - 1);
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
    }

    public void updateLoreForDisplay(ItemStack item, long dropTime) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        List<Component> lore = meta.lore();
        long now = System.currentTimeMillis();
        long timeLeft = EXPIRE_MS - (now - dropTime);
        if (timeLeft < 0) timeLeft = 0;
        Duration duration = Duration.ofMillis(timeLeft);
        String timeStr = String.format("%d天%d小时%d分钟", duration.toDays(), duration.toHoursPart(), duration.toMinutesPart());
        for (int i = 0; i < lore.size(); i++) {
            String plainText = LegacyComponentSerializer.legacySection().serialize(lore.get(i));
            if (plainText.contains("保鲜期")) {
                lore.set(i, Component.text("保鲜期: " + timeStr, NamedTextColor.GREEN));
                break;
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public TrashCategory getCategory(Material m) {
        if (m.isEdible() || m == Material.POTION || m == Material.MILK_BUCKET || m == Material.CAKE || m == Material.HONEY_BOTTLE) return TrashCategory.FOOD;
        String name = m.name();
        if (name.endsWith("_SWORD") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.contains("BOW") || name.equals("SHIELD") || name.equals("TRIDENT") || name.equals("CROSSBOW")) return TrashCategory.COMBAT;
        if (name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("FISHING_ROD") || name.equals("FLINT_AND_STEEL") || name.equals("SHEARS") || name.contains("BUCKET") || name.equals("CLOCK") || name.equals("COMPASS") || name.equals("SPYGLASS") || name.equals("SADDLE") || name.equals("NAME_TAG") || name.equals("LEAD")) return TrashCategory.TOOLS;
        if (name.endsWith("_INGOT") || name.endsWith("_NUGGET") || name.endsWith("_DUST") || name.endsWith("_BALL") || m == Material.DIAMOND || m == Material.EMERALD || m == Material.NETHER_STAR || m == Material.STICK || m == Material.LEATHER || name.endsWith("_DYE") || name.equals("PAPER")) return TrashCategory.MATERIALS;
        if (m.isBlock() && !m.isAir()) return TrashCategory.BLOCKS;
        return TrashCategory.OTHERS;
    }

    // --- GUI 操作 ---

    public void openDropGui(Player player) {
        Inventory inv = Bukkit.createInventory(new DropGuiHolder(), 54, Component.text(">> 垃圾桶 <<", NamedTextColor.RED));
        ItemStack whitePane = createPane(Material.WHITE_STAINED_GLASS_PANE, " ");
        ItemStack blackPane = createPane(Material.BLACK_STAINED_GLASS_PANE, "-----------");

        for (int i = 0; i < 9; i++) inv.setItem(i, whitePane);
        ItemStack btn = new ItemStack(Material.BUNDLE);
        ItemMeta btnMeta = btn.getItemMeta();
        btnMeta.displayName(Component.text(">> 掏垃圾 <<", NamedTextColor.GOLD));
        btnMeta.lore(Collections.singletonList(Component.text("我想掏垃圾！", NamedTextColor.YELLOW)));
        btn.setItemMeta(btnMeta);
        inv.setItem(4, btn);
        for (int i = 9; i < 18; i++) inv.setItem(i, blackPane);
        player.openInventory(inv);
    }

    public void openPublicBinGui(Player player, int page, TrashCategory category) {
        Inventory inv = Bukkit.createInventory(new PublicBinHolder(page, category), 54, Component.text("公共垃圾箱 - " + category.getName() + " (第" + (page + 1) + "页)"));

        List<TrashItem> filteredList;
        if (category == TrashCategory.ALL) {
            filteredList = new ArrayList<>(trashItems);
        } else {
            filteredList = trashItems.stream().filter(t -> getCategory(t.getItem().getType()) == category).collect(Collectors.toList());
        }

        int itemsPerPage = 36;
        int totalItems = filteredList.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            TrashItem trash = filteredList.get(i);
            ItemStack displayItem = trash.getItem().clone();
            updateLoreForDisplay(displayItem, trash.getTimestamp());
            inv.setItem(i - startIndex, displayItem);
        }

        ItemStack blackPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inv.setItem(i, blackPane);

        inv.setItem(40, createNavButton(Material.BARRIER, "我要丢垃圾", NamedTextColor.RED));

        if (page > 0) inv.setItem(45, createNavButton(Material.ARROW, "上一页", NamedTextColor.GREEN));
        inv.setItem(46, createCategoryButton(TrashCategory.FOOD, category));
        inv.setItem(47, createCategoryButton(TrashCategory.BLOCKS, category));
        inv.setItem(48, createCategoryButton(TrashCategory.COMBAT, category));
        inv.setItem(49, createCategoryButton(TrashCategory.ALL, category));
        inv.setItem(50, createCategoryButton(TrashCategory.TOOLS, category));
        inv.setItem(51, createCategoryButton(TrashCategory.MATERIALS, category));
        inv.setItem(52, createCategoryButton(TrashCategory.OTHERS, category));
        if (page < totalPages - 1) inv.setItem(53, createNavButton(Material.ARROW, "下一页", NamedTextColor.GREEN));

        player.openInventory(inv);
    }

    private ItemStack createPane(Material mat, String name) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GRAY));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createNavButton(Material mat, String name, NamedTextColor color) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, color));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCategoryButton(TrashCategory cat, TrashCategory current) {
        ItemStack stack = new ItemStack(cat.getIcon());
        ItemMeta meta = stack.getItemMeta();
        if (cat == current) {
            meta.displayName(Component.text("▶ " + cat.getName() + " ◀", NamedTextColor.GREEN));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.displayName(Component.text(cat.getName(), NamedTextColor.YELLOW));
        }
        meta.lore(Collections.singletonList(Component.text("点击筛选此分类", NamedTextColor.GRAY)));
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isTrashCanStructure(Block clicked) {
        Block lectern = null;
        if (clicked.getType() == Material.LECTERN) lectern = clicked;
        else if (clicked.getType() == Material.BARREL) lectern = clicked.getRelative(0, 1, 0);
        if (lectern == null || lectern.getType() != Material.LECTERN) return false;
        Block barrel = lectern.getRelative(0, -1, 0);
        Block wire = lectern.getRelative(0, -2, 0);
        Block rBlock = lectern.getRelative(0, -3, 0);
        return barrel.getType() == Material.BARREL && wire.getType() == Material.REDSTONE_WIRE && rBlock.getType() == Material.REDSTONE_BLOCK;
    }

    public Set<UUID> getSwitchingMenus() { return switchingMenus; }
    public List<TrashItem> getTrashItems() { return trashItems; }
}