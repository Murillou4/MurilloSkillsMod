package com.murilloskills.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.skills.MurilloSkillsList;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerSkillCoreAdapter {
    private static final Gson GSON = new Gson();
    private static final String DAILY_CHALLENGES_EXTENSION = "fabric.dailyChallenges";

    private PlayerSkillCoreAdapter() {
    }

    public static PlayerSkillDataCore toCore(PlayerSkillData data) {
        PlayerSkillDataCore core = new PlayerSkillDataCore();
        data.normalizeParagonState();

        for (Map.Entry<MurilloSkillsList, PlayerSkillData.SkillStats> entry : data.skills.entrySet()) {
            PlayerSkillData.SkillStats stats = entry.getValue();
            core.setSkill(toCoreSkill(entry.getKey()), stats.level, stats.xp, stats.lastAbilityUse, stats.prestige);
        }

        List<SkillType> selected = new ArrayList<>();
        for (MurilloSkillsList skill : data.getSelectedSkills()) {
            selected.add(toCoreSkill(skill));
        }
        core.setSelectedSkillsDirect(selected);

        Set<SkillType> paragons = EnumSet.noneOf(SkillType.class);
        for (MurilloSkillsList skill : data.getParagonSkills()) {
            paragons.add(toCoreSkill(skill));
        }
        core.setParagonSkillsDirect(paragons);
        core.setActiveParagonSkill(data.getActiveParagonSkill() == null ? null : toCoreSkill(data.getActiveParagonSkill()));
        core.setSkillToggles(data.skillToggles);
        core.setAchievementStats(data.achievementStats);
        if (data.dailyChallenges != null) {
            core.getExtensions().put(DAILY_CHALLENGES_EXTENSION, GSON.toJsonTree(data.dailyChallenges));
        }
        core.normalizeParagonState();
        return core;
    }

    public static PlayerSkillData fromCore(PlayerSkillDataCore core) {
        PlayerSkillData data = new PlayerSkillData();
        core.normalizeParagonState();

        for (Map.Entry<SkillType, SkillStatsCore> entry : core.mutableSkills().entrySet()) {
            SkillStatsCore stats = entry.getValue();
            data.setSkill(fromCoreSkill(entry.getKey()), stats.getLevel(), stats.getXp(),
                    stats.getLastAbilityUse(), stats.getPrestige());
        }

        data.selectedSkills.clear();
        for (SkillType skill : core.getSelectedSkills()) {
            data.selectedSkills.add(fromCoreSkill(skill));
        }

        data.clearAllParagonSkills();
        for (SkillType skill : core.getParagonSkills()) {
            data.paragonSkills.add(fromCoreSkill(skill));
        }
        data.paragonSkill = core.getActiveParagonSkill() == null ? null : fromCoreSkill(core.getActiveParagonSkill());
        data.skillToggles.clear();
        data.skillToggles.putAll(core.getSkillToggles());
        data.achievementStats.clear();
        data.achievementStats.putAll(core.getAchievementStats());
        JsonElement dailyChallenges = core.getExtensions().get(DAILY_CHALLENGES_EXTENSION);
        if (dailyChallenges != null && !dailyChallenges.isJsonNull()) {
            try {
                data.dailyChallenges = GSON.fromJson(
                        dailyChallenges,
                        com.murilloskills.utils.DailyChallengeManager.PlayerChallengeData.class);
            } catch (Exception ignored) {
                data.dailyChallenges = null;
            }
        }
        data.normalizeParagonState();
        return data;
    }

    public static void copyInto(PlayerSkillData source, PlayerSkillData target) {
        target.skills.clear();
        target.skills.putAll(source.skills);
        target.paragonSkill = source.paragonSkill;
        target.paragonSkills.clear();
        target.paragonSkills.addAll(source.paragonSkills);
        target.selectedSkills.clear();
        target.selectedSkills.addAll(source.selectedSkills);
        target.skillToggles.clear();
        target.skillToggles.putAll(source.skillToggles);
        target.achievementStats.clear();
        target.achievementStats.putAll(source.achievementStats);
        target.dailyChallenges = source.dailyChallenges;
        target.normalizeParagonState();
    }

    private static SkillType toCoreSkill(MurilloSkillsList skill) {
        return SkillType.valueOf(skill.name());
    }

    private static MurilloSkillsList fromCoreSkill(SkillType skill) {
        return MurilloSkillsList.valueOf(skill.name());
    }
}
