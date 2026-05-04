package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
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

/**
 * Mixin for CropBlock to implement Fertile Ground perk scaling.
 * Crops grow faster for nearby Farmer players with level-scaled extra growth.
 */
@Mixin(CropBlock.class)
public class CropBlockMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-CropBlockMixin");

    /**
     * Injects at the end of randomTick to potentially add extra growth.
     * The bonus growth boost scales up to multiple extra growth stages.
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void accelerateCropGrowth(BlockState state, ServerWorld world, BlockPos pos, Random random,
            CallbackInfo ci) {
        // Find the nearest player who might have planted this crop
        // Using optimized spatial search instead of iterating all players
        ServerPlayerEntity nearestFarmer = murilloskills$findNearestFarmer(world, pos,
                SkillConfig.getFarmerFertileGroundRadius());

        if (nearestFarmer == null) {
            return;
        }

        PlayerSkillData playerData = nearestFarmer
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        // Check if player has Farmer as a selected skill
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

        CropBlock cropBlock = (CropBlock) (Object) this;
        BlockState currentState = world.getBlockState(pos);
        if (currentState.getBlock() != cropBlock || cropBlock.isMature(currentState)) {
            return;
        }

        int currentAge = cropBlock.getAge(currentState);
        int maxAge = cropBlock.getMaxAge();
        int newAge = Math.min(maxAge, currentAge + extraGrowth);

        if (newAge > currentAge) {
            BlockState grownState = cropBlock.withAge(newAge);
            world.setBlockState(pos, grownState, 2);

            // Spawn green particles to show the boost visually
            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    Math.min(8, 2 + extraGrowth * 2),
                    0.3, 0.2, 0.3,
                    0.0
            );
        }
    }

    /**
     * Finds the nearest player with Farmer skill in the given radius.
     * Uses Minecraft's optimized spatial partitioning for O(log n) performance
     * instead of O(n) iteration over all players.
     */
    @Unique
    private ServerPlayerEntity murilloskills$findNearestFarmer(ServerWorld world, BlockPos pos, int radius) {
        // Use Minecraft's optimized getClosestPlayer with spatial partitioning
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
