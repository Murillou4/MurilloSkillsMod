package com.murilloskills.forge112.client.data;

public final class UltmineClientState112 {
    private static boolean held;
    private static int previewBlocks;
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
