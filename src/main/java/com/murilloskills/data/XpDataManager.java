package com.murilloskills.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.murilloskills.MurilloSkills;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.entity.EntityType;
import net.minecraft.block.Block;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads XP values and level curve settings from data packs.
 *
 * Expected files:
 * - data/murilloskills/xp_values/<skill>.json
 * - data/murilloskills/level_curve.json
 *
 * XP values are keyed by block/entity identifiers or tag identifiers.
 */
public final class XpDataManager {
    private static final Gson GSON = new Gson();
    private static final String XP_VALUES_PATH = "xp_values";
    private static final String LEVEL_CURVE_PATH = "level_curve.json";

    private static final Map<String, XpValues> XP_VALUES_BY_SKILL = new HashMap<>();
    private static LevelCurve levelCurveOverride = null;

    private XpDataManager() {
    }

    public static void registerReloadListener() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ReloadListener());
    }

    public static LevelCurve getLevelCurveOverride() {
        return levelCurveOverride;
    }

    public static XpValues getValuesForSkill(String skill) {
        return XP_VALUES_BY_SKILL.get(skill.toLowerCase());
    }

    public static int getBlockXp(String skill, Block block) {
        XpValues values = getValuesForSkill(skill);
        if (values == null) {
            return 0;
        }
        Identifier id = Registries.BLOCK.getId(block);
        Integer direct = values.blockIdXp.get(id.toString());
        if (direct != null) {
            return direct;
        }
        for (TagEntry<Block> entry : values.blockTagXp) {
            if (block.getRegistryEntry().isIn(entry.tag)) {
                return entry.xp;
            }
        }
        return 0;
    }

    public static int getEntityXp(String skill, EntityType<?> entityType) {
        XpValues values = getValuesForSkill(skill);
        if (values == null) {
            return 0;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(entityType);
        Integer direct = values.entityIdXp.get(id.toString());
        if (direct != null) {
            return direct;
        }
        for (TagEntry<EntityType<?>> entry : values.entityTagXp) {
            if (entityType.isIn(entry.tag)) {
                return entry.xp;
            }
        }
        return 0;
    }

    private static void clear() {
        XP_VALUES_BY_SKILL.clear();
        levelCurveOverride = null;
    }

    private static final class ReloadListener implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return Identifier.of(MurilloSkills.MOD_ID, "xp_data_loader");
        }

        @Override
        public void reload(ResourceManager manager) {
            clear();
            loadLevelCurve(manager);
            loadXpValues(manager);
        }

        private void loadLevelCurve(ResourceManager manager) {
            Identifier id = Identifier.of(MurilloSkills.MOD_ID, LEVEL_CURVE_PATH);
            manager.getResource(id).ifPresent(resource -> {
                try (BufferedReader reader = resource.getReader()) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json == null) {
                        return;
                    }
                    int base = json.has("base") ? json.get("base").getAsInt() : 60;
                    int multiplier = json.has("multiplier") ? json.get("multiplier").getAsInt() : 15;
                    int exponent = json.has("exponent") ? json.get("exponent").getAsInt() : 2;
                    levelCurveOverride = new LevelCurve(base, multiplier, exponent);
                    MurilloSkills.LOGGER.info("Loaded XP level curve override from data pack.");
                } catch (IOException | JsonSyntaxException e) {
                    MurilloSkills.LOGGER.warn("Failed to load level curve data: {}", id, e);
                }
            });
        }

        private void loadXpValues(ResourceManager manager) {
            Map<Identifier, Resource> resources = manager.findResources(XP_VALUES_PATH,
                    path -> path.getPath().endsWith(".json"));

            for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                Identifier id = entry.getKey();
                String fileName = id.getPath().substring(XP_VALUES_PATH.length() + 1);
                if (!fileName.endsWith(".json")) {
                    continue;
                }
                String skillName = fileName.substring(0, fileName.length() - 5);
                try (BufferedReader reader = entry.getValue().getReader()) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json == null) {
                        continue;
                    }
                    XpValues values = parseXpValues(json);
                    XP_VALUES_BY_SKILL.put(skillName.toLowerCase(), values);
                    MurilloSkills.LOGGER.info("Loaded XP values for skill {} from {}", skillName, id);
                } catch (IOException | JsonSyntaxException e) {
                    MurilloSkills.LOGGER.warn("Failed to load XP values from {}", id, e);
                }
            }
        }

        private XpValues parseXpValues(JsonObject json) {
            XpValues values = new XpValues();
            values.blockIdXp.putAll(parseIntMap(json, "block_ids"));
            values.entityIdXp.putAll(parseIntMap(json, "entity_ids"));
            values.blockTagXp.addAll(parseTagMap(json, "block_tags", RegistryKeys.BLOCK));
            values.entityTagXp.addAll(parseTagMap(json, "entity_tags", RegistryKeys.ENTITY_TYPE));
            return values;
        }

        private Map<String, Integer> parseIntMap(JsonObject json, String key) {
            Map<String, Integer> map = new HashMap<>();
            if (!json.has(key) || !json.get(key).isJsonObject()) {
                return map;
            }
            JsonObject obj = json.getAsJsonObject(key);
            for (String entryKey : obj.keySet()) {
                map.put(entryKey, obj.get(entryKey).getAsInt());
            }
            return map;
        }

        private <T> List<TagEntry<T>> parseTagMap(JsonObject json, String key, RegistryKey<Registry<T>> registryKey) {
            List<TagEntry<T>> list = new ArrayList<>();
            if (!json.has(key) || !json.get(key).isJsonObject()) {
                return list;
            }
            JsonObject obj = json.getAsJsonObject(key);
            for (String entryKey : obj.keySet()) {
                Identifier id = Identifier.tryParse(entryKey);
                if (id == null) {
                    continue;
                }
                TagKey<T> tagKey = TagKey.of(registryKey, id);
                list.add(new TagEntry<>(tagKey, obj.get(entryKey).getAsInt()));
            }
            return list;
        }
    }

    public record LevelCurve(int base, int multiplier, int exponent) {
    }

    public static final class XpValues {
        private final Map<String, Integer> blockIdXp = new HashMap<>();
        private final Map<String, Integer> entityIdXp = new HashMap<>();
        private final List<TagEntry<Block>> blockTagXp = new ArrayList<>();
        private final List<TagEntry<EntityType<?>>> entityTagXp = new ArrayList<>();
    }

    private record TagEntry<T>(TagKey<T> tag, int xp) {
    }
}
