package com.murilloskills.core.data;

import com.google.gson.JsonElement;
import com.murilloskills.core.config.SkillProgressionConfig;
import com.murilloskills.core.config.SkillType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerSkillDataCore {
    private final EnumMap<SkillType, SkillStatsCore> skills = new EnumMap<SkillType, SkillStatsCore>(SkillType.class);
    private SkillType activeParagonSkill;
    private EnumSet<SkillType> paragonSkills = EnumSet.noneOf(SkillType.class);
    private List<SkillType> selectedSkills = new ArrayList<SkillType>();
    private Map<String, Boolean> skillToggles = new HashMap<String, Boolean>();
    private Map<String, Integer> achievementStats = new HashMap<String, Integer>();
    private Map<String, JsonElement> extensions = new HashMap<String, JsonElement>();

    public PlayerSkillDataCore() {
        for (SkillType skill : SkillType.values()) {
            skills.put(skill, new SkillStatsCore(0, 0.0, -1L, 0));
        }
    }

    public EnumMap<SkillType, SkillStatsCore> mutableSkills() {
        return skills;
    }

    public SkillStatsCore getSkill(SkillType skill) {
        return skills.get(skill);
    }

    public void setSkill(SkillType skill, int level, double xp, long lastAbilityUse, int prestige) {
        skills.put(skill, new SkillStatsCore(level, xp, lastAbilityUse, prestige));
    }

    public boolean hasSelectedSkills(SkillProgressionConfig config) {
        return selectedSkills != null && selectedSkills.size() >= config.getMaxSelectedSkills();
    }

    public boolean isSkillSelected(SkillType skill) {
        return selectedSkills != null && selectedSkills.contains(skill);
    }

    public boolean setSelectedSkills(List<SkillType> skillsToSelect, SkillProgressionConfig config) {
        if (skillsToSelect == null || skillsToSelect.isEmpty()) {
            return false;
        }

        Set<SkillType> potentialSelection = new HashSet<SkillType>(selectedSkills);
        potentialSelection.addAll(skillsToSelect);
        if (potentialSelection.size() > config.getMaxSelectedSkills()) {
            return false;
        }

        selectedSkills = new ArrayList<SkillType>(potentialSelection);
        return true;
    }

    public List<SkillType> getSelectedSkills() {
        return Collections.unmodifiableList(selectedSkills);
    }

    public void setSelectedSkillsDirect(List<SkillType> selectedSkills) {
        this.selectedSkills = selectedSkills == null
                ? new ArrayList<SkillType>()
                : new ArrayList<SkillType>(selectedSkills);
    }

    public Set<SkillType> getParagonSkills() {
        normalizeParagonState();
        if (paragonSkills.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(paragonSkills));
    }

    public void setParagonSkillsDirect(Set<SkillType> paragonSkills) {
        this.paragonSkills = paragonSkills == null || paragonSkills.isEmpty()
                ? EnumSet.noneOf(SkillType.class)
                : EnumSet.copyOf(paragonSkills);
        normalizeParagonState();
    }

    public SkillType getActiveParagonSkill() {
        normalizeParagonState();
        if (activeParagonSkill != null && paragonSkills.contains(activeParagonSkill)) {
            return activeParagonSkill;
        }
        SkillType masterParagon = getMasterParagonSkill();
        return masterParagon != null ? masterParagon : (paragonSkills.isEmpty() ? null : paragonSkills.iterator().next());
    }

    public void setActiveParagonSkill(SkillType activeParagonSkill) {
        this.activeParagonSkill = activeParagonSkill;
        normalizeParagonState();
    }

    public SkillType getMasterParagonSkill() {
        normalizeParagonState();
        for (SkillType skill : paragonSkills) {
            if (skill.isMasterClass()) {
                return skill;
            }
        }
        return null;
    }

    public boolean isParagonSkill(SkillType skill) {
        normalizeParagonState();
        return skill != null && paragonSkills.contains(skill);
    }

    public boolean canActivateParagonSkill(SkillType skill) {
        if (skill == null) {
            return false;
        }
        normalizeParagonState();
        if (paragonSkills.contains(skill)) {
            return false;
        }
        if (skill.isMasterClass()) {
            return getMasterParagonSkill() == null;
        }
        return skill.isSubClass();
    }

    public boolean activateParagonSkill(SkillType skill) {
        if (!canActivateParagonSkill(skill)) {
            return false;
        }
        paragonSkills.add(skill);
        if (activeParagonSkill == null || skill.isMasterClass()) {
            activeParagonSkill = skill;
        }
        normalizeParagonState();
        return true;
    }

    public void clearParagonSkill(SkillType skill) {
        if (skill == null) {
            return;
        }
        normalizeParagonState();
        paragonSkills.remove(skill);
        if (activeParagonSkill == skill) {
            activeParagonSkill = chooseActiveParagonSkill();
        }
    }

    public void clearAllParagonSkills() {
        paragonSkills.clear();
        activeParagonSkill = null;
    }

    public boolean getToggle(SkillType skill, String toggleName, boolean defaultValue) {
        String key = skill.name() + "." + toggleName;
        Boolean value = skillToggles.get(key);
        return value == null ? defaultValue : value.booleanValue();
    }

    public void setToggle(SkillType skill, String toggleName, boolean value) {
        skillToggles.put(skill.name() + "." + toggleName, Boolean.valueOf(value));
    }

    public Map<String, Boolean> getSkillToggles() {
        return skillToggles;
    }

    public void setSkillToggles(Map<String, Boolean> skillToggles) {
        this.skillToggles = skillToggles == null
                ? new HashMap<String, Boolean>()
                : new HashMap<String, Boolean>(skillToggles);
    }

    public Map<String, Integer> getAchievementStats() {
        return achievementStats;
    }

    public void setAchievementStats(Map<String, Integer> achievementStats) {
        this.achievementStats = achievementStats == null
                ? new HashMap<String, Integer>()
                : new HashMap<String, Integer>(achievementStats);
    }

    public Map<String, JsonElement> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, JsonElement> extensions) {
        this.extensions = extensions == null
                ? new HashMap<String, JsonElement>()
                : new HashMap<String, JsonElement>(extensions);
    }

    public XpAddResult addXpToSkill(SkillType skill, int amount, SkillProgressionConfig config) {
        if (selectedSkills.isEmpty() || !isSkillSelected(skill)) {
            return XpAddResult.NO_CHANGE;
        }

        SkillStatsCore stats = skills.get(skill);
        int adjustedAmount = Math.round(amount * PrestigeRules.getXpMultiplier(stats.getPrestige(), config));
        int maxLevelAllowed = isParagonSkill(skill) ? config.getMaxLevel() : config.getMaxLevel() - 1;
        return stats.addXp(adjustedAmount, maxLevelAllowed, config);
    }

    public void normalizeParagonState() {
        if (paragonSkills == null) {
            paragonSkills = EnumSet.noneOf(SkillType.class);
        }

        if (activeParagonSkill != null) {
            paragonSkills.add(activeParagonSkill);
        }

        SkillType keptMaster = null;
        if (activeParagonSkill != null && activeParagonSkill.isMasterClass()
                && paragonSkills.contains(activeParagonSkill)) {
            keptMaster = activeParagonSkill;
        }

        for (SkillType skill : SkillType.values()) {
            if (skill.isMasterClass() && paragonSkills.contains(skill)) {
                if (keptMaster == null) {
                    keptMaster = skill;
                } else if (keptMaster != skill) {
                    paragonSkills.remove(skill);
                }
            }
        }

        if (activeParagonSkill == null || !paragonSkills.contains(activeParagonSkill)
                || (keptMaster != null && activeParagonSkill.isSubClass())) {
            activeParagonSkill = chooseActiveParagonSkill();
        }
    }

    private SkillType chooseActiveParagonSkill() {
        for (SkillType skill : paragonSkills) {
            if (skill.isMasterClass()) {
                return skill;
            }
        }
        return paragonSkills.isEmpty() ? null : paragonSkills.iterator().next();
    }
}
