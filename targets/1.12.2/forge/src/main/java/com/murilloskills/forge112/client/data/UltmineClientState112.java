package com.murilloskills.forge112.client.data;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UltmineClientState112 {
    private static boolean held;
    private static int previewBlocks;
    private static List<BlockPos> preview = new ArrayList<BlockPos>();
    private static String lastResult = "";
    private static long lastResultAt;

    private UltmineClientState112() {
    }

    public static boolean isHeld() {
        return held;
    }

    public static void setHeld(boolean value) {
        held = value;
    }

    public static int getPreviewBlocks() {
        return previewBlocks;
    }

    public static void setPreviewBlocks(int value) {
        previewBlocks = Math.max(0, value);
    }

    public static List<BlockPos> getPreview() {
        return Collections.unmodifiableList(preview);
    }

    public static void setPreview(List<BlockPos> values) {
        preview = new ArrayList<BlockPos>();
        if (values != null) {
            for (BlockPos pos : values) {
                if (pos != null) {
                    preview.add(pos.toImmutable());
                }
            }
        }
        previewBlocks = preview.size();
    }

    public static void clearPreview() {
        preview = new ArrayList<BlockPos>();
        previewBlocks = 0;
    }

    public static String getLastResult() {
        return lastResult;
    }

    public static long getLastResultAt() {
        return lastResultAt;
    }

    public static void setLastResult(String value) {
        lastResult = value == null ? "" : value;
        lastResultAt = System.currentTimeMillis();
    }
}
