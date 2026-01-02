package cn.rabitown.rabisystem.modules.prefix.listener;

import cn.rabitown.rabisystem.modules.prefix.PrefixModule;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PrefixListener implements Listener {

    private final PrefixModule module;

    public PrefixListener(PrefixModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // 通过 Manager 获取前缀
        String prefix = module.getManager().getFinalPrefix(player);

        if (!prefix.isEmpty()) {
            // 将颜色代码转换 (& -> §)
            String coloredPrefix = ChatColor.translateAlternateColorCodes('&', prefix);

            // 聊天栏不显示修复逻辑: [前缀] + [变白§f] + [原有的 <名字> 消息]
            event.setFormat(coloredPrefix + "§f" + event.getFormat());
        }
    }
}