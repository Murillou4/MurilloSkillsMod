package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for PlayerEntity to track damage taken for daily challenges.
 * In MC 1.21, damage method signature is: damage(ServerWorld, DamageSource,
 * float)
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEventsMixin {

    /**
     * Track when player takes damage
     */
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamageTaken(ServerWorld world, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Only track if damage was actually taken
        if (cir.getReturnValue()) {
            ChallengeEventsHandler.onDamageTaken(serverPlayer, amount);
        }
    }
}
