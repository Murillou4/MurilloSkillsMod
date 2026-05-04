package com.murilloskills.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinerXpGetterTest {
    @Test
    void likelyOreIdsIncludeModdedOreNamingConventions() {
        assertTrue(MinerXpGetter.isLikelyOreId("techreborn:bauxite_ore"));
        assertTrue(MinerXpGetter.isLikelyOreId("advanced_reborn:deepslate_iridium_ore"));
        assertTrue(MinerXpGetter.isLikelyOreId("minecraft:ancient_debris"));

        assertFalse(MinerXpGetter.isLikelyOreId("minecraft:stone"));
        assertFalse(MinerXpGetter.isLikelyOreId("toms_storage:inventory_connector"));
    }

    @Test
    void moddedOreDisplayNamesKeepModNamespaceVisible() {
        assertEquals("Bauxite (Techreborn)", MinerXpGetter.humanizeModdedOreName("techreborn:bauxite_ore"));
        assertEquals("Iridium (Advanced Reborn)", MinerXpGetter.humanizeModdedOreName("advanced_reborn:deepslate_iridium_ore"));
    }
}
