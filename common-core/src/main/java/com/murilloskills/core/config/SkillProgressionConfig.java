package com.murilloskills.core.config;

public final class SkillProgressionConfig {
    public static final SkillProgressionConfig DEFAULT = new SkillProgressionConfig(
            100,
            3,
            60,
            15,
            2,
            0.05f,
            0.02f,
            0.05f,
            0.50f,
            10);

    private final int maxLevel;
    private final int maxSelectedSkills;
    private final int xpBase;
    private final int xpMultiplier;
    private final int xpExponent;
    private final float prestigeXpBonus;
    private final float prestigePassiveBonus;
    private final float prestigeCooldownReductionPerLevel;
    private final float maxPrestigeCooldownReduction;
    private final int maxPrestigeLevel;

    public SkillProgressionConfig(
            int maxLevel,
            int maxSelectedSkills,
            int xpBase,
            int xpMultiplier,
            int xpExponent,
            float prestigeXpBonus,
            float prestigePassiveBonus,
            float prestigeCooldownReductionPerLevel,
            float maxPrestigeCooldownReduction,
            int maxPrestigeLevel) {
        this.maxLevel = maxLevel;
        this.maxSelectedSkills = maxSelectedSkills;
        this.xpBase = xpBase;
        this.xpMultiplier = xpMultiplier;
        this.xpExponent = xpExponent;
        this.prestigeXpBonus = prestigeXpBonus;
        this.prestigePassiveBonus = prestigePassiveBonus;
        this.prestigeCooldownReductionPerLevel = prestigeCooldownReductionPerLevel;
        this.maxPrestigeCooldownReduction = maxPrestigeCooldownReduction;
        this.maxPrestigeLevel = maxPrestigeLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getMaxSelectedSkills() {
        return Math.max(1, Math.min(maxSelectedSkills, SkillType.values().length));
    }

    public int getXpForLevel(int level) {
        return xpBase + (level * xpMultiplier) + (xpExponent * level * level);
    }

    public float getPrestigeXpBonus() {
        return prestigeXpBonus;
    }

    public float getPrestigePassiveBonus() {
        return prestigePassiveBonus;
    }

    public float getPrestigeCooldownReductionPerLevel() {
        return prestigeCooldownReductionPerLevel;
    }

    public float getMaxPrestigeCooldownReduction() {
        return maxPrestigeCooldownReduction;
    }

    public int getMaxPrestigeLevel() {
        return maxPrestigeLevel;
    }
}
