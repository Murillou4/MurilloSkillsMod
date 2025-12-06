package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to extend breath time underwater for Explorer level 20+.
 * Part of the "Aquatic" perk.
 * 
 * Uses ModifyVariable on the air decrement logic instead of getMaxAir.
 */
@Mixin(LivingEntity.class)
public abstract class ExplorerBreathMixin {

    /**
     * Modify the air consumption when underwater.
     * This intercepts the air value before it's decremented.
     * We double the air when the entity would normally lose 1, giving 50% longer breath.
     */
    @ModifyVariable(method = "baseTick", at = @At("STORE"), ordinal = 0)
    private int modifyAirForExplorer(int air) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayerEntity player)) {
            return air;
        }
        
        if (player.getEntityWorld().isClient()) {
            return air;
        }

        try {
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = state.getPlayerData(player);

            // Check if Explorer is selected
            if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
                return air;
            }

            int level = playerData.getSkill(MurilloSkillsList.EXPLORER).level;

            // If Aquatic perk is unlocked and player is losing air, slow down air consumption
            if (ExplorerSkill.hasAquatic(level) && air < player.getAir()) {
                // Skip every other air decrement (effectively 50% slower air loss)
                if (player.age % 2 == 0) {
                    return player.getAir(); // Return current air to cancel this decrement
                }
            }
        } catch (Exception e) {
            // Silent fail
        }

        return air;
    }
}
