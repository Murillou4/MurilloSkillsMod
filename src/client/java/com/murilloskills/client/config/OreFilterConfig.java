package com.murilloskills.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.murilloskills.network.MinerScanResultPayload.OreType;
import com.murilloskills.utils.OreFilterLimits;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Client-side configuration for ore filter preferences.
 * Stores which ores to show and custom colors.
 */
public class OreFilterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "murilloskills_ore_filter.json";

    private static OreFilterData data;

    public enum DisplayMode {
        XRAY // Show through walls
    }

    public static class OreFilterData {
        public Map<String, OreSettings> oreSettings = new java.util.HashMap<>();

        public DisplayMode displayMode = DisplayMode.XRAY;
        public int maxOres = OreFilterLimits.DEFAULT_ORES; // Max ores to display
        public boolean prioritizeRare = true; // Show diamond/emerald/debris first
        public boolean moddedOresEnabledByDefault = true;

        public OreFilterData() {
            // Initialize with defaults
            oreSettings.put("COAL", new OreSettings(true, 0x333333));
            oreSettings.put("COPPER", new OreSettings(true, 0xE87B35));
            oreSettings.put("IRON", new OreSettings(true, 0xD8AF93));
            oreSettings.put("GOLD", new OreSettings(true, 0xFFD700));
            oreSettings.put("LAPIS", new OreSettings(true, 0x2626CC));
            oreSettings.put("REDSTONE", new OreSettings(true, 0xFF0000));
            oreSettings.put("DIAMOND", new OreSettings(true, 0x4AEDD9));
            oreSettings.put("EMERALD", new OreSettings(true, 0x00FF00));
            oreSettings.put("ANCIENT_DEBRIS", new OreSettings(true, 0x7B4F3A));
            oreSettings.put("NETHER_QUARTZ", new OreSettings(true, 0xE8E4D8));
            oreSettings.put("NETHER_GOLD", new OreSettings(true, 0xFFD700));
            oreSettings.put("OTHER", new OreSettings(true, 0x888888));
        }
    }

    public static class OreSettings {
        public boolean enabled;
        public int customColor; // -1 means use default color

        public OreSettings() {
            this.enabled = true;
            this.customColor = -1;
        }

        public OreSettings(boolean enabled, int defaultColor) {
            this.enabled = enabled;
            this.customColor = defaultColor;
        }
    }

    public static void load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                boolean needsModdedOreMigration = !json.contains("\"moddedOresEnabledByDefault\"");
                data = GSON.fromJson(json, OreFilterData.class);
                if (data == null) {
                    data = createDefaultData();
                }
                normalizeLoadedData(needsModdedOreMigration);
                if (needsModdedOreMigration) {
                    save();
                }
            } catch (IOException e) {
                data = createDefaultData();
                save();
            }
        } else {
            data = createDefaultData();
            save();
        }
    }

    private static OreFilterData createDefaultData() {
        OreFilterData defaults = new OreFilterData();
        defaults.displayMode = DisplayMode.XRAY;
        defaults.maxOres = OreFilterLimits.DEFAULT_ORES;
        return defaults;
    }

    private static void normalizeLoadedData(boolean enableOtherOres) {
        if (data.oreSettings == null) {
            data.oreSettings = new java.util.HashMap<>();
        }

        OreFilterData defaults = new OreFilterData();
        for (Map.Entry<String, OreSettings> entry : defaults.oreSettings.entrySet()) {
            data.oreSettings.putIfAbsent(entry.getKey(), entry.getValue());
        }

        if (enableOtherOres) {
            data.oreSettings.put("OTHER", new OreSettings(true, 0x888888));
            data.moddedOresEnabledByDefault = true;
        }
        data.displayMode = DisplayMode.XRAY;
        data.maxOres = OreFilterLimits.clampMaxOres(data.maxOres);
    }

    public static void save() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(data));
        } catch (IOException e) {
            // Silent fail for client config
        }
    }

    public static OreFilterData get() {
        if (data == null) {
            load();
        }
        return data;
    }

    public static boolean isOreEnabled(OreType type) {
        OreSettings settings = get().oreSettings.get(type.name());
        return settings != null && settings.enabled;
    }

    public static void setOreEnabled(OreType type, boolean enabled) {
        OreSettings settings = get().oreSettings.get(type.name());
        if (settings != null) {
            settings.enabled = enabled;
        }
    }

    public static void toggleOre(OreType type) {
        setOreEnabled(type, !isOreEnabled(type));
    }

    public static DisplayMode getDisplayMode() {
        return DisplayMode.XRAY;
    }

    public static void setDisplayMode(DisplayMode mode) {
        get().displayMode = DisplayMode.XRAY;
    }

    public static int getMaxOres() {
        return OreFilterLimits.clampMaxOres(get().maxOres);
    }

    public static void setMaxOres(int max) {
        get().maxOres = OreFilterLimits.clampMaxOres(max);
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}
