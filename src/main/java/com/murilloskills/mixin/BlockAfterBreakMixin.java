package com.murilloskills.mixin;

import com.murilloskills.skills.MeltingTouchHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockAfterBreakMixin {
    @Inject(method = "afterBreak", at = @At("HEAD"), cancellable = true)
    private void murilloskills$dropMeltedOre(World world, PlayerEntity player, BlockPos pos, BlockState state,
            BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (MeltingTouchHandler.tryDropMelted((Block) (Object) this, world, player, pos, state, blockEntity, tool)) {
            ci.cancel();
        }
    }
}
