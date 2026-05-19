package com.murilloskills.forge112.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.XpAddResult;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.murilloskills.forge112.MurilloSkillsForge112.CONFIG;
import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;
import static com.murilloskills.forge112.MurilloSkillsForge112.STORE;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;

public final class Forge112DailyChallengeManager {
    private static final String EXTENSION = "dailyChallenges112";
    private static final int CHALLENGE_COUNT = 3;

    private Forge112DailyChallengeManager() {
    }

    public static List<Challenge> ensure(EntityPlayer player) {
        PlayerSkillDataCore data = data(player);
        long day = day(player);
        JsonObject root = root(data);
        if (!root.has("day") || root.get("day").getAsLong() != day || !root.has("challenges")) {
            root = generate(player, data, day);
            data.getExtensions().put(EXTENSION, root);
            STORE.save(player.getUniqueID());
        }
        return read(root);
    }

    public static void sync(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        List<Challenge> challenges = ensure(player);
        for (Challenge challenge : challenges) {
            Forge112Notifications.challenge(player, challenge.skill, challenge.label, challenge.progress,
                    challenge.target, challenge.reward);
        }
    }

    public static void record(EntityPlayer player, SkillType skill, String action, int amount) {
        if (player == null || player.world == null || player.world.isRemote || skill == null || amount <= 0) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        if (!data.isSkillSelected(skill)) {
            return;
        }
        long day = day(player);
        JsonObject root = root(data);
        if (!root.has("day") || root.get("day").getAsLong() != day || !root.has("challenges")) {
            root = generate(player, data, day);
        }
        JsonArray array = root.getAsJsonArray("challenges");
        boolean changed = false;
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject challenge = element.getAsJsonObject();
            SkillType challengeSkill = parseSkill(challenge.get("skill").getAsString());
            if (challengeSkill != skill || bool(challenge, "completed")) {
                continue;
            }
            int progress = intValue(challenge, "progress", 0) + amount;
            int target = intValue(challenge, "target", 1);
            challenge.addProperty("progress", Math.min(progress, target));
            changed = true;
            if (progress >= target) {
                challenge.addProperty("completed", true);
                int reward = intValue(challenge, "reward", 100);
                XpAddResult result = data.addXpToSkill(skill, reward, CONFIG);
                Forge112Notifications.challenge(player, skill, labelFor(skill), target, target, reward);
                Forge112Notifications.xp(player, skill, reward, "daily challenge");
                if (result.isLeveledUp()) {
                    Forge112Notifications.levelUp(player, skill, result.getOldLevel(), result.getNewLevel());
                }
                LOG.info("[MurilloSkills][1.12.2][Daily] {} completed {} via {}", player.getName(), skill, action);
            } else if (progress == 1 || progress % 5 == 0 || progress + amount >= target) {
                Forge112Notifications.challenge(player, skill, labelFor(skill), Math.min(progress, target), target,
                        intValue(challenge, "reward", 100));
            }
        }
        if (changed) {
            data.getExtensions().put(EXTENSION, root);
            STORE.save(player.getUniqueID());
        }
    }

    public static List<Challenge> view(PlayerSkillDataCore data, long day) {
        if (data == null) {
            return new ArrayList<Challenge>();
        }
        JsonObject root = root(data);
        if (!root.has("day") || root.get("day").getAsLong() != day || !root.has("challenges")) {
            return new ArrayList<Challenge>();
        }
        return read(root);
    }

    private static JsonObject generate(EntityPlayer player, PlayerSkillDataCore data, long day) {
        JsonObject root = new JsonObject();
        root.addProperty("day", day);
        JsonArray array = new JsonArray();
        List<SkillType> pool = new ArrayList<SkillType>();
        if (data.getSelectedSkills().isEmpty()) {
            for (SkillType skill : SkillType.values()) {
                pool.add(skill);
            }
        } else {
            pool.addAll(data.getSelectedSkills());
        }
        Random random = new Random(player.getUniqueID().getLeastSignificantBits() ^ day * 31L);
        Set<SkillType> used = EnumSet.noneOf(SkillType.class);
        while (!pool.isEmpty() && array.size() < CHALLENGE_COUNT) {
            SkillType skill = pool.remove(random.nextInt(pool.size()));
            if (!used.add(skill)) {
                continue;
            }
            JsonObject challenge = new JsonObject();
            challenge.addProperty("skill", skill.name());
            challenge.addProperty("label", labelFor(skill));
            challenge.addProperty("target", targetFor(data, skill));
            challenge.addProperty("progress", 0);
            challenge.addProperty("reward", rewardFor(data, skill));
            challenge.addProperty("completed", false);
            array.add(challenge);
        }
        root.add("challenges", array);
        return root;
    }

    private static JsonObject root(PlayerSkillDataCore data) {
        JsonElement element = data.getExtensions().get(EXTENSION);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static List<Challenge> read(JsonObject root) {
        List<Challenge> challenges = new ArrayList<Challenge>();
        if (!root.has("challenges") || !root.get("challenges").isJsonArray()) {
            return challenges;
        }
        for (JsonElement element : root.getAsJsonArray("challenges")) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            SkillType skill = parseSkill(stringValue(object, "skill", ""));
            if (skill == null) {
                continue;
            }
            challenges.add(new Challenge(skill, stringValue(object, "label", labelFor(skill)),
                    intValue(object, "target", 1), intValue(object, "progress", 0),
                    bool(object, "completed"), intValue(object, "reward", 100)));
        }
        return challenges;
    }

    private static long day(EntityPlayer player) {
        return player == null || player.world == null ? 0L : player.world.getTotalWorldTime() / 24000L;
    }

    private static int targetFor(PlayerSkillDataCore data, SkillType skill) {
        int level = data.getSkill(skill).getLevel();
        switch (skill) {
            case MINER: return 18 + level / 8;
            case BUILDER: return 20 + level / 8;
            case WARRIOR: return 8 + level / 14;
            case ARCHER: return 6 + level / 14;
            case FARMER: return 16 + level / 9;
            case FISHER: return 5 + level / 16;
            case BLACKSMITH: return 8 + level / 12;
            case EXPLORER: return 6 + level / 12;
            default: return 10;
        }
    }

    private static int rewardFor(PlayerSkillDataCore data, SkillType skill) {
        return 120 + data.getSkill(skill).getLevel() * 4 + data.getSkill(skill).getPrestige() * 40;
    }

    private static String labelFor(SkillType skill) {
        switch (skill) {
            case MINER: return "Mine blocks";
            case BUILDER: return "Place blocks";
            case WARRIOR: return "Defeat mobs";
            case ARCHER: return "Land shots";
            case FARMER: return "Harvest crops";
            case FISHER: return "Catch fish";
            case BLACKSMITH: return "Craft or smelt";
            case EXPLORER: return "Explore";
            default: return "Progress";
        }
    }

    private static SkillType parseSkill(String value) {
        try {
            return value == null ? null : SkillType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String stringValue(JsonObject object, String name, String fallback) {
        return object.has(name) ? object.get(name).getAsString() : fallback;
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        try {
            return object.has(name) ? object.get(name).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String name) {
        try {
            return object.has(name) && object.get(name).getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static final class Challenge {
        public final SkillType skill;
        public final String label;
        public final int target;
        public final int progress;
        public final boolean completed;
        public final int reward;

        private Challenge(SkillType skill, String label, int target, int progress, boolean completed, int reward) {
            this.skill = skill;
            this.label = label;
            this.target = target;
            this.progress = progress;
            this.completed = completed;
            this.reward = reward;
        }
    }
}
