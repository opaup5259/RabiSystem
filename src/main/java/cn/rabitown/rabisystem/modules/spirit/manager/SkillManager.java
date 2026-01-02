package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.skill.SkillType;
import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private final SpiritModule module;

    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillManager(SpiritModule module) {
        this.module = module;
    }

    public static void castSkill(Player p, SpiritProfile profile, String skillId) {
        SkillType skill = SkillType.fromId(skillId);
        if (skill == null) return;

        // 1. 检查MP
        if (profile.getMana() < skill.getCost()) {
            p.sendActionBar("§c[!] MP不足，需要 " + skill.getCost() + " 点灵力。");
            p.playSound(p.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 1f, 1f);
            return;
        }

        // 2. 检查冷却
        long now = System.currentTimeMillis();
        Map<String, Long> pCds = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long cdEnd = pCds.getOrDefault(skillId, 0L);
        if (now < cdEnd) {
            long left = (cdEnd - now) / 1000;
            p.sendActionBar("§c[!] 技能冷却中 (" + left + "s)");
            return;
        }

        // 3. 执行效果 (简易 Switch)
        boolean success = executeSkillEffect(p, profile, skill);

        if (success) {
            // 扣除MP
            profile.addMana(-skill.getCost());
            // 设置冷却
            pCds.put(skillId, now + (skill.getCooldown() * 1000L));

            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);
            p.sendActionBar("§b✨ 释放技能: " + skill.getName());

            SpiritUtils.getConfigManager().saveProfile(profile);
        }
    }

    private static boolean executeSkillEffect(Player p, SpiritProfile profile, SkillType skill) {
        switch (skill.getId()) {
            case "star_heal":
                // [修改] 星光抚慰：让周围10米生物失去AI 5秒
                int count = 0;
                for (org.bukkit.entity.Entity e : p.getNearbyEntities(10, 10, 10)) {
                    if (e instanceof org.bukkit.entity.Mob mob) { // Mob 包含怪物和动物
                        // 移除 AI (暂时停止行动和攻击)
                        mob.setAware(false);
                        // 播放安抚特效
                        mob.getWorld().spawnParticle(org.bukkit.Particle.HEART, mob.getEyeLocation(), 3, 0.3, 0.3, 0.3);
                        count++;

                        // 5秒后恢复
                        new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                if (mob.isValid()) mob.setAware(true);
                            }
                        }.runTaskLater(SpiritUtils.getPlugin(), 100L);
                    }
                }
                p.sendActionBar("§a[!] 星光抚慰生效，安抚了 " + count + " 个生物。");
                return true;

            // [新增] 荧光指引 (给主人夜视)
            case "star_light":
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 30 * 20, 0)); // 30秒
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 2f);
                p.spawnParticle(org.bukkit.Particle.GLOW, p.getEyeLocation(), 5, 0.5, 0.5, 0.5, 0.1);
                p.sendActionBar("§b[!] 荧光指引已开启 (30s)。");
                return true;

            // [新增] 暗影突袭 (小精灵瞬移攻击)
            case "shadow_strike":
                // 射线检测 15格 内的生物 (排除自己和小精灵)
                org.bukkit.entity.Allay spirit = SpiritUtils.getSpiritManager().getSpiritEntity(p.getUniqueId());
                if (spirit == null || !spirit.isValid()) return false;

                org.bukkit.util.RayTraceResult ray = p.getWorld().rayTraceEntities(
                        p.getEyeLocation(),
                        p.getEyeLocation().getDirection(),
                        15.0,
                        e -> e instanceof org.bukkit.entity.LivingEntity && !e.equals(p) && !e.equals(spirit)
                );

                if (ray != null && ray.getHitEntity() instanceof org.bukkit.entity.LivingEntity target) {
                    Location targetLoc = target.getLocation();

                    // 1. 特效：瞬移背刺
                    Location behind = targetLoc.clone().add(targetLoc.getDirection().multiply(-1).setY(0).normalize());
                    spirit.teleport(behind);

                    // 2. 伤害 (固定 10)
                    target.damage(10.0, p);

                    // 3. 反馈
                    p.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);
                    p.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, targetLoc.add(0, 1, 0), 1);
                    p.getWorld().spawnParticle(org.bukkit.Particle.SOUL, targetLoc, 10, 0.5, 1, 0.5, 0.1);

                    p.sendActionBar("§5[!] 暗影突袭命中: " + (target.getCustomName() != null ? target.getCustomName() : target.getName()));
                    return true;
                } else {
                    p.sendActionBar("§c[!] 未检测到有效目标 (15米内)。");
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 2f);
                    return false; // 返回 false 不扣除 MP 和 CD
                }

                // [新增] 虚空护盾 (抗性提升)
            case "shadow_shield":
                // 给予 抗性提升 II (Amplifier 1) 持续 5秒 (通常这是用于紧急防御)
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 5 * 20, 1));
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
                p.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, p.getLocation().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.1);
                p.sendActionBar("§5[!] 虚空护盾已展开 (5s)。");
                return true;
            case "star_tools_1":
                SpiritMenus.openQuickToolsMenu(p, profile, 1);
                return true; // 属于 QUICK 技能，通常不扣大量CD，但这里配置了CD
            case "star_tools_2":
                SpiritMenus.openQuickToolsMenu(p, profile, 2);
                return true;

            case "shadow_walker":
                // [新增] 灵界行者：隐身+无敌+穿墙+视觉
                handleSpiritWalker(p);
                return true;

            case "shadow_mark":
                // [新增] 暗影标记：高亮周围敌对生物
                int marked = 0;
                for (org.bukkit.entity.Entity e : p.getNearbyEntities(12, 12, 12)) {
                    if (e instanceof org.bukkit.entity.Monster) { // 仅限怪物
                        e.setGlowing(true); // 发光
                        // 10秒后取消
                        new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                if (e.isValid()) e.setGlowing(false);
                            }
                        }.runTaskLater(SpiritUtils.getPlugin(), 200L);
                        marked++;
                    }
                }
                p.sendActionBar("§5[!] 暗影视野已标记 " + marked + " 个潜在威胁。");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 0.5f);
                return true;

            // ... 其他原有 case ...
        }
        return false;
    }

    private static void handleSpiritWalker(Player p) {
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
        if (profile == null) return;

        // 1. 设置状态
        profile.setSpiritWalking(true);

        // 2. 给予 Buff
        // 隐身 (隐藏身形)
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 220, 0, false, false));
        // 极速 (模拟灵魂疾行的高机动性, Speed IV)
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 220, 3, false, false));
        // 夜视 (保持视野清晰，配合粒子营造氛围)
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 220, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 220, 0, false, false));

        // 3. 播放启动音效 (模拟进入灵界)
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f); // 低沉的激活声
        p.getWorld().playSound(p.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1f, 1.0f); // 诡异氛围音

        p.sendTitle("§f§k||| §7灵界行走 §f§k|||", "§7虚实之间，万物静寂...", 5, 80, 10);

        // 4. 持续特效任务 (模拟苍白之园氛围)
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // 安全检查
                if (!p.isOnline() || !profile.isSpiritWalking()) {
                    this.cancel();
                    cleanup(p, profile);
                    return;
                }

                // 持续时间结束 (5秒 = 100 ticks)
                if (ticks >= 200) {
                    this.cancel();
                    cleanup(p, profile);
                    return;
                }

                // --- 视觉效果：幽冥灵界 ---
                Location loc = p.getLocation();

                // 1. 苍白氛围：大范围的白色飘落物，模拟苍白之园的死寂
                p.getWorld().spawnParticle(org.bukkit.Particle.WHITE_ASH, loc.add(0, 1, 0), 15, 4, 3, 4, 0);

                // 2. 阴暗侵蚀：黑色的灰烬，增加压抑感和肮脏感
                p.getWorld().spawnParticle(org.bukkit.Particle.ASH, loc, 10, 2, 2, 2, 0);

                // 3. 亡魂缠绕：偶尔飘起的灵魂粒子，暗示这里是生人勿近的灵界
                if (ticks % 5 == 0) {
                    p.getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, loc, 2, 0.5, 1, 0.5, 0.02);
                }

                // --- 听觉效果：心理恐惧 ---

                // 1. "The Creaking" (嘎吱作响)：保留原有的木头扭曲声，模拟身后有人
                if (ticks % 20 == 0) { // 稍微降低频率，让它更突兀
                    // 音调极低 (0.1)，听起来像枯树断裂或老旧地板
                    p.playSound(loc, Sound.BLOCK_CHERRY_WOOD_STEP, 0.8f, 0.1f);
                }

                // 2. "Heartbeat" (心跳)：制造紧张感，模拟监守者逼近的压迫力
                if (ticks % 20 == 0) {
                    p.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.5f);
                }

                // 3. "Ominous Wind" (不祥之风)：1.21 新增的试炼刷怪笼环境音，非常有“闹鬼”的感觉
                if (ticks % 40 == 0) {
                    p.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_AMBIENT_OMINOUS, 0.6f, 0.5f);
                }

                // 偶发惊悚：随机的灵魂尖啸或低语
                if (ticks % 50 == 0 && Math.random() < 0.3) {
                    p.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 0.5f, 0.5f);
                }

                // 4. 移动拖尾：灵魂残响
                // 检测玩家是否有位移 (速度向量 > 0)
                if (p.getVelocity().lengthSquared() > 0.01) {
                    // 脚底生成灵魂火粒子
                    p.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 0.2, 0), 2, 0.1, 0.0, 0.1, 0.01);
                    // 偶尔生成幽匿灵魂
                    if (Math.random() < 0.3) {
                        p.getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, p.getLocation().add(0, 0.5, 0), 1, 0.2, 0.2, 0.2, 0.02);
                    }
                }
                ticks += 5;
            }
        }.runTaskTimer(SpiritUtils.getPlugin(), 0L, 5L);
    }

    private static void cleanup(Player p, SpiritProfile profile) {
        // 恢复状态
        profile.setSpiritWalking(false);

        // 移除 Buff (虽然时间到了会自动消失，但为了保险手动清除，特别是隐身)
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.removePotionEffect(PotionEffectType.SPEED);
        p.removePotionEffect(PotionEffectType.DARKNESS);
        p.removePotionEffect(PotionEffectType.GLOWING);

        // 退出提示
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
        p.sendActionBar("§7[!] 已脱离灵界，实体化完成。");
    }
}