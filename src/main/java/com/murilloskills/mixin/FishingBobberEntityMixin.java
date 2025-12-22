package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
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
     * Passive: +0.5% fishing speed per level (max 50% at level 100)
     * Level 10: -25% wait time
     * Rain Dance: Additional +50% speed
     * 
     * This method reduces wait time by applying extra countdown decrements each
     * tick
     * based on the total speed bonus. For example, with 50% bonus, waitCountdown
     * is reduced by 1 extra every other tick on average.
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
            PlayerSkillData playerData = serverPlayer
                    .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (!playerData.isSkillSelected(MurilloSkillsList.FISHER)) {
                return;
            }

            int level = playerData.getSkill(MurilloSkillsList.FISHER).level;
            int prestige = playerData.getSkill(MurilloSkillsList.FISHER).prestige;

            // Only modify if we have wait countdown active
            if (this.waitCountdown > 0) {
                // Calculate speed multiplier from passive bonus (0.5% per level, with prestige
                // bonus)
                float speedBonus = FisherSkill.getFishingSpeedBonus(level, prestige);

                // Level 10 perk: -25% wait time
                float waitReduction = 1.0f - FisherSkill.getWaitTimeMultiplier(level);

                // Rain Dance: Additional +50% speed bonus
                if (FisherSkill.isRainDanceActive(serverPlayer)) {
                    speedBonus += SkillConfig.FISHER_RAIN_DANCE_SPEED_BONUS;
                }

                // Total reduction multiplier (e.g., 0.50 speedBonus + 0.25 waitReduction = 0.75
                // total)
                float totalReduction = speedBonus + waitReduction;

                // Apply reduction in two parts:

                // 1. Guaranteed extra decrements (integer part)
                int guaranteedExtra = (int) totalReduction;
                this.waitCountdown -= guaranteedExtra;

                // 2. Probabilistic extra decrement (fractional part)
                float fractionalPart = totalReduction - guaranteedExtra;
                if (fractionalPart > 0 && serverPlayer.getRandom().nextFloat() < fractionalPart) {
                    this.waitCountdown--;
                }

                // Ensure waitCountdown doesn't go negative
                if (this.waitCountdown < 0) {
                    this.waitCountdown = 0;
                }
            }
        } catch (Exception e) {
            // Silently ignore errors to not spam logs on tick
        }
    }
}
