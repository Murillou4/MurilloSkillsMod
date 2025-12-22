package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.block.BlockState;
import net.minecraft.block.MagmaBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to cancel Magma Block damage for Explorer level 80+.
 * Part of the "Nether Walker" perk.
 */
@Mixin(MagmaBlock.class)
public class MagmaBlockMixin {

    @Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
    private void cancelMagmaDamageForExplorer(World world, BlockPos pos, BlockState state, Entity entity,
            CallbackInfo ci) {
        if (world.isClient())
            return;

        if (entity instanceof ServerPlayerEntity player) {
            PlayerSkillData playerData = player
                    .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Check if Explorer is selected
            if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER))
                return;

            int level = playerData.getSkill(MurilloSkillsList.EXPLORER).level;

            // Cancel damage if Nether Walker perk is unlocked
            if (ExplorerSkill.hasNetherWalker(level)) {
                ci.cancel();
            }
        }
    }
}
