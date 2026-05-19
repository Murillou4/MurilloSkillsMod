package com.murilloskills.forge112.client.data;

public final class TerminalMachineTargetClientState112 {
    private static String targetName = "";
    private static int amount;
    private static long expiresAt;

    private TerminalMachineTargetClientState112() {
    }

    public static void setTarget(String name, int count, long durationMs) {
        targetName = name == null ? "" : name;
        amount = Math.max(0, count);
        expiresAt = System.currentTimeMillis() + Math.max(0L, durationMs);
    }

    public static void clear() {
        targetName = "";
        amount = 0;
        expiresAt = 0L;
    }

    public static boolean isActive() {
        return targetName.length() > 0 && System.currentTimeMillis() <= expiresAt;
    }

    public static String getTargetName() {
        return targetName;
    }

    public static int getAmount() {
        return amount;
    }
}
