package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to restore normal movement speed on Soul Sand for Explorer level 80+.
 * Part of the "Nether Walker" perk.
 * 
 * We inject into Entity.getVelocityMultiplier to override the slowdown
 * when an Explorer with Nether Walker is on Soul Sand.
 */
@Mixin(Entity.class)
public class SoulSandMixin {

    /**
     * Modify the velocity multiplier to return 1.0 (no slowdown) for Explorer
     * players
     * with the Nether Walker perk when on Soul Sand.
     */
    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void removeSlowdownForNetherWalker(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;

        // Only apply on server side for server players
        if (self.getEntityWorld().isClient()) {
            return;
        }

        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }

        // Check if the returned value indicates slowdown (less than 1.0)
        float currentMultiplier = cir.getReturnValue();
        if (currentMultiplier >= 1.0f) {
            return;
        }

        // Check if player is on Soul Sand
        BlockState blockState = self.getBlockStateAtPos();
        Block block = blockState.getBlock();

        if (block != Blocks.SOUL_SAND && block != Blocks.SOUL_SOIL) {
            return;
        }

        try {
            PlayerSkillData playerData = player
                    .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
                int level = playerData.getSkill(MurilloSkillsList.EXPLORER).level;

                if (ExplorerSkill.hasNetherWalker(level)) {
                    cir.setReturnValue(1.0f); // Normal speed, no slowdown
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
}
