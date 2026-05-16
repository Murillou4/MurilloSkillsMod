package com.murilloskills.core.data;

import com.murilloskills.core.config.SkillProgressionConfig;
import com.murilloskills.core.config.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSkillDataCoreTest {
    @Test
    void masterClassesAllowOnlyOneParagonButSubClassesCanStack() {
        PlayerSkillDataCore data = new PlayerSkillDataCore();

        assertTrue(data.activateParagonSkill(SkillType.MINER));
        assertFalse(data.activateParagonSkill(SkillType.WARRIOR));
        assertFalse(data.activateParagonSkill(SkillType.ARCHER));

        assertTrue(data.activateParagonSkill(SkillType.FARMER));
        assertTrue(data.activateParagonSkill(SkillType.FISHER));
        assertTrue(data.activateParagonSkill(SkillType.BUILDER));
        assertTrue(data.activateParagonSkill(SkillType.BLACKSMITH));
        assertTrue(data.activateParagonSkill(SkillType.EXPLORER));

        assertEquals(SkillType.MINER, data.getMasterParagonSkill());
        assertEquals(SkillType.MINER, data.getActiveParagonSkill());
        assertEquals(new HashSet<SkillType>(Arrays.asList(
                SkillType.MINER,
                SkillType.FARMER,
                SkillType.FISHER,
                SkillType.BUILDER,
                SkillType.BLACKSMITH,
                SkillType.EXPLORER)), data.getParagonSkills());
    }

    @Test
    void onlyParagonSkillsCanProgressFromLevelNinetyNineToOneHundred() {
        SkillProgressionConfig config = SkillProgressionConfig.DEFAULT;
        PlayerSkillDataCore data = new PlayerSkillDataCore();
        data.setSelectedSkills(Arrays.asList(SkillType.FARMER), config);
        data.setSkill(SkillType.FARMER, 99, 0.0, -1L, 0);

        data.addXpToSkill(SkillType.FARMER, config.getXpForLevel(99) * 2, config);
        assertEquals(99, data.getSkill(SkillType.FARMER).getLevel());

        assertTrue(data.activateParagonSkill(SkillType.FARMER));
        data.addXpToSkill(SkillType.FARMER, config.getXpForLevel(99) * 2, config);

        assertEquals(100, data.getSkill(SkillType.FARMER).getLevel());
    }
}
