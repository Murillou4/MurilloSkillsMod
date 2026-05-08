package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.FarmerTreeTracker;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Applies Farmer fertile growth to saplings and marks generated logs for one
 * Farmer double-harvest roll.
 */
@Mixin(SaplingBlock.class)
public class SaplingBlockMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-SaplingBlockMixin");

    @Unique
    private static final ThreadLocal<Set<BlockPos>> MURILLOSKILLS$PRE_GENERATE_LOGS = new ThreadLocal<>();

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void murilloskills$accelerateSaplingGrowth(BlockState state, ServerWorld world, BlockPos pos,
            Random random, CallbackInfo ci) {
        if (world.getLightLevel(pos.up()) < 9) {
            return;
        }

        ServerPlayerEntity nearestFarmer = murilloskills$findNearestFarmer(world, pos,
                SkillConfig.getFarmerFertileGroundRadius());
        if (nearestFarmer == null) {
            return;
        }

        PlayerSkillData playerData = nearestFarmer
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        if (!playerData.isSkillSelected(MurilloSkillsList.FARMER)) {
            return;
        }

        int farmerLevel = playerData.getSkill(MurilloSkillsList.FARMER).level;
        float growthBoost = FarmerSkill.getFertileGroundGrowthBoost(farmerLevel);
        if (growthBoost <= 0.0f) {
            return;
        }

        int extraGrowth = (int) growthBoost;
        float fractionalGrowth = growthBoost - extraGrowth;
        if (fractionalGrowth > 0.0f && random.nextFloat() < fractionalGrowth) {
            extraGrowth++;
        }
        if (extraGrowth <= 0) {
            return;
        }

        SaplingBlock saplingBlock = (SaplingBlock) (Object) this;
        for (int i = 0; i < extraGrowth; i++) {
            BlockState currentState = world.getBlockState(pos);
            if (currentState.getBlock() != saplingBlock) {
                break;
            }
            saplingBlock.generate(world, pos, currentState, random);
        }
    }

    @Inject(method = "generate", at = @At("HEAD"))
    private void murilloskills$captureLogsBeforeGenerate(ServerWorld world, BlockPos pos, BlockState state,
            Random random, CallbackInfo ci) {
        MURILLOSKILLS$PRE_GENERATE_LOGS.set(FarmerTreeTracker.collectNearbyLogs(world, pos));
    }

    @Inject(method = "generate", at = @At("RETURN"))
    private void murilloskills$markGeneratedLogs(ServerWorld world, BlockPos pos, BlockState state,
            Random random, CallbackInfo ci) {
        try {
            FarmerTreeTracker.markNewGeneratedLogs(world, pos, MURILLOSKILLS$PRE_GENERATE_LOGS.get());
        } finally {
            MURILLOSKILLS$PRE_GENERATE_LOGS.remove();
        }
    }

    @Unique
    private ServerPlayerEntity murilloskills$findNearestFarmer(ServerWorld world, BlockPos pos, int radius) {
        PlayerEntity closest = world.getClosestPlayer(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                radius,
                player -> {
                    if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                        return false;
                    }
                    try {
                        return serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS)
                                .isSkillSelected(MurilloSkillsList.FARMER);
                    } catch (Exception e) {
                        LOGGER.debug("Error checking farmer skill for {}: {}", player.getName().getString(),
                                e.getMessage());
                        return false;
                    }
                });

        return closest instanceof ServerPlayerEntity sp ? sp : null;
    }
}
