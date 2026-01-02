package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.ExperienceSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SpiritInteractListener implements Listener {

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        // 判断是否是本插件的小精灵
        if (e.getEntity() instanceof Allay && SpiritUtils.getSpiritManager().isSpirit(e.getEntity())) {
            // 如果原因是自然恢复(REGEN)或吃饱恢复(SATIATED)，则取消
            if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN ||
                    e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Allay)) return;
        Allay spirit = (Allay) e.getRightClicked();

        if (!SpiritUtils.getSpiritManager().isSpirit(spirit)) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        UUID spiritOwnerId = null; // 需要一种反向查找机制，或者简单的遍历

        // 简单的反向查找 (在生产环境中建议在 Entity PDC 中存储 OwnerUUID)
        for (var entry : SpiritUtils.getSpiritManager().getActiveSpirits().entrySet()) {
            if (entry.getValue().equals(spirit)) {
                spiritOwnerId = entry.getKey();
                break;
            }
        }

        if (spiritOwnerId == null || !p.getUniqueId().equals(spiritOwnerId)) {
            p.playSound(spirit.getLocation(), Sound.ENTITY_ALLAY_HURT, 0.5f, 2f);
            return;
        }

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
        triggerStopAndLook(p, spirit, profile);
        // Shift + 右键 = 抚摸
        if (p.isSneaking()) {
            handlePetting(p, spirit, profile);
            return;
        }

        // 喂食判断
        ItemStack handItem = p.getInventory().getItemInMainHand();
        if (handItem.getType().isEdible() && spirit.getHealth() < spirit.getAttribute(Attribute.MAX_HEALTH).getValue()) {
            handleFeeding(p, spirit, profile, handItem);
            return;
        }

        // 默认打开菜单
        SpiritMenus.openMainMenu(p, profile);
    }

    // 新增：让小精灵停下来看向主人
    private void triggerStopAndLook(Player p, Allay spirit, SpiritProfile profile) {
        // 1. 设置暂停时间为当前时间 + 3000毫秒 (3秒)
        profile.setPauseUntil(System.currentTimeMillis() + 3000);

        // 2. 停止寻路
        spirit.getPathfinder().stopPathfinding();

        // 3. 强制转向主人
        Location spiritLoc = spirit.getLocation();
        Location playerLoc = p.getLocation();

        // 计算方向向量
        Vector dir = playerLoc.toVector().subtract(spiritLoc.toVector()).normalize();

        // 设置转向 (保留当前的XYZ，只改变Yaw和Pitch)
        Location lookLocation = spiritLoc.clone();
        lookLocation.setDirection(dir);

        // 传送以改变朝向 (Spigot API 限制，这是改变实体朝向最直接的方法)
        spirit.teleport(lookLocation);
    }

    private void handlePetting(Player p, Allay s, SpiritProfile profile) {
        long now = System.currentTimeMillis();
        // 1. 检查动作冷却 (3秒) - 防止刷屏
        if (now - profile.getLastPetActionTime() < 3000) {
            return;
        }
        profile.setLastPetActionTime(now);

        // 检查心情获取冷却 (60秒)
        if (now - profile.getLastPetMoodTime() >= 60000) {
            profile.addMood(10);
            // ➕ 新增：增加统计计数
            profile.addStat("pet_count", 1);
            // 触发成就检查：抚摸次数
            AchievementManager.check(p, profile, "pet_count");
            profile.setLastPetMoodTime(now);
            p.sendMessage("§d❤ 你抚摸了 " + profile.getName() + "，它感到非常开心！ (+10心情)");
            // ———— 经验获取 ————
            ExperienceSystem.grantExp(p, profile, ExperienceSystem.ExpType.PET, 10);
        } else {
            // 冷却中仅提示动作，不加心情
            p.sendActionBar("§7你轻轻抚摸了小精灵...");
        }

        s.getWorld().spawnParticle(Particle.HEART, s.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3);
        s.getWorld().playSound(s.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 1.5f);
//        p.sendMessage("§d❤ 你抚摸了 " + profile.getName() + "，它看起来很开心！");
    }


    /**
     * 辅助方法：获取 Material 对应的饱食度
     * (Minecraft 1.20.5+ 推荐使用 ItemComponent 获取，此处提供兼容写法)
     */
    private int getFoodLevel(Material m) {
        return switch (m) {
            case COOKED_BEEF, COOKED_PORKCHOP -> 8;
            case GOLDEN_CARROT -> 6;
            case BREAD, COOKED_CHICKEN, COOKED_MUTTON, COOKED_SALMON -> 5;
            case APPLE, BEEF, PORKCHOP, MUTTON, CHICKEN -> 4;
            case MELON_SLICE, SWEET_BERRIES -> 2;
            default -> 2;
        };
    }

    private void handleFeeding(Player p, Allay s, SpiritProfile profile, ItemStack food) {

        if (food == null) return;
        if (food.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            AchievementManager.check(p, profile, "feed_luxury");
        } else if (food.getType() == Material.ROTTEN_FLESH || food.getType() == Material.SPIDER_EYE) {
            AchievementManager.check(p, profile, "feed_gross");
        }

        if (!food.getType().isEdible()) return;
        // 1. 本次喂食的基础回血计算 (向上取整)
        double healValue = Math.ceil(getFoodLevel(food.getType()) / 2.0);
        double currentHealth = s.getHealth();
        double maxHealth = s.getAttribute(Attribute.MAX_HEALTH).getValue();

        // 如果血量已满则停止
        if (currentHealth >= maxHealth) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§7" + profile.getName() + " 已经吃得饱饱的了~"));
            return;
        }

        // 计算实际增加的血量
        double actualHeal = Math.min(healValue, maxHealth - currentHealth);
        s.setHealth(currentHealth + actualHeal);

        // 消耗物品并播放即时音效
        food.subtract(1);
        s.getWorld().playSound(s.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.2f);

        long now = System.currentTimeMillis();

        // 2. 周期统计与结算逻辑
        // 检查是否已经开启了一个 3 秒的进食周期
        if (now - profile.getCurrentFeedCycleStart() > 3000) {
            // --- 开启新周期 ---
            profile.setCurrentFeedCycleStart(now);
            profile.setCycleHealAmount(actualHeal);

            // 3. 开启异步任务，3秒后准时结算
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    // 周期结束，计算最终加成
                    int totalHealed = (int) profile.getCycleHealAmount();
                    if (totalHealed > 0 && p.isOnline()) {
                        profile.addStat("feed_count", 1);
                        // 触发成就检查：投喂次数
                        AchievementManager.check(p, profile, "feed_count");
                        // --- 核心逻辑修改：发放经验 = 总回血量 / 2 ---
                        int finalExp = totalHealed / 2;

                        if (finalExp > 0) {
                            // 运行 exputil 封装的方法
                            ExperienceSystem.grantExp(p, profile, ExperienceSystem.ExpType.FEED, finalExp);
                            // 心情同步增加
                            profile.addMood(finalExp);
                        }

                        // 播放结算特效
                        s.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, s.getLocation().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2);
                    }

                    // 4. 重要：重置当前周期状态，以便下次喂食开启新周期
                    profile.setCycleHealAmount(0);
                    profile.setCurrentFeedCycleStart(0);
                }
            }.runTaskLater(SpiritUtils.getPlugin(), 60L); // 20 ticks * 3s = 60 ticks

        } else {
            // --- 周期内累加 ---
            profile.addCycleHealAmount(actualHeal);
            p.sendActionBar("§e\uD83D\uDE0B 吧唧吧唧... 正在享用美食 (已回复 +" + (int)profile.getCycleHealAmount() + ")");
        }
    }

    // ———— 免疫伤害 ————
    @EventHandler
    public void onSpiritDamage(EntityDamageEvent e) {
        // 检查受击者是否为悦灵
        if (!(e.getEntity() instanceof Allay)) return;
        // 检查是否为本插件管理的小精灵
        if (SpiritUtils.getSpiritManager().isSpirit(e.getEntity())) {
            // 彻底取消所有类型的伤害 (包括物理攻击、火焰、虚空、药水等)
            e.setCancelled(true);
        }
    }
}