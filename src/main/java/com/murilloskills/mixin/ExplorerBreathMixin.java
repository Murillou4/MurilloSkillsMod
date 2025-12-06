package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.skills.MurilloSkillsList;
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
 * holders.
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
     * After baseTick, if air was lost and player has Aquatic perk, restore half the
     * lost air.
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

        int currentAir = player.getAir();
        int previousAir = this.murilloskills$previousAir;

        // Only act if air was actually lost
        if (previousAir <= 0 || currentAir >= previousAir) {
            return;
        }

        try {
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = state.getPlayerData(player);

            // Check if Explorer is selected
            if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
                return;
            }

            int level = playerData.getSkill(MurilloSkillsList.EXPLORER).level;

            // If Aquatic perk is unlocked, restore half the lost air
            if (ExplorerSkill.hasAquatic(level)) {
                int airLost = previousAir - currentAir;
                // Every other tick, restore the lost air (effectively 50% slower air loss)
                if (player.age % 2 == 0) {
                    player.setAir(currentAir + airLost);
                }
            }
        } catch (Exception e) {
            // Silent fail - don't crash the game for perk logic
        }
    }
}
