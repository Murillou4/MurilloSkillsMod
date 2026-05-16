package com.murilloskills.core.data;

import com.murilloskills.core.config.SkillProgressionConfig;

public class SkillStatsCore {
    private int level;
    private double xp;
    private long lastAbilityUse;
    private int prestige;

    public SkillStatsCore() {
        this(0, 0.0, -1L, 0);
    }

    public SkillStatsCore(int level, double xp, long lastAbilityUse, int prestige) {
        this.level = level;
        this.xp = xp;
        this.lastAbilityUse = lastAbilityUse;
        this.prestige = prestige;
    }

    public XpAddResult addXp(int amount, int maxLevelAllowed, SkillProgressionConfig config) {
        int oldLevel = level;
        if (level >= maxLevelAllowed) {
            return XpAddResult.NO_CHANGE;
        }

        xp += amount;
        boolean leveledUp = false;
        while (xp >= config.getXpForLevel(level) && level < maxLevelAllowed) {
            xp -= config.getXpForLevel(level);
            level++;
            leveledUp = true;
        }

        if (level >= maxLevelAllowed) {
            xp = 0.0;
        }

        return new XpAddResult(leveledUp, oldLevel, level);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
    }

    public long getLastAbilityUse() {
        return lastAbilityUse;
    }

    public void setLastAbilityUse(long lastAbilityUse) {
        this.lastAbilityUse = lastAbilityUse;
    }

    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestige) {
        this.prestige = prestige;
    }
}
