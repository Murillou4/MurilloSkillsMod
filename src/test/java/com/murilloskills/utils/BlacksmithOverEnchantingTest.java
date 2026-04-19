package com.murilloskills.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlacksmithOverEnchantingTest {

    @Test
    void unlocksAtLevelNinetyNine() {
        assertFalse(BlacksmithOverEnchanting.isUnlocked(98));
        assertTrue(BlacksmithOverEnchanting.isUnlocked(99));
    }

    @Test
    void equalVanillaCapLevelsIncreaseByOne() {
        assertTrue(BlacksmithOverEnchanting.shouldOverEnchant(3, 3, 3));
        assertEquals(4, BlacksmithOverEnchanting.getOverEnchantResultLevel(3, 3, 3));
    }

    @Test
    void mixedLevelsDoNotJumpPastHighestLevel() {
        assertFalse(BlacksmithOverEnchanting.shouldOverEnchant(4, 5, 3));
        assertEquals(5, BlacksmithOverEnchanting.getOverEnchantResultLevel(4, 5, 3));
    }

    @Test
    void capStopsAtEight() {
        assertFalse(BlacksmithOverEnchanting.shouldOverEnchant(8, 8, 3));
        assertEquals(8, BlacksmithOverEnchanting.getOverEnchantResultLevel(8, 8, 3));
    }

    @Test
    void extraCostScalesWithOverEnchantDepth() {
        assertEquals(6, BlacksmithOverEnchanting.getExtraAnvilCost(3, 4));
        assertEquals(16, BlacksmithOverEnchanting.getExtraAnvilCost(3, 5));
        assertEquals(30, BlacksmithOverEnchanting.getExtraAnvilCost(3, 6));
    }
}
