package com.murilloskills.data;

import com.murilloskills.config.ModConfig;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSkillDataParagonClassTest {

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
    void masterClassesAllowOnlyOneParagonButSubClassesCanStack() {
        PlayerSkillData data = new PlayerSkillData();

        assertTrue(data.activateParagonSkill(MurilloSkillsList.MINER));
        assertFalse(data.activateParagonSkill(MurilloSkillsList.WARRIOR));
        assertFalse(data.activateParagonSkill(MurilloSkillsList.ARCHER));

        assertTrue(data.activateParagonSkill(MurilloSkillsList.FARMER));
        assertTrue(data.activateParagonSkill(MurilloSkillsList.FISHER));
        assertTrue(data.activateParagonSkill(MurilloSkillsList.BUILDER));
        assertTrue(data.activateParagonSkill(MurilloSkillsList.BLACKSMITH));
        assertTrue(data.activateParagonSkill(MurilloSkillsList.EXPLORER));

        assertEquals(MurilloSkillsList.MINER, data.getMasterParagonSkill());
        assertEquals(MurilloSkillsList.MINER, data.getActiveParagonSkill());
        assertEquals(Set.of(
                MurilloSkillsList.MINER,
                MurilloSkillsList.FARMER,
                MurilloSkillsList.FISHER,
                MurilloSkillsList.BUILDER,
                MurilloSkillsList.BLACKSMITH,
                MurilloSkillsList.EXPLORER), data.getParagonSkills());
    }

    @Test
    void legacyParagonFieldIsPromotedIntoParagonSet() {
        PlayerSkillData data = new PlayerSkillData();
        data.paragonSkill = MurilloSkillsList.BUILDER;

        data.normalizeParagonState();

        assertTrue(data.isParagonSkill(MurilloSkillsList.BUILDER));
        assertEquals(MurilloSkillsList.BUILDER, data.getActiveParagonSkill());
    }

    @Test
    void onlyParagonSkillsCanProgressFromLevelNinetyNineToOneHundred() {
        PlayerSkillData data = new PlayerSkillData();
        data.setSelectedSkills(List.of(MurilloSkillsList.FARMER));
        data.setSkill(MurilloSkillsList.FARMER, 99, 0);

        data.addXpToSkill(MurilloSkillsList.FARMER, SkillConfig.getXpForLevel(99) * 2);
        assertEquals(99, data.getSkill(MurilloSkillsList.FARMER).level);

        assertTrue(data.activateParagonSkill(MurilloSkillsList.FARMER));
        data.addXpToSkill(MurilloSkillsList.FARMER, SkillConfig.getXpForLevel(99) * 2);

        assertEquals(100, data.getSkill(MurilloSkillsList.FARMER).level);
    }
}
