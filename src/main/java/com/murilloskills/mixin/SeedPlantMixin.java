package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to track when players plant seeds for daily challenges.
 * This covers vanilla planting (when area planting is not active).
 * Targets BlockItem.postPlacement which is called after a block is successfully
 * placed.
 */
@Mixin(BlockItem.class)
public abstract class SeedPlantMixin {

    /**
     * Track when a crop block is placed (seed planted).
     * Note: In MC 1.21+ the placer parameter changed from LivingEntity to
     * PlayerEntity
     */
    @Inject(method = "postPlacement", at = @At("HEAD"))
    private void onPostPlacement(BlockPos pos, World world, PlayerEntity placer, ItemStack itemStack,
            BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) {
            return;
        }

        if (placer == null) {
            return;
        }

        if (!(placer instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Check if the placed block is a crop
        if (state.getBlock() instanceof CropBlock) {
            ChallengeEventsHandler.onSeedsPlanted(serverPlayer);
        }
    }
}
