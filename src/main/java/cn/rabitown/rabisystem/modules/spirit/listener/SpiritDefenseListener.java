package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.utils.ExperienceSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpiritDefenseListener implements Listener {

    /**
     * 灵魂代偿核心逻辑：拦截致命伤害
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSoulCompensate(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        // 1. 如果不死图腾已经复活了玩家，小精灵不出手
//        if (e.isCancelled()) return;
        if (!e.isCancelled()) return;

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());

        // 2. 严格的权限与状态判定
        if (profile == null || !profile.isSummoned() || profile.getLevel() < 80 || !profile.isSoulCompensateEnabled())
            return;

        // 3. 代价检查
        if (profile.getMood() < 50) {
            p.sendActionBar(Component.text("§c[!] 小精灵精力不足，无法发动灵魂代偿！"));
            return;
        }

        // --- 触发代偿 ---
        e.setCancelled(false);

        profile.setMood(profile.getMood() - 50);

        // 恢复状态：延迟 1 tick 再次设置血量，防止被原版残留逻辑覆盖
        p.setHealth(15.0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 0));

        // 视觉反馈
        Allay spirit = SpiritUtils.getSpiritManager().getActiveSpirits().get(p.getUniqueId());
        if (spirit != null) {
            drawCompensateLine(spirit, p);
            spirit.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, spirit.getLocation(), 40, 0.5, 0.5, 0.5, 0.2);
            spirit.getWorld().playSound(spirit.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        }

        // 核心：调用 killSpirit 让小精灵进入 10 分钟重聚
        SpiritUtils.getSpiritManager().killSpirit(p.getUniqueId());

        p.sendMessage("§d§l🛡️ 誓约守护！§f小精灵燃烧了灵体挡在了你身前！");
        p.sendActionBar(Component.text("§d✨ 灵魂代偿生效 - 小精灵已消失并进入重聚状态..."));

        ExperienceSystem.grantExp(p, profile, ExperienceSystem.ExpType.BUFF, 10);
    }

    /**
     * 修复：只有【未解锁代偿能力】且【真实死亡】的玩家，才执行惩罚逻辑
     */
    @EventHandler
    public void onPlayerRealDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());

        if (profile == null || !profile.isSummoned()) return;

        // --- 🐾 猫猫补丁：等级判定 ---
        if (profile.getLevel() >= 80) {
            SpiritUtils.getSpiritManager().killSpirit(p.getUniqueId());
            p.sendMessage("§7灵灯的光芒黯淡了，它正在重聚灵魂……(10分钟CD)");
        }
    }

    /**
     * 绘制代偿时的能量连线粒子
     */
    private void drawCompensateLine(Allay spirit, Player p) {
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > 15 || !p.isOnline() || !spirit.isValid()) {
                    this.cancel();
                    return;
                }
                Location start = spirit.getLocation().add(0, 0.5, 0);
                Location end = p.getLocation().add(0, 1, 0);
                Vector dir = end.toVector().subtract(start.toVector()).normalize();

                for (double d = 0; d < start.distance(end); d += 0.4) {
                    Location point = start.clone().add(dir.clone().multiply(d));
                    spirit.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.02);
                }
                t++;
            }
        }.runTaskTimer(SpiritUtils.getPlugin(), 0L, 1L);
    }
}