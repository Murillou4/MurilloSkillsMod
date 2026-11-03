package com.murilloskills.skills;

/** Time targets include reference throughput from tools and milestone efficiency.
 * Each row is a session budget, not a time gate. No clock is used to grant XP. */
public final class ProgressionCurve {
    private ProgressionCurve() {}
    public static double minutesForLevel(int level) {
        if (level < 10) return 6;
        if (level < 25) return 12;
        if (level < 50) return 19.2;
        if (level < 75) return 28.8;
        return 38.4;
    }
    public static double referenceEfficiency(SkillType skill, int level) {
        // Measurable production multipliers, deliberately below raw area volume.
        double passive = 1 + level * 0.005;
        double tools = (skill==SkillType.MINER || skill==SkillType.BUILDER || skill==SkillType.FARMER)
                ? level>=75?1.6:level>=50?1.4:level>=25?1.2:1 : 1;
        return passive * tools;
    }
    public static double required(SkillType skill, int level) {
        if (level >= 100) return 0;
        return Math.ceil(minutesForLevel(level)*skill.referenceRate*10*referenceEfficiency(skill,level)*1.04);
    }
    public static double areaWeight(int count) { return count<=0?0:Math.min(8,1+(count-1)*0.25); }
    public static int toolLimit(int unlockLevel, int branchBonus) {
        int n=unlockLevel>=100?144:unlockLevel>=75?72:unlockLevel>=50?36:unlockLevel>=30?18:unlockLevel>=15?9:0;
        return Math.min(256,n + branchBonus * n / 4+(unlockLevel>=55?9:0));
    }
    public static double xpBonus(int prestige) { return 1+Math.clamp(prestige,0,ProgressionService.MAX_PRESTIGE)*0.1; }
    public static double passiveBonus(int prestige) { return 1+Math.clamp(prestige,0,ProgressionService.MAX_PRESTIGE)*0.01; }
}

