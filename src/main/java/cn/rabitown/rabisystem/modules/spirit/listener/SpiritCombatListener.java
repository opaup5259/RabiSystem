package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.utils.ExperienceSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.LevelSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SpiritCombatListener implements Listener {

    // 1. 玩家攻击别人 -> 拦截
    @EventHandler(priority = EventPriority.LOWEST) // 最低优先级拦截，防止触发后续逻辑
    public void onPlayerTryAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
            if (profile != null && profile.isSpiritWalking()) {
                e.setCancelled(true);
                // p.sendActionBar("§7[灵界] 虚化状态无法攻击实体"); // 可选提示
            }
        }
    }

    // 2. 玩家被攻击 -> 拦截 (无敌)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTakeDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
            if (profile != null && profile.isSpiritWalking()) {
                e.setCancelled(true); // 免疫所有类型伤害
            }
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        Player killer = entity.getKiller();

        // 必须是玩家击杀
        if (killer == null) return;

        // 必须小精灵在场
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(killer.getUniqueId());
        if (profile == null || !profile.isSummoned()) return;

        // 1. 击杀怪物成就
        if (entity instanceof Monster) { // 只有敌对生物算数 (Monster 接口包含僵尸、骷髅等)
            profile.addStat("kill_mob", 1);
            AchievementManager.check(killer, profile, "kill_mob");
        }

        // 2. Boss 击杀成就
        if (entity instanceof EnderDragon || entity instanceof Wither) {
            AchievementManager.check(killer, profile, "boss_fight");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) // 提高优先级确保触发
    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        // 1. 基本判定：攻击者必须是玩家，受击者不能是玩家
        if (!(e.getDamager() instanceof Player player)) return;
        if (e.getEntity() instanceof Player) return;

        // 2. 获取档案并检查状态
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(player.getUniqueId());

        // 核心检查：档案不为空、已召唤、等级达标、且开关已开启
        if (profile == null || !profile.isSummoned() || profile.getLevel() < 30) return;
        if (!profile.isResonanceEnabled()) return; // 检查你刚添加的开关变量

        // 从 LevelSystem 获取共鸣参数
        int level = profile.getLevel();
        int amplifier = LevelSystem.getResonanceAmplifier(level);
        int duration = LevelSystem.getResonanceDurationTicks(level);
        long cdMillis = LevelSystem.getResonanceCooldown(level);
        String tier = LevelSystem.getResonanceTierName(level);

        // 4. CD 判定
        long now = System.currentTimeMillis();
        if (now - profile.getLastResonanceTime() < cdMillis) return;

        // 5. 触发效果
        profile.setLastResonanceTime(now);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, amplifier));
        // 让小精灵在主人身边环绕并发出悦耳的声音
        Allay allay = SpiritUtils.getSpiritManager().getActiveSpirits().get(player.getUniqueId());
        if (allay != null) {
            allay.getWorld().spawnParticle(Particle.HEART, allay.getLocation().add(0, 0.5, 0), 3);
        }

        // 6. 播放反馈
        // 特效 (注意：Particle 枚举在不同版本名不同，1.20.4+ 推荐使用 ANGRY_VILLAGER)
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1);

        // 音效
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);

        // 7. 经验发放与提示
        ExperienceSystem.grantExp(player, profile, ExperienceSystem.ExpType.BUFF, 5);
        player.sendActionBar(Component.text("§6✨ 灵力共鸣 " + tier + " 已激活！ §7(力量增幅中)"));
        AchievementManager.check(player, profile, "buff_trigger");
    }

    // 监听：玩家攻击生物
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurstAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity target) || target instanceof Player) return;

        triggerSpiritBurst(p, target);
    }

    // 监听：玩家被生物攻击
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurstBeAttacked(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!(e.getDamager() instanceof LivingEntity attacker) || attacker instanceof Player) return;

        triggerSpiritBurst(p, attacker);
    }

    /**
     * 核心逻辑：触发灵力迸发
     */
    private void triggerSpiritBurst(Player p, LivingEntity target) {
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
        if (profile == null || !profile.isSummoned() || profile.getLevel() < 50
                || !profile.isBurstEnabled() || profile.isBursting()) return;

        // CD 与 数值获取
        int level = profile.getLevel();
        double damage = LevelSystem.getBurstDamage(level);
        long cdMillis = LevelSystem.getBurstCooldown(level);
        if (System.currentTimeMillis() - profile.getLastBurstTime() < cdMillis) return;

        Allay spirit = SpiritUtils.getSpiritManager().getActiveSpirits().get(p.getUniqueId());
        if (spirit == null || !spirit.isValid()) return;

        profile.setBursting(true);
        // 1. 记录初始位置，防止上天
        final Location anchorLocation = spirit.getLocation();
        // 2. 禁用 AI（这能阻止它自发漂浮）
        spirit.setAI(false);
        profile.markBusy();

        // --- 🔊 音效优化：3秒持续蓄力音效 ---
        // 使用 WARDEN_SONIC_CHARGE，Pitch 设为 1.5f 让其听起来更像高频魔法，而不是低沉的怪物声
        // 只需要在开始时播放一次，它会自动持续播放约 3 秒
        spirit.getWorld().playSound(spirit.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 3.0f, 1.5f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 60; // 3秒

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || !spirit.isValid()) {
                    profile.setBursting(false);
                    this.cancel();
                    return;
                }

                if (ticks < maxTicks) {
                    Location sLoc = spirit.getLocation().add(0, 0.5, 0);
                    Location tLoc = target.getEyeLocation();
                    Vector dir = tLoc.toVector().subtract(sLoc.toVector()).normalize();

                    // --- 1. 实时绘制魔法阵 ---
                    // 在小精灵前方 0.8 格处生成
                    Location circleCenter = sLoc.clone().add(dir.clone().multiply(0.8));
                    drawMagicCircle(circleCenter, dir, ticks);

                    // --- 视觉补偿：让小精灵强行面向目标 ---
                    sLoc = anchorLocation.clone(); // 使用锚点位置
                    Vector lookDir = target.getLocation().subtract(sLoc).toVector();
                    sLoc.setDirection(lookDir);
                    spirit.teleport(sLoc);

                    // --- 2. 蓄力引线 (逐渐增强) ---
                    for (double d = 0.8; d < sLoc.distance(tLoc); d += 0.5) {
                        Location point = sLoc.clone().add(dir.clone().multiply(d));
                        spirit.getWorld().spawnParticle(Particle.WAX_OFF, point, 1, 0, 0, 0, 0);
                    }

                    if (ticks % 5 == 0) {
                        float pitch = 0.5f + ((float) ticks / maxTicks) * 1.5f;
                        spirit.getWorld().playSound(sLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, pitch);
                    }
                    ticks++;
                } else {
                    // --- 3. 爆发瞬间 ---
                    profile.setLastBurstTime(System.currentTimeMillis());
                    cleanup();
                    profile.setBursting(false);
                    target.damage(damage, p);

                    // 瞬间冲击特效
                    spirit.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 1);
                    spirit.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 1);
                    spirit.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);

                    ExperienceSystem.grantExp(p, profile, ExperienceSystem.ExpType.DAMAGE, (int)damage);
                    this.cancel();

                    AchievementManager.check(p, profile, "burst_trigger");
                    if (target.isDead()) {
                        AchievementManager.check(p, profile, "burst_kill");
                    }
                }
            }
            /**
             * 统一清理逻辑：恢复速度与状态
             */
            private void cleanup() {
                profile.setBursting(false);
                if (spirit.isValid()) {
                    // 恢复 AI
                    spirit.setAI(true);
                }
            }
        }.runTaskTimer(SpiritUtils.getPlugin(), 0L, 1L);
    }

    /**
     * 魔法阵绘制工具：绘制一个垂直于 dir 向量的圆阵
     */
    private void drawMagicCircle(Location center, Vector dir, int ticks) {
        double radius = 0.6 * 1.6;
        // 计算正交向量
        Vector v1 = new Vector(dir.getZ(), 0, -dir.getX()).normalize();
        if (v1.length() == 0) v1 = new Vector(0, 1, 0); // 处理垂直向下的情况
        Vector v2 = dir.getCrossProduct(v1).normalize();

        // 绘制圆环 (粒子数随 ticks 增加而加密，模拟充能)
        int points = 15 + (ticks / 2);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            // 让魔法阵缓慢自转
            angle += (ticks * 0.1);

            Vector offset = v1.clone().multiply(Math.cos(angle) * radius)
                    .add(v2.clone().multiply(Math.sin(angle) * radius));

            Location pLoc = center.clone().add(offset);
            // 使用两种颜色交替
            Particle part = (i % 2 == 0) ? Particle.SOUL_FIRE_FLAME : Particle.WITCH;
            center.getWorld().spawnParticle(part, pLoc, 1, 0, 0, 0, 0);
        }

        // 阵中心点点缀
        center.getWorld().spawnParticle(Particle.ENCHANT, center, 3, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * 被动技能：噬灵 (攻击回蓝)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPassiveDrain(EntityDamageByEntityEvent e) {
        // 1. 攻击者必须是玩家
        if (!(e.getDamager() instanceof Player player)) return;

        // 2. 检查是否学习了技能
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(player.getUniqueId());
        if (profile == null || !profile.isSummoned()) return;

        if (profile.isSkillUnlocked("shadow_passive_drain")) {
            // 3. 概率触发 (30%)
            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.3) {
                // 4. 回复 MP (3-5 点)
                double restore = 3 + java.util.concurrent.ThreadLocalRandom.current().nextInt(3);

                if (profile.getMana() < profile.getMaxMana()) {
                    profile.addMana(restore);

                    // 轻微的音效提示
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 2.0f);
                    // 可以在这里加个微弱的粒子效果
                    // player.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, player.getLocation().add(0, 1, 0), 1, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }
    }
}