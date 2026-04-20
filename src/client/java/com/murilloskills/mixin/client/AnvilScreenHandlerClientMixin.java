package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Client-side Blacksmith anvil accessor.
 *
 * The server mixin applies the discount and syncs the discounted {@code levelCost}
 * to the client, so this mixin no longer re-applies anything. The original
 * (pre-discount) value shown in the dual-cost label is reverse-computed at render
 * time from the Blacksmith discount formula, which sidesteps the sync-ordering
 * problems we used to hit when tracking it via {@code updateResult}.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerClientMixin implements BlacksmithCostAccessor {

    @Override
    public int murilloskills$getOriginalLevelCost() {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        int discountedCost = handler.getLevelCost();
        if (discountedCost <= 0) {
            return 0;
        }

        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return discountedCost;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BLACKSMITH);
        int level = stats != null ? stats.level : 0;
        if (level < SkillConfig.getBlacksmithEfficientAnvilLevel()) {
            return discountedCost;
        }

        float discount = SkillConfig.getBlacksmithAnvilDiscount(level);
        if (discount <= 0.0f || discount >= 1.0f) {
            return discountedCost;
        }

        int originalCost = Math.round(discountedCost / (1.0f - discount));
        return Math.max(discountedCost, originalCost);
    }

    @Override
    public int[] murilloskills$getOriginalEnchantmentPower() {
        return null;
    }
}
