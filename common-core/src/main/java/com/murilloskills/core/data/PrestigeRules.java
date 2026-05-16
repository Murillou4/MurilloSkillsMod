package com.murilloskills.core.data;

import com.murilloskills.core.config.SkillProgressionConfig;
import com.murilloskills.core.config.SkillType;

public final class PrestigeRules {
    private PrestigeRules() {
    }

    public static boolean canPrestige(PlayerSkillDataCore data, SkillType skill, SkillProgressionConfig config) {
        SkillStatsCore stats = data.getSkill(skill);
        return data.isParagonSkill(skill)
                && stats.getLevel() >= config.getMaxLevel()
                && stats.getPrestige() < config.getMaxPrestigeLevel();
    }

    public static float getXpMultiplier(int prestigeLevel, SkillProgressionConfig config) {
        return 1.0f + (prestigeLevel * config.getPrestigeXpBonus());
    }

    public static float getPassiveMultiplier(int prestigeLevel, SkillProgressionConfig config) {
        return 1.0f + (prestigeLevel * config.getPrestigePassiveBonus());
    }

    public static float getCooldownReduction(int prestigeLevel, SkillProgressionConfig config) {
        float reduction = prestigeLevel * config.getPrestigeCooldownReductionPerLevel();
        return Math.min(reduction, config.getMaxPrestigeCooldownReduction());
    }
}
