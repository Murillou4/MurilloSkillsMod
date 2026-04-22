package com.murilloskills.utils;

import com.murilloskills.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillConfigBlacksmithDiscountTest {

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
    void anvilDiscountStartsAtFortyPercentAndCapsAtNinetyPercent() {
        resetConfig();
        assertEquals(0.40f, SkillConfig.getBlacksmithAnvilDiscount(25));
        assertEquals(0.90f, SkillConfig.getBlacksmithAnvilDiscount(100));
    }

    @Test
    void enchantingRequirementUsesDiscountCurveForDisplayedCost() {
        resetConfig();
        assertEquals(18, SkillConfig.getBlacksmithEnchantingTableRequirement(25, 30));
        assertEquals(12, SkillConfig.getBlacksmithEnchantingTableRequirement(100, 30));
    }

    @Test
    void enchantingRefundNeverConsumesLessThanOneLevel() {
        resetConfig();
        assertEquals(1, SkillConfig.getBlacksmithEnchantingTableRefundLevels(25, 3));
        assertEquals(2, SkillConfig.getBlacksmithEnchantingTableRefundLevels(100, 3));
        assertEquals(0, SkillConfig.getBlacksmithEnchantingTableRefundLevels(100, 1));
    }

    @Test
    void overEnchantingUsesLegacyFallbacksWhenConfigFieldsAreMissing() {
        resetConfig();
        ModConfig.ConfigData config = ModConfig.get();

        // Simulate legacy JSON missing newer over-enchant fields (Gson may deserialize as zero).
        config.blacksmith.overEnchantLevel = 0;
        config.blacksmith.overEnchantMaxLevel = 0;
        config.blacksmith.overEnchantBaseCost = 0;
        config.blacksmith.overEnchantStepCost = 0;
        config.blacksmith.superEnchantChance = 0.0f;

        assertEquals(SkillConfig.BLACKSMITH_OVERENCHANT_LEVEL, SkillConfig.getBlacksmithOverEnchantUnlockLevel());
        assertEquals(SkillConfig.BLACKSMITH_OVERENCHANT_MAX_LEVEL, SkillConfig.getBlacksmithOverEnchantMaxLevel());
        assertEquals(SkillConfig.BLACKSMITH_OVERENCHANT_BASE_COST, SkillConfig.getBlacksmithOverEnchantBaseCost());
        assertEquals(SkillConfig.BLACKSMITH_OVERENCHANT_STEP_COST, SkillConfig.getBlacksmithOverEnchantStepCost());
        assertEquals(SkillConfig.BLACKSMITH_SUPER_ENCHANT_CHANCE, SkillConfig.getBlacksmithSuperEnchantChance());
    }
}
