package com.murilloskills.data;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class TerminalMachineTargetClientState {
    private static final List<Target> TARGETS = new ArrayList<>();
    private static int selectedIndex = -1;

    private TerminalMachineTargetClientState() {
    }

    public static void set(BlockPos pos, Direction face) {
        addOrSelect(pos, face, "");
    }

    public static void addOrSelect(BlockPos pos, Direction face, String name) {
        if (pos == null || face == null) {
            return;
        }
        BlockPos immutablePos = pos.toImmutable();
        for (int i = 0; i < TARGETS.size(); i++) {
            Target existing = TARGETS.get(i);
            if (existing.pos().equals(immutablePos)) {
                TARGETS.set(i, new Target(immutablePos, face, normalizeName(name, existing.name())));
                selectedIndex = i;
                return;
            }
        }
        TARGETS.add(new Target(immutablePos, face, normalizeName(name, "Machine")));
        selectedIndex = TARGETS.size() - 1;
    }

    public static void clear() {
        TARGETS.clear();
        selectedIndex = -1;
    }

    public static boolean hasTarget() {
        return getSelectedTarget() != null;
    }

    public static BlockPos getTargetPos() {
        Target target = getSelectedTarget();
        return target == null ? null : target.pos();
    }

    public static Direction getTargetFace() {
        Target target = getSelectedTarget();
        return target == null ? null : target.face();
    }

    public static Target getSelectedTarget() {
        if (selectedIndex < 0 || selectedIndex >= TARGETS.size()) {
            return TARGETS.isEmpty() ? null : TARGETS.get(0);
        }
        return TARGETS.get(selectedIndex);
    }

    public static int getSelectedIndex() {
        if (selectedIndex < 0 || selectedIndex >= TARGETS.size()) {
            return TARGETS.isEmpty() ? -1 : 0;
        }
        return selectedIndex;
    }

    public static void select(int index) {
        if (index >= 0 && index < TARGETS.size()) {
            selectedIndex = index;
        }
    }

    public static void remove(BlockPos pos) {
        if (pos == null) {
            return;
        }
        BlockPos immutablePos = pos.toImmutable();
        for (int i = 0; i < TARGETS.size(); i++) {
            if (!TARGETS.get(i).pos().equals(immutablePos)) {
                continue;
            }
            TARGETS.remove(i);
            if (TARGETS.isEmpty()) {
                selectedIndex = -1;
            } else if (selectedIndex >= TARGETS.size()) {
                selectedIndex = TARGETS.size() - 1;
            } else if (selectedIndex > i) {
                selectedIndex--;
            }
            return;
        }
    }

    public static List<Target> getTargetsSnapshot() {
        return List.copyOf(TARGETS);
    }

    public static int getTargetCount() {
        return TARGETS.size();
    }

    private static String normalizeName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback == null || fallback.isBlank() ? "Machine" : fallback;
        }
        return name;
    }

    public record Target(BlockPos pos, Direction face, String name) {
    }
}
