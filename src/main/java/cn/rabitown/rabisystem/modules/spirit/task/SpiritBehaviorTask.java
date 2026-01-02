package cn.rabitown.rabisystem.modules.spirit.task;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.utils.ExperienceSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.LevelSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.MusicLibrary;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SpiritBehaviorTask extends BukkitRunnable {

    private final Map<UUID, Allay> activeSpirits = new HashMap<>();
    // 社交状态缓存 (Transient)
    private final Map<UUID, SocialState> socialStates = new HashMap<>();
    private final Map<UUID, Long> socialCooldown = new HashMap<>();
    private final Map<UUID, Long> socialStartTime = new HashMap<>();
    private int autoEatTimer = 0;
    private int duplicateCheckTimer = 0; // 防止每Tick检测卡顿
    private int mp_ticks = 0;


    @Override
    public void run() {
        autoEatTimer++;
        duplicateCheckTimer++;
        mp_ticks++;
        boolean isEatTick = autoEatTimer % 40 == 0; // 每 2秒执行一次自动进食检测
        boolean isDupeCheckTick = duplicateCheckTimer % 100 == 0; // [新增] 每 5秒执行一次大范围查重

        long now = System.currentTimeMillis();
        Map<UUID, Allay> spirits = SpiritUtils.getSpiritManager().getActiveSpirits();

        // 清理无效的社交状态
        socialStates.keySet().removeIf(uuid -> !spirits.containsKey(uuid));

        // 复制一份Values防止并发修改（虽然是主线程，但这是好习惯）
        List<Allay> allayList = new ArrayList<>(spirits.values());

        for (Map.Entry<UUID, Allay> entry : spirits.entrySet()) {
            UUID ownerId = entry.getKey();
            Allay spirit = entry.getValue();
            Player owner = Bukkit.getPlayer(ownerId);
            SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(ownerId);

            // 1. 基础有效性检查 (若主人下线或实体消失，交给Manager处理)
            if (owner == null || !owner.isOnline() || !spirit.isValid()) {
                SpiritUtils.getSpiritManager().despawnSpirit(ownerId);
                continue;
            }
            // 1. 状态自检 (看门狗)
            if (!spirit.isAware()) {
                // 获取当前时间与最后一次忙碌时间的差值
                long timeSinceBusy = System.currentTimeMillis() - profile.getLastBusyTime();

                // 如果处于无意识状态超过 5秒 (5000ms)，说明卡住了 (通常技能动画不会超过3-4秒)
                if (timeSinceBusy > 5000) {
                    spirit.setAware(true); // 强制唤醒
                    // 可选：播放一个小音效提示 debug
                    owner.playSound(spirit.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.5f, 2.0f);
                }
            }


            // =========================================================
            // 🛑 [核心修改] 高优先级检测器
            // 如果检测器认为需要立即干预（距离过远、有重复等），则跳过本Tick所有其他行为
            // =========================================================
            if (checkSafetyAndDistance(owner, spirit, profile, isDupeCheckTick)) {
                continue;
            }

            // --- 以下是距离安全 (< 8 米) 时的正常悠闲行为 ---

            // 2. 生命反哺 (治疗逻辑)
            handleHealing(owner, spirit, profile, now);

            // 3. 社交与跟随逻辑
            boolean onCooldown = socialCooldown.getOrDefault(ownerId, 0L) > now;
            SocialState state = socialStates.computeIfAbsent(ownerId, k -> new SocialState());

            // --- 心情 自然陪伴 (+1 / 60s)  ---
            if (now - profile.getLastNaturalMoodTime() >= 60000) {
                profile.addMood(1);
                profile.setLastNaturalMoodTime(now);
            }

            // 如果主人状态不佳，强制跟随模式
            if (owner.getHealth() < 12.0) {
                doFollowLogic(owner, spirit);
            } else {
                // 尝试社交
                if (state.type != BehaviorType.NONE) {
                    doInteractionLogic(spirit, state, allayList, now, ownerId, profile);
                } else {
                    // 尝试寻找伙伴
                    List<Allay> neighbors = getNeighbors(spirit, allayList, 8.0);
                    // 只有在没冷却、且周围有朋友时才社交
                    if (!onCooldown && !neighbors.isEmpty() && neighbors.size() <= 5) {
                        joinInteraction(spirit, neighbors, state, now, ownerId);
                    } else {
                        doFollowLogic(owner, spirit);
                    }
                }
            }

            // --- 4 自然恢复与经验 ---
            handleNaturalRegen(spirit, profile, now);

            // 执行自动进食检测
            if (isEatTick && profile.isAutoEat()) {
                handleAutoEat(spirit, profile);
            }

            // 执行物品拾取检测 (正常状态下才允许捡东西)
            handleVoidGravity(entry.getValue(), profile, owner);

            // --- MP 自然恢复 ---
            // 每秒恢复一次 (runTaskTimer 是 5 ticks，所以每 4 次 tick 执行一次)
            if (mp_ticks % 4 == 0) { // 约 1秒
                // 基础恢复量 1.0
                double regen = 1.0;

                // 心情影响
                if (profile.getMood() > 80) regen += 0.5;
                if (profile.getMood() < 30) regen -= 0.5;

                // 被动技能影响
                if (profile.isSkillUnlocked("star_passive_mp")) regen += 0.2; // 20%

                if (profile.getMana() < profile.getMaxMana()) {
                    profile.addMana(regen);
                }

            }
        }

        // 更新音乐节拍
        updateMusicTicks();
    }

    private void handleNaturalRegen(Allay spirit, SpiritProfile profile, long now) {
        long regenCooldown = LevelSystem.getNaturalRegenInterval(profile.getLevel());
        if (now - profile.getLastNaturalRegenTime() >= regenCooldown) {
            double maxHealth = spirit.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (spirit.getHealth() < maxHealth) {
                spirit.setHealth(Math.min(maxHealth, spirit.getHealth() + 1));
            }
            profile.addExp(1);
            profile.setLastNaturalRegenTime(now);
        }
    }

    /**
     * 🛡️ 核心检测器：负责距离控制、强制跟随与防重影
     *
     * @return true 表示触发了强制行为（如传送、强行赶路），应中断后续逻辑
     */
    private boolean checkSafetyAndDistance(Player owner, Allay spirit, SpiritProfile profile, boolean checkDupes) {
        Location ownerLoc = owner.getLocation();
        Location spiritLoc = spirit.getLocation();

        // 1. 跨世界检测
        if (!spirit.getWorld().equals(owner.getWorld())) {
            spirit.teleport(ownerLoc.add(0, 1, 0));
            resetSocialAndTasks(profile, owner.getUniqueId());
            return true;
        }

        double distSq = spiritLoc.distanceSquared(ownerLoc);

        // 2. 防重影检测 (检测重复实体)
        // 只有距离过远(可能区块加载导致复制) 或 定时检查时触发，节省性能
        if (checkDupes || distSq > 400) {
            cleanupDuplicateSpirits(owner, spirit);
        }

        // 3. 距离 > 21米 (21*21 = 441) -> 强制传送/重召
        if (distSq > 441) {
            // 尝试传送
            spirit.teleport(ownerLoc.add(0, 1, 0));

            // 再次检查 (如果传送失败，比如被插件拦截或未知Bug)
            if (spirit.getLocation().distanceSquared(ownerLoc) > 441) {
                SpiritUtils.getPlugin().getLogger().warning("小精灵传送失败，执行强制重召: " + owner.getName());
                // 销毁并重召
                SpiritUtils.getSpiritManager().despawnSpirit(owner.getUniqueId());
                SpiritUtils.getSpiritManager().restoreSpirit(owner);
            }

            resetSocialAndTasks(profile, owner.getUniqueId());
            return true; // 既然传送了，就别动了，下个Tick再说
        }

        // 4. 距离 > 8米 (8*8 = 64) -> 强制打断一切，优先跟随
        if (distSq > 64) {
            // 打断正在进行的任何任务
            resetSocialAndTasks(profile, owner.getUniqueId());

            // 强制移动
            spirit.getPathfinder().moveTo(owner, 1.6); // 1.6 倍速全速跟随

            // 如果还在玩，强制停下看向主人
            if (spirit.getTarget() != owner) {
                spirit.setTarget(owner);
            }

            return true; // 返回 true，run() 方法会 continue，阻止后续的拾取/社交代码执行
        }

        return false; // 距离合适，允许自由活动
    }

    /**
     * 辅助：重置社交状态和任务目标
     */
    private void resetSocialAndTasks(SpiritProfile profile, UUID ownerId) {
        // 1. 清除社交状态
        resetSocialState(ownerId);
        // 2. 放弃物品拾取目标 (防止还想着那个掉落物)
        profile.setTargetItemId(null);
        // 3. 增加社交冷却，防止刚回来又跑去玩 (5秒冷静期)
        socialCooldown.put(ownerId, System.currentTimeMillis() + 5000);
    }

    /**
     * 清理当前玩家周围多余的、不属于当前记录的小精灵
     */
    private void cleanupDuplicateSpirits(Player owner, Allay currentSpirit) {
        // 搜索玩家周围 30 格内的实体
        for (Entity e : owner.getNearbyEntities(30, 30, 30)) {
            if (e instanceof Allay && !e.equals(currentSpirit)) {
                // 如果这个实体被标记为本插件的小精灵
                if (SpiritUtils.getSpiritManager().isSpirit(e)) {
                    // 进一步判定：名字是否匹配（防止误删别人的）
                    net.kyori.adventure.text.Component nameComp = e.customName();
                    if (nameComp != null) {
                        String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(nameComp);
                        // 如果这个名字和当前玩家小精灵的名字完全一样，则视为重影
                        if (plainName.equals(SpiritUtils.getSpiritManager().getProfile(owner.getUniqueId()).getName())) {
                            e.remove();
                            // 视觉反馈：冒出一点烟雾
                            owner.getWorld().spawnParticle(Particle.SMOKE, e.getLocation(), 5);
                        }
                    }
                }
            }
        }
    }

    private void handleAutoEat(Allay spirit, SpiritProfile profile) {
        // 只有在血量不满时才进食
        if ((int) spirit.getHealth() >= (int) profile.getMaxHealth()) return;

        ItemStack[] foodBag = profile.getFoodBag();
        for (int i = 0; i < foodBag.length; i++) {
            ItemStack item = foodBag[i];
            if (item != null && item.getType().isEdible()) {
                // 排除禁忌食物
                Material type = item.getType();
                if (type == Material.GOLDEN_APPLE || type == Material.ROTTEN_FLESH ||
                        type == Material.SPIDER_EYE || type == Material.PUFFERFISH) continue;

                // 计算回复量 = 饱食度 / 2 (向上取整)
                // 这里假设通过 NMS 或配置文件获取饱食度，简单实现可映射常见食物
                double regen = 2.0;
                if (type == Material.COOKED_BEEF) regen = 4.0;

                spirit.setHealth(Math.min(profile.getMaxHealth(), spirit.getHealth() + regen));
                spirit.getWorld().playSound(spirit.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);

                // 消耗一个食物
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) foodBag[i] = null;

                profile.setFoodBag(foodBag); // 更新容器
                // ➕ 统计自动进食次数
                profile.addStat("auto_eat_count", 1);
                //  获取玩家并触发成就检查：自动进食
                Player owner = Bukkit.getPlayer(profile.getOwnerId());
                if (owner != null && owner.isOnline()) {
                    AchievementManager.check(owner, profile, "auto_eat_count");
                }

                break; // 每次检测只吃一个
            }
        }
    }

    private void handleHealing(Player owner, Allay spirit, SpiritProfile profile, long now) {
        // 检查心情状态：抑郁状态（心情<30且等级<50）停止回血
        if (profile.getMood() < 30 && profile.getLevel() < 50) return;

        // 检查触发条件：主人血量必须低于 12 点
        if (owner.getHealth() < 12.0) {

            // 检查反哺冷却时间 (5秒 CD)
            if (now - profile.getLastHealTime() < 5000) return;

            profile.setLastHealTime(now);
            // 治疗消耗与效果逻辑 [cite: 42, 44, 51, 63]
            double cost = 2.0; // 固定消耗 2 HP [cite: 42]
            double healAmount = LevelSystem.getHealAmount(profile.getLevel());

            // 检查小精灵是否有足够的血量进行反哺 || 检查心情
            if (spirit.getHealth() > cost) {

                spirit.setHealth(spirit.getHealth() - cost);
                spirit.playHurtAnimation(0);

                // 执行回血
                double newHealth = Math.min(owner.getAttribute(Attribute.MAX_HEALTH).getValue(),
                        owner.getHealth() + healAmount);
                owner.setHealth(newHealth);
                owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

                // 扣除心情惩罚：等级 >= 50 (共鸣) 后免疫 [cite: 18, 60]
                int moodCost = LevelSystem.getHealMoodCost(profile.getLevel());
                if (moodCost > 0) {
                    profile.addMood(-moodCost);
                }

                // --- 经验获取 ---
                ExperienceSystem.grantExp(owner, profile, ExperienceSystem.ExpType.HEAL, 3);

            }
        }
    }

    private void doFollowLogic(Player owner, Allay spirit) {
        if (spirit.getLocation().distance(owner.getLocation()) > 3.0) {
            spirit.getPathfinder().moveTo(owner, 1.6);
        } else if (!spirit.getPathfinder().hasPath() && ThreadLocalRandom.current().nextDouble() < 0.05) {
            // 随机闲逛
            Location rnd = owner.getLocation().add(Math.random() * 6 - 3, Math.random() * 2, Math.random() * 6 - 3);
            if (rnd.getBlock().getType() == Material.AIR) spirit.getPathfinder().moveTo(rnd, 0.8);
        }
    }

    // --- 社交与音乐逻辑 (从原代码移植并封装) ---
    // 为节省篇幅，这里保留核心结构，具体BehaviorType实现逻辑同原代码
    // 关键是将 SocialState 内部类提取出来或放在这里
    private void doInteractionLogic(Allay spirit, SocialState state, List<Allay> all, long now, UUID ownerId, SpiritProfile profile) {
        // 这里填入原代码 doInteractionBehavior 的逻辑
        // 注意：原代码的 MusicBox 类需改为调用 MusicLibrary
        List<Allay> allSpiritsList = new ArrayList<>(activeSpirits.values());
        List<Allay> neighbors = getNeighbors(spirit, allSpiritsList, 8.0);
        if (state.type == BehaviorType.CIRCLE) {
            Location center = spirit.getLocation().clone();
            for (Allay n : neighbors) center.add(n.getLocation());
            center.multiply(1.0 / (neighbors.size() + 1));
            Vector dir = spirit.getLocation().toVector().subtract(center.toVector());
            if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
            spirit.getPathfinder().moveTo(center.add(dir.normalize().multiply(2.5)), 0.6);
            if (now - state.lastSoundTime > 3000 && ThreadLocalRandom.current().nextDouble() < 0.05) {
                spirit.getWorld().playSound(spirit.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 1.5f);
                state.lastSoundTime = now;
            }

        } else if (state.type == BehaviorType.TRAIN) {
            Entity t = (state.targetSpirit != null) ? Bukkit.getEntity(state.targetSpirit) : null;
            if (t == null || !t.isValid() || t.getLocation().distance(spirit.getLocation()) > 10) {
                if (!neighbors.isEmpty()) state.targetSpirit = neighbors.get(0).getUniqueId();
                else state.type = BehaviorType.NONE;
            } else if (t instanceof Allay) {
                if (spirit.getLocation().distance(t.getLocation()) > 2.0) spirit.getPathfinder().moveTo((Allay) t, 1.0);
                else {
                    spirit.getPathfinder().stopPathfinding();
                    Location l = t.getLocation().clone();
                    l.setDirection(l.toVector().subtract(spirit.getLocation().toVector()));
                    spirit.setRotation(l.getYaw(), 0);
                }
            }

        } else if (state.type == BehaviorType.CHAT) {
            if (!neighbors.isEmpty()) {
                Allay n = neighbors.get(0);
                if (spirit.getLocation().distance(n.getLocation()) > 2.2) {
                    Vector dir = spirit.getLocation().toVector().subtract(n.getLocation().toVector());
                    if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
                    spirit.getPathfinder().moveTo(n.getLocation().add(dir.normalize().multiply(2.2)), 0.5);
                } else {
                    spirit.getPathfinder().stopPathfinding();
                    if (now - state.lastLookTime > 3000) {
                        Location l = n.getLocation().clone();
                        l.setDirection(l.toVector().subtract(spirit.getLocation().toVector()));
                        spirit.setRotation(l.getYaw(), 0);
                        state.lastLookTime = now + 2000;
                    }
                    if (now - state.lastSoundTime > 2000 && ThreadLocalRandom.current().nextDouble() < 0.1) {
                        spirit.getWorld().playSound(spirit.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 1f);
                        state.lastSoundTime = now;
                    }
                }
            }

        } else if (state.type == BehaviorType.DANCE) {
            Location center = spirit.getLocation().clone();
            for (Allay n : neighbors) center.add(n.getLocation());
            center.multiply(1.0 / (neighbors.size() + 1));
            Vector dir = spirit.getLocation().toVector().subtract(center.toVector());
            if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
            Location target = center.add(dir.normalize().multiply(2.0));
            if (spirit.getLocation().distance(target) > 0.5) spirit.getPathfinder().moveTo(target, 0.6);
            else {
                spirit.getPathfinder().stopPathfinding();
                spirit.setRotation(spirit.getLocation().getYaw() + 15, 0);

                // ➕ 触发跳舞成就
                AchievementManager.check(Bukkit.getPlayer(ownerId), profile, "party_time");
                // 音乐播放逻辑
                if (state.currentSong == MusicLibrary.SongType.RANDOM) {
                    if (now - state.lastSoundTime > 800) {
                        spirit.getWorld().playSound(spirit.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f + (float) ThreadLocalRandom.current().nextDouble());
                        state.lastSoundTime = now;
                        MusicLibrary.spawnMusicParticle(spirit);
                    }
                } else {
                    // ➕ 插入成就检查点 2: 圣诞快乐 (Padoru)
                    if (state.currentSong == MusicLibrary.SongType.PADORU) {
                        AchievementManager.check(Bukkit.getPlayer(ownerId), profile, "music_padoru");
                    }
                    if (state.shouldPlayNote) {
                        List<MusicLibrary.MusicBox.MusicNote> sheet = MusicLibrary.MusicBox.getSong(state.currentSong);
                        if (!sheet.isEmpty()) {
                            // 播放上一个音符 (因为 index 已经加了)
                            int idx = state.noteIndex - 1;
                            if (idx < 0) idx = sheet.size() - 1;
                            MusicLibrary.MusicBox.MusicNote note = sheet.get(idx);
                            if (note.pitch > 0.1) {
                                spirit.getWorld().playSound(spirit.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, note.pitch);
                                MusicLibrary.spawnMusicParticle(spirit);
                            }
                        }
                    }
                }
            }
        }
    }

    private UUID findOwner(Allay s) {
        for (Map.Entry<UUID, Allay> e : activeSpirits.entrySet()) if (e.getValue().equals(s)) return e.getKey();
        return null;
    }

    private void joinInteraction(Allay me, List<Allay> neighbors, SocialState state, long now, UUID ownerId) {
        socialStartTime.put(ownerId, now);
        // ➕ 触发社交成就
        AchievementManager.check(Bukkit.getPlayer(ownerId),
                SpiritUtils.getSpiritManager().getProfile(ownerId), "meet_friend");
        // 加入现有
        for (Allay n : neighbors) {
            UUID nid = findOwner(n);
            if (nid != null && socialStates.containsKey(nid)) {
                SocialState ns = socialStates.get(nid);
                if (ns.type != BehaviorType.NONE) {
                    state.type = ns.type;
                    state.currentSong = ns.currentSong;
                    state.noteIndex = ns.noteIndex;
                    state.noteWaitTicks = ns.noteWaitTicks;
                    if (state.type == BehaviorType.TRAIN) state.targetSpirit = n.getUniqueId();
                    return;
                }
            }
        }
        // 发起新互动
        int rand = ThreadLocalRandom.current().nextInt(100);
        if (rand < 50) { // 50% Dance
            state.type = BehaviorType.DANCE;
            // 选歌
            List<MusicLibrary.SongType> songs = new ArrayList<>(Arrays.asList(MusicLibrary.SongType.values()));
            songs.remove(MusicLibrary.SongType.RANDOM);
            if (ThreadLocalRandom.current().nextDouble() < 0.8) {
                state.currentSong = songs.get(ThreadLocalRandom.current().nextInt(songs.size()));
            } else {
                state.currentSong = MusicLibrary.SongType.RANDOM;
            }
            state.noteIndex = 0;
            state.noteWaitTicks = 0;
            state.shouldPlayNote = false;
        } else if (rand < 70) state.type = BehaviorType.CIRCLE;
        else if (rand < 90) {
            state.type = BehaviorType.TRAIN;
            state.targetSpirit = neighbors.get(0).getUniqueId();
        } else state.type = BehaviorType.CHAT;
    }

    private void updateMusicTicks() {
        // 音乐播放器的 tick 更新逻辑
        for (SocialState state : socialStates.values()) {
            state.shouldPlayNote = false; // 每 Tick 重置播放标记

            // 只有处于舞蹈模式且选择了具体曲目时才处理节拍
            if (state.type == BehaviorType.DANCE && state.currentSong != MusicLibrary.SongType.RANDOM) {
                List<MusicLibrary.MusicBox.MusicNote> sheet = MusicLibrary.MusicBox.getSong(state.currentSong);
                if (!sheet.isEmpty()) {
                    state.noteWaitTicks--;
                    if (state.noteWaitTicks <= 0) {
                        state.shouldPlayNote = true; // 触发播放逻辑

                        // 准备下一音符数据
                        MusicLibrary.MusicBox.MusicNote currentNote = sheet.get(state.noteIndex);
                        state.noteWaitTicks = currentNote.delay;
                        state.noteIndex++;

                        // 循环播放
                        if (state.noteIndex >= sheet.size()) {
                            state.noteIndex = 0;
                        }
                    }
                }
            }
        }
    }

    private void resetSocialState(UUID ownerId) {
        socialStates.remove(ownerId);
    }

    private List<Allay> getNeighbors(Allay me, List<Allay> all, double r) {
        return all.stream().filter(o -> !o.equals(me) && o.getWorld().equals(me.getWorld()) && o.getLocation().distance(me.getLocation()) <= r).collect(Collectors.toList());
    }

    // 内部类: 社交状态
    public static class SocialState {
        BehaviorType type = BehaviorType.NONE;
        UUID targetSpirit = null;
        // 音乐控制
        MusicLibrary.SongType currentSong = MusicLibrary.SongType.RANDOM;
        int noteIndex = 0;
        int noteWaitTicks = 0;
        boolean shouldPlayNote = false;
        long lastSoundTime = 0;
        long lastLookTime = 0;
        long interactionStartTime = 0; // 记录进入互动状态的时间
    }

    public enum BehaviorType {NONE, CIRCLE, TRAIN, CHAT, DANCE}

    private void handleVoidGravity(Allay spirit, SpiritProfile profile, Player owner) {
        // 1. 基础检查
        if (profile.getLevel() < 30 || !profile.isVoidGravityEnabled()) return;

        // 2. 检查当前追踪的目标是否仍然有效
        org.bukkit.entity.Item targetItem = null;
        if (profile.getTargetItemId() != null) {
            Entity entity = Bukkit.getEntity(profile.getTargetItemId());
            // 关键点：如果物品被玩家捡走、被插件清理或自然消失，isValid() 会返回 false
            if (entity instanceof org.bukkit.entity.Item item && item.isValid() && !item.isDead()) {
                targetItem = item;
            } else {
                // 目标已消失：重置目标，让小精灵停下
                profile.setTargetItemId(null);
                spirit.getPathfinder().stopPathfinding();
            }
        }

        // 3. 如果没有目标，则在范围内搜索
        double range = profile.getPickupRange();
        if (targetItem == null) {
            Collection<Entity> nearby = spirit.getWorld().getNearbyEntities(spirit.getLocation(), range, range, range);
            for (Entity e : nearby) {
                if (e instanceof org.bukkit.entity.Item item && item.getPickupDelay() <= 0) {
                    // 过滤逻辑
                    Material type = item.getItemStack().getType();
                    if (profile.isFilterEnabled()) {
                        boolean inList = profile.getFilterList().contains(type);
                        if (profile.isWhitelistMode() ? !inList : inList) continue;
                    }

                    targetItem = item;
                    profile.setTargetItemId(item.getUniqueId());
                    break; // 每次只锁定一个目标
                }
            }
        }

        // 4. 执行追踪动作
        if (targetItem != null) {
            double dist = spirit.getLocation().distance(targetItem.getLocation());

            if (dist > 1.2) {
                // 距离超过1.2格：加速飞向物品
                spirit.getPathfinder().moveTo(targetItem.getLocation(), 1.4);
            } else {
                // 到达目的地：尝试拾取
                ItemStack toAdd = targetItem.getItemStack();
                if (addItemToSpiritBackpack(profile, toAdd)) {
                    // 成功捡起：播放玩家捡起音效
                    spirit.getWorld().playSound(spirit.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
                    targetItem.remove();
                    profile.setTargetItemId(null);
                    // ➕ 增加拾取统计 (按数量加)
                    profile.addStat("gravity_pickup", toAdd.getAmount());
                    // 触发成就检查：虚空引力拾取
                    AchievementManager.check(owner, profile, "gravity_pickup");
                } else {
                    // 背包满了：放弃目标，防止原地死循环
                    profile.setTargetItemId(null);
                    spirit.getPathfinder().stopPathfinding();
                }
            }
        }
    }

    /**
     * 辅助方法：将物品尝试放入 Profile 的背包数组中
     *
     * @return 是否成功添加（哪怕只添加了部分堆叠）
     */
    private boolean addItemToSpiritBackpack(SpiritProfile profile, ItemStack itemToAdd) {
        ItemStack[] backpack = profile.getBackpack();
        boolean changed = false;

        // 🛑 关键修改：定义有效存储格数
        // 如果你的背包最后一格是“返回”或“装饰”，这里就减 1
        // 如果最后一行都是装饰，就减 9
        int reservedSlots = 1;
        int validSize = backpack.length - reservedSlots;

        // 防止数组越界（比如背包极小的情况）
        if (validSize < 0) validSize = 0;

        // 第一轮：尝试合并到已有的相同物品堆叠中 (只检查有效区域)
        for (int i = 0; i < validSize; i++) {
            ItemStack slot = backpack[i];
            if (slot != null && slot.isSimilar(itemToAdd)) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, itemToAdd.getAmount());
                    slot.setAmount(slot.getAmount() + toAdd);
                    itemToAdd.setAmount(itemToAdd.getAmount() - toAdd);
                    changed = true;
                }
            }
            if (itemToAdd.getAmount() <= 0) break;
        }

        // 第二轮：如果还有剩余，找空位放入 (只检查有效区域)
        if (itemToAdd.getAmount() > 0) {
            for (int i = 0; i < validSize; i++) {
                if (backpack[i] == null || backpack[i].getType() == Material.AIR) {
                    backpack[i] = itemToAdd.clone();
                    itemToAdd.setAmount(0);
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            profile.setBackpack(backpack); // 写回 Profile 内存
            /*// --- 🏆 满载而归成就检查 (只检查有效区域) ---
            boolean isFull = true;
            for (int i = 0; i < validSize; i++) {
                ItemStack is = backpack[i];
                if (is == null || is.getType() == Material.AIR) {
                    isFull = false;
                    break;
                }
            }

            if (isFull) {
                Player p = org.bukkit.Bukkit.getPlayer(profile.getOwnerId());
                if (p != null) {
                    cn.rabitown.manager.AchievementManager.check(p, profile, "backpack_full");
                }
            }*/
            return itemToAdd.getAmount() <= 0;
        }
        return false;
    }


}