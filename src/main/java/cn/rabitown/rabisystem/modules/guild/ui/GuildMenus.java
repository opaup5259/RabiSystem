package cn.rabitown.rabisystem.modules.guild.ui;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.guild.data.*;
import cn.rabitown.rabisystem.modules.guild.manager.GuildManager;
import cn.rabitown.rabisystem.modules.playtime.PlayTimeModule;
import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GuildMenus {

    // Helper to get manager
    private static GuildManager getManager() {
        return ((cn.rabitown.rabisystem.modules.guild.GuildModule) RabiSystem.getModuleManager().getModule("guild")).getManager();
    }

    // Custom Holder
    public static class GuildHolder implements InventoryHolder {
        private final String type;
        private final int page;
        private final Object data;

        public GuildHolder(String type, int page, Object data) { this.type = type; this.page = page; this.data = data; }
        public String getType() { return type; }
        public int getPage() { return page; }
        public Object getData() { return data; }
        @Override public Inventory getInventory() { return null; }
    }

    // --- 1. 创建/寻找公会 (No Guild) ---
    public static void openNoGuildMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new GuildHolder("NO_GUILD", 0, null), 27, Component.text("§8公会系统"));

        // Create
        inv.setItem(11, createItem(Material.ANVIL, "§a§l创建小队", "§7创建一个新的队伍，", "§7邀请伙伴一起冒险！"));

        // Find
        inv.setItem(13, createItem(Material.SPYGLASS, "§e§l寻找小队", "§7查看公开或需密码的队伍。"));

        // Pending Invites (Check if any)
        boolean hasInvite = false;
        for (GuildData g : getManager().getAllGuilds().values()) {
            if (g.isInvited(player.getUniqueId())) { hasInvite = true; break; }
        }
        if (hasInvite) {
            inv.setItem(15, createItem(Material.PAPER, "§b§l查看邀请", "§7你有待处理的入队邀请！"));
        } else {
            inv.setItem(15, createItem(Material.BARRIER, "§7暂无邀请"));
        }

        fillBg(inv);
        player.openInventory(inv);
    }

    // --- 2. 寻找小队列表 ---
    public static void openFinderMenu(Player player, int page) {
        List<GuildData> visibleGuilds = getManager().getAllGuilds().values().stream()
                .filter(g -> g.getType() != GuildType.INVITE)
                .collect(Collectors.toList());

        Inventory inv = Bukkit.createInventory(new GuildHolder("FINDER", page, null), 54, Component.text("§8寻找公会 - 第 " + (page + 1) + " 页"));

        int start = page * 45;
        int end = Math.min(start + 45, visibleGuilds.size());

        for (int i = start; i < end; i++) {
            GuildData g = visibleGuilds.get(i);
            String status = g.getType() == GuildType.PUBLIC ? "§a[点击加入]" : "§e[点击输入密码]";
            inv.setItem(i - start, createItem(g.getIcon(), "§6" + g.getName(),
                    "§7队长: " + getName(g.getOwner()),
                    "§7人数: " + g.getMembers().size(),
                    "§7模式: " + g.getType().getDisplay(),
                    "", status));
        }

        addPagination(inv, page, (int) Math.ceil(visibleGuilds.size() / 45.0));
        inv.setItem(49, createItem(Material.IRON_DOOR, "§c返回"));
        player.openInventory(inv);
    }

    // --- 3. 邀请列表 (PlayTime联动) ---
    public static void openInviteList(Player player, int page) {
        GuildData guild = getManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) return;

        // 获取数据
        PlayTimeModule ptModule = (PlayTimeModule) RabiSystem.getModuleManager().getModule("playtime");
        Map<UUID, Long> times = ptModule.getManager().getTotalPlaytimeCache();

        // 排序：在线优先，然后按名字
        List<UUID> players = new ArrayList<>(times.keySet());
        players.removeIf(uuid -> guild.hasMember(uuid)); // 排除已有成员

        players.sort((a, b) -> {
            boolean aOnline = Bukkit.getPlayer(a) != null;
            boolean bOnline = Bukkit.getPlayer(b) != null;
            if (aOnline && !bOnline) return -1;
            if (!aOnline && bOnline) return 1;
            return 0; // 简单排序
        });

        Inventory inv = Bukkit.createInventory(new GuildHolder("INVITE_LIST", page, null), 54, Component.text("§8邀请玩家"));

        int start = page * 45;
        int end = Math.min(start + 45, players.size());

        for (int i = start; i < end; i++) {
            UUID uuid = players.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            boolean isOnline = op.isOnline();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(Component.text((isOnline ? "§a" : "§7") + (op.getName() != null ? op.getName() : "Unknown")));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7状态: " + (isOnline ? "§a在线" : "§7离线")));
            if (guild.isInvited(uuid)) {
                lore.add(Component.text("§e[已发送邀请]"));
            } else {
                lore.add(Component.text("§b[点击邀请]"));
            }
            meta.lore(lore);

            // Store UUID
            meta.getPersistentDataContainer().set(new NamespacedKey(RabiSystem.getInstance(), "target_uuid"), PersistentDataType.STRING, uuid.toString());

            head.setItemMeta(meta);
            inv.setItem(i - start, head);
        }

        addPagination(inv, page, (int) Math.ceil(players.size() / 45.0));
        inv.setItem(49, createItem(Material.IRON_DOOR, "§c返回公会"));
        player.openInventory(inv);
    }

    // --- 4. 公会详情 (Home) ---
    public static void openGuildDetail(Player player, GuildData guild) {
        Inventory inv = Bukkit.createInventory(new GuildHolder("DETAIL", 0, guild), 54, Component.text("§8公会详情: " + guild.getName()));

        GuildMember self = guild.getMember(player.getUniqueId());
        boolean isAdmin = self.getRank().getLevel() >= 2;
        boolean isLeader = self.getRank() == GuildRank.LEADER;

        // Info
        inv.setItem(4, createItem(guild.getIcon(), "§6§l" + guild.getName(),
                "§7模式: " + guild.getType().getDisplay(),
                "§7人数: " + guild.getMembers().size()));

        // Members (Slots 18-44)
        List<GuildMember> members = new ArrayList<>(guild.getMembers().values());
        // Sort: Rank desc, then Join time
        members.sort((a, b) -> {
            if (a.getRank().getLevel() != b.getRank().getLevel()) return b.getRank().getLevel() - a.getRank().getLevel();
            return Long.compare(a.getJoinTime(), b.getJoinTime());
        });

        for (int i = 0; i < Math.min(members.size(), 27); i++) {
            GuildMember m = members.get(i);
            inv.setItem(18 + i, createMemberHead(m));
        }

        // Actions
        if (isAdmin) {
            inv.setItem(46, createItem(Material.WRITABLE_BOOK, "§e邀请玩家", "§7邀请新成员加入"));
            if (isLeader) {
                inv.setItem(47, createItem(Material.NAME_TAG, "§e重命名/改图标", "§7左键: 改名", "§7右键: 手持物品设为图标"));
                inv.setItem(48, createItem(Material.COMPARATOR, "§e更改模式", "§7当前: " + guild.getType().getDisplay()));
                inv.setItem(52, createItem(Material.TNT, "§c§l解散公会", "§7双击确认解散"));
            }
        }
        inv.setItem(53, createItem(Material.RED_BED, "§c离开公会"));

        fillBg(inv);
        player.openInventory(inv);
    }

    // --- Anvil Inputs ---
    public static void openAnvilInput(Player player, String title, GuildManager.InputType type, Object data) {
        new AnvilGUI.Builder()
                .plugin(RabiSystem.getInstance())
                .title(title)
                .text("在此输入...")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = stateSnapshot.getText();

                    if (type == GuildManager.InputType.CREATE_NAME) {
                        getManager().createGuild(player, text);
                    } else if (type == GuildManager.InputType.RENAME) {
                        GuildData g = (GuildData) data;
                        g.setName(text);
                        getManager().save();
                        player.sendMessage("§a公会名称已更新！");
                    } else if (type == GuildManager.InputType.PASSWORD) {
                        GuildData g = (GuildData) data;
                        if (g.getPassword().equals(text)) {
                            getManager().joinPublicGuild(player, g);
                        } else {
                            player.sendMessage("§c密码错误！");
                        }
                    }

                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    // --- Helpers ---

    private static ItemStack createMemberHead(GuildMember member) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(member.getUuid());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(op);

        meta.displayName(Component.text(member.getRank().getDisplay() + " " + (op.getName() != null ? op.getName() : "Unknown")));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7加入时间: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(member.getJoinTime()))));

        // Spirit Info
        SpiritModule spiritModule = (SpiritModule) RabiSystem.getModuleManager().getModule("spirit");
        if (spiritModule != null) {
            SpiritProfile profile = spiritModule.getSpiritManager().getProfile(member.getUuid());
            lore.add(Component.text("§d灵契等级: Lv." + profile.getLevel()));
        }

        // PlayTime Info
        PlayTimeModule ptModule = (PlayTimeModule) RabiSystem.getModuleManager().getModule("playtime");
        if (ptModule != null) {
            Long time = ptModule.getManager().getTotalPlaytimeCache().get(member.getUuid());
            long hours = (time != null ? time : 0) / 1000 / 3600;
            lore.add(Component.text("§a游戏时长: " + hours + "小时"));
        }

        // Location
        if (op.isOnline()) {
            Player p = op.getPlayer();
            String world = p.getWorld().getName();
            String dim = switch(p.getWorld().getEnvironment()) {
                case NORMAL -> "主世界";
                case NETHER -> "下界";
                case THE_END -> "末地";
                default -> world;
            };
            lore.add(Component.text("§e当前位置: " + dim));
        } else {
            lore.add(Component.text("§7(离线)"));
        }

        // Management Hint
        lore.add(Component.empty());
        lore.add(Component.text("§8[左键: 详情/管理]")); // 可以在这里做更多管理菜单

        meta.lore(lore);
        // Store Member UUID for click handling
        meta.getPersistentDataContainer().set(new NamespacedKey(RabiSystem.getInstance(), "member_uuid"), PersistentDataType.STRING, member.getUuid().toString());
        head.setItemMeta(meta);
        return head;
    }

    private static void fillBg(Inventory inv) {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        if (lore.length > 0) {
            List<Component> cLore = new ArrayList<>();
            for (String s : lore) cLore.add(Component.text(s));
            meta.lore(cLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static void addPagination(Inventory inv, int page, int total) {
        if (page > 0) inv.setItem(45, createItem(Material.ARROW, "§e上一页"));
        if (page < total - 1) inv.setItem(53, createItem(Material.ARROW, "§e下一页"));
    }

    private static String getName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "Unknown";
    }
}
