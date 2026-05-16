package com.murilloskills.core.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlayerSkillJsonCodec {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PlayerSkillJsonCodec() {
    }

    public static void write(Path path, PlayerSkillDataCore data) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(fromCore(data), writer);
        }
    }

    public static PlayerSkillDataCore read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return toCore(GSON.fromJson(reader, SaveFile.class));
        }
    }

    public static SaveFile fromCore(PlayerSkillDataCore data) {
        data.normalizeParagonState();
        SaveFile saveFile = new SaveFile();
        saveFile.schemaVersion = CURRENT_SCHEMA_VERSION;
        SkillType activeParagon = data.getActiveParagonSkill();
        saveFile.activeParagonSkill = activeParagon == null ? null : activeParagon.name();

        for (SkillType skill : data.getParagonSkills()) {
            saveFile.paragonSkills.add(skill.name());
        }
        for (SkillType skill : data.getSelectedSkills()) {
            saveFile.selectedSkills.add(skill.name());
        }
        for (Map.Entry<SkillType, SkillStatsCore> entry : data.mutableSkills().entrySet()) {
            saveFile.skills.put(entry.getKey().name(), SkillEntry.from(entry.getValue()));
        }
        saveFile.skillToggles.putAll(data.getSkillToggles());
        saveFile.achievementStats.putAll(data.getAchievementStats());
        saveFile.extensions.putAll(data.getExtensions());
        return saveFile;
    }

    public static PlayerSkillDataCore toCore(SaveFile saveFile) {
        PlayerSkillDataCore data = new PlayerSkillDataCore();
        if (saveFile == null) {
            return data;
        }

        for (Map.Entry<String, SkillEntry> entry : safeSkills(saveFile).entrySet()) {
            SkillType skill = parseSkill(entry.getKey());
            if (skill != null && entry.getValue() != null) {
                SkillEntry value = entry.getValue();
                data.setSkill(skill, value.level, value.xp, value.lastAbilityUse, value.prestige);
            }
        }

        List<SkillType> selected = new ArrayList<SkillType>();
        for (String name : safeList(saveFile.selectedSkills)) {
            SkillType skill = parseSkill(name);
            if (skill != null) {
                selected.add(skill);
            }
        }
        data.setSelectedSkillsDirect(selected);

        List<SkillType> paragons = new ArrayList<SkillType>();
        for (String name : safeList(saveFile.paragonSkills)) {
            SkillType skill = parseSkill(name);
            if (skill != null) {
                paragons.add(skill);
            }
        }
        data.setParagonSkillsDirect(new java.util.HashSet<SkillType>(paragons));
        data.setActiveParagonSkill(parseSkill(saveFile.activeParagonSkill));
        data.setSkillToggles(saveFile.skillToggles);
        data.setAchievementStats(saveFile.achievementStats);
        data.setExtensions(saveFile.extensions);
        data.normalizeParagonState();
        return data;
    }

    private static SkillType parseSkill(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return SkillType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Map<String, SkillEntry> safeSkills(SaveFile saveFile) {
        return saveFile.skills == null ? new HashMap<String, SkillEntry>() : saveFile.skills;
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? new ArrayList<String>() : values;
    }

    public static final class SaveFile {
        public int schemaVersion;
        public String activeParagonSkill;
        public List<String> paragonSkills = new ArrayList<String>();
        public List<String> selectedSkills = new ArrayList<String>();
        public Map<String, SkillEntry> skills = new HashMap<String, SkillEntry>();
        public Map<String, Boolean> skillToggles = new HashMap<String, Boolean>();
        public Map<String, Integer> achievementStats = new HashMap<String, Integer>();
        public Map<String, com.google.gson.JsonElement> extensions = new HashMap<String, com.google.gson.JsonElement>();
    }

    public static final class SkillEntry {
        public int level;
        public double xp;
        public long lastAbilityUse;
        public int prestige;

        static SkillEntry from(SkillStatsCore stats) {
            SkillEntry entry = new SkillEntry();
            entry.level = stats.getLevel();
            entry.xp = stats.getXp();
            entry.lastAbilityUse = stats.getLastAbilityUse();
            entry.prestige = stats.getPrestige();
            return entry;
        }
    }
}
