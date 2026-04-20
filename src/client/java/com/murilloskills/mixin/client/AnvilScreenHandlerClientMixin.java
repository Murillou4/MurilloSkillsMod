package com.murilloskills.mixin.client;

import com.murilloskills.accessor.AnvilCostSyncAccessor;
import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;

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

    @Override
    public int murilloskills$getOriginalLevelCost() {
        if ((Object) this instanceof AnvilCostSyncAccessor accessor) {
            int synced = accessor.murilloskills$getSyncedOriginalLevelCost();
            if (synced > 0) {
                return synced;
            }
        }
        return ((AnvilScreenHandler) (Object) this).getLevelCost();
    }

    @Override
    public int[] murilloskills$getOriginalEnchantmentPower() {
        return null;
    }
}
