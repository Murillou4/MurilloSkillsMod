package com.murilloskills.data;

import com.murilloskills.skills.MurilloSkillsList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientSkillData {
    private static final Map<MurilloSkillsList, SkillGlobalState.SkillStats> skills = new HashMap<>();
    private static MurilloSkillsList paragonSkill = null;
    private static List<MurilloSkillsList> selectedSkills = new ArrayList<>();

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

    public static void setSelectedSkills(List<MurilloSkillsList> skills) {
        selectedSkills.clear();
        if (skills != null) {
            selectedSkills.addAll(skills);
        }
    }

    public static List<MurilloSkillsList> getSelectedSkills() {
        return new ArrayList<>(selectedSkills);
    }

    public static boolean hasSelectedSkills() {
        return selectedSkills != null && selectedSkills.size() == 3;
    }

    public static boolean isSkillSelected(MurilloSkillsList skill) {
        return selectedSkills != null && selectedSkills.contains(skill);
    }

    public static SkillGlobalState.SkillStats get(MurilloSkillsList skill) {
        return skills.getOrDefault(skill, new SkillGlobalState.SkillStats(0, 0));
    }

    public static Map<MurilloSkillsList, SkillGlobalState.SkillStats> getAll() {
        return skills;
    }
}