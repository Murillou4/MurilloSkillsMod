package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for FishingBobberEntity to hook into fishing events.
 * - Applies fishing speed bonus
 * - Applies wait time reduction
 */
@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {

    @Shadow
    private int waitCountdown;

    @Shadow
    public abstract PlayerEntity getPlayerOwner();

    /**
     * Modifies the wait time for fishing based on Fisher skill level.
     * Level 10: -25% wait time
     * Rain Dance: Additional +50% speed
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void modifyWaitTime(CallbackInfo ci) {
        PlayerEntity player = getPlayerOwner();
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        try {
            SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
            var playerData = state.getPlayerData(serverPlayer);

            if (!playerData.isSkillSelected(MurilloSkillsList.FISHER)) {
                return;
            }

            int level = playerData.getSkill(MurilloSkillsList.FISHER).level;

            // Only modify if we have wait countdown active
            if (this.waitCountdown > 0) {
                // Calculate speed multiplier
                float speedBonus = FisherSkill.getFishingSpeedBonus(level);

                // Level 10: -25% wait time
                float waitMultiplier = FisherSkill.getWaitTimeMultiplier(level);

                // Rain Dance: Additional speed bonus
                if (FisherSkill.isRainDanceActive(serverPlayer)) {
                    speedBonus += SkillConfig.FISHER_RAIN_DANCE_SPEED_BONUS;
                }

                // Apply speed reduction (higher speed = lower wait time)
                // We reduce wait countdown faster by occasionally ticking twice
                float totalReduction = speedBonus + (1.0f - waitMultiplier);
                if (totalReduction > 0 && serverPlayer.getRandom().nextFloat() < totalReduction) {
                    this.waitCountdown--;
                }
            }
        } catch (Exception e) {
            // Silently ignore errors to not spam logs on tick
        }
    }
}
