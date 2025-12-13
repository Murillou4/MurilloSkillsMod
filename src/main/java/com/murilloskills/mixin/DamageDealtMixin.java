package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to track damage dealt by players for daily challenges.
 * Targets LivingEntity.damage to detect when entities take damage from player
 * attacks.
 */
@Mixin(LivingEntity.class)
public abstract class DamageDealtMixin {

    /**
     * Track when an entity takes damage from a player.
     * This is called when any LivingEntity receives damage.
     */
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamageDealt(ServerWorld world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        // Only process if damage was actually dealt
        if (!cir.getReturnValue()) {
            return;
        }

        // Check if damage was dealt by a player
        if (source.getAttacker() instanceof PlayerEntity player) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ChallengeEventsHandler.onDamageDealt(serverPlayer, amount);
            }
        }
    }
}
