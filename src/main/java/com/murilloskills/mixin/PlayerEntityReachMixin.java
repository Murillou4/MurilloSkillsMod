package com.murilloskills.mixin;

import com.murilloskills.utils.BuilderReachHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend block interaction range for Builder skill.
 * 
 * Perks applied:
 * - Base: +0.05 blocks per level (max +5 at level 100)
 * - Level 10: +1 block reach
 * - Level 75: +5 blocks reach (cumulative)
 * 
 * Server-side reach path for modern versions where vanilla exposes interaction
 * range through PlayerEntity.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityReachMixin {

    /**
     * Injects at the return of getBlockInteractionRange to add Builder reach bonus.
     */
    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void murilloskills$onGetServerBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        murilloskills$applyServerBuilderReach(cir);
    }

    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void murilloskills$onGetServerEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        murilloskills$applyServerBuilderReach(cir);
    }

    private void murilloskills$applyServerBuilderReach(CallbackInfoReturnable<Double> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        double bonus = BuilderReachHelper.getReachBonus(serverPlayer);
        if (bonus > 0.0D) {
            cir.setReturnValue(cir.getReturnValueD() + bonus);
        }
    }
}
