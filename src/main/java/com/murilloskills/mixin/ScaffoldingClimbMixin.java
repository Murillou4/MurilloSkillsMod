package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to increase scaffolding climbing speed for Builder skill.
 * Level 50+: 50% faster climbing on scaffolding blocks.
 */
@Mixin(LivingEntity.class)
public abstract class ScaffoldingClimbMixin {

    @Inject(method = "travel", at = @At("TAIL"))
    private void onTravelEnd(Vec3d movementInput, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only process for server players
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Check if on or inside scaffolding
        BlockPos pos = player.getBlockPos();
        BlockState blockAtFeet = player.getEntityWorld().getBlockState(pos);
        BlockState blockBelow = player.getEntityWorld().getBlockState(pos.down());

        boolean onScaffolding = blockAtFeet.getBlock() == Blocks.SCAFFOLDING
                || blockBelow.getBlock() == Blocks.SCAFFOLDING;

        if (!onScaffolding) {
            return;
        }

        try {
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = state.getPlayerData(player);

            if (!playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
                return;
            }

            int level = playerData.getSkill(MurilloSkillsList.BUILDER).level;

            if (BuilderSkill.hasScaffoldSpeedBoost(level)) {
                Vec3d velocity = player.getVelocity();
                float boostMultiplier = 0.25f; // 50% boost for climbing up
                float descentBoostMultiplier = 2.0f; // 100% boost for going down

                // Check if player is sneaking (descending) or jumping (ascending)
                if (player.isSneaking() && velocity.y < 0) {
                    // Faster descent when sneaking
                    double extraY = velocity.y * descentBoostMultiplier;
                    player.setVelocity(velocity.x, velocity.y + extraY, velocity.z);
                    player.velocityModified = true;
                } else if (velocity.y > 0.001) {
                    // Faster ascent when jumping/climbing
                    double extraY = velocity.y * boostMultiplier;
                    player.setVelocity(velocity.x, velocity.y + extraY, velocity.z);
                    player.velocityModified = true;
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
}
