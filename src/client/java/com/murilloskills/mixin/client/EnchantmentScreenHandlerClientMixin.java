package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.screen.EnchantmentScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Client-side Blacksmith enchanting handler accessor.
 *
 * The discounted requirement is applied at render time in
 * {@link EnchantmentScreenMixin} (after the server has synced the vanilla
 * {@code enchantmentPower} values via {@code setProperty}). This class only
 * exposes storage for the pre-discount numbers so the overlay can show them
 * with a strikethrough.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerClientMixin implements BlacksmithCostAccessor {

    @Unique
    private final int[] murilloskills$originalEnchantmentPower = new int[3];

    @Override
    public int murilloskills$getOriginalLevelCost() {
        return 0;
    }

    @Override
    public int[] murilloskills$getOriginalEnchantmentPower() {
        return this.murilloskills$originalEnchantmentPower;
    }
}
