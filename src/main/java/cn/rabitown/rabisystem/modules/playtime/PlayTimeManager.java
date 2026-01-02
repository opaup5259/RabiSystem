package cn.rabitown.rabisystem.modules.playtime;

import cn.rabitown.rabisystem.modules.prefix.utils.PrefixUtils;
import cn.rabitown.rabisystem.modules.prefix.manager.PrefixManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class PlayTimeManager {

    private final PlayTimeModule module;
    private final Map<UUID, Long> totalPlaytime = new HashMap<>(); // 总时长
    private final Map<UUID, Long> sessionStart = new HashMap<>();  // 本次登录时间
    private BukkitTask checkTask;

    // --- 游戏时长配置 ---
    private static final long TIME_30H = 30L * 60 * 60 * 1000;  // 30小时
    private static final long TIME_100H = 100L * 60 * 60 * 1000; // 100小时

    private static final String PREFIX_SPROUT = "&a|🌱| "; // 绿色豆芽
    private static final String PREFIX_FLOWER = "&e|🌸| "; // 粉色花朵
    private static final String PREFIX_CROWN = "&e|👑| ";  // 金色皇冠
    private static final int ITEMS_PER_ROW = 9;

    public PlayTimeManager(PlayTimeModule module) {
        this.module = module;
        // 加载数据
        module.getConfigManager().loadPlayTimes(totalPlaytime);
    }

    public void startTasks() {
        // --- 定时任务：每分钟检查一次是否毕业 ---
        checkTask = Bukkit.getScheduler().runTaskTimer(module.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                checkSprout(p);
            }
        }, 1200L, 1200L);
    }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
        // 保存所有在线玩家数据
        for (Player p : Bukkit.getOnlinePlayers()) {
            savePlayerSession(p.getUniqueId());
        }
        module.getConfigManager().savePlayTimes(totalPlaytime);
    }

    // 处理重载插件逻辑
    public void handleReload() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            sessionStart.put(p.getUniqueId(), now);
            checkSprout(p);
        }
    }

    public void onPlayerJoin(Player p) {
        sessionStart.put(p.getUniqueId(), System.currentTimeMillis());
        checkSprout(p);
    }

    public void onPlayerQuit(Player p) {
        savePlayerSession(p.getUniqueId());
    }

    private void savePlayerSession(UUID uuid) {
        if (sessionStart.containsKey(uuid)) {
            long start = sessionStart.get(uuid);
            long sessionTime = System.currentTimeMillis() - start;
            totalPlaytime.put(uuid, totalPlaytime.getOrDefault(uuid, 0L) + sessionTime);
            sessionStart.remove(uuid);
        }
    }

    // --- 核心逻辑：豆芽检查 ---
    private void checkSprout(Player player) {
        PrefixManager prefixManager = PrefixUtils.getManager();
        // 如果前缀模块被禁用了，直接返回，防止报错
        if (prefixManager == null) return;

        long currentTotal = getActualTotalTime(player.getUniqueId());
        String prefix;

        if (currentTotal < TIME_30H) {
            prefix = PREFIX_SPROUT;
        } else if (currentTotal < TIME_100H) {
            prefix = PREFIX_FLOWER;
        } else {
            prefix = PREFIX_CROWN;
        }

        // 更新前缀
        prefixManager.updatePrefix(player, "playtime_rank", prefix, 10);
    }

    private long getActualTotalTime(UUID uuid) {
        long history = totalPlaytime.getOrDefault(uuid, 0L);
        if (sessionStart.containsKey(uuid)) {
            long currentSession = System.currentTimeMillis() - sessionStart.get(uuid);
            return history + currentSession;
        }
        return history;
    }

    private void updateAllOnlineCache() {
        long now = System.currentTimeMillis();
        for(Player p : Bukkit.getOnlinePlayers()) {
            savePlayerSession(p.getUniqueId());
            sessionStart.put(p.getUniqueId(), now);
            checkSprout(p);
        }
    }

    // --- GUI 构建逻辑 ---
    public void openLeaderboard(Player player, int page) {
        updateAllOnlineCache();

        List<Map.Entry<UUID, Long>> sortedList = totalPlaytime.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        int totalPlayers = sortedList.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / ITEMS_PER_ROW);
        if (totalPages == 0) totalPages = 1;

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        String title = "游戏时长排行榜 - 第 " + page + " 页";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Row 1
        if (page > 1) {
            inv.setItem(0, createItem(Material.PAPER, "§e上一页", "§7点击前往第 " + (page - 1) + " 页"));
        }
        inv.setItem(4, createItem(Material.BOOK, "§b当前页: " + page + " / " + totalPages, "§7共 " + totalPlayers + " 名玩家记录"));
        if (page < totalPages) {
            inv.setItem(8, createItem(Material.PAPER, "§e下一页", "§7点击前往第 " + (page + 1) + " 页"));
        }

        // Row 2
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 9; i < 18; i++) inv.setItem(i, glass);

        // Row 3
        int startIndex = (page - 1) * ITEMS_PER_ROW;
        int endIndex = Math.min(startIndex + ITEMS_PER_ROW, totalPlayers);

        int slot = 18;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Long> entry = sortedList.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getKey());

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                String name = target.getName() != null ? target.getName() : "未知";
                meta.setDisplayName("§e" + name + " §6#" + (i + 1));
                meta.setLore(Collections.singletonList("§7在线时长: §f" + formatDuration(entry.getValue())));
                skull.setItemMeta(meta);
            }
            inv.setItem(slot++, skull);
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String formatDuration(long millis) {
        long totalMinutes = millis / 1000 / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) return hours + "小时 " + minutes + "分钟";
        return minutes + "分钟";
    }
}