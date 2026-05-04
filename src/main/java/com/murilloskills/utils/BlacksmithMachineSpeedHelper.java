package com.murilloskills.utils;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public final class BlacksmithMachineSpeedHelper {
    private static final double REBORN_CORE_SPEED_CAP = 0.99D;

    private BlacksmithMachineSpeedHelper() {
    }

    public static int getBestNearbyBlacksmithLevel(ServerWorld world, BlockPos pos) {
        int radius = SkillConfig.getBlacksmithFurnaceSpeedRadius();
        Box searchBox = new Box(pos).expand(radius);
        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                ServerPlayerEntity.class, searchBox, player -> true);

        int bestLevel = 0;
        for (ServerPlayerEntity player : nearbyPlayers) {
            PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            if (data.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
                int level = data.getSkill(MurilloSkillsList.BLACKSMITH).level;
                bestLevel = Math.max(bestLevel, level);
            }
        }

        return bestLevel;
    }

    public static float getDirectSpeedMultiplier(int level) {
        if (level <= 0) {
            return 1.0f;
        }

        float normalizedLevel = Math.min(1.0f, level / (float) SkillConfig.getMaxLevel());
        float effectiveMaxMultiplier = SkillConfig.getBlacksmithFurnaceSpeedEffectiveMaxMultiplier();
        float maxExtraProgress = Math.max(0.0f, effectiveMaxMultiplier - 1.0f);
        return 1.0f + normalizedLevel * maxExtraProgress;
    }

    public static int getExtraProgressTicks(ServerWorld world, int level) {
        float extraFloat = getDirectSpeedMultiplier(level) - 1.0f;
        if (extraFloat <= 0.0f) {
            return 0;
        }

        int extraTicks = (int) extraFloat;
        float fractional = extraFloat - extraTicks;
        if (fractional > 0.0f && world.random.nextFloat() < fractional) {
            extraTicks++;
        }

        return extraTicks;
    }

    public static double getRebornCoreSpeedBonus(int level) {
        float directMultiplier = getDirectSpeedMultiplier(level);
        if (directMultiplier <= 1.0f) {
            return 0.0D;
        }

        double speedReduction = 1.0D - (1.0D / directMultiplier);
        return Math.min(REBORN_CORE_SPEED_CAP, Math.max(0.0D, speedReduction));
    }

    public static void spawnSpeedParticles(ServerWorld world, BlockPos pos) {
        if (world.getTime() % 10 != 0) {
            return;
        }

        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                2,
                0.2, 0.1, 0.2,
                0.01);
    }
}
