package com.murilloskills.mixin;

import com.murilloskills.impl.ExplorerSkill;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to extend breath time underwater for Explorer level 20+.
 * Part of the "Aquatic" perk.
 * 
 * Uses @Inject at HEAD to track air and at TAIL to restore air for Aquatic perk
 * holders. Business logic is delegated to ExplorerSkill.handleWaterBreathing().
 */
@Mixin(LivingEntity.class)
public abstract class ExplorerBreathMixin {

    @Unique
    private int murilloskills$previousAir = -1;

    /**
     * Store the current air value before baseTick processes.
     */
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void storeAirBeforeTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        this.murilloskills$previousAir = self.getAir();
    }

    /**
     * After baseTick, delegate to ExplorerSkill for Aquatic perk logic.
     * This effectively gives 50% longer breath time underwater.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void restoreAirForExplorer(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }

        if (player.getEntityWorld().isClient()) {
            return;
        }

        // Delegate all business logic to ExplorerSkill
        ExplorerSkill.handleWaterBreathing(player, this.murilloskills$previousAir);
    }
}
