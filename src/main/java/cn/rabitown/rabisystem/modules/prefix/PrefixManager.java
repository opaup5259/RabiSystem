package cn.rabitown.rabisystem.modules.prefix.manager;

import cn.rabitown.rabisystem.modules.prefix.PrefixModule;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PrefixManager {

    private final PrefixModule module;
    // 存储结构: PlayerUUID -> { PluginKey -> PrefixNode }
    private final Map<UUID, Map<String, PrefixNode>> prefixCache = new ConcurrentHashMap<>();

    public PrefixManager(PrefixModule module) {
        this.module = module;
    }

    public void clearAll() {
        prefixCache.clear();
    }

    /**
     * 更新玩家的前缀
     * @param player 玩家
     * @param key 标识 (如 "playtime", "afk")
     * @param prefix 前缀内容
     * @param priority 权重
     */
    public void updatePrefix(Player player, String key, String prefix, int priority) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        prefixCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Map<String, PrefixNode> playerPrefixes = prefixCache.get(uuid);

        if (prefix == null) {
            playerPrefixes.remove(key);
        } else {
            playerPrefixes.put(key, new PrefixNode(prefix, priority));
        }

        refreshTab(player);
    }

    /**
     * 获取玩家当前拼接后的完整前缀字符串
     */
    public String getFinalPrefix(Player player) {
        Map<String, PrefixNode> nodes = prefixCache.get(player.getUniqueId());
        if (nodes == null || nodes.isEmpty()) return "";

        // 按优先级排序拼接 (权重大的在左边)
        return nodes.values().stream()
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .map(PrefixNode::text)
                .collect(Collectors.joining());
    }

    private void refreshTab(Player player) {
        String finalPrefix = getFinalPrefix(player);

        // 调用 TAB API
        try {
            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if (tabPlayer != null) {
                TabListFormatManager formatManager = TabAPI.getInstance().getTabListFormatManager();
                if (formatManager != null) {
                    // TAB 列表颜色污染修复
                    String result = finalPrefix.isEmpty() ? null : finalPrefix + "§r";
                    formatManager.setPrefix(tabPlayer, result);
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            // TAB 插件可能未加载或版本不兼容，忽略
        }
    }

    // 数据记录 (Record, Java 16+)
    public record PrefixNode(String text, int priority) {}
}