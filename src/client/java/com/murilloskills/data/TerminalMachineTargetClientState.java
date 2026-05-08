package com.murilloskills.data;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class TerminalMachineTargetClientState {
    private static BlockPos targetPos;
    private static Direction targetFace;

    private TerminalMachineTargetClientState() {
    }

    public static void set(BlockPos pos, Direction face) {
        targetPos = pos == null ? null : pos.toImmutable();
        targetFace = face;
    }

    public static void clear() {
        targetPos = null;
        targetFace = null;
    }

    public static boolean hasTarget() {
        return targetPos != null && targetFace != null;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }

    public static Direction getTargetFace() {
        return targetFace;
    }
}
