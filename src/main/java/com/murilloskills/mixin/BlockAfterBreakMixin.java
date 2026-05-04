package com.murilloskills.mixin;

import com.murilloskills.skills.MeltingTouchHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Hooks the result of {@link Block#getDroppedStacks(BlockState, ServerWorld, BlockPos, BlockEntity, Entity, ItemStack)}
 * to swap raw ore drops for their smelted counterparts when Melting Touch is active.
 *
 * Working at this level means vanilla still owns experience orbs, MINED stat tracking, and exhaustion —
 * we only replace the items the loot context produced, so behaviour stays consistent for non-meltable drops.
 */
@Mixin(Block.class)
public class BlockAfterBreakMixin {
    @Inject(
            method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true)
    private static void murilloskills$meltDrops(BlockState state, ServerWorld world, BlockPos pos,
            BlockEntity blockEntity, Entity entity, ItemStack tool,
            CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        List<ItemStack> melted = MeltingTouchHandler.tryMeltDrops(state.getBlock(), world, entity, tool, original);
        if (melted != null) {
            cir.setReturnValue(melted);
        }
    }
}
