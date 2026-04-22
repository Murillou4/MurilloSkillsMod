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
        public boolean magnetEnabled = false;
        public int magnetRange = 8;
        public java.util.List<String> trashItems = new java.util.ArrayList<>();
        public java.util.List<String> legacyBlockedBlocks = new java.util.ArrayList<>();
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
                if (data.trashItems == null) data.trashItems = new java.util.ArrayList<>();
                if (data.legacyBlockedBlocks == null) data.legacyBlockedBlocks = new java.util.ArrayList<>();
                if (data.shapePrefs == null) data.shapePrefs = new java.util.HashMap<>();
                java.util.List<String> normalizedBlockedBlocks = new java.util.ArrayList<>();
                for (String blockId : data.legacyBlockedBlocks) {
                    String normalized = normalizeResourceId(blockId);
                    if (!normalized.isEmpty() && !normalizedBlockedBlocks.contains(normalized)) {
                        normalizedBlockedBlocks.add(normalized);
                    }
                }
                data.legacyBlockedBlocks = normalizedBlockedBlocks;
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

    // --- Magnet ---

    public static boolean isMagnetEnabled() {
        return get().magnetEnabled;
    }

    public static void setMagnetEnabled(boolean value) {
        get().magnetEnabled = value;
    }

    public static void toggleMagnet() {
        get().magnetEnabled = !get().magnetEnabled;
    }

    public static int getMagnetRange() {
        return get().magnetRange;
    }

    public static void setMagnetRange(int range) {
        get().magnetRange = Math.max(1, Math.min(range, 32));
    }

    // --- Trash ---

    public static java.util.List<String> getTrashItems() {
        return get().trashItems;
    }

    public static void addTrashItem(String itemId) {
        if (!get().trashItems.contains(itemId)) {
            get().trashItems.add(itemId);
        }
    }

    public static void removeTrashItem(String itemId) {
        get().trashItems.remove(itemId);
    }

    public static boolean isTrashItem(String itemId) {
        return get().trashItems.contains(itemId);
    }

    // --- Legacy Classic block lock ---

    public static java.util.List<String> getLegacyBlockedBlocks() {
        return get().legacyBlockedBlocks;
    }

    public static void addLegacyBlockedBlock(String blockId) {
        String normalized = normalizeResourceId(blockId);
        if (!normalized.isEmpty() && !get().legacyBlockedBlocks.contains(normalized)) {
            get().legacyBlockedBlocks.add(normalized);
        }
    }

    public static void removeLegacyBlockedBlock(String blockId) {
        String normalized = normalizeResourceId(blockId);
        if (!normalized.isEmpty()) {
            get().legacyBlockedBlocks.remove(normalized);
        }
    }

    public static boolean isLegacyBlockedBlock(String blockId) {
        String normalized = normalizeResourceId(blockId);
        return !normalized.isEmpty() && get().legacyBlockedBlocks.contains(normalized);
    }

    private static String normalizeResourceId(String id) {
        if (id == null) {
            return "";
        }
        String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        return normalized;
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
