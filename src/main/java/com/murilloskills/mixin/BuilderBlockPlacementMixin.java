package com.murilloskills.mixin;

import com.murilloskills.events.BlockPlacementHandler;
import com.murilloskills.skills.UltPlacePlanner;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fires only after BlockItem placement succeeds, preventing Builder XP exploits on failed clicks.
 */
@Mixin(BlockItem.class)
public abstract class BuilderBlockPlacementMixin {
    @Unique
    private static final ThreadLocal<PlacementCapture> MURILLOSKILLS$PLACEMENT_CAPTURE = new ThreadLocal<>();

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"))
    private void murilloskills$capturePlacementContext(ItemPlacementContext context,
            CallbackInfoReturnable<ActionResult> cir) {
        if (context == null || context.getPlayer() == null) {
            MURILLOSKILLS$PLACEMENT_CAPTURE.remove();
            return;
        }

        ItemStack capturedStack = context.getStack().copy();
        if (!capturedStack.isEmpty()) {
            capturedStack.setCount(1);
        }

        BlockItem self = (BlockItem) (Object) this;
        Map<BlockPos, BlockState> previousStates = new LinkedHashMap<>();
        UltPlacePlanner.PlannedPlacement primaryPlacement = UltPlacePlanner.predictPrimaryPlacement(self, context);
        if (primaryPlacement != null) {
            for (UltPlacePlanner.PreviewBlock previewBlock : primaryPlacement.footprint()) {
                previousStates.put(previewBlock.pos().toImmutable(),
                        context.getWorld().getBlockState(previewBlock.pos()));
            }
        } else {
            previousStates.put(context.getBlockPos().toImmutable(),
                    context.getWorld().getBlockState(context.getBlockPos()));
        }

        MURILLOSKILLS$PLACEMENT_CAPTURE.set(new PlacementCapture(
                context.getSide(),
                context.getHand(),
                capturedStack,
                context.getHitPos(),
                previousStates));
    }

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void murilloskills$clearFailedPlacementCapture(ItemPlacementContext context,
            CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue() == null || !cir.getReturnValue().isAccepted()) {
            MURILLOSKILLS$PLACEMENT_CAPTURE.remove();
        }
    }

    @Inject(method = "postPlacement", at = @At("RETURN"))
    private void murilloskills$onSuccessfulPlacement(BlockPos pos, World world, PlayerEntity placer,
            ItemStack itemStack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        PlacementCapture capture = MURILLOSKILLS$PLACEMENT_CAPTURE.get();
        try {
            if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (!(placer instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            Direction face = capture != null && capture.face() != null ? capture.face() : Direction.UP;
            Hand hand = capture != null && capture.hand() != null ? capture.hand() : Hand.MAIN_HAND;
            ItemStack sourceStack = capture != null && !capture.stack().isEmpty() ? capture.stack() : itemStack.copy();
            if (!sourceStack.isEmpty()) {
                sourceStack.setCount(1);
            }
            Vec3d hitPos = capture != null ? capture.hitPos() : pos.toCenterPos();
            Map<BlockPos, BlockState> previousStates = capture != null
                    ? capture.previousStates()
                    : Map.of(pos.toImmutable(), world.getBlockState(pos));

            BlockPlacementHandler.onBlockPlaced(serverPlayer, serverWorld, pos.toImmutable(), state.getBlock(),
                    face, hand, sourceStack, hitPos, previousStates);
        } finally {
            MURILLOSKILLS$PLACEMENT_CAPTURE.remove();
        }
    }

    @Unique
    private record PlacementCapture(Direction face, Hand hand, ItemStack stack, Vec3d hitPos,
            Map<BlockPos, BlockState> previousStates) {
    }
}
