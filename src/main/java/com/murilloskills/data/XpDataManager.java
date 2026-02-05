package com.murilloskills.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.murilloskills.MurilloSkills;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class XpDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static XpCurveDefinition curve = XpCurveDefinition.defaultsFromConfig();
    private static XpValuesDefinition values = XpValuesDefinition.defaultsFromConfig();

    private XpDataManager() {
    }

    public static void reload(ResourceManager manager) {
        XpCurveDefinition defaultCurve = XpCurveDefinition.defaultsFromConfig();
        XpValuesDefinition defaultValues = XpValuesDefinition.defaultsFromConfig();

        curve = loadResource(manager, "xp_curves/default.json", XpCurveDefinition.class, defaultCurve);

        XpValuesDefinition loadedValues = new XpValuesDefinition();
        loadedValues.miner = loadResource(manager, "xp_values/miner.json", XpValuesDefinition.Miner.class,
                defaultValues.miner);
        loadedValues.warrior = loadResource(manager, "xp_values/warrior.json", XpValuesDefinition.Warrior.class,
                defaultValues.warrior);
        loadedValues.archer = loadResource(manager, "xp_values/archer.json", XpValuesDefinition.Archer.class,
                defaultValues.archer);
        loadedValues.farmer = loadResource(manager, "xp_values/farmer.json", XpValuesDefinition.Farmer.class,
                defaultValues.farmer);
        loadedValues.fisher = loadResource(manager, "xp_values/fisher.json", XpValuesDefinition.Fisher.class,
                defaultValues.fisher);
        loadedValues.blacksmith = loadResource(manager, "xp_values/blacksmith.json", XpValuesDefinition.Blacksmith.class,
                defaultValues.blacksmith);
        loadedValues.builder = loadResource(manager, "xp_values/builder.json", XpValuesDefinition.Builder.class,
                defaultValues.builder);
        loadedValues.explorer = loadResource(manager, "xp_values/explorer.json", XpValuesDefinition.Explorer.class,
                defaultValues.explorer);

        loadedValues.applyDefaults(defaultValues);
        values = loadedValues;
    }

    public static void applySync(XpCurveDefinition syncedCurve, XpValuesDefinition syncedValues) {
        if (syncedCurve != null) {
            curve = syncedCurve;
        }
        if (syncedValues != null) {
            XpValuesDefinition defaults = XpValuesDefinition.defaultsFromConfig();
            syncedValues.applyDefaults(defaults);
            values = syncedValues;
        }
    }

    public static XpCurveDefinition getCurve() {
        return curve;
    }

    public static XpValuesDefinition getValues() {
        return values;
    }

    public static String toJson(Object data) {
        return GSON.toJson(data);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    private static <T> T loadResource(ResourceManager manager, String path, Class<T> type, T fallback) {
        Identifier id = Identifier.of(MurilloSkills.MOD_ID, path);
        try {
            Optional<Resource> resource = manager.getResource(id);
            if (resource.isEmpty()) {
                return fallback;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
                T value = GSON.fromJson(reader, type);
                return value != null ? value : fallback;
            }
        } catch (Exception e) {
            MurilloSkills.LOGGER.warn("Failed to load XP data from {}", id, e);
            return fallback;
        }
    }
}
