package com.murilloskills.mixin.client;

import com.murilloskills.accessor.AnvilCostSyncAccessor;
import com.murilloskills.client.ui.BlacksmithCostAccessor;
import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithOverEnchanting;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side Blacksmith anvil accessor.
 *
 * The server mixin applies the discount and syncs the discounted {@code levelCost}
 * to the client, so this mixin no longer re-applies anything. The original
 * (pre-discount) value shown in the dual-cost label is read from the synced
 * server-side property, avoiding client-side guesswork.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerClientMixin implements BlacksmithCostAccessor {

    @Unique
    private static final int MURILLOSKILLS_BLACKSMITH_MASTERY_MAX_ANVIL_COST = 25;

    @Shadow
    @Final
    private Property levelCost;

    @Unique
    private int murilloskills$fallbackOriginalLevelCost;

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void murilloskills$syncDiscountAfterClientRecalc(CallbackInfo ci) {
        this.murilloskills$applyClientDiscountFallback();
    }

    @Override
    public int murilloskills$getOriginalLevelCost() {
        if ((Object) this instanceof AnvilCostSyncAccessor accessor) {
            int synced = accessor.murilloskills$getSyncedOriginalLevelCost();
            if (synced > 0) {
                return synced;
            }
        }
        if (this.murilloskills$fallbackOriginalLevelCost > 0) {
            return this.murilloskills$fallbackOriginalLevelCost;
        }
        return ((AnvilScreenHandler) (Object) this).getLevelCost();
    }

    @Override
    public int[] murilloskills$getOriginalEnchantmentPower() {
        return null;
    }

    @Unique
    private void murilloskills$applyClientDiscountFallback() {
        int currentCost = this.levelCost.get();
        if (currentCost <= 0 || currentCost >= 40) {
            return;
        }

        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }
        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BLACKSMITH);
        int level = stats != null ? stats.level : 0;
        if (level < SkillConfig.getBlacksmithEfficientAnvilLevel()) {
            return;
        }

        int originalCost = this.murilloskills$getOriginalLevelCost();
        if (originalCost <= 0) {
            return;
        }

        // Only correct the client when it drifted back to the vanilla value.
        if (currentCost != originalCost) {
            return;
        }

        this.murilloskills$fallbackOriginalLevelCost = Math.max(this.murilloskills$fallbackOriginalLevelCost, originalCost);

        int discounted = Math.max(1,
                Math.round(originalCost * (1.0f - SkillConfig.getBlacksmithAnvilDiscount(level))));
        if (BlacksmithOverEnchanting.isUnlocked(level)) {
            discounted = Math.min(discounted, MURILLOSKILLS_BLACKSMITH_MASTERY_MAX_ANVIL_COST);
        }

        if (discounted < currentCost) {
            this.levelCost.set(discounted);
        }
    }
}
