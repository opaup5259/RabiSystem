package cn.rabitown.rabisystem.modules.spirit.utils;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import java.util.Calendar;

public class ExperienceSystem {

    /**
     * 统一处理经验增加，包含日期重置和每日上限逻辑
     * @param player 主人对象
     * @param profile 灵契档案
     * @param type 经验来源类型
     * @param amount 初始增加的数值
     */
    public static void grantExp(Player player, SpiritProfile profile, ExpType type, int amount) {
        // 1. 跨天检测与重置
        checkAndResetDaily(profile);

        // 2. 获取该类型的当前进度和上限
        int current = type.getCurrent(profile);
        int limit = type.getLimit();
        int finalGrantAmount = 0;

        // 3. 经验获取逻辑判断
        if (current < limit) {
            // 未达到上限：正常获取，但不能超过硬上限补齐部分
            finalGrantAmount = Math.min(amount, limit - current);
        } else if (type.isSoftLimit()) {
            // 已达到上限且为“软上限”模式：经验减半，向上取整
            // 使用 (int) Math.ceil(amount / 2.0)
            finalGrantAmount = (int) Math.ceil(amount / 2.0);
        }

        // 4. 执行增加经验
        if (finalGrantAmount > 0) {
            profile.addExp(finalGrantAmount);
            type.addProgress(profile, finalGrantAmount);

            //自然陪伴，不发送actionbar
            if (type == ExpType.COMPANION) {
                return;
            }
            // 5. 动态显示逻辑
            StringBuilder sb = new StringBuilder();
            sb.append("§a[").append(profile.getName()).append("] ");
            sb.append(type.getLabel()).append(" +").append(finalGrantAmount).append(" XP");

            // --- 核心修改：根据 softLimit 决定后缀显示 ---
            if (!type.isSoftLimit()) {
                // 硬上限类型：始终显示当前进度 (当前/上限)
                sb.append(" §7(").append(type.getCurrent(profile)).append("/").append(limit).append(")");
            }

            player.sendActionBar(Component.text(sb.toString()));
        }
    }

    private static void checkAndResetDaily(SpiritProfile profile) {
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);

        if (profile.getLastLoginDate() != today) {
            profile.setLastLoginDate(today);
            profile.resetDailyProgress();
        }
    }

    public enum ExpType {
        PET("摸摸头", 30, false),
        COMPANION("自然陪伴", 120, false),
        SOCIAL("社交互动", 30, false),
        FEED("美食投喂", 30, false),
        HEAL("生命反哺", 60, false),
        BUFF("灵力共鸣", 120, true),   // 对应文档该项经验超过120后减半
        DAMAGE("灵力迸发", 120, true), // 对应文档该项经验超过120后减半
        SIGNIN("签到契约", 60, false); // 新增签到类型，上限 60 XP

        private final String label;
        private final int limit;
        private final boolean softLimit; // true为软上限，到达后经验减半 [cite: 31, 38]

        ExpType(String label, int limit, boolean softLimit) {
            this.label = label;
            this.limit = limit;
            this.softLimit = softLimit;
        }

        public String getLabel() { return label; }
        public int getLimit() { return limit; }
        public boolean isSoftLimit() { return softLimit; }

        // 动态关联 Profile 中的字段
        public int getCurrent(SpiritProfile p) {
            return switch (this) {
                case PET -> p.getDailyPetExp();
                case COMPANION -> p.getDailyCompanionExp();
                case SOCIAL -> p.getDailySocialExp();
                case FEED -> p.getDailyFeedExp();
                case HEAL -> p.getDailyHealExp();
                case BUFF -> p.getDailyBuffExp();
                case DAMAGE -> p.getDailyDamageExp();
                case SIGNIN -> p.getDailySignInExp();
            };
        }

        public void addProgress(SpiritProfile p, int val) {
            switch (this) {
                case PET -> p.addDailyPetExp(val);
                case COMPANION -> p.addDailyCompanionExp(val);
                case SOCIAL -> p.addDailySocialExp(val);
                case FEED -> p.addDailyFeedExp(val);
                case HEAL -> p.addDailyHealExp(val);
                case BUFF -> p.addDailyBuffExp(val);
                case DAMAGE -> p.addDailyDamageExp(val);
                case SIGNIN -> p.addDailySignInExp(val);
            }
        }
    }
}