package com.murilloskills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murilloskills.skills.MurilloSkillsList;

import java.util.*;

public class PlayerSkillData {
    public static final int MAX_SELECTED_SKILLS = 3;

    public EnumMap<MurilloSkillsList, SkillStats> skills = new EnumMap<>(MurilloSkillsList.class);
    public MurilloSkillsList paragonSkill = null; // The one skill allowed to reach 100
    public List<MurilloSkillsList> selectedSkills = new ArrayList<>(); // The 2 main skills chosen by player

    // Persistent toggles for skill features (key format: "skillName.toggleName")
    // Examples: "EXPLORER.nightVision", "EXPLORER.stepAssist",
    // "FARMER.areaPlanting"
    public Map<String, Boolean> skillToggles = new HashMap<>();

    // Achievement tracking stats (key = achievement stat name, value = count)
    public Map<String, Integer> achievementStats = new HashMap<>();

    public PlayerSkillData() {
        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            skills.put(skill, new SkillStats(0, 0.0, -1, 0)); // -1 = nunca usou, 0 = sem prestige
        }
    }

    public PlayerSkillData(MurilloSkillsList paragonSkill, EnumMap<MurilloSkillsList, SkillStats> skills,
            List<MurilloSkillsList> selectedSkills) {
        this.paragonSkill = paragonSkill;
        this.skills = skills;
        this.selectedSkills = selectedSkills != null ? new ArrayList<>(selectedSkills) : new ArrayList<>();
    }

    /**
     * Check if player has selected their main skills
     */
    public boolean hasSelectedSkills() {
        return selectedSkills != null && selectedSkills.size() == MAX_SELECTED_SKILLS;
    }

    /**
     * Check if a skill is one of the selected main skills
     */
    public boolean isSkillSelected(MurilloSkillsList skill) {
        return selectedSkills != null && selectedSkills.contains(skill);
    }

    /**
     * Set the selected skills (1 to 3). Returns true if successful.
     * Appends to existing selection if allowed.
     */
    public boolean setSelectedSkills(List<MurilloSkillsList> skills) {
        if (skills == null || skills.isEmpty()) {
            return false;
        }

        // Calculate potential total
        Set<MurilloSkillsList> potentialSelection = new HashSet<>(selectedSkills);
        potentialSelection.addAll(skills);

        if (potentialSelection.size() > MAX_SELECTED_SKILLS) {
            return false;
        }

        // Apply Update
        this.selectedSkills = new ArrayList<>(potentialSelection);
        return true;
    }

    /**
     * Get the list of selected skills (unmodifiable)
     */
    public List<MurilloSkillsList> getSelectedSkills() {
        return Collections.unmodifiableList(selectedSkills);
    }

    /**
     * Gets a toggle value for a skill feature. Returns the default value if not
     * set.
     *
     * @param skill        The skill enum
     * @param toggleName   The toggle name (e.g., "nightVision", "stepAssist")
     * @param defaultValue The default value if toggle is not set
     */
    public boolean getToggle(MurilloSkillsList skill, String toggleName, boolean defaultValue) {
        String key = skill.name() + "." + toggleName;
        return skillToggles.getOrDefault(key, defaultValue);
    }

    /**
     * Sets a toggle value for a skill feature.
     *
     * @param skill      The skill enum
     * @param toggleName The toggle name (e.g., "nightVision", "stepAssist")
     * @param value      The toggle value
     */
    public void setToggle(MurilloSkillsList skill, String toggleName, boolean value) {
        String key = skill.name() + "." + toggleName;
        skillToggles.put(key, value);
    }

    /**
     * Central logic for adding XP and handling Paragon System constraints.
     *
     * @return XpAddResult containing level transition info for milestone detection
     */
    public XpAddResult addXpToSkill(MurilloSkillsList skill, int amount) {
        // RESTRICTION: If player hasn't selected ANY skills yet, block all XP
        if (selectedSkills.isEmpty()) {
            return XpAddResult.NO_CHANGE;
        }

        // RESTRICTION: If skill is not in selectedSkills, block XP
        if (!isSkillSelected(skill)) {
            return XpAddResult.NO_CHANGE;
        }

        SkillStats stats = skills.get(skill);

        // Apply prestige XP bonus multiplier
        float xpMultiplier = com.murilloskills.utils.PrestigeManager.getXpMultiplier(stats.prestige);
        int adjustedAmount = Math.round(amount * xpMultiplier);

        int maxLevelAllowed = com.murilloskills.utils.SkillConfig.getMaxLevel() - 1; // Default cap at 99
        // Nível 100 só é permitido se paragonSkill estiver definido E for igual à skill
        // atual
        // Jogador deve travar no 99 até ir no menu e selecionar o Paragon
        if (paragonSkill != null && paragonSkill == skill) {
            maxLevelAllowed = com.murilloskills.utils.SkillConfig.getMaxLevel();
        }

        return stats.addXp(adjustedAmount, maxLevelAllowed);
    }

    public void setSkill(MurilloSkillsList skill, int level, double xp) {
        setSkill(skill, level, xp, -1); // -1 = nunca usou a habilidade
    }

    public void setSkill(MurilloSkillsList skill, int level, double xp, long lastAbilityUse) {
        setSkill(skill, level, xp, lastAbilityUse, 0);
    }

    public void setSkill(MurilloSkillsList skill, int level, double xp, long lastAbilityUse, int prestige) {
        skills.put(skill, new SkillStats(level, xp, lastAbilityUse, prestige));
    }

    public SkillStats getSkill(MurilloSkillsList skill) {
        return skills.get(skill);
    }

    // Updated Codec to include all fields with proper error handling for migration
    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("paragonSkill")
                    .forGetter(d -> Optional.ofNullable(d.paragonSkill).map(Enum::name)),
            Codec.unboundedMap(Codec.STRING, SkillStats.CODEC).optionalFieldOf("skills", Map.of()).forGetter(d -> {
                Map<String, SkillStats> map = new HashMap<>();
                d.skills.forEach((k, v) -> map.put(k.name(), v));
                return map;
            }),
            Codec.STRING.listOf().optionalFieldOf("selectedSkills", List.of())
                    .forGetter(d -> d.selectedSkills.stream().map(Enum::name).toList()),
            Codec.unboundedMap(Codec.STRING, Codec.BOOL).optionalFieldOf("skillToggles", Map.of())
                    .forGetter(d -> d.skillToggles),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("achievementStats", Map.of())
                    .forGetter(d -> d.achievementStats))
            .apply(instance, (paragonOpt, skillsMap, selectedList, toggles, achStats) -> {
                PlayerSkillData data = new PlayerSkillData();

                // Parse paragon skill (safe)
                data.paragonSkill = paragonOpt.flatMap(name -> {
                    try {
                        return Optional.of(MurilloSkillsList.valueOf(name));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).orElse(null);

                // Parse skills map (safe)
                skillsMap.forEach((k, v) -> {
                    try {
                        data.skills.put(MurilloSkillsList.valueOf(k), v);
                    } catch (Exception ignored) {
                    }
                });

                // Ensure all enums are present
                for (MurilloSkillsList s : MurilloSkillsList.values()) {
                    data.skills.putIfAbsent(s, new SkillStats(0, 0, -1, 0));
                }

                // Parse selectedSkills (safe)
                for (String name : selectedList) {
                    try {
                        data.selectedSkills.add(MurilloSkillsList.valueOf(name));
                    } catch (Exception ignored) {
                    }
                }

                // Copy toggles and achievement stats
                data.skillToggles = new HashMap<>(toggles);
                data.achievementStats = new HashMap<>(achStats);

                return data;
            }));

    public static class SkillStats {
        public int level;
        public double xp;
        public long lastAbilityUse; // Timestamp do último uso da habilidade (World Time)
        public int prestige; // Nível de prestígio (0-10)

        public SkillStats(int level, double xp) {
            this(level, xp, -1, 0); // -1 = nunca usou, 0 = sem prestige
        }

        public SkillStats(int level, double xp, long lastAbilityUse) {
            this(level, xp, lastAbilityUse, 0);
        }

        public SkillStats(int level, double xp, long lastAbilityUse, int prestige) {
            this.level = level;
            this.xp = xp;
            this.lastAbilityUse = lastAbilityUse;
            this.prestige = prestige;
        }

        public static final Codec<SkillStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("level").forGetter(s -> s.level),
                Codec.DOUBLE.fieldOf("xp").forGetter(s -> s.xp),
                Codec.LONG.optionalFieldOf("lastAbilityUse", 0L).forGetter(s -> s.lastAbilityUse),
                Codec.INT.optionalFieldOf("prestige", 0).forGetter(s -> s.prestige))
                .apply(instance, SkillStats::new));

        // Logic moved to respect dynamic cap
        public XpAddResult addXp(int amount, int maxLevelAllowed) {
            int oldLevel = this.level;

            if (this.level >= maxLevelAllowed)
                return XpAddResult.NO_CHANGE;

            this.xp += amount;
            boolean leveledUp = false;

            while (this.xp >= getXpNeededForNextLevel() && this.level < maxLevelAllowed) {
                this.xp -= getXpNeededForNextLevel();
                this.level++;
                leveledUp = true;
            }

            // Cap XP if max level reached
            if (this.level >= maxLevelAllowed) {
                this.xp = 0; // Or keep it as "Maxed"
            }

            return new XpAddResult(leveledUp, oldLevel, this.level);
        }

        private int getXpNeededForNextLevel() {
            // Formula: Base + (Level * Multiplier) + (Exponent * Level^2)
            // This is the balanced curve - less aggressive than the old formula
            return com.murilloskills.utils.SkillConfig.getXpForLevel(this.level);
        }
    }

    /**
     * Result record for XP addition operations.
     * Contains information about level transitions for milestone detection.
     */
    public record XpAddResult(boolean leveledUp, int oldLevel, int newLevel) {
        /** Constant for when no XP change occurred */
        public static final XpAddResult NO_CHANGE = new XpAddResult(false, 0, 0);
    }
}
