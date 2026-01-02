package cn.rabitown.rabisystem.modules.whitelist.listener;

import cn.rabitown.rabisystem.modules.whitelist.WhitelistModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public class WhitelistListener implements Listener {

    private final WhitelistModule module;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WhitelistListener(WhitelistModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        FileConfiguration config = module.getPlugin().getConfig();
        String prefix = config.getString("whitelist.messages.prefix", "");

        if (module.getManager().isVisitor(player)) {
            // 场景 1: 访客 (不在白名单) -> 发送提示
//            List<String> msgList = config.getStringList("whitelist.messages.visitor-join");
            List<String> msgList = config.getStringList("whitelist.messages.pending-rules");
            for (String line : msgList) {
                player.sendMessage(mm.deserialize(prefix + line));
            }
        } else if (!module.getManager().hasAccepted(player)) {
            // 场景 2: 待同意 (在白名单但未同意) -> 发送规则
            Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
                List<String> msgList = config.getStringList("whitelist.messages.pending-rules");
                for (String line : msgList) {
                    player.sendMessage(mm.deserialize(line.replace("{player}", player.getName())));
                }
            }, 20L);
        }
    }

    // --- 🛡️ 限制机制 ---

    // 1. 禁止受击 (无敌)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (module.getManager().isRestricted(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 2. 禁止攻击别人
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (module.getManager().isRestricted(player)) {
                sendBlockMessage(player);
                event.setCancelled(true);
            }
        }
    }

    // 3. 禁止被怪物仇恨
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMobTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (module.getManager().isRestricted(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 4. 禁止拾取物品
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && module.getManager().isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    // 5. 禁止拾取经验
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickupExp(com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent event) {
        if (module.getManager().isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --- 💬 交互拦截 ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!module.getManager().isRestricted(player)) return;

        String rootLabel = event.getMessage().split(" ")[0].toLowerCase();
        // 豁免白名单指令和登录指令
        if (rootLabel.equals("/wl") || rootLabel.equals("/whitelist") || rootLabel.equals("/rabiwl") ||
                rootLabel.equals("/register") || rootLabel.equals("/r") ||
                rootLabel.equals("/login") || rootLabel.equals("/l")) return;

        event.setCancelled(true);
        player.sendMessage(mm.deserialize(module.getPlugin().getConfig().getString("whitelist.messages.command-blocked")));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!module.getManager().isRestricted(player)) return;

        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        // 拦截同意/拒绝指令，其他聊天放行
        if (msg.equalsIgnoreCase("同意")) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.getManager().performAccept(player));
        } else if (msg.equalsIgnoreCase("拒绝")) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.getManager().performDeny(player));
        }
    }

    // --- 🚫 物理交互拦截 ---
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (module.getManager().isRestricted(event.getPlayer())) {
            sendBlockMessage(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (module.getManager().isRestricted(event.getPlayer())) {
            sendBlockMessage(event.getPlayer());
            event.setCancelled(true);
        }
    }

    // 交互拦截：按钮、门、拉杆、压力板等
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (module.getManager().isRestricted(event.getPlayer())) {

            // 仅在右键点击方块且是主手时发送提示
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
                sendBlockMessage(event.getPlayer());
            }
            // 强制取消所有交互（包括物理踩踏如耕地、压力板）
            event.setCancelled(true);
        }
    }

    /**
     * 发送操作被拦截的提示
     */
    private void sendBlockMessage(Player player) {
        String msg;
        // 场景 A: 访客 (不在白名单)
        if (module.getManager().isVisitor(player)) {
            msg = module.getPlugin().getConfig().getString("whitelist.messages.visitor-actionbar", "<red>⚠ 你不在白名单中，无法操作！");
        }
        // 场景 B: 待同意 (在白名单但未同意)
        else {
            msg = module.getPlugin().getConfig().getString("whitelist.messages.action-blocked", "<red>⚠ 请先同意规则");
        }
        player.sendActionBar(mm.deserialize(msg));
    }
}