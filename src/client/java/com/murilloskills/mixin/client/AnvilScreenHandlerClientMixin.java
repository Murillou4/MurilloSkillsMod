package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
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
        return 0;
    }

    @Override
    public int[] murilloskills$getOriginalEnchantmentPower() {
        return null;
    }
}
