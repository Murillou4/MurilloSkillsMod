package com.murilloskills.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.MinerScanResultPayload.OreType;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.OreFilterLimits;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Client-side configuration for ore filter preferences.
 * Stores which ores to show and custom colors.
 */
public class OreFilterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "murilloskills_ore_filter.json";
    private static final List<OreFilterOption> VANILLA_OPTIONS = List.of(
            vanillaOption(OreType.COAL, "Coal"),
            vanillaOption(OreType.COPPER, "Copper"),
            vanillaOption(OreType.IRON, "Iron"),
            vanillaOption(OreType.GOLD, "Gold"),
            vanillaOption(OreType.LAPIS, "Lapis Lazuli"),
            vanillaOption(OreType.REDSTONE, "Redstone"),
            vanillaOption(OreType.DIAMOND, "Diamond"),
            vanillaOption(OreType.EMERALD, "Emerald"),
            vanillaOption(OreType.ANCIENT_DEBRIS, "Netherite"),
            vanillaOption(OreType.NETHER_QUARTZ, "Quartz"),
            vanillaOption(OreType.NETHER_GOLD, "Nether Gold"));

    private static OreFilterData data;

    public enum DisplayMode {
        XRAY // Show through walls
    }

    public static class OreFilterData {
        public Map<String, OreSettings> oreSettings = new java.util.HashMap<>();
        public Map<String, String> oreDisplayNames = new java.util.HashMap<>();

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

            for (OreFilterOption option : VANILLA_OPTIONS) {
                oreDisplayNames.put(option.key(), option.displayName());
            }
        }
    }

    public record OreFilterOption(String key, String displayName, int color, boolean vanilla) {
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
        if (data.oreDisplayNames == null) {
            data.oreDisplayNames = new java.util.HashMap<>();
        }

        OreFilterData defaults = new OreFilterData();
        for (Map.Entry<String, OreSettings> entry : defaults.oreSettings.entrySet()) {
            data.oreSettings.putIfAbsent(entry.getKey(), entry.getValue());
        }
        data.oreSettings.remove("OTHER");

        for (OreFilterOption option : VANILLA_OPTIONS) {
            data.oreDisplayNames.put(option.key(), option.displayName());
        }

        if (enableOtherOres) {
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
        return isOreEnabled(type.name());
    }

    public static boolean isOreEnabled(MinerScanResultPayload.OreEntry entry) {
        rememberOre(entry);
        return isOreEnabled(entry.filterKey());
    }

    public static boolean isOreEnabled(String key) {
        OreSettings settings = get().oreSettings.get(key);
        return settings != null && settings.enabled;
    }

    public static void setOreEnabled(OreType type, boolean enabled) {
        setOreEnabled(type.name(), enabled);
    }

    public static void setOreEnabled(String key, boolean enabled) {
        OreSettings settings = get().oreSettings.get(key);
        if (settings != null) {
            settings.enabled = enabled;
        }
    }

    public static void toggleOre(OreType type) {
        toggleOre(type.name());
    }

    public static void toggleOre(String key) {
        setOreEnabled(key, !isOreEnabled(key));
    }

    public static void rememberScannedOres(List<MinerScanResultPayload.OreEntry> ores) {
        if (ores == null || ores.isEmpty()) {
            return;
        }

        for (MinerScanResultPayload.OreEntry ore : ores) {
            rememberOre(ore);
        }
    }

    /**
     * Walks the block registry once on world join and registers any modded ore (block id ending in
     * {@code _ore} or equal to {@code ancient_debris}) so it shows up in the filter screen even before
     * the player triggers a Miner scan in range of it.
     */
    public static void discoverModdedOres() {
        OreFilterData current = get();
        boolean changed = false;
        for (Identifier id : Registries.BLOCK.getIds()) {
            if (id == null) {
                continue;
            }
            String key = id.toString();
            if (isVanillaKey(key) || isVanillaOreId(id) || !MinerXpGetter.isLikelyOreId(key)) {
                continue;
            }
            if (!current.oreSettings.containsKey(key)) {
                current.oreSettings.put(key,
                        new OreSettings(current.moddedOresEnabledByDefault, generateModdedOreColor(key)));
                changed = true;
            }
            current.oreDisplayNames.putIfAbsent(key, MinerXpGetter.humanizeModdedOreName(key));
        }
        if (changed) {
            save();
        }
    }

    private static boolean isVanillaOreId(Identifier id) {
        return "minecraft".equals(id.getNamespace());
    }

    private static int generateModdedOreColor(String blockId) {
        int[] colors = {
                0xFF00D1FF, 0xFFFF6B6B, 0xFFFFD166, 0xFF8BFF7A,
                0xFFB088FF, 0xFFFF8AD8, 0xFF78E08F, 0xFFFF9F43,
                0xFF70A1FF, 0xFF7BED9F, 0xFFECCC68, 0xFFEA8685
        };
        int index = Math.floorMod(blockId == null ? 0 : blockId.hashCode(), colors.length);
        return colors[index];
    }

    public static List<OreFilterOption> getFilterOptions() {
        OreFilterData current = get();
        List<OreFilterOption> options = new ArrayList<>(VANILLA_OPTIONS);

        current.oreSettings.entrySet().stream()
                .filter(entry -> !isVanillaKey(entry.getKey()))
                .sorted(Comparator.comparing(entry -> getOreDisplayName(entry.getKey())))
                .forEach(entry -> options.add(new OreFilterOption(
                        entry.getKey(),
                        getOreDisplayName(entry.getKey()),
                        entry.getValue().customColor,
                        false)));

        return options;
    }

    public static String getOreDisplayName(String key) {
        return get().oreDisplayNames.getOrDefault(key, key);
    }

    public static int getOreColor(String key, int fallbackColor) {
        OreSettings settings = get().oreSettings.get(key);
        if (settings == null || settings.customColor < 0) {
            return fallbackColor;
        }
        return settings.customColor;
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

    private static void rememberOre(MinerScanResultPayload.OreEntry entry) {
        if (entry == null) {
            return;
        }

        String key = entry.filterKey();
        if (key == null || key.isBlank() || "OTHER".equals(key)) {
            return;
        }

        OreFilterData current = get();
        current.oreSettings.putIfAbsent(key, new OreSettings(true, entry.color()));
        current.oreDisplayNames.putIfAbsent(key, entry.displayName());
    }

    private static boolean isVanillaKey(String key) {
        for (OreFilterOption option : VANILLA_OPTIONS) {
            if (option.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static OreFilterOption vanillaOption(OreType type, String displayName) {
        return new OreFilterOption(type.name(), displayName, type.color, true);
    }
}
