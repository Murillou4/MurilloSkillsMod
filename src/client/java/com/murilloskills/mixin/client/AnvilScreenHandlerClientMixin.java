package com.murilloskills.mixin.client;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.mixin.ForgingScreenHandlerAccessor;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side Blacksmith anvil discount.
 *
 * The server-side mixin ({@link com.murilloskills.mixin.AnvilScreenHandlerMixin})
 * discounts the cost and syncs the Property, but in single-player (and even in
 * multiplayer on tick boundaries) the client's own {@code updateResult} runs
 * whenever slots sync and overwrites the Property with the vanilla value.
 * This mixin reapplies the same discount on the client using {@link ClientSkillData}
 * so the displayed level cost stays discounted regardless of sync ordering.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerClientMixin {

    @Shadow
    @Final
    private Property levelCost;

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void murilloskills$applyClientAnvilDiscount(CallbackInfo ci) {
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        PlayerEntity player = accessor.getPlayer();
        if (player == null || !player.getEntityWorld().isClient()) {
            return;
        }

        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BLACKSMITH);
        int level = stats != null ? stats.level : 0;
        if (level < SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL) {
            return;
        }

        int currentCost = this.levelCost.get();
        if (currentCost <= 0) {
            return;
        }

        float discount = SkillConfig.getBlacksmithAnvilDiscount(level);
        int discountedCost = Math.max(1, (int) (currentCost * (1.0f - discount)));
        if (discountedCost != currentCost) {
            this.levelCost.set(discountedCost);
        }
    }
}
