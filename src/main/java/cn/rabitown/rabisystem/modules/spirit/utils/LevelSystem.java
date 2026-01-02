package cn.rabitown.rabisystem.modules.spirit.utils;

public class LevelSystem {

    /**
     * 获取指定等级的阶段称号
     * 严格对应文档表格
     */
    public static String getStageName(int level) {
        if (level <= 10) return "初识"; // 1-10
        if (level <= 30) return "羁绊"; // 11-30
        if (level <= 50) return "共鸣"; // 31-50
        if (level <= 80) return "灵契"; // 51-80
        return "双星";                   // 81-100+
    }

    /**
     * 获取从当前等级升级到下一级所需的经验值
     * 严格按照文档表格的范围进行线性计算
     *
     */
    public static int getExpToNextLevel(int currentLevel) {
        if (currentLevel >= 100) return 9999999; // 满级封顶

        // 阶段 1: 初识 (1-10级) -> 经验需求: 100 - 500
        if (currentLevel < 10) {
            return interpolate(currentLevel, 1, 10, 100, 500);
        }
        // 阶段 2: 羁绊 (11-30级) -> 经验需求: 500 - 1200
        if (currentLevel < 30) {
            return interpolate(currentLevel, 10, 30, 500, 1200);
        }
        // 阶段 3: 共鸣 (31-50级) -> 经验需求: 1200 - 2000
        if (currentLevel < 50) {
            return interpolate(currentLevel, 30, 50, 1200, 2000);
        }
        // 阶段 4: 灵契 (51-80级) -> 经验需求: 2000 - 3500
        if (currentLevel < 80) {
            return interpolate(currentLevel, 50, 80, 2000, 3500);
        }
        // 阶段 5: 双星 (81-100级) -> 经验需求: 3500 - 5000+
        return interpolate(currentLevel, 80, 100, 3500, 5000);
    }

    /**
     * 线性插值计算工具
     * 计算当前等级在区间内的具体数值
     *
     * @param current 当前等级
     * @param minLv 区间最小等级
     * @param maxLv 区间最大等级
     * @param minExp 起始经验要求
     * @param maxExp 结束经验要求
     */
    private static int interpolate(int current, int minLv, int maxLv, int minExp, int maxExp) {
        double ratio = (double) (current - minLv) / (maxLv - minLv);
        return (int) (minExp + ratio * (maxExp - minExp));
    }

    /**
     * 获取从 1级 升级到 targetLevel 所需的【累计总经验】
     * 用于计算满级后的溢出经验
     */
    public static double getTotalExpToReachLevel(int targetLevel) {
        double total = 0;
        // 从 1 级开始累加每一级的需求
        for (int i = 1; i < targetLevel; i++) {
            total += getExpToNextLevel(i);
        }
        return total;
    }

    /**
     * 根据玩家的总经验值，反向计算出当前等级
     * 这是一个准确的累加计算
     */
    public static int calculateLevel(double totalExp) {
        int level = 1;
        // 循环扣除每一级所需的经验，直到剩余经验不足以升级
        while (level < 100) {
            int needed = getExpToNextLevel(level);
            if (totalExp >= needed) {
                totalExp -= needed;
                level++;
            } else {
                break;
            }
        }
        return level;
    }

    /**
     * 获取当前等级内的剩余经验 (用于显示进度条: 2450/3500)
     * 例如总经验 5000，升到LvX消耗了4500，则返回 500
     */
    public static double getCurrentLevelExp(double totalExp) {
        int level = 1;
        while (level < 100) {
            int needed = getExpToNextLevel(level);
            if (totalExp >= needed) {
                totalExp -= needed;
                level++;
            } else {
                break;
            }
        }
        return totalExp;
    }

    // --- 基础属性 ---
    public static double getMaxHealth(int level) {
        if (level >= 80) return 25.0;
        if (level >= 50) return 20.0;
        if (level >= 30) return 15.0;
        return 10.0;
    }
    public static int getBackpackSize(int level) {
        if (level >= 100) return 54;
        if (level >= 50) return 27;
        if (level >= 10) return 18;
        return 9;
    }
    public static int getFoodBagSlots(int level) {
        if (level < 10) return 1;
        if (level < 30) return 2;
        if (level < 50) return 3;
        if (level < 80) return 4;
        return 5;
    }

    /**
     * 根据当前等级计算应获得的技能点数 (不含额外点数)
     * 规则: Lv.10, 30, 50, 80, 100 各获得 1 点
     */
    public static int getLevelSkillPoints(int level) {
        int points = 0;
        if (level >= 10) points++;
        if (level >= 30) points++;
        if (level >= 50) points++;
        if (level >= 80) points++;
        if (level >= 100) points++;
        return points;
    }

    /**
     * 自然回血间隔 (毫秒)
     */
    public static long getNaturalRegenInterval(int level) {
        // Lv.10及以上加速: 50秒，否则 60秒
        return (level >= 10) ? 50 * 1000L : 60 * 1000L;
    }

    // --- 技能：生命反哺 (Heal) ---
    public static double getHealAmount(int level) {
        if (level >= 80) return 4.0;
        if (level >= 30) return 3.0;
        if (level >= 10) return 2.0;
        return 1.0;
    }

    public static int getHealMoodCost(int level) {
        // Lv.50 (共鸣) 后免疫心情惩罚
        return (level >= 50) ? 0 : 2;
    }

    // --- 技能：灵力共鸣 (Resonance/Buff) ---
    public static int getResonanceDurationTicks(int level) {
        // Lv.50 以上持续 10秒(200tick)，否则 8秒(160tick)
        return (level >= 50) ? 200 : 160;
    }

    public static int getResonanceAmplifier(int level) {
        // Lv.50 以上力量 II (1)，否则力量 I (0)
        return (level >= 50) ? 1 : 0;
    }

    public static String getResonanceTierName(int level) {
        return (level >= 50) ? "II" : "I";
    }

    public static long getResonanceCooldown(int level) {
        // Lv.50 以上 60秒，否则 80秒
        return (level >= 50) ? 60 * 1000L : 80 * 1000L;
    }

    // --- 技能：灵力迸发 (Burst/Damage) ---
    public static double getBurstDamage(int level) {
        if (level >= 100) return 35.0;
        if (level >= 80) return 25.0;
        return 15.0;
    }

    public static long getBurstCooldown(int level) {
        if (level >= 100) return 12 * 1000L;
        if (level >= 80) return 15 * 1000L;
        return 18 * 1000L;
    }

    // --- 技能：虚空引力 ---
    public static double getPickupRange(int level) {
        if (level < 30) return 0.0;
        return 5.0 + Math.floor((level - 30) / 20.0);
    }
}