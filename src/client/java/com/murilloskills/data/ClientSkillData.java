package com.murilloskills.data;

import com.murilloskills.skills.MurilloSkillsList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientSkillData {
    private static final Map<MurilloSkillsList, PlayerSkillData.SkillStats> skills = new HashMap<>();
    private static MurilloSkillsList paragonSkill = null;
    private static List<MurilloSkillsList> selectedSkills = new ArrayList<>();

    // Daily Challenges data
    private static List<ChallengeInfo> dailyChallenges = new ArrayList<>();
    private static String challengeDateKey = "";
    private static boolean allChallengesComplete = false;

    public static void update(Map<MurilloSkillsList, PlayerSkillData.SkillStats> newSkills) {
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

    public static PlayerSkillData.SkillStats get(MurilloSkillsList skill) {
        return skills.getOrDefault(skill, new PlayerSkillData.SkillStats(0, 0));
    }

    public static Map<MurilloSkillsList, PlayerSkillData.SkillStats> getAll() {
        return skills;
    }

    // ============ Prestige Methods ============

    /**
     * Gets prestige level for a skill.
     */
    public static int getPrestige(MurilloSkillsList skill) {
        PlayerSkillData.SkillStats stats = skills.get(skill);
        return stats != null ? stats.prestige : 0;
    }

    /**
     * Checks if skill can prestige (level 100, not at max prestige).
     */
    public static boolean canPrestige(MurilloSkillsList skill) {
        PlayerSkillData.SkillStats stats = skills.get(skill);
        if (stats == null)
            return false;
        return stats.level >= 100 && stats.prestige < 10 && skill == paragonSkill;
    }

    // ============ Daily Challenges Methods ============

    /**
     * Updates daily challenges from server sync.
     */
    public static void updateDailyChallenges(List<ChallengeInfo> challenges, String dateKey, boolean allComplete) {
        dailyChallenges.clear();
        dailyChallenges.addAll(challenges);
        challengeDateKey = dateKey;
        allChallengesComplete = allComplete;
    }

    public static List<ChallengeInfo> getDailyChallenges() {
        return new ArrayList<>(dailyChallenges);
    }

    public static String getChallengeDateKey() {
        return challengeDateKey;
    }

    public static boolean areAllChallengesComplete() {
        return allChallengesComplete;
    }

    public static int getCompletedChallengeCount() {
        return (int) dailyChallenges.stream().filter(c -> c.completed).count();
    }

    /**
     * Client-side challenge info for display.
     */
    public record ChallengeInfo(
            String type,
            String skillName,
            int target,
            int progress,
            boolean completed,
            int xpReward) {
        public float getProgressPercentage() {
            return target > 0 ? (float) progress / target : 0f;
        }
    }
}
