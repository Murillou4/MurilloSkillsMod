package com.murilloskills.mixin;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.MinecraftVersionCompat;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerMiningSpeedMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void murilloskills$applyServerMinerSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (MinecraftVersionCompat.getAttributeInstance(player, "block_break_speed") != null) {
            return;
        }

        try {
            PlayerSkillData data = ModAttachments.getOrCreate(player);
            if (!data.isSkillSelected(MurilloSkillsList.MINER) && !data.isParagonSkill(MurilloSkillsList.MINER)) {
                return;
            }

            PlayerSkillData.SkillStats stats = data.getSkill(MurilloSkillsList.MINER);
            float multiplier = MinerSkill.getMiningSpeedMultiplier(stats.level, stats.prestige);
            if (multiplier > 1.0f) {
                cir.setReturnValue(cir.getReturnValue() * multiplier);
            }
        } catch (Exception ignored) {
            // Keep vanilla mining behavior if skill data is unavailable during login or dimension changes.
        }
    }
}
