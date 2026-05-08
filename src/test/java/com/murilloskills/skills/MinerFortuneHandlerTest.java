package com.murilloskills.skills;

import com.murilloskills.config.ModConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinerFortuneHandlerTest {
    @BeforeEach
    void resetConfig() {
        try {
            Field configField = ModConfig.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(null, new ModConfig.ConfigData());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize ModConfig for test", e);
        }
    }

    @Test
    void oreBlocksKeepMinerFortuneBeforeResourceUnlock() {
        assertTrue(MinerFortuneHandler.shouldApplySkillFortune("techreborn:bauxite_ore", 34, 0));
        assertEquals(1, MinerFortuneHandler.getSkillFortuneBonus(34, 0, "techreborn:bauxite_ore"));
    }

    @Test
    void rawResourceBlocksKeepMinerFortuneBeforeResourceUnlock() {
        assertTrue(MinerFortuneHandler.shouldApplySkillFortune("minecraft:raw_iron_block", 34, 0));
        assertEquals(1, MinerFortuneHandler.getSkillFortuneBonus(34, 0, "minecraft:raw_iron_block"));
    }

    @Test
    void glowstoneAlwaysReceivesMinerFortune() {
        assertTrue(MinerFortuneHandler.shouldApplySkillFortune("minecraft:glowstone", 1, 0));
        assertTrue(MinerFortuneHandler.shouldApplySkillFortune("minecraft:glowstone", 75, 0));
        assertEquals(2, MinerFortuneHandler.getSkillFortuneBonus(75, 0, "minecraft:glowstone"));
    }

    @Test
    void prestigeKeepsResourceFortuneUnlockedAfterReset() {
        assertTrue(MinerFortuneHandler.shouldApplySkillFortune("minecraft:glowstone", 1, 2));
        assertEquals(1, MinerFortuneHandler.getSkillFortuneBonus(1, 2, "minecraft:glowstone"));
    }

    @Test
    void leavesDoNotReceiveMinerFortuneWithoutAxe() {
        assertFalse(MinerFortuneHandler.shouldApplySkillFortune("minecraft:oak_leaves", 50, 0, null));
        assertEquals(0, MinerFortuneHandler.getSkillFortuneBonus(50, 0, "minecraft:oak_leaves", null));
        assertFalse(MinerFortuneHandler.shouldApplySkillFortune("minecraft:oak_leaves", 100, 8, null));
        assertEquals(0, MinerFortuneHandler.getSkillFortuneBonus(100, 8, "minecraft:oak_leaves", null));
    }

    @Test
    void moddedLeavesIdsAreRecognized() {
        assertTrue(MinerFortuneHandler.isLeavesBlockId("biomesoplenty:fir_leaves"));
        assertTrue(MinerFortuneHandler.isLeavesBlockId("twilightforest:dark_leaves"));
        assertFalse(MinerFortuneHandler.isLeavesBlockId("minecraft:glowstone"));
    }

    @Test
    void missingBlockContextDoesNotReceiveMinerFortune() {
        assertFalse(MinerFortuneHandler.shouldApplySkillFortune(null, 100, 0));
        assertEquals(0, MinerFortuneHandler.getSkillFortuneBonus(100, 0, null));
    }
}
