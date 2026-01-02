package cn.rabitown.rabisystem.modules.spirit.task;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.utils.ExperienceSystem;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CompanionTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(player.getUniqueId());

            // 基础条件：档案存在、已召唤、且开关正常（如果有开关的话）
            if (profile == null || !profile.isSummoned()) continue;

            Allay spirit = SpiritUtils.getSpiritManager().getActiveSpirits().get(player.getUniqueId());

            // 判定条件：小精灵必须有效、在同一个世界、距离玩家不能太远（比如32格内）
            if (spirit != null && spirit.isValid() && spirit.getWorld().equals(player.getWorld())) {
                double distanceSq = spirit.getLocation().distanceSquared(player.getLocation());

                if (distanceSq <= 1024) { // 32格的平方
                    // 逻辑：累加计数
                    int count = profile.getCompanionTickCounter() + 20; // 每次执行加 20 秒

                    if (count >= 60) { // 满 60 秒
                        // 发放经验
                        ExperienceSystem.grantExp(player, profile, ExperienceSystem.ExpType.COMPANION, 2);
                        profile.setCompanionTickCounter(0); // 重置计时

                        // ➕ 统计在线时长
                        profile.addSummonTime(60); // 增加60秒
                        AchievementManager.check(player, profile, "time_online");
                    } else {
                        profile.setCompanionTickCounter(count);
                    }
                }
            }
        }
    }
}