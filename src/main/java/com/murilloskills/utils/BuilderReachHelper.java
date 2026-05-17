package com.murilloskills.utils;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BuilderReachHelper {
    private BuilderReachHelper() {
    }

    public static double getReachBonus(ServerPlayerEntity player) {
        if (player == null) {
            return 0.0D;
        }

        PlayerSkillData data = ModAttachments.getOrCreate(player);
        if (!data.isSkillSelected(MurilloSkillsList.BUILDER)) {
            return 0.0D;
        }

        PlayerSkillData.SkillStats stats = data.getSkill(MurilloSkillsList.BUILDER);
        if (stats.level <= 0) {
            return 0.0D;
        }

        return BuilderSkill.getReachBonus(stats.level, stats.prestige);
    }

    public static double extendRange(ServerPlayerEntity player, double vanillaRange) {
        double bonus = getReachBonus(player);
        return bonus > 0.0D ? vanillaRange + bonus : vanillaRange;
    }

    public static double extendSquaredRange(ServerPlayerEntity player, double vanillaSquaredRange) {
        double bonus = getReachBonus(player);
        if (bonus <= 0.0D || vanillaSquaredRange <= 0.0D) {
            return vanillaSquaredRange;
        }

        double vanillaRange = Math.sqrt(vanillaSquaredRange);
        double extendedRange = vanillaRange + bonus;
        return extendedRange * extendedRange;
    }
}
