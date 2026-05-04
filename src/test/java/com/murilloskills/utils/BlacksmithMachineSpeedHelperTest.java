package com.murilloskills.utils;

import com.murilloskills.config.ModConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlacksmithMachineSpeedHelperTest {
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
    void directMultiplierMatchesFurnaceCurve() {
        assertEquals(1.0f, BlacksmithMachineSpeedHelper.getDirectSpeedMultiplier(0));
        assertEquals(4.1f, BlacksmithMachineSpeedHelper.getDirectSpeedMultiplier(50), 0.0001f);
        assertEquals(7.2f, BlacksmithMachineSpeedHelper.getDirectSpeedMultiplier(100), 0.0001f);
    }

    @Test
    void rebornCoreBonusConvertsDirectMultiplierToTimeReduction() {
        assertEquals(0.0D, BlacksmithMachineSpeedHelper.getRebornCoreSpeedBonus(0), 0.0001D);
        assertEquals(0.7561D, BlacksmithMachineSpeedHelper.getRebornCoreSpeedBonus(50), 0.0001D);
        assertEquals(0.8611D, BlacksmithMachineSpeedHelper.getRebornCoreSpeedBonus(100), 0.0001D);
    }

    @Test
    void rebornCoreBonusIsCappedBeforeMachineCap() {
        ModConfig.get().blacksmith.furnaceSpeedMaxMultiplier = 1000.0f;

        assertEquals(0.99D, BlacksmithMachineSpeedHelper.getRebornCoreSpeedBonus(100), 0.0001D);
    }
}
