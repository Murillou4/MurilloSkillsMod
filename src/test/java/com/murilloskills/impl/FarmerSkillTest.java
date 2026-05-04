package com.murilloskills.impl;

import com.murilloskills.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmerSkillTest {

    private static void resetConfig() {
        try {
            Field configField = ModConfig.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(null, new ModConfig.ConfigData());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize ModConfig for test", e);
        }
    }

    @Test
    void fertileGroundGrowthBoostScalesProgressivelyToThreeHundredPercent() {
        resetConfig();

        assertEquals(0.0f, FarmerSkill.getFertileGroundGrowthBoost(24), 0.0001f);
        assertEquals(0.25f, FarmerSkill.getFertileGroundGrowthBoost(25), 0.0001f);
        assertEquals(3.0f, FarmerSkill.getFertileGroundGrowthBoost(100), 0.0001f);
        assertEquals(300, FarmerSkill.getFertileGroundGrowthPercent(100));
    }

    @Test
    void fertileGroundGrowthBoostIncreasesBetweenUnlockAndMaxLevel() {
        resetConfig();

        float level25 = FarmerSkill.getFertileGroundGrowthBoost(25);
        float level50 = FarmerSkill.getFertileGroundGrowthBoost(50);
        float level75 = FarmerSkill.getFertileGroundGrowthBoost(75);
        float level100 = FarmerSkill.getFertileGroundGrowthBoost(100);

        assertTrue(level50 > level25);
        assertTrue(level75 > level50);
        assertTrue(level100 > level75);
    }

    @Test
    void areaRadiusUnlocksAtFarmerMilestones() {
        assertEquals(0, FarmerSkill.getMaxAreaPlantingRadius(24));
        assertEquals(1, FarmerSkill.getMaxAreaPlantingRadius(25));
        assertEquals(2, FarmerSkill.getMaxAreaPlantingRadius(50));
        assertEquals(3, FarmerSkill.getMaxAreaPlantingRadius(75));
        assertEquals(4, FarmerSkill.getMaxAreaPlantingRadius(99));
    }

    @Test
    void areaModeCyclesThroughUnlockedSizesThenDisables() {
        assertEquals(1, FarmerSkill.getNextAreaPlantingRadius(25, 0));
        assertEquals(0, FarmerSkill.getNextAreaPlantingRadius(25, 1));

        assertEquals(1, FarmerSkill.getNextAreaPlantingRadius(50, 0));
        assertEquals(2, FarmerSkill.getNextAreaPlantingRadius(50, 1));
        assertEquals(0, FarmerSkill.getNextAreaPlantingRadius(50, 2));

        assertEquals(4, FarmerSkill.getNextAreaPlantingRadius(99, 3));
        assertEquals(0, FarmerSkill.getNextAreaPlantingRadius(99, 4));
    }

    @Test
    void areaDiameterMatchesSelectedRadius() {
        assertEquals(0, FarmerSkill.getAreaPlantingDiameter(0));
        assertEquals(3, FarmerSkill.getAreaPlantingDiameter(1));
        assertEquals(5, FarmerSkill.getAreaPlantingDiameter(2));
        assertEquals(9, FarmerSkill.getAreaPlantingDiameter(4));
    }
}
