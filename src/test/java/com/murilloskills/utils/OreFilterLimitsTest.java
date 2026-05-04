package com.murilloskills.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OreFilterLimitsTest {
    @Test
    void maxOreLimitClampsToConfiguredRange() {
        assertEquals(500, OreFilterLimits.clampMaxOres(999));
        assertEquals(5, OreFilterLimits.clampMaxOres(1));
        assertEquals(250, OreFilterLimits.clampMaxOres(250));
    }
}
