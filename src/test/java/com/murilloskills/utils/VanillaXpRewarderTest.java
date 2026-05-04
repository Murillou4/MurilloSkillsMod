package com.murilloskills.utils;

import com.murilloskills.data.PlayerSkillData.XpAddResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaXpRewarderTest {

    @Test
    void detectsMilestonesAgainAfterPrestigeResetCycle() {
        XpAddResult result = new XpAddResult(true, 1, 100);

        assertEquals(List.of(10, 25, 50, 75, 100), VanillaXpRewarder.getCrossedMilestones(result));
    }

    @Test
    void detectsMilestonesAgainAfterManualResetCycle() {
        XpAddResult result = new XpAddResult(true, 0, 75);

        assertEquals(List.of(10, 25, 50, 75), VanillaXpRewarder.getCrossedMilestones(result));
    }

    @Test
    void ignoresMilestonesWhenNoLevelWasGained() {
        assertTrue(VanillaXpRewarder.getCrossedMilestones(XpAddResult.NO_CHANGE).isEmpty());
    }
}
