package cn.rabitown.rabisystem.modules.afk.manager;

import cn.rabitown.rabisystem.modules.afk.AFKModule;
import cn.rabitown.rabisystem.modules.prefix.utils.PrefixUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;

import java.util.*;
import java.util.stream.Collectors;

public class AFKManager {

    private final AFKModule module;

    // 状态存储
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Long> afkStartTime = new HashMap<>();
    private final Map<UUID, Visuals> afkVisuals = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();

    // 任务引用
    private BukkitTask checkTask;
    private BukkitTask animTask;
    private BukkitTask boardTask;

    public AFKManager(AFKModule module) {
        this.module = module;
    }

    public void startTasks() {
        // 1. 自动AFK检测
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long limit = module.getConfigManager().getAutoAfkSeconds();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isAFK(p)) {
                        long last = lastActivity.getOrDefault(p.getUniqueId(), now);
                        if ((now - last) / 1000 >= limit) {
                            enterAFK(p);
                        }
                    } else {
                        updateAFKState(p);
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 20L);

        // 2. 动画循环
        animTask = new BukkitRunnable() {
            @Override
            public void run() {
                float angle = (System.currentTimeMillis() % 2000) / 2000f * 360f;
                for (Visuals v : afkVisuals.values()) {
                    if (v.itemDisplay != null && v.itemDisplay.isValid()) {
                        v.rotateItem(angle);
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 1L);

        // 3. 计分板刷新
        boardTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (afkStartTime.isEmpty()) return;
                List<Map.Entry<String, Long>> top = getTopPlayers();
                for (UUID uuid : afkStartTime.keySet()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        updateScoreboard(p, top);
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 20L, 200L);
    }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
        if (animTask != null) animTask.cancel();
        if (boardTask != null) boardTask.cancel();

        for (UUID uuid : new ArrayList<>(afkStartTime.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) exitAFK(p);
        }
        module.getConfigManager().saveData();
    }

    // --- 核心业务逻辑 ---

    public boolean isAFK(Player p) {
        return afkStartTime.containsKey(p.getUniqueId());
    }

    public void updateLastActivity(Player p) {
        lastActivity.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public void removeLastActivity(Player p) {
        lastActivity.remove(p.getUniqueId());
    }

    public void enterAFK(Player p) {
        if (isAFK(p)) return;

        long now = System.currentTimeMillis();
        afkStartTime.put(p.getUniqueId(), now);

        p.setCollidable(false);

        Component msg = Component.text()
                .append(Component.text(p.getName() + " ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("丢下大家，一个人跑去", NamedTextColor.GRAY))
                .append(Component.text("摸鱼挂机", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" 了！真是个大懒虫喵！", NamedTextColor.GRAY))
                .build();
        Bukkit.broadcast(msg);

        // 调用 Prefix 模块更新前缀 (如果模块存在)
        if (PrefixUtils.getManager() != null) {
            PrefixUtils.getManager().updatePrefix(p, "afk", "§7♿ §r", 20);
        }

        createVisuals(p);

        previousScoreboards.put(p.getUniqueId(), p.getScoreboard());
        updateScoreboard(p, getTopPlayers());
    }

    public void exitAFK(Player p) {
        if (!isAFK(p)) return;

        long start = afkStartTime.remove(p.getUniqueId());
        long durationMillis = System.currentTimeMillis() - start;
        long minutes = durationMillis / 60000;

        p.setCollidable(true);

        int gainedExp = (int) minutes;
        if (gainedExp > 0) {
            p.giveExp(gainedExp);
        }

        Component broadcastMsg = Component.text()
                .append(Component.text(p.getName() + " ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("终于舍得回来了？这次一共偷懒了 ", NamedTextColor.GRAY))
                .append(Component.text(minutes, NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" 分钟, ", NamedTextColor.GRAY))
                .append(Component.text("共获得了 ", NamedTextColor.GRAY))
                .append(Component.text(gainedExp + " 点经验值", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" 喵！", NamedTextColor.GRAY))
                .build();
        Bukkit.broadcast(broadcastMsg);

        // 移除前缀
        if (PrefixUtils.getManager() != null) {
            PrefixUtils.getManager().updatePrefix(p, "afk", null, 0);
        }

        // 保存数据
        long total = module.getConfigManager().getData().getLong("players." + p.getUniqueId() + ".total-time", 0);
        long newTotal = total + durationMillis;
        module.getConfigManager().getData().set("players." + p.getUniqueId() + ".name", p.getName());
        module.getConfigManager().getData().set("players." + p.getUniqueId() + ".total-time", newTotal);
        module.getConfigManager().saveData();

        removeVisuals(p.getUniqueId());
        updateLastActivity(p);

        // 恢复计分板
        if (previousScoreboards.containsKey(p.getUniqueId())) {
            p.setScoreboard(previousScoreboards.get(p.getUniqueId()));
            previousScoreboards.remove(p.getUniqueId());
        } else {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void updateAFKState(Player p) {
        long start = afkStartTime.get(p.getUniqueId());
        long minutes = (System.currentTimeMillis() - start) / 60000;

        p.sendActionBar(Component.text("你已摸鱼挂机 " + minutes + " 分钟", NamedTextColor.YELLOW));

        Visuals v = afkVisuals.get(p.getUniqueId());
        if (v != null) {
            v.teleportTo(p.getLocation());
            v.updateTimeText(minutes);
        }
    }

    public void resetActivity(Player p) {
        if (isAFK(p)) {
            exitAFK(p);
        }
        updateLastActivity(p);
    }

    // --- 排行榜相关 ---

    public List<Map.Entry<String, Long>> getTopPlayers() {
        Map<String, Long> tempMap = new HashMap<>();
        ConfigurationSection sec = module.getConfigManager().getData().getConfigurationSection("players");

        if (sec != null) {
            for (String uuidStr : sec.getKeys(false)) {
                String name = sec.getString(uuidStr + ".name", "Unknown");
                long time = sec.getLong(uuidStr + ".total-time", 0);

                // 加上当前正在挂机的时间
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    if (afkStartTime.containsKey(uuid)) {
                        time += (System.currentTimeMillis() - afkStartTime.get(uuid));
                    }
                } catch (Exception ignored) {}

                tempMap.put(name, time);
            }
        }
        return tempMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    public void sendRankMessage(Player p) {
        List<Map.Entry<String, Long>> sorted = getTopPlayers();

        p.sendMessage(Component.text("====== 🐟 摸鱼排行榜 🐟 ======", NamedTextColor.GOLD));

        int limit = Math.min(sorted.size(), 5);
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            String timeStr = formatDuration(entry.getValue());
            p.sendMessage(Component.text()
                    .append(Component.text((i + 1) + "> ", NamedTextColor.YELLOW))
                    .append(Component.text(entry.getKey() + " ", NamedTextColor.AQUA))
                    .append(Component.text(timeStr, NamedTextColor.GRAY))
                    .build());
        }
        p.sendMessage(Component.text("============================", NamedTextColor.GOLD));

        long myTime = module.getConfigManager().getData().getLong("players." + p.getUniqueId() + ".total-time", 0);
        if (isAFK(p)) {
            myTime += (System.currentTimeMillis() - afkStartTime.get(p.getUniqueId()));
        }

        int myRank = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(p.getName())) {
                myRank = i + 1;
                break;
            }
        }

        p.sendMessage(Component.text()
                .append(Component.text("你的摸鱼总时长: ", NamedTextColor.GREEN))
                .append(Component.text(formatDuration(myTime), NamedTextColor.WHITE))
                .append(Component.text(myRank != -1 ? " (排名第 " + myRank + ")" : " (未上榜)", NamedTextColor.GRAY))
                .build());
    }

    private void updateScoreboard(Player p, List<Map.Entry<String, Long>> topList) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("RabiAFK", Criteria.DUMMY, Component.text("🎣 摸鱼排行榜", NamedTextColor.GOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int limit = Math.min(topList.size(), 10);
        int score = 15;

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = topList.get(i);
            String name = entry.getKey();
            String timeStr = formatShortDuration(entry.getValue());
            String line = "§e" + (i + 1) + ". §b" + name + " §7" + timeStr;
            if (line.length() > 38) line = line.substring(0, 38);
            obj.getScore(line).setScore(score--);
        }
        p.setScoreboard(board);
    }

    // --- 视觉特效内部类 ---

    private void createVisuals(Player p) {
        Location loc = p.getLocation();
        TextDisplay text = (TextDisplay) p.getWorld().spawnEntity(loc.clone().add(0, 2.3, 0), EntityType.TEXT_DISPLAY);
        text.setBillboard(Display.Billboard.CENTER);
        text.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        ItemDisplay item = (ItemDisplay) p.getWorld().spawnEntity(loc.clone().add(0, 3.4, 0), EntityType.ITEM_DISPLAY);
        item.setItemStack(new ItemStack(Material.BARRIER));
        item.setBillboard(Display.Billboard.FIXED);

        Transformation transform = item.getTransformation();
        transform.getScale().set(0.5f);
        item.setTransformation(transform);

        Visuals v = new Visuals(text, item, p.getName());
        v.updateTimeText(0);
        afkVisuals.put(p.getUniqueId(), v);
    }

    private void removeVisuals(UUID uuid) {
        Visuals v = afkVisuals.remove(uuid);
        if (v != null) v.remove();
    }

    private static class Visuals {
        TextDisplay textDisplay;
        ItemDisplay itemDisplay;
        String playerName;

        public Visuals(TextDisplay text, ItemDisplay item, String name) {
            this.textDisplay = text;
            this.itemDisplay = item;
            this.playerName = name;
        }

        public void updateTimeText(long minutes) {
            if (textDisplay == null || !textDisplay.isValid()) return;
            Component content = Component.text()
                    .append(Component.text("[" + playerName + " 偷懒中…]", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("已经摸鱼 " + minutes + " 分钟", NamedTextColor.WHITE))
                    .append(Component.newline())
                    .append(Component.text("金刚不坏", NamedTextColor.GOLD))
                    .build();
            textDisplay.text(content);
        }

        public void rotateItem(float angleDegrees) {
            if (itemDisplay == null || !itemDisplay.isValid()) return;
            Transformation t = itemDisplay.getTransformation();
            t.getLeftRotation().set(new AxisAngle4f((float) Math.toRadians(angleDegrees), 0, 1, 0));
            itemDisplay.setTransformation(t);
        }

        public void teleportTo(Location playerLoc) {
            if (textDisplay != null && textDisplay.isValid())
                textDisplay.teleport(playerLoc.clone().add(0, 2.3, 0));
            if (itemDisplay != null && itemDisplay.isValid())
                itemDisplay.teleport(playerLoc.clone().add(0, 3.4, 0));
        }

        public void remove() {
            if (textDisplay != null) textDisplay.remove();
            if (itemDisplay != null) itemDisplay.remove();
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d小时%02d分钟%02d秒", h, m, s);
    }

    private String formatShortDuration(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h > 0) return h + "h " + m + "m";
        return m + "m " + (seconds % 60) + "s";
    }
}