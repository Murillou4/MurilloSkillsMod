package com.murilloskills.data;

/** Unlocks follow highestLevel; numerical bonuses follow level in each prestige. */
public final class SkillStats {
    public int level, highestLevel, prestige, branch;
    public double xp;
    public int claimedMilestone;
    public long cooldownUntil;
    public int unlockLevel() { return Math.max(level, highestLevel); }
    public int activeTier() { return unlockLevel() / 20; }
    public int branchTier() { return branch == 0 ? 0 : unlockLevel() >= 75 ? 3 : unlockLevel() >= 50 ? 2 : unlockLevel() >= 25 ? 1 : 0; }
}

