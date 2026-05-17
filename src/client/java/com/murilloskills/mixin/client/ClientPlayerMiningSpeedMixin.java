package com.murilloskills.mixin.client;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.MinecraftVersionCompat;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ClientPlayerMiningSpeedMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void murilloskills$applyClientMinerSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player != self) {
            return;
        }
        if (MinecraftVersionCompat.getAttributeInstance(self, "block_break_speed") != null) {
            return;
        }

        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.MINER)
                && !ClientSkillData.isParagonSkill(MurilloSkillsList.MINER)) {
            return;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.MINER);
        float multiplier = MinerSkill.getMiningSpeedMultiplier(stats.level, stats.prestige);
        if (multiplier > 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }
}
