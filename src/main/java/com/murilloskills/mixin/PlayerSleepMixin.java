package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track when players sleep through the night for daily challenges.
 * Targets ServerWorld.tick to detect when the night is skipped due to sleeping
 * players.
 */
@Mixin(ServerWorld.class)
public abstract class PlayerSleepMixin {

    /**
     * Track when players successfully sleep through the night.
     * We inject after the sleep wake up to track completion.
     */
    @Inject(method = "wakeSleepingPlayers", at = @At("HEAD"))
    private void onWakeSleepingPlayers(CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;

        // Only count if it's transitioning from night to day (actual sleep, not just
        // waking up)
        // The wakeSleepingPlayers is called when the night is successfully skipped
        for (var player : world.getPlayers()) {
            if (player.isSleeping()) {
                ChallengeEventsHandler.onSleepComplete(player);
            }
        }
    }
}
