package com.murilloskills.client.ui;

/**
 * Bridge between Blacksmith client-side mixins and the screens that
 * render the discounted cost. Implemented on screen handlers via mixin
 * so the screens can pull both the original (vanilla) cost and the
 * discounted one to render the dual-value label.
 */
public interface BlacksmithCostAccessor {
    int murilloskills$getOriginalLevelCost();

    int[] murilloskills$getOriginalEnchantmentPower();
}
