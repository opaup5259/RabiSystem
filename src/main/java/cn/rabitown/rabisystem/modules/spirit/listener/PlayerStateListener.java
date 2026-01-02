package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SkillManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SpiritManager;
import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class PlayerStateListener implements Listener {
    private final Map<UUID, Long> lastSneakTime = new HashMap<>();
    private final Map<UUID, SwapRecord> swapRecords = new HashMap<>();
    // 1. 定义一个变量来存模块对象
//    private final SpiritModule module;
    // 2. ✅ 必须添加这个接收 SpiritModule 的构造函数
//    public PlayerStateListener(SpiritModule module) {
//        this.module = module;
//    }

    private static class SwapRecord {
        long lastTime;
        int count;

        public SwapRecord(long time, int count) {
            this.lastTime = time;
            this.count = count;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        // 获取档案
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(uuid);

        // 获取当前日期 (YYYYMMDD)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int today = cal.get(java.util.Calendar.YEAR) * 10000 +
                (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
                cal.get(java.util.Calendar.DAY_OF_MONTH);

        // 如果记录的最后登录日期不等于今天，说明是今日第一次上线
        if (profile.getLastLoginDate() != today) {
            profile.resetDailyProgress(); // 重置今日所有经验进度
            profile.setLastLoginDate(today); // 更新日期标记
            SpiritUtils.getConfigManager().saveProfile(profile); // 立即写盘
        }

        // 延迟 1 Tick 执行，确保玩家坐标已完全初始化
        Bukkit.getScheduler().runTaskLater(SpiritUtils.getPlugin(), () -> {
            SpiritUtils.getSpiritManager().restoreSpirit(p);

            // 根据自己的设置，刷新对其他所有小精灵的可见性
            SpiritUtils.getSpiritManager().refreshVisibilityForPlayer(p);

            // 进服时强制检查一次等级成就
            // 这样老玩家上线时，系统会根据他当前的等级，一次性补发所有应得的成就
            AchievementManager.check(p, profile, "level_update");
        }, 1L);



        // 2. 刷新可见性 (旧代码的 refreshVisibility 逻辑)
        // 现在的逻辑：遍历所有活跃的小精灵，根据设置决定是否对该玩家显示
        SpiritManager manager = SpiritUtils.getSpiritManager();
        for (Map.Entry<UUID, Allay> entry : manager.getActiveSpirits().entrySet()) {
            Allay spirit = entry.getValue();
            // 这里可以加入逻辑：如果 spirit的主人把 spirit 隐藏了，或者加入黑名单逻辑
            // 目前默认都显示
            if (!p.getUniqueId().equals(entry.getKey())) {
                p.showEntity(SpiritUtils.getPlugin(), spirit);
            }
        }

        // [新增] 首次登录/未领取检测 + 白名单检查
        if (!profile.hasReceivedFirstLantern()) {
            // 异步检查白名单，防止卡主线程 (读取文件IO)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (checkRabiWhitelist(p.getName())) {
                        // 回到主线程发放物品
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                giveBoundLantern(p);
                                // 标记为已领取并保存
                                profile.setReceivedFirstLantern(true);
                                SpiritUtils.getConfigManager().saveProfile(profile);
                                p.sendMessage("§e[灵契] §f检测到您是白名单认证玩家，已发放【羁绊提灯】！");
                            }
                        }.runTask(SpiritUtils.getPlugin());
                    }
                }
            }.runTaskAsynchronously(SpiritUtils.getPlugin());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // 玩家退出时销毁实体，但不改变 isSummoned 状态（为了下次上线重连）
        // 这里的 false 参数表示不保存 isSummoned=false 到配置，仅内存移除实体
        SpiritUtils.getSpiritManager().despawnSpirit(e.getPlayer().getUniqueId());
    }

    /**
     * 检查玩家的小精灵是否处于重聚状态
     *
     * @param p       玩家对象
     * @param profile 玩家的灵契档案
     * @return 如果处于重聚状态返回 true（已拦截），否则返回 false（放行）
     */
    public boolean checkReunionStatus(Player p, SpiritProfile profile) {
        long now = System.currentTimeMillis();
        long expireTime = profile.getReunionExpireTime();

        // 1. 判断是否处于重聚 CD 中
        if (expireTime > now) {
            long remainingMillis = expireTime - now;
            long minutes = (remainingMillis / 1000) / 60;
            long seconds = (remainingMillis / 1000) % 60;

            // 2. 发送提示信息与音效
            p.sendActionBar("§c[!] 灵灯的光芒微弱，灵魂碎片正在缓慢重聚...");
            p.sendActionBar("§7还需等待: §f" + minutes + "分" + seconds + "秒");
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);

            // 3. 延迟 1 Tick 打开菜单（防止指令/交互冲突）
            new BukkitRunnable() {
                @Override
                public void run() {
                    SpiritMenus.openMainMenu(p, profile);
                }
            }.runTaskLater(SpiritUtils.getPlugin(), 1L);

            return true; // 已拦截
        }

        return false; // 未在重聚中，放行
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Player p = e.getPlayer();
        ItemStack handItem = p.getInventory().getItemInMainHand();

        // 必须持有灵魂灯笼（包括普通和已绑定的）
        if (handItem.getType() != Material.SOUL_LANTERN) return;

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
        long now = System.currentTimeMillis();

        // 双击潜行检测
        if (now - lastSneakTime.getOrDefault(p.getUniqueId(), 0L) < 500) {
            // --- 逻辑 A：如果小精灵已在场，直接打开主菜单 ---
            if (SpiritUtils.getSpiritManager().getSpiritEntity(p.getUniqueId()) != null) {
                handleLanternTransformation(p, handItem);
                SpiritMenus.openMainMenu(p, profile);
                lastSneakTime.remove(p.getUniqueId());
                return;
            }

            // --- 逻辑 B：执行召唤流程 ---
            if (checkReunionStatus(p, profile)) return;

            // 触发召唤
            SpiritUtils.getSpiritManager().summonSpirit(p);

            // 处理灯笼变更：不再扣除，而是转化
            handleLanternTransformation(p, handItem);

            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
            p.sendActionBar(net.kyori.adventure.text.Component.text("✨ 灯火摇曳，古老的灵契回应了你的呼唤...", NamedTextColor.AQUA));

            lastSneakTime.remove(p.getUniqueId());
        } else {
            lastSneakTime.put(p.getUniqueId(), now);
        }
    }

    // 连按 F 收回逻辑
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(uuid);

        // 检查：如果玩家并没有召唤小精灵，或者副手不是空的，则忽略
        if (!profile.isSummoned() || p.getInventory().getItemInOffHand().getType() != Material.AIR) return;

        long n = System.currentTimeMillis();
        SwapRecord r = swapRecords.getOrDefault(uuid, new SwapRecord(0, 0));

        if (n - r.lastTime > 500) r.count = 0;
        r.count++;
        r.lastTime = n;
        swapRecords.put(uuid, r);

        // 连续按了3次 F
        if (r.count >= 3) {
            despawnSpirit(e, p, uuid, profile);
            swapRecords.remove(uuid);
        }
    }

    /**
     * 收回小精灵
     */
    private void despawnSpirit(PlayerSwapHandItemsEvent e, Player p, UUID uuid, SpiritProfile profile) {
        e.setCancelled(true);

        // --- 执行收回 ---
        // 1. 销毁实体 (despawnSpirit 方法内部会自动保存当前血量到 profile)
        SpiritUtils.getSpiritManager().despawnSpirit(uuid);

        // 2. 更新状态：标记为手动关闭
        profile.setSummoned(false);
        SpiritUtils.getConfigManager().saveProfile(profile); // 保存到磁盘

        // 3. 社交状态清理 (Task 会自动清理，无需再手动操作 socialStates)
        // 4. 返还灯笼
//            p.getInventory().setItemInOffHand(new ItemStack(Material.SOUL_LANTERN));

        // 5. 特效反馈
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 2f);
        p.sendActionBar(Component.text("§b💨 小精灵化作一缕青烟，回到了灯笼里休息。", NamedTextColor.BLUE));
    }

    /**
     * [重构] 统一获取“羁绊提灯”的物品实例
     * 避免代码重复，方便以后统一修改 Lore 或材质
     */
    private ItemStack getBoundLanternItem() {
        ItemStack boundLantern = new ItemStack(Material.SOUL_LANTERN);
        ItemMeta meta = boundLantern.getItemMeta();
        meta.setDisplayName("§3§l✦ §b羁绊提灯 §3§l✦");

        List<String> lore = new ArrayList<>();
        lore.add("§7§o双击潜行以呼唤或管理你的小精灵");
        lore.add("§8§m------------------------------");
        lore.add("§f“指引灵魂归途的微光，");
        lore.add("§f 如今与你的生命紧密相连。”");
        lore.add("§8§m------------------------------");

        meta.setLore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        boundLantern.setItemMeta(meta);

        return boundLantern;
    }

    /**
     * [优化] 检查 RabiWhitelist 白名单
     * 逻辑：先检查插件是否加载 -> 再读取文件确认名单
     */
    private boolean checkRabiWhitelist(String playerName) {
        // 1. 利用 softdepend 的特性，先检查插件是否真的在运行
        // 如果 RabiWhitelist 没加载，说明服务器可能没装这个插件，那就直接跳过，不发奖励
        if (!Bukkit.getPluginManager().isPluginEnabled("RabiWhitelist")) {
            return false;
        }

        // 2. 插件在运行，我们再去读它的配置文件
        // 这样做的好处是：不需要在 Maven 里引入 RabiWhitelist 的 jar 包也能读取数据
        File whitelistFile = new File("plugins/RabiWhitelist/data.yml");
        if (!whitelistFile.exists()) {
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(whitelistFile);
        List<String> acceptedRules = config.getStringList("accepted-rules");

        return acceptedRules.contains(playerName);
    }

    /**
     * [修改] 发放逻辑，调用封装好的方法
     */
    private void giveBoundLantern(Player p) {
        // 调用封装好的方法获取物品
        ItemStack boundLantern = getBoundLanternItem();

        HashMap<Integer, ItemStack> left = p.getInventory().addItem(boundLantern);
        if (!left.isEmpty()) {
            for (ItemStack item : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), item);
            }
            p.sendMessage("§c[注意] §f背包已满，提灯已掉落在脚下。");
        }
    }

    /**
     * [修改] 原有的潜行转换逻辑，也调用封装好的方法
     */
    private void handleLanternTransformation(Player player, ItemStack handItem) {
        ItemMeta meta = handItem.getItemMeta();
        if (meta.hasDisplayName() && meta.getDisplayName().contains("羁绊提灯")) {
            return;
        }

        // 调用封装好的方法，代码瞬间清爽了！
        ItemStack boundLantern = getBoundLanternItem();

        // --- 下面是原有的堆叠处理逻辑，保持不变 ---
        int amount = handItem.getAmount();
        if (amount == 1) {
            player.getInventory().setItemInMainHand(boundLantern);
        } else {
            player.getInventory().setItemInMainHand(boundLantern);
            ItemStack remainingLanterns = new ItemStack(Material.SOUL_LANTERN, amount - 1);
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(remainingLanterns);
            if (!leftOver.isEmpty()) {
                for (ItemStack drop : leftOver.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage("§c[提示] §f背包已满，剩余的普通灵魂灯笼已掉落在脚下。");
            }
        }
    }

    @EventHandler
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
            if (profile != null && profile.isSpiritWalking()) {
                e.setCancelled(true); // 禁止拾取
            }
        }
    }

    @EventHandler
    public void onSkillTrigger(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return; // 必须潜行

        ItemStack hand = p.getInventory().getItemInMainHand();
        // 必须手持灯笼
        if (hand.getType() != Material.SOUL_LANTERN) return;

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());

        // 如果已召唤，且装备了主动技能
        if (profile.isSummoned() && profile.getActiveSkillId() != null) {
            e.setCancelled(true); // 阻止换手

            // 触发主动技能
            SkillManager.castSkill(p, profile, profile.getActiveSkillId());
        }
    }
}