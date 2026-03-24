package com.murilloskills.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.murilloskills.skills.UltmineShape;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Client-side configuration for Ultmine preferences.
 * Stores per-shape depth/length/variant and global toggles.
 */
public class UltmineClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "murilloskills_ultmine.json";

    private static UltmineData data;

    public static class UltmineData {
        public boolean dropsToInventory = true;
        public boolean sameBlockOnly = false;
        public boolean xpDirectToPlayer = false;
        public Map<String, ShapePrefs> shapePrefs = new java.util.HashMap<>();

        public UltmineData() {
            for (UltmineShape shape : UltmineShape.values()) {
                shapePrefs.put(shape.name(), new ShapePrefs());
            }
        }
    }

    public static class ShapePrefs {
        public int depth = -1;   // -1 = use server default
        public int length = -1;  // -1 = use server default
        public int variant = 0;

        public ShapePrefs() {}
    }

    public static void load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                data = GSON.fromJson(json, UltmineData.class);
                if (data == null) data = new UltmineData();
                // Ensure all shapes have entries
                for (UltmineShape shape : UltmineShape.values()) {
                    data.shapePrefs.putIfAbsent(shape.name(), new ShapePrefs());
                }
            } catch (IOException e) {
                data = new UltmineData();
                save();
            }
        } else {
            data = new UltmineData();
            save();
        }
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

    public static UltmineData get() {
        if (data == null) load();
        return data;
    }

    // --- Global toggles ---

    public static boolean isDropsToInventory() {
        return get().dropsToInventory;
    }

    public static void setDropsToInventory(boolean value) {
        get().dropsToInventory = value;
    }

    public static void toggleDropsToInventory() {
        get().dropsToInventory = !get().dropsToInventory;
    }

    public static boolean isSameBlockOnly() {
        return get().sameBlockOnly;
    }

    public static void setSameBlockOnly(boolean value) {
        get().sameBlockOnly = value;
    }

    public static void toggleSameBlockOnly() {
        get().sameBlockOnly = !get().sameBlockOnly;
    }

    public static boolean isXpDirectToPlayer() {
        return get().xpDirectToPlayer;
    }

    public static void setXpDirectToPlayer(boolean value) {
        get().xpDirectToPlayer = value;
    }

    public static void toggleXpDirectToPlayer() {
        get().xpDirectToPlayer = !get().xpDirectToPlayer;
    }

    // --- Per-shape prefs ---

    private static ShapePrefs getPrefs(UltmineShape shape) {
        return get().shapePrefs.computeIfAbsent(shape.name(), k -> new ShapePrefs());
    }

    public static int getShapeDepth(UltmineShape shape) {
        return getPrefs(shape).depth;
    }

    public static void setShapeDepth(UltmineShape shape, int depth) {
        getPrefs(shape).depth = depth;
    }

    public static int getShapeLength(UltmineShape shape) {
        return getPrefs(shape).length;
    }

    public static void setShapeLength(UltmineShape shape, int length) {
        getPrefs(shape).length = length;
    }

    public static int getShapeVariant(UltmineShape shape) {
        int v = getPrefs(shape).variant;
        return Math.max(0, Math.min(v, UltmineShape.getVariantCount(shape) - 1));
    }

    public static void setShapeVariant(UltmineShape shape, int variant) {
        getPrefs(shape).variant = Math.max(0, Math.min(variant, UltmineShape.getVariantCount(shape) - 1));
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}
