package cn.rabitown.rabisystem.modules.spirit.task;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.*;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpiritAppTask extends BukkitRunnable {
    private int ticks = 0;

    @Override
    public void run() {
        ticks++;
        // 限制每秒处理的在线玩家，或者简单增加过滤
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 增加缓存，避免频繁从 Manager 获取 Profile
            SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(player.getUniqueId());

            // --- 等级校验拦截 ---
            // 如果玩家等级小于该特效要求的等级，则不渲染（视作 NONE）
            SpiritEffectType currentEffect = profile.getActiveEffect();
            if (profile.getLevel() < currentEffect.getRequiredLevel()) {
                continue;
            }

            // 如果没开启特效或者没召唤，直接跳过，不进行后续任何计算
            if (!profile.isEffectsEnabled() || !profile.isSummoned()) continue;

            Allay spirit = SpiritUtils.getSpiritManager().getActiveSpirits().get(player.getUniqueId());

            // 关键：如果实体无效，立即清理并跳过
            if (spirit == null || !spirit.isValid() || spirit.isDead()) {
                continue;
            }


            renderEffect(spirit, profile.getActiveEffect());
        }
    }

    private void renderEffect(Allay spirit, SpiritEffectType type) {
        Location loc = spirit.getLocation().add(0, 0.4, 0);

        // 使用枚举进行判断
        switch (type) {
            case BOND: // ID "1"
                spirit.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.1, 0.1, 0.1, 0.01);
                break;
            case RESONANCE: // ID "2"
                spirit.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.05, 0.05, 0.05, 0.02);
                break;
            case COVENANT: // ID "3"
                drawHorizontalCircle(loc.clone().subtract(0, 0.3, 0), 0.5, Particle.WITCH);
                break;
            case BINARY_STAR: // ID "4"
                renderBinaryStar(loc);
                break;
            case GALAXY:
                renderGalaxy(loc);
                break;
            case VOLCANO:
                renderVolcano(loc);
                break;
            default:
                break;
        }
    }

    private void renderBinaryStar(Location loc) {
        // 旋转速度与半径
        double speed = ticks * 0.15;
        double radius = 0.7;

        // --- 第一颗星：青色轨道 (倾斜 45 度) ---
        double x1 = Math.cos(speed) * radius;
        double y1 = Math.sin(speed) * radius * 0.5; // Y轴加入波动
        double z1 = Math.sin(speed) * radius;

        Location star1 = loc.clone().add(x1, y1, z1);
        // 使用较细碎的青色火焰
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, star1, 1, 0, 0, 0, 0);

        // --- 第二颗星：白色轨道 (倾斜 -45 度且相位相反) ---
        double x2 = Math.cos(speed + Math.PI) * radius;
        double y2 = Math.sin(speed + Math.PI) * radius * -0.5; // Y轴反向波动
        double z2 = Math.sin(speed + Math.PI) * radius;

        Location star2 = loc.clone().add(x2, y2, z2);
        // 使用明亮的闪烁粒子
        loc.getWorld().spawnParticle(Particle.END_ROD, star2, 1, 0, 0, 0, 0);

        // --- 3. 拖尾效果：模拟星云粉尘 ---
        if (ticks % 2 == 0) {
            // 将 Particle.GLOW 替换为 Particle.ELECTRIC_SPARK 或 Particle.INSTANT_EFFECT
            // 这些粒子在 1.21 中不需要额外的 Color 参数
            loc.getWorld().spawnParticle(Particle.WITCH, star1, 1, 0.02, 0.02, 0.02, 0.01);
            loc.getWorld().spawnParticle(Particle.INSTANT_EFFECT, star2, 1, 0.02, 0.02, 0.02, 0.01);
        }

        // --- 4. 核心脉冲 ---
        if (ticks % 20 == 0) {
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * 绘制水平圆环工具方法
     */
    private void drawHorizontalCircle(Location center, double radius, Particle particle) {
        // 每刻只画圆的一部分，或者全画（1tick 20次开销可控）
        int points = 8;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i / points) + (ticks * 0.1); // 随时间自转
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(particle, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }

    /**
     * 🌌 抽奖特效：星河 (Galaxy) - 版本 C (Final)
     * 调整：喷流改为单向向下倾泻，模拟星瀑感
     */
    private void renderGalaxy(Location loc) {
        // 1. 中心位置：腰腹部悬浮
        Location center = loc.clone().add(0, 0.3, 0);

        // 2. 吸积盘粒子 (稀疏设定)
        int diskParticles = 11;
        double maxRadius = 1.6;

        for (int i = 0; i < diskParticles; i++) {
            double r = 0.4 + Math.pow(Math.random(), 2) * (maxRadius - 0.4);

            // 慢速旋转
            double speed = 0.15 / r;
            double angle = (ticks * speed) + (i * (Math.PI * 2 / 5));

            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            double y = Math.cos(angle + ticks * 0.025) * 0.2 * r;

            Location pLoc = center.clone().add(x, y, z);

            Particle.DustOptions dust;
            if (r < 0.8) {
                dust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 150 + (int)(Math.random()*50), 0), 0.6f);
            } else {
                dust = new Particle.DustOptions(org.bukkit.Color.fromRGB(40, 0, 180 + (int)(Math.random()*75)), 0.5f);
            }
            center.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dust);
        }

        // 吸积盘的「微弱旋转音」
        if (ticks % 20 == 0) {
            center.getWorld().playSound(
                    center,
                    Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                    0.18f,                         // 非常轻
                    0.9f + (float)Math.random()*0.2f // 微随机音高
            );
        }

        // 偶发的空间低语
        if (ticks % 60 == 0 && Math.random() < 0.6) {
            center.getWorld().playSound(
                    center,
                    Sound.ENTITY_ENDERMAN_AMBIENT,
                    0.12f,     // 极低
                    0.45f      // 很低的音高
            );
        }
        // 视界核心
        if (ticks % 2 == 0) {
            for (int k = 0; k < 6; k++) {
                double theta = (Math.PI * 2 / 6) * k + (ticks * 0.05);
                double horizonR = 0.35;
                center.getWorld().spawnParticle(Particle.DUST,
                        center.clone().add(Math.cos(theta)*horizonR, 0, Math.sin(theta)*horizonR),
                        1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 0, 0), 0.8f));
            }
        }

        // 3. 喷流调整：仅向下喷射 (星瀑)
        if (ticks % 2 == 0) {
            // 增加一组循环，让向下喷射的粒子更密集，弥补去掉上方的空缺
            for (int j = 0; j < 3; j++) {
                double offsetX = (Math.random() - 0.5) * 0.3; // 稍微加宽一点点
                double offsetZ = (Math.random() - 0.5) * 0.3;
                Location jetOrigin = center.clone().add(offsetX, 0, offsetZ);

                // 仅保留向下喷射
                center.getWorld().spawnParticle(Particle.END_ROD, jetOrigin, 0,
                        0, -0.25, 0, // 速度稍微加快一点点，增加垂坠感
                        1);
            }

            // 仅保留底部烟雾 (模拟能量触地消散)
            double jetHeight = 1.6; // 稍微延伸一点触地感
            center.getWorld().spawnParticle(Particle.WAX_OFF, center.clone().add(0, -jetHeight, 0), 1, 0.2, 0.1, 0.2, 0);

            if (ticks % 6 == 0) { // 不要太频繁
                center.getWorld().playSound(
                        center,
                        Sound.BLOCK_BEACON_AMBIENT,
                        0.22f,
                        0.6f + (float)Math.random()*0.15f
                );
            }
        }

        // 环境扭曲
        if (ticks % 40 == 0) {
            center.getWorld().spawnParticle(Particle.WITCH, center, 3, 0.2, 0.2, 0.2, 0);
        }
    }

    /**
     * 🌋 抽奖特效：熔岩 (Volcano)
     * 效果：地裂岩浆护盾，伴随不稳定的火焰喷发与黑烟
     */
    private void renderVolcano(Location loc) {
        Location center = loc.clone().add(0, 0.5, 0);

        // 1. 环绕护盾 (三个火球高速旋转)
        int orbs = 3;
        double orbRadius = 0.9;
        double speed = ticks * 0.25; // 旋转速度较快

        for (int i = 0; i < orbs; i++) {
            double angle = speed + ((2 * Math.PI / orbs) * i);
            double x = Math.cos(angle) * orbRadius;
            double z = Math.sin(angle) * orbRadius;
            // Y轴上下浮动
            double y = Math.sin(ticks * 0.1 + i) * 0.3;

            Location orbLoc = center.clone().add(x, y, z);

            // 火焰核心
            center.getWorld().spawnParticle(Particle.FLAME, orbLoc, 1, 0, 0, 0, 0.02);
            // 熔岩滴落感
            if (ticks % 4 == 0) {
                center.getWorld().spawnParticle(Particle.FALLING_LAVA, orbLoc, 1, 0, 0, 0, 0);
            }
        }

        // 2. 底部地裂效果 (脚下)
        if (ticks % 10 == 0) {
            Location footLoc = loc.clone().subtract(0, 0.2, 0);
            // 随机生成一个圆面上的点
            double r = 0.6 * Math.sqrt(Math.random());
            double theta = Math.random() * 2 * Math.PI;
            Location ground = footLoc.add(r * Math.cos(theta), 0, r * Math.sin(theta));

            // 只有落地时才显示LAVA粒子，制造岩浆涌动感
            center.getWorld().spawnParticle(Particle.LAVA, ground, 1, 0, 0, 0, 0);
        }

        // 3. 升腾的浓烟与热浪
        if (ticks % 3 == 0) {
            // 随机位置向上飘
            double offsetX = (Math.random() - 0.5) * 0.8;
            double offsetZ = (Math.random() - 0.5) * 0.8;
            Location smokeLoc = center.clone().add(offsetX, -0.5, offsetZ);

            // LARGE_SMOKE 模拟火山灰
            center.getWorld().spawnParticle(Particle.LARGE_SMOKE, smokeLoc, 1, 0, 0.1, 0, 0.05);
        }

        // 4. 偶尔的喷发 (Burst)
        if (ticks % 40 == 0) {
            // 像火山喷发一样向上冲出粒子
            center.getWorld().spawnParticle(Particle.FLAME, center, 10, 0.2, 0.5, 0.2, 0.2);
            center.getWorld().playSound(center, org.bukkit.Sound.BLOCK_LAVA_POP, 1f, 1.5f);
        }
    }
}