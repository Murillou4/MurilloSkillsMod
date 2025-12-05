package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for CropBlock to implement Fertile Ground perk (Level 25).
 * Crops grow 25% faster for players with the Farmer skill at level 25+.
 */
@Mixin(CropBlock.class)
public class CropBlockMixin {

    /**
     * Injects at the start of randomTick to potentially add extra growth.
     * For players with Fertile Ground perk, there's a 25% chance to grow again.
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void accelerateCropGrowth(BlockState state, ServerWorld world, BlockPos pos, Random random,
            CallbackInfo ci) {
        // Find the nearest player who might have planted this crop
        // (Using a radius check since we can't track who planted each crop)
        ServerPlayerEntity nearestFarmer = findNearestFarmer(world, pos, 32);

        if (nearestFarmer == null) {
            return;
        }

        SkillGlobalState skillState = SkillGlobalState.getServerState(world.getServer());
        var playerData = skillState.getPlayerData(nearestFarmer);

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
     */
    private ServerPlayerEntity findNearestFarmer(ServerWorld world, BlockPos pos, int radius) {
        ServerPlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (ServerPlayerEntity player : world.getPlayers()) {
            double dist = player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            if (dist < radius * radius && dist < nearestDist) {
                SkillGlobalState state = SkillGlobalState.getServerState(world.getServer());
                if (state.getPlayerData(player).isSkillSelected(MurilloSkillsList.FARMER)) {
                    nearest = player;
                    nearestDist = dist;
                }
            }
        }

        return nearest;
    }
}
