package com.murilloskills.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FarmerSkillTest {

    @Test
    void fertileGroundGrowthChanceScalesAtMilestones() {
        assertEquals(0.0f, FarmerSkill.getFertileGroundGrowthChance(24));
        assertEquals(0.25f, FarmerSkill.getFertileGroundGrowthChance(25));
        assertEquals(0.50f, FarmerSkill.getFertileGroundGrowthChance(50));
        assertEquals(0.75f, FarmerSkill.getFertileGroundGrowthChance(75));
        assertEquals(0.99f, FarmerSkill.getFertileGroundGrowthChance(99));
        assertEquals(0.99f, FarmerSkill.getFertileGroundGrowthChance(100));
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
