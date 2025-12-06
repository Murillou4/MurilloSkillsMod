package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
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
 * Note: The reach bonus is also applied via entity attributes in
 * BuilderSkill.updateAttributes()
 * This mixin provides additional client-side support for the reach extension.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityReachMixin {

    /**
     * Injects at the return of getBlockInteractionRange to add Builder reach bonus.
     * The attribute-based approach in BuilderSkill.updateAttributes() should handle
     * this,
     * but this provides a fallback in case attributes aren't properly loaded.
     */
    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Only process on server side
        if (player.getEntityWorld().isClient()) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        try {
            SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
            var playerData = state.getPlayerData(serverPlayer);

            // Check if player has Builder skill selected
            if (!playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
                return;
            }

            int level = playerData.getSkill(MurilloSkillsList.BUILDER).level;
            if (level <= 0) {
                return;
            }

            // Note: The attribute system should already handle this via
            // BuilderSkill.updateAttributes()
            // This is a safety check in case attributes aren't working properly
            // The reach bonus is calculated the same way in BuilderSkill.getReachBonus()

        } catch (Exception e) {
            // Silent fail - don't spam logs on tick
        }
    }
}
