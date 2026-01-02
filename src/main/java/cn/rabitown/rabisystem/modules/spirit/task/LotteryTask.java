package cn.rabitown.rabisystem.modules.spirit.task;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.LotteryManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LotteryTask extends BukkitRunnable {
    private final Player player;
    private final Allay spirit;
    private final SpiritProfile profile;

    private final List<ItemDisplay> displays = new ArrayList<>();
    private final LotteryManager.Reward finalReward;

    private ItemDisplay winnerDisplay;
    private Location centerLocation;

    private int ticks = 0;
    private final int ROTATION_DURATION = 100;
    private boolean isFacingLocked = false;

    // 状态控制
    private boolean isRitualStarted = false;
    private final Location targetLocation; // 目标悬停点
    private int approachTicks = 0;         // 飞行超时计数

    public LotteryTask(Player player, Allay spirit, SpiritProfile profile) {
        this.player = player;
        this.spirit = spirit;
        this.profile = profile;
        this.finalReward = LotteryManager.getRandomReward();

        // 1. 计算目标点：玩家面前 2.5 格，并抬高 1.5 格 (悬停在视线高度)
        Vector direction = player.getLocation().getDirection().clone();
        direction.setY(0).normalize();
        this.targetLocation = player.getLocation().add(direction.multiply(3.0)).add(0, 1.0, 0);

        // 让目标点面向玩家 (为了稍后瞬移调整朝向时自然)
        Location lookAtPlayer = player.getEyeLocation().subtract(targetLocation).toVector().toLocation(player.getWorld());
        this.targetLocation.setDirection(lookAtPlayer.getDirection());

        // 2. 下达移动指令 (开启导航)
        // 关键修复：此时绝对不能关 AI，否则飞不动
        spirit.setAI(true);
        spirit.getPathfinder().moveTo(targetLocation, 1.8);

        // 3. 播放召唤音效
        player.playSound(spirit.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, 1f, 1.2f);
    }

    @Override
    public void run() {
        // 安全检查
        if (!player.isOnline() || !spirit.isValid()) {
            cleanup();
            return;
        }

        // --- 阶段一：飞行靠近 ---
        if (!isRitualStarted) {
            approachTicks++;
            double distanceSq = spirit.getLocation().distanceSquared(targetLocation);

            // 判定：距离小于 1.5格 (到达) OR 飞行超过 3秒 (超时强制开始)
            if (distanceSq < 1.5 || approachTicks > 60) {
//                startRitual(); // 到位了，开始仪式！
                isRitualStarted = true;
                return;
            } /*else {
                // 持续导航：每 10 tick 补发一次指令，防止因意外停下
                if (approachTicks % 10 == 0) {
                    spirit.getPathfinder().moveTo(targetLocation, 1.8);
                }
                // 飞行拖尾粒子
                spirit.getWorld().spawnParticle(Particle.WAX_OFF, spirit.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            }*/
            if(approachTicks %10 == 0){
                spirit.getPathfinder().moveTo(targetLocation, 1.5);
            }
            spirit.getWorld().spawnParticle(Particle.WAX_OFF, spirit.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);

            return; // 还在飞，跳过后续逻辑
        }

        // 1.5
        if(isRitualStarted && !isFacingLocked){
            Location spiritLoc = spirit.getLocation();
            Vector toPlayer = player.getEyeLocation()
                    .toVector()
                    .subtract(spiritLoc.toVector());

            float targetYaw = getYawFromVector(toPlayer);
            float currentYaw = spiritLoc.getYaw();

            float diff = Math.abs(wrapYaw(targetYaw - currentYaw));

            // 允许 8 度以内，视觉上已经是“看着玩家”
            if (diff < 8.0f) {
                isFacingLocked = true;

                // 现在才锁 AI
                spirit.setAI(false);
                profile.markBusy();
                spirit.setInvulnerable(true);
                spirit.getPathfinder().stopPathfinding();

                // 初始化中心点
                this.centerLocation = spirit.getLocation().add(0, 1.5, 0);

                playRitualStartEffects();
            } else {
                // 维持注视（Allay 会自然转头）
//                spirit.getLookAtPlayerGoal().start(player);
                spirit.lookAt(player);
            }
            return;
        }

        // --- 阶段二：仪式动画 ---
        ticks++;

        // 1. 旋转阶段
        if (ticks < ROTATION_DURATION) {
            handleRotationPhase();
        }
        // 2. 锁定前奏
        else if (ticks == ROTATION_DURATION) {
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.5f, 1.5f);
            player.spawnParticle(Particle.FLASH, centerLocation, 1);
        }
        // 3. 剔除阶段
        else if (ticks < ROTATION_DURATION + 40) {
            if ((ticks - ROTATION_DURATION) % 8 == 0) {
                removeNextWrongItem();
            }
        }
        // 4. 最终展示
        else if (ticks == ROTATION_DURATION + 40) {
            showFinalReward();
        }
        // 5. 发放奖励并结束
        else if (ticks >= ROTATION_DURATION + 70) {
            grantReward();
        }
    }

    private float getYawFromVector(Vector v) {
        return (float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ()));
    }

    private float wrapYaw(float yaw) {
        yaw %= 360;
        if (yaw >= 180) yaw -= 360;
        if (yaw < -180) yaw += 360;
        return yaw;
    }

    /**
     * 正式开始仪式：锁定位置、生成星星、播放音效
     * (只有小精灵飞到位了才会调用这里)
     */
    private void playRitualStartEffects() {
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_ACTIVATE, 2.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        spirit.getWorld().spawnParticle(
                Particle.SONIC_BOOM,
                spirit.getLocation().add(0, 1, 0),
                1
        );

        this.isRitualStarted = true;

        // 1. 位置微调：瞬移到精准坐标并面向玩家
        // 因为之前已经飞得很近了，这个瞬移几乎看不出来，但能保证星阵绝对居中
        Location fixLoc = targetLocation.clone();
        Vector dir = player.getEyeLocation().toVector().subtract(fixLoc.toVector()).normalize();
        fixLoc.setDirection(dir);
//        spirit.teleport(fixLoc);

        // 2. 锁定状态 (现在才关 AI)
        spirit.setAI(false);
        profile.markBusy();
        spirit.setInvulnerable(true);
        spirit.getPathfinder().stopPathfinding();

        // 3. 初始化中心点 (头顶)
        this.centerLocation = spirit.getLocation().add(0, 1.5, 0);

        // 4. 生成奖池显示
        List<LotteryManager.Reward> pool = LotteryManager.getDisplayItems(4);
        pool.add(finalReward);

        for (LotteryManager.Reward reward : pool) {
            ItemDisplay display = centerLocation.getWorld().spawn(centerLocation, ItemDisplay.class, d -> {
                d.setItemStack(reward.getDisplayItem());
                Transformation t = d.getTransformation();
                t.getScale().set(0.5f);
                d.setTransformation(t);
                d.setInterpolationDuration(5);
                d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            });
            displays.add(display);
        }

        // 锁定胜利显示项 (最后一个)
        this.winnerDisplay = displays.get(displays.size() - 1);

        // 5. 播放仪式开始音效
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_ACTIVATE, 2.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        // 视觉爆发
        spirit.getWorld().spawnParticle(Particle.SONIC_BOOM, spirit.getLocation().add(0, 1, 0), 1);
    }

    private void handleRotationPhase() {
        double speed = 0.2 + (ticks * 0.015);
        double angleOffset = ticks * speed;
        double radius = 1.5;

        for (int i = 0; i < displays.size(); i++) {
            double angle = angleOffset + (i * 2 * Math.PI / displays.size());
            Location loc = centerLocation.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            loc.setYaw(loc.getYaw() + 5);
            displays.get(i).teleport(loc);
            centerLocation.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
        }

        if (ticks % 5 == 0) {
            float pitch = 0.5f + (ticks / (float) ROTATION_DURATION) * 1.5f;
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, pitch);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, pitch);
        }
    }

    private void removeNextWrongItem() {
        for (ItemDisplay d : displays) {
            if (!d.equals(winnerDisplay)) {
                d.getWorld().spawnParticle(Particle.SMOKE, d.getLocation(), 10, 0.1, 0.1, 0.1, 0.05);
                player.playSound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                d.remove();
                displays.remove(d);
                break;
            }
        }
    }

    private void showFinalReward() {
        if (winnerDisplay == null || !winnerDisplay.isValid()) return;

        winnerDisplay.teleport(centerLocation);
        Transformation t = winnerDisplay.getTransformation();
        t.getScale().set(1.2f);
        winnerDisplay.setTransformation(t);
        winnerDisplay.setInterpolationDuration(10);

        playRaritySound(finalReward.getRarity());
        centerLocation.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, centerLocation, 30, 0.5, 0.5, 0.5);
    }

    private void playRaritySound(LotteryManager.Rarity rarity) {
        switch (rarity) {
            case GOLD -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.2f);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 2.0f);
            }
            case PURPLE -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
            }
            case AQUA -> {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
            }
            default -> {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
            }
        }
    }

    private void grantReward() {
        finalReward.grant(player, profile);

        String color = switch (finalReward.getRarity()) {
            case GOLD -> "§6§l";
            case PURPLE -> "§d§l";
            case AQUA -> "§b§l";
            default -> "§f";
        };

        player.sendMessage("§8[§d灵契§8] §f星辰为你带来了: " + finalReward.getRarity().getPrefix()
                + " §7- " + color + finalReward.getName());

        cleanup();
    }

    private void cleanup() {
        displays.forEach(d -> { if(d.isValid()) d.remove(); });
        isRitualStarted = false;
        spirit.setAI(true); // 记得恢复AI
        spirit.setInvulnerable(false);
        this.cancel();
    }
}