package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to track when players shear sheep for daily challenges.
 * Targets SheepEntity.interactMob to detect when a player successfully shears a
 * sheep.
 */
@Mixin(SheepEntity.class)
public abstract class SheepShearMixin {

    /**
     * Track when a sheep is sheared by a player.
     */
    @Inject(method = "interactMob", at = @At("RETURN"))
    private void onShear(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Only track if the interaction was successful (SUCCESS or CONSUME)
        ActionResult result = cir.getReturnValue();
        if (result != ActionResult.SUCCESS && result != ActionResult.CONSUME) {
            return;
        }

        if (player.getEntityWorld().isClient()) {
            return;
        }

        // Check if player was holding shears
        var stack = player.getStackInHand(hand);
        if (stack.getItem() != net.minecraft.item.Items.SHEARS) {
            return;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            ChallengeEventsHandler.onSheepSheared(serverPlayer);
        }
    }
}
