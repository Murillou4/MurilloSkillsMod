package com.murilloskills.data;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import java.util.HashMap;
import java.util.Map;

public class ClientSkillData {
    private static final Map<MurilloSkillsList, SkillGlobalState.SkillStats> skills = new HashMap<>();
    private static MurilloSkillsList paragonSkill = null;

    public static void update(Map<MurilloSkillsList, SkillGlobalState.SkillStats> newSkills) {
        skills.clear();
        skills.putAll(newSkills);
    }

    public static void setParagonSkill(MurilloSkillsList skill) {
        paragonSkill = skill;
    }

    public static MurilloSkillsList getParagonSkill() {
        return paragonSkill;
    }

    public static SkillGlobalState.SkillStats get(MurilloSkillsList skill) {
        return skills.getOrDefault(skill, new SkillGlobalState.SkillStats(0, 0));
    }

    public static Map<MurilloSkillsList, SkillGlobalState.SkillStats> getAll() {
        return skills;
    }
}