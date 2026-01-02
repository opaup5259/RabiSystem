package cn.rabitown.rabisystem.modules.warpStone.manager;

import cn.rabitown.rabisystem.modules.warpStone.WarpStoneModule;
import cn.rabitown.rabisystem.modules.warpStone.data.WarpStone;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {

    private final WarpStoneModule module;
    private final Map<String, WarpStone> warpStones = new HashMap<>();
    private final Map<UUID, SignSession> signSessions = new ConcurrentHashMap<>();
    private BukkitTask particleTask;

    public WarpManager(WarpStoneModule module) {
        this.module = module;
        // 加载数据
        module.getConfigManager().loadWarpStones(warpStones);
    }

    public void startTasks() {
        // 粒子特效任务
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (WarpStone stone : warpStones.values()) {
                    if (stone.getLocation().getWorld() == null) continue;
                    Location center = stone.getLocation().clone().add(0.5, 0.5, 0.5);
                    // 只有在区块加载时才渲染
                    if (center.getChunk().isLoaded()) {
                        stone.getLocation().getWorld().spawnParticle(Particle.PORTAL, center, 20, 0.3, 0.6, 0.3, 0.5);
                    }
                }
            }
        }.runTaskTimer(module.getPlugin(), 0L, 20L);
    }

    public void stopTasks() {
        if (particleTask != null) particleTask.cancel();
        // 恢复所有正在进行输入的告示牌
        for (SignSession session : signSessions.values()) {
            session.signLoc.getBlock().setBlockData(session.originalBlockData);
        }
        signSessions.clear();
        // 保存数据
        module.getConfigManager().saveWarpStones(warpStones);
    }

    public void createWarpStone(Player player, String name, Location loc) {
        // 构建基座外观 (可选，保持原味)
        Block down = loc.getBlock().getRelative(0, -1, 0);
        down.setType(Material.STONE_BRICK_WALL);
        if (down.getBlockData() instanceof Wall wall) {
            wall.setUp(true);
            wall.setHeight(BlockFace.NORTH, Wall.Height.NONE);
            wall.setHeight(BlockFace.EAST, Wall.Height.NONE);
            wall.setHeight(BlockFace.SOUTH, Wall.Height.NONE);
            wall.setHeight(BlockFace.WEST, Wall.Height.NONE);
            down.setBlockData(wall);
        }

        WarpStone stone = new WarpStone(name, loc, player.getUniqueId(), true, System.currentTimeMillis());
        warpStones.put(name, stone);
        sendActionBar(player, ChatColor.GREEN + "传送石创建成功: " + name);
        module.getConfigManager().saveWarpStones(warpStones);
    }

    public WarpStone getWarpStoneAt(Location loc) {
        for (WarpStone stone : warpStones.values()) {
            if (stone.getLocation().getBlockX() == loc.getBlockX() &&
                    stone.getLocation().getBlockY() == loc.getBlockY() &&
                    stone.getLocation().getBlockZ() == loc.getBlockZ() &&
                    stone.getLocation().getWorld().getName().equals(loc.getWorld().getName())) {
                return stone;
            }
        }
        return null;
    }

    public boolean checkLimit(Player player) {
        if (player.isOp() || player.hasPermission("rabi.warpstone.admin")) return true;

        long currentCount = warpStones.values().stream().filter(s -> s.getOwner().equals(player.getUniqueId())).count();
        // 读取 config.yml 中的默认限制 (如果没配则默认为5)
        int max = module.getPlugin().getConfig().getInt("modules.warp.max-stones", 5);

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith("rabi.warpstone.max.")) {
                try {
                    int limit = Integer.parseInt(perm.substring("rabi.warpstone.max.".length()));
                    if (limit > max) max = limit;
                } catch (NumberFormatException ignored) {}
            }
        }
        return currentCount < max;
    }

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message));
    }

    // --- 告示牌会话逻辑 ---
    public void openSignInput(Player player, InputType type, Object data) {
        Location loc = player.getLocation().add(0, 2, 0);
        // 如果头顶有方块，再往上一格
        if (!loc.getBlock().getType().isAir()) loc.add(0, 1, 0);

        Block block = loc.getBlock();
        BlockData oldData = block.getBlockData();
        block.setType(Material.OAK_SIGN);

        signSessions.put(player.getUniqueId(), new SignSession(type, data, loc, oldData));

        Bukkit.getScheduler().runTask(module.getPlugin(), () -> {
            if (block.getState() instanceof Sign sign) {
                if (type == InputType.CREATE) {
                    sign.setLine(0, "在此输入");
                    sign.setLine(1, "传送石名称");
                } else if (type == InputType.SHARE) {
                    sign.setLine(0, "在此输入");
                    sign.setLine(1, "玩家ID");
                }
                sign.update();
                player.openSign(sign);
            }
        });
    }

    public Map<String, WarpStone> getWarpStones() { return warpStones; }
    public Map<UUID, SignSession> getSignSessions() { return signSessions; }

    public enum InputType { CREATE, SHARE }

    public static class SignSession {
        public InputType type;
        public Object data;
        public Location signLoc;
        public BlockData originalBlockData;

        public SignSession(InputType type, Object data, Location signLoc, BlockData originalBlockData) {
            this.type = type;
            this.data = data;
            this.signLoc = signLoc;
            this.originalBlockData = originalBlockData;
        }
    }
}