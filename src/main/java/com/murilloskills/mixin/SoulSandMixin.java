package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to restore normal movement speed on Soul Sand for Explorer level 80+.
 * Part of the "Nether Walker" perk.
 * 
 * Note: Soul Sand slows entities via the SoulSpeedEnchantment check in the
 * entity movement code.
 * This mixin prevents the slowdown by setting the entity's velocity
 * multiplication back to normal.
 */
@Mixin(SoulSandBlock.class)
public class SoulSandMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void preventSlowdownForExplorer(BlockState state, World world, BlockPos pos, Entity entity,
            CallbackInfo ci) {
        if (world.isClient())
            return;

        if (entity instanceof ServerPlayerEntity player) {
            SkillGlobalState globalState = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = globalState.getPlayerData(player);

            // Check if Explorer is selected
            if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER))
                return;

            int level = playerData.getSkill(MurilloSkillsList.EXPLORER).level;

            // Cancel the collision handling (which applies slowdown) if Nether Walker perk
            // is unlocked
            if (ExplorerSkill.hasNetherWalker(level)) {
                ci.cancel();
            }
        }
    }
}
