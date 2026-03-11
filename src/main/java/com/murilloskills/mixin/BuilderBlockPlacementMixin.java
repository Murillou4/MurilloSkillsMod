package com.murilloskills.mixin;

import com.murilloskills.events.BlockPlacementHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires only after BlockItem placement succeeds, preventing Builder XP exploits on failed clicks.
 */
@Mixin(BlockItem.class)
public abstract class BuilderBlockPlacementMixin {

    @Inject(method = "postPlacement", at = @At("RETURN"))
    private void murilloskills$onSuccessfulPlacement(BlockPos pos, World world, PlayerEntity placer,
            ItemStack itemStack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || world.isClient() || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!(placer instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        BlockPlacementHandler.onBlockPlaced(serverPlayer, serverWorld, pos.toImmutable(), state.getBlock());
    }
}
