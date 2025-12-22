package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
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
 * Mixin for CropBlock to implement Fertile Ground perk (Level 25).
 * Crops grow 25% faster for players with the Farmer skill at level 25+.
 */
@Mixin(CropBlock.class)
public class CropBlockMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-CropBlockMixin");

    /**
     * Injects at the start of randomTick to potentially add extra growth.
     * For players with Fertile Ground perk, there's a 25% chance to grow again.
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void accelerateCropGrowth(BlockState state, ServerWorld world, BlockPos pos, Random random,
            CallbackInfo ci) {
        // Find the nearest player who might have planted this crop
        // Using optimized spatial search instead of iterating all players
        ServerPlayerEntity nearestFarmer = murilloskills$findNearestFarmer(world, pos,
                SkillConfig.FARMER_FERTILE_GROUND_RADIUS);

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

        // Fertile Ground perk (Level 25+): 25% chance for bonus growth
        if (farmerLevel >= SkillConfig.FARMER_FERTILE_GROUND_LEVEL) {
            if (random.nextFloat() < SkillConfig.FARMER_FERTILE_GROUND_SPEED) {
                CropBlock cropBlock = (CropBlock) (Object) this;

                // Only grow if not already mature
                if (!cropBlock.isMature(state)) {
                    int currentAge = cropBlock.getAge(state);
                    int maxAge = cropBlock.getMaxAge();

                    if (currentAge < maxAge) {
                        BlockState grownState = cropBlock.withAge(currentAge + 1);
                        world.setBlockState(pos, grownState, 2);
                    }
                }
            }
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
