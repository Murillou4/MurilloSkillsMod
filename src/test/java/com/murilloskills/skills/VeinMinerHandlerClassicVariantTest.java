package com.murilloskills.skills;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeinMinerHandlerClassicVariantTest {

    @Test
    void classicVariantZeroDoesNotIncludeConnectedOres() {
        assertFalse(ClassicUltmineTargetRules.isConnectedOreCandidate(
                "minecraft:deepslate",
                "techreborn:bauxite_ore",
                0,
                Set.of()));
    }

    @Test
    void connectedOresVariantIncludesModdedOreIds() {
        assertTrue(ClassicUltmineTargetRules.isConnectedOreCandidate(
                "minecraft:deepslate",
                "techreborn:bauxite_ore",
                1,
                Set.of()));
        assertTrue(ClassicUltmineTargetRules.isConnectedOreCandidate(
                "minecraft:cobbled_deepslate",
                "advanced_reborn:deepslate_iridium_ore",
                1,
                Set.of()));
    }

    @Test
    void lockedClassicBlocksAreExcluded() {
        Set<String> blocked = Set.of("minecraft:deepslate", "techreborn:bauxite_ore");

        assertTrue(ClassicUltmineTargetRules.isOriginBlocked("minecraft:deepslate", blocked));
        assertFalse(ClassicUltmineTargetRules.shouldExpandIntoConnectedOres("minecraft:deepslate", 1, blocked));
        assertFalse(ClassicUltmineTargetRules.isConnectedOreCandidate(
                "minecraft:cobblestone",
                "techreborn:bauxite_ore",
                1,
                blocked));
    }

    @Test
    void oreOriginsDoNotPullSurroundingStoneOrDeepslate() {
        assertFalse(ClassicUltmineTargetRules.shouldExpandIntoConnectedOres(
                "techreborn:bauxite_ore",
                1,
                Set.of()));
        assertFalse(ClassicUltmineTargetRules.isConnectedOreCandidate(
                "advanced_reborn:deepslate_iridium_ore",
                "minecraft:deepslate",
                1,
                Set.of()));
    }
}
