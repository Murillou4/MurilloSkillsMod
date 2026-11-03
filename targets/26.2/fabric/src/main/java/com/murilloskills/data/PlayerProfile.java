package com.murilloskills.data;

import com.murilloskills.skills.SkillType;
import java.util.*;

public final class PlayerProfile {
    public int schemaVersion = 2;
    public EnumMap<SkillType, SkillStats> skills = new EnumMap<>(SkillType.class);
    public List<SkillType> selected = new ArrayList<>();
    public SkillType paragon, equipped;
    public int seals;
    public long globalCooldownUntil;
    public boolean importedLegacy, hasChosenParagon;
    public List<Contract> contracts = new ArrayList<>();
    public int completedSets;
    public Set<String> achievements = new HashSet<>();
    public Set<String> discoveries = new HashSet<>();
    public Set<String> toggles = new HashSet<>();
    public Map<String,Boolean> featureSettings = new HashMap<>();
    public ToolPreferences tools = new ToolPreferences();
    public PlayerProfile() { for (var skill:SkillType.values()) skills.put(skill,new SkillStats()); }
    public SkillStats stats(SkillType skill) { return skills.computeIfAbsent(skill, k->new SkillStats()); }
    public boolean selected(SkillType skill) { return skill != null && selected.contains(skill); }
    public boolean unlocked(SkillType skill, int level) { return selected(skill) && stats(skill).unlockLevel() >= level; }
    public boolean fresh() { return skills.values().stream().allMatch(s->s.level==0&&s.prestige==0&&s.xp==0) && completedSets==0; }
}

