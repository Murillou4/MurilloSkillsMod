package com.murilloskills.forge112.utils;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;

public final class Forge112SkillSynergyManager {
    private Forge112SkillSynergyManager() {
    }

    public static int applyXpBonus(PlayerSkillDataCore data, SkillType skill, int amount) {
        if (data == null || skill == null || amount <= 0) {
            return amount;
        }
        double multiplier = 1.0D;
        if (skill == SkillType.MINER && hasPair(data, SkillType.MINER, SkillType.BUILDER)) {
            multiplier += 0.05D;
        }
        if (skill == SkillType.BUILDER && hasPair(data, SkillType.MINER, SkillType.BUILDER)) {
            multiplier += 0.05D;
        }
        if (skill == SkillType.FARMER && hasPair(data, SkillType.FARMER, SkillType.FISHER)) {
            multiplier += 0.05D;
        }
        if (skill == SkillType.FISHER && hasPair(data, SkillType.FARMER, SkillType.FISHER)) {
            multiplier += 0.05D;
        }
        if (skill == SkillType.BLACKSMITH && hasPair(data, SkillType.BLACKSMITH, SkillType.WARRIOR)) {
            multiplier += 0.05D;
        }
        if (skill == SkillType.EXPLORER && hasPair(data, SkillType.EXPLORER, SkillType.ARCHER)) {
            multiplier += 0.05D;
        }
        return Math.max(1, (int) Math.round(amount * multiplier));
    }

    public static float outgoingDamageMultiplier(PlayerSkillDataCore data, boolean arrow) {
        if (data == null) {
            return 1.0F;
        }
        float multiplier = 1.0F;
        if (hasPair(data, SkillType.WARRIOR, SkillType.BLACKSMITH)) {
            multiplier += 0.05F;
        }
        if (arrow && hasPair(data, SkillType.ARCHER, SkillType.EXPLORER)) {
            multiplier += 0.05F;
        }
        return multiplier;
    }

    public static boolean hasPair(PlayerSkillDataCore data, SkillType first, SkillType second) {
        return data != null && data.isSkillSelected(first) && data.isSkillSelected(second);
    }
}
