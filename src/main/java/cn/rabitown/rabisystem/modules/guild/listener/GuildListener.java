package cn.rabitown.rabisystem.modules.guild.listener;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.guild.GuildModule;
import cn.rabitown.rabisystem.modules.guild.data.*;
import cn.rabitown.rabisystem.modules.guild.manager.GuildManager;
import cn.rabitown.rabisystem.modules.guild.ui.GuildMenus;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class GuildListener implements Listener {

    private final GuildModule module;

    public GuildListener(GuildModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof GuildMenus.GuildHolder holder)) return;

        e.setCancelled(true);
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        String type = holder.getType();
        GuildManager manager = module.getManager();

        if (type.equals("NO_GUILD")) {
            if (e.getSlot() == 11) { // Create
                GuildMenus.openAnvilInput(player, "输入小队昵称", GuildManager.InputType.CREATE_NAME, null);
            } else if (e.getSlot() == 13) { // Find
                GuildMenus.openFinderMenu(player, 0);
            } else if (e.getSlot() == 15 && current.getType() == Material.PAPER) {
                // TODO: Show Pending Invites Menu (简化：暂时仅提示)
                player.sendMessage("§e请查看聊天记录中的邀请信息。");
            }
        }

        else if (type.equals("FINDER")) {
            if (current.getType() == Material.IRON_DOOR) {
                GuildMenus.openNoGuildMenu(player);
                return;
            }
            if (e.getSlot() >= 0 && e.getSlot() < 45) { // Guilds
                // Hacky way to find guild by list index is tricky with pagination
                // Better: find by name match or NBT if we added it.
                // For simplicity, let's assuming strict ordering or parsing name from item
                String name = PlainTextComponentSerializer.plainText().serialize(current.displayName()).trim();
                for (GuildData g : manager.getAllGuilds().values()) {
                    if (g.getName().equals(name)) {
                        if (g.getType() == GuildType.PUBLIC) {
                            manager.joinPublicGuild(player, g);
                            player.closeInventory();
                        } else if (g.getType() == GuildType.PASSWORD) {
                            GuildMenus.openAnvilInput(player, "输入密码", GuildManager.InputType.PASSWORD, g);
                        }
                        break;
                    }
                }
            }
        }

        else if (type.equals("DETAIL")) {
            GuildData guild = (GuildData) holder.getData();
            if (current.getType() == Material.RED_BED) { // Leave
                guild.removeMember(player.getUniqueId());
                manager.save();
                player.sendMessage("§c你离开了公会。");
                player.closeInventory();
                manager.broadcastToGuild(guild, "§c" + player.getName() + " 离开了公会。");
                return;
            }
            if (current.getType() == Material.TNT) { // Disband
                if (e.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
                    manager.disbandGuild(player);
                    player.closeInventory();
                } else {
                    player.sendMessage("§c请双击确认解散！");
                }
                return;
            }
            if (current.getType() == Material.WRITABLE_BOOK) { // Invite
                GuildMenus.openInviteList(player, 0);
                return;
            }
            if (current.getType() == Material.COMPARATOR) { // Toggle Mode
                GuildType next = switch(guild.getType()) {
                    case INVITE -> GuildType.PUBLIC;
                    case PUBLIC -> GuildType.PASSWORD;
                    case PASSWORD -> GuildType.INVITE;
                };
                guild.setType(next);
                manager.save();
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                GuildMenus.openGuildDetail(player, guild);
                return;
            }
            if (current.getType() == Material.NAME_TAG) { // Rename / Icon
                if (e.isLeftClick()) {
                    GuildMenus.openAnvilInput(player, "新公会名称", GuildManager.InputType.RENAME, guild);
                } else if (e.isRightClick()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() != Material.AIR) {
                        guild.setIcon(hand.getType());
                        manager.save();
                        player.sendMessage("§a图标已更新！");
                        GuildMenus.openGuildDetail(player, guild);
                    } else {
                        player.sendMessage("§c请手持物品！");
                    }
                }
                return;
            }
        }

        else if (type.equals("INVITE_LIST")) {
            if (current.getType() == Material.IRON_DOOR) {
                GuildMenus.openGuildDetail(player, manager.getPlayerGuild(player.getUniqueId()));
                return;
            }
            NamespacedKey key = new NamespacedKey(RabiSystem.getInstance(), "target_uuid");
            if (current.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String uuidStr = current.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                Player target = Bukkit.getPlayer(UUID.fromString(uuidStr));
                if (target != null && target.isOnline()) {
                    manager.invitePlayer(player, target);
                    player.closeInventory();
                } else {
                    player.sendMessage("§c玩家不在线，暂时无法邀请（离线邀请暂未实装）");
                }
            }
        }
    }
}
