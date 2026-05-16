package com.murilloskills.core.io;

import com.murilloskills.core.config.SkillProgressionConfig;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSkillJsonCodecTest {
    @TempDir
    Path tempDir;

    @Test
    void roundTripsVersionedPlayerSaveFile() throws Exception {
        PlayerSkillDataCore data = new PlayerSkillDataCore();
        data.setSelectedSkills(Arrays.asList(SkillType.MINER, SkillType.FARMER), SkillProgressionConfig.DEFAULT);
        data.activateParagonSkill(SkillType.MINER);
        data.setSkill(SkillType.MINER, 42, 123.5, 88L, 2);
        data.setToggle(SkillType.MINER, "autoTorch", true);
        data.getAchievementStats().put("first_diamond", Integer.valueOf(1));
        data.getExtensions().put("fabric.dailyChallenges", new JsonPrimitive("kept"));

        Path save = tempDir.resolve("player.json");
        PlayerSkillJsonCodec.write(save, data);
        PlayerSkillDataCore loaded = PlayerSkillJsonCodec.read(save);

        assertEquals(SkillType.MINER, loaded.getActiveParagonSkill());
        assertTrue(loaded.getSelectedSkills().contains(SkillType.FARMER));
        assertEquals(42, loaded.getSkill(SkillType.MINER).getLevel());
        assertEquals(123.5, loaded.getSkill(SkillType.MINER).getXp(), 0.001);
        assertTrue(loaded.getToggle(SkillType.MINER, "autoTorch", false));
        assertEquals(Integer.valueOf(1), loaded.getAchievementStats().get("first_diamond"));
        assertEquals("kept", loaded.getExtensions().get("fabric.dailyChallenges").getAsString());
    }
}
