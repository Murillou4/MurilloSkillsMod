package com.murilloskills.mixin.client;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class BuilderPlayerReachClientMixin {
    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void murilloskills$onGetClientBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        murilloskills$applyClientBuilderReach(cir);
    }

    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void murilloskills$onGetClientEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        murilloskills$applyClientBuilderReach(cir);
    }

    private void murilloskills$applyClientBuilderReach(CallbackInfoReturnable<Double> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (MinecraftClient.getInstance().player != player
                || !ClientSkillData.isSkillSelected(MurilloSkillsList.BUILDER)) {
            return;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BUILDER);
        double bonus = BuilderSkill.getReachBonus(stats.level, stats.prestige);
        if (bonus > 0.0D) {
            cir.setReturnValue(cir.getReturnValueD() + bonus);
        }
    }
}
