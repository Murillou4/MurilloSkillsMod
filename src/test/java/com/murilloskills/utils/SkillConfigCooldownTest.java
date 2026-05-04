package com.murilloskills.utils;

import com.murilloskills.config.ModConfig;
import com.murilloskills.skills.MurilloSkillsList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillConfigCooldownTest {
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
    void prestigeReducesAbilityCooldownByFivePercentPerLevel() {
        assertEquals(1200, SkillConfig.getPrestigeAdjustedCooldown(1200, 0));
        assertEquals(1140, SkillConfig.getPrestigeAdjustedCooldown(1200, 1));
        assertEquals(600, SkillConfig.getPrestigeAdjustedCooldown(1200, 10));
    }

    @Test
    void prestigeCooldownReductionUsesConfiguredCap() {
        ModConfig.get().prestige.maxCooldownReduction = 0.25f;

        assertEquals(900, SkillConfig.getPrestigeAdjustedCooldown(1200, 10));
    }

    @Test
    void skillAbilityCooldownTicksUsesSkillBaseAndPrestige() {
        assertEquals(1620L, SkillConfig.getAbilityCooldownTicks(MurilloSkillsList.FARMER, 2));
    }
}
