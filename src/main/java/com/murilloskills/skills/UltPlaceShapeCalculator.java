package com.murilloskills.skills;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared shape math for Builder UltPlace preview and execution.
 */
public final class UltPlaceShapeCalculator {

    private UltPlaceShapeCalculator() {
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltPlaceShape shape, int size, int length,
            Direction face, Vec3d lookVec, int variant) {
        return getShapeBlocks(origin, shape, size, length, face, lookVec, snapToCardinal(lookVec, Direction.NORTH),
                variant, UltPlaceAnchorMode.CENTER, UltPlaceRotationMode.AUTO, 1);
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltPlaceShape shape, int size, int length,
            int height, Direction face, Vec3d lookVec, int variant) {
        return getShapeBlocks(origin, shape, size, length, height, face, lookVec,
                snapToCardinal(lookVec, Direction.NORTH), variant, UltPlaceAnchorMode.CENTER,
                UltPlaceRotationMode.AUTO, 1);
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltPlaceShape shape, int size, int length,
            Direction face, Vec3d lookVec, Direction horizontalFacing, int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode) {
        return getShapeBlocks(origin, shape, size, length, face, lookVec, horizontalFacing, variant,
                anchorMode, rotationMode, 1);
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltPlaceShape shape, int size, int length,
            int height, Direction face, Vec3d lookVec, Direction horizontalFacing, int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode) {
        return getShapeBlocks(origin, shape, size, length, height, face, lookVec, horizontalFacing, variant,
                anchorMode, rotationMode, 1);
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltPlaceShape shape, int size, int length,
            Direction face, Vec3d lookVec, Direction horizontalFacing, int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing) {
        return getShapeBlocks(origin, shape, size, length, 1, face, lookVec, horizontalFacing, variant,
                anchorMode, rotationMode, spacing);
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltPlaceShape shape, int size, int length,
            int height, Direction face, Vec3d lookVec, Direction horizontalFacing, int variant,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        UltPlaceShape safeShape = shape == null ? UltPlaceShape.PLANE_NXN : shape;
        int safeSize = Math.max(1, size);
        int safeLength = Math.max(1, length);
        int safeHeight = Math.max(1, height);
        Direction safeFace = face == null ? Direction.UP : face;
        Vec3d safeLook = lookVec == null ? Vec3d.of(safeFace.getVector()) : lookVec;
        Direction safeHorizontalFacing = horizontalFacing != null && horizontalFacing.getAxis().isHorizontal()
                ? horizontalFacing
                : snapToCardinal(safeLook, Direction.NORTH);
        UltPlaceAnchorMode safeAnchorMode = UltPlaceAnchorMode.normalize(safeShape, anchorMode);
        UltPlaceRotationMode safeRotationMode = UltPlaceRotationMode.normalize(safeShape, rotationMode);
        int safeSpacing = safeShape.supportsSpacing() ? Math.max(1, spacing) : 1;

        switch (safeShape) {
            case PLANE_NXN -> addPlane(origin, safeFace, safeSize, safeHorizontalFacing, safeAnchorMode,
                    safeRotationMode, safeSpacing, positions);
            case HORIZONTAL_BOX -> addHorizontalBox(origin, safeSize, safeHeight, safeLength, safeFace, safeLook,
                    safeHorizontalFacing, safeAnchorMode, safeRotationMode, safeSpacing, positions);
            case LINE -> addLine(origin, safeLength, safeLook, safeFace, safeHorizontalFacing, safeAnchorMode,
                    safeRotationMode, safeSpacing, positions);
            case WALL -> addWall(origin, safeSize, safeLength, safeFace, safeLook, safeHorizontalFacing,
                    safeAnchorMode, safeRotationMode, safeSpacing, positions);
            case STAIRS -> addStairs(origin, safeLength, safeLook, safeFace, safeHorizontalFacing, variant == 1,
                    safeAnchorMode, safeRotationMode, positions);
            case COLUMN -> addColumn(origin, safeLength, variant == 1, safeAnchorMode, safeSpacing, positions);
            case TUNNEL_3X3 -> addTunnel(origin, safeLength, safeLook, safeFace, safeHorizontalFacing,
                    safeAnchorMode, safeRotationMode, positions);
            case CIRCLE -> addCircle(origin, safeSize, positions);
            case SPHERE_SHELL -> addSphereShell(origin, safeSize, positions);
            case SINGLE -> positions.add(origin.toImmutable());
        }

        return new ArrayList<>(positions);
    }

    private static void addPlane(BlockPos origin, Direction face, int size, Direction horizontalFacing,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing, Set<BlockPos> out) {
        PlaneAxes axes = resolvePlaneAxes(face, horizontalFacing, rotationMode);
        addPlanar(origin, axes.axisA(), axes.axisB(), axes.normal(), size, size, 1, anchorMode, anchorMode,
                spacing, out);
    }

    private static void addHorizontalBox(BlockPos origin, int width, int height, int length, Direction face,
            Vec3d lookVec, Direction horizontalFacing, UltPlaceAnchorMode anchorMode,
            UltPlaceRotationMode rotationMode, int spacing, Set<BlockPos> out) {
        Direction forward = resolveHorizontalDirection(lookVec, face, horizontalFacing, rotationMode);
        Direction right = rotateClockwise(forward);
        IntVector vertical = face == Direction.DOWN ? new IntVector(0, -1, 0) : new IntVector(0, 1, 0);
        addPlanar(origin, vector(right), vector(forward), vertical, width, length, height, anchorMode, anchorMode,
                spacing, out);
    }

    private static void addPlanar(BlockPos origin, IntVector axisA, IntVector axisB, IntVector normal, int width,
            int height, int depth, UltPlaceAnchorMode axisAMode, UltPlaceAnchorMode axisBMode, int spacing,
            Set<BlockPos> out) {
        int[] rangeA = anchoredRange(width, axisAMode);
        int[] rangeB = anchoredRange(height, axisBMode);
        int safeSpacing = Math.max(1, spacing);

        for (int layer = 0; layer < depth; layer++) {
            for (int a = rangeA[0]; a <= rangeA[1]; a++) {
                if (Math.floorMod(a, safeSpacing) != 0) {
                    continue;
                }
                for (int b = rangeB[0]; b <= rangeB[1]; b++) {
                    if (Math.floorMod(b, safeSpacing) != 0) {
                        continue;
                    }
                    out.add(origin.add(
                            normal.x() * layer + axisA.x() * a + axisB.x() * b,
                            normal.y() * layer + axisA.y() * a + axisB.y() * b,
                            normal.z() * layer + axisA.z() * a + axisB.z() * b));
                }
            }
        }
    }

    private static void addLine(BlockPos origin, int length, Vec3d lookVec, Direction face, Direction horizontalFacing,
            UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode, int spacing, Set<BlockPos> out) {
        Direction direction = resolveLineDirection(lookVec, face, horizontalFacing, rotationMode);
        int[] range = anchoredRange(length, anchorMode);
        int safeSpacing = Math.max(1, spacing);
        for (int i = range[0]; i <= range[1]; i++) {
            if (Math.floorMod(i, safeSpacing) != 0) {
                continue;
            }
            out.add(origin.offset(direction, i).toImmutable());
        }
    }

    private static void addWall(BlockPos origin, int width, int height, Direction face, Vec3d lookVec,
            Direction horizontalFacing, UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode,
            int spacing, Set<BlockPos> out) {
        Direction wallFace = resolveWallFace(face, lookVec, horizontalFacing, rotationMode);
        PlaneAxes axes = resolvePlaneAxes(wallFace, horizontalFacing, rotationMode);
        addPlanar(origin, axes.axisA(), axes.axisB(), axes.normal(), width, height, 1, anchorMode, anchorMode,
                spacing, out);
    }

    private static void addStairs(BlockPos origin, int length, Vec3d lookVec, Direction face,
            Direction horizontalFacing, boolean goDown, UltPlaceAnchorMode anchorMode,
            UltPlaceRotationMode rotationMode, Set<BlockPos> out) {
        Direction forward = resolveHorizontalDirection(lookVec, face, horizontalFacing, rotationMode);
        int yStep = goDown ? -1 : 1;
        int[] range = anchoredRange(length, anchorMode);

        for (int i = range[0]; i <= range[1]; i++) {
            BlockPos step = origin.offset(forward, i);
            out.add(new BlockPos(step.getX(), origin.getY() + i * yStep, step.getZ()));
        }
    }

    private static void addColumn(BlockPos origin, int length, boolean downward, UltPlaceAnchorMode anchorMode,
            int spacing, Set<BlockPos> out) {
        Direction direction = downward ? Direction.DOWN : Direction.UP;
        int[] range = anchoredRange(length, anchorMode);
        int safeSpacing = Math.max(1, spacing);
        for (int i = range[0]; i <= range[1]; i++) {
            if (Math.floorMod(i, safeSpacing) != 0) {
                continue;
            }
            out.add(origin.offset(direction, i).toImmutable());
        }
    }

    private static void addTunnel(BlockPos origin, int length, Vec3d lookVec, Direction face,
            Direction horizontalFacing, UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode,
            Set<BlockPos> out) {
        Direction forward = resolveHorizontalDirection(lookVec, face, horizontalFacing, rotationMode);
        IntVector axisA = forward.getAxis() == Direction.Axis.X
                ? new IntVector(0, 0, 1)
                : new IntVector(1, 0, 0);
        IntVector axisB = new IntVector(0, 1, 0);
        int[] range = anchoredRange(length, anchorMode);

        for (int i = range[0]; i <= range[1]; i++) {
            BlockPos center = origin.offset(forward, i);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    out.add(center.add(
                            axisA.x() * a + axisB.x() * b,
                            axisA.y() * a + axisB.y() * b,
                            axisA.z() * a + axisB.z() * b));
                }
            }
        }
    }

    private static void addCircle(BlockPos origin, int diameter, Set<BlockPos> out) {
        double radius = Math.max(0.0, (diameter - 1) / 2.0);
        int bound = (int) Math.ceil(radius);

        for (int dx = -bound; dx <= bound; dx++) {
            for (int dz = -bound; dz <= bound; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance <= radius + 0.25) {
                    out.add(origin.add(dx, 0, dz).toImmutable());
                }
            }
        }
    }

    private static void addSphereShell(BlockPos origin, int diameter, Set<BlockPos> out) {
        double radius = Math.max(0.0, (diameter - 1) / 2.0);
        int bound = (int) Math.ceil(radius);
        double innerRadius = Math.max(0.0, radius - 1.0);

        for (int dx = -bound; dx <= bound; dx++) {
            for (int dy = -bound; dy <= bound; dy++) {
                for (int dz = -bound; dz <= bound; dz++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance <= radius + 0.25 && distance >= Math.max(0.0, innerRadius - 0.25)) {
                        out.add(origin.add(dx, dy, dz).toImmutable());
                    }
                }
            }
        }
    }

    private static Direction resolveLineDirection(Vec3d lookVec, Direction face, Direction horizontalFacing,
            UltPlaceRotationMode rotationMode) {
        return switch (rotationMode) {
            case FACE_LOCKED -> face == null ? Direction.NORTH : face;
            case PLAYER_FACING -> horizontalFacing == null ? Direction.NORTH : horizontalFacing;
            case AUTO -> resolveAutoLineDirection(lookVec, face, horizontalFacing);
        };
    }

    private static Direction resolveAutoLineDirection(Vec3d lookVec, Direction face, Direction horizontalFacing) {
        // A linha sempre se estende paralela à face clicada (ao longo da superfície),
        // nunca atravessando o bloco em que se está colocando.
        Direction.Axis excludeAxis = face != null ? face.getAxis() : null;
        Vec3d safeLook = lookVec == null ? Vec3d.ZERO : lookVec;
        Direction best = null;
        double bestDot = -Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (excludeAxis != null && direction.getAxis() == excludeAxis) {
                continue;
            }
            double dot = direction.getOffsetX() * safeLook.x
                    + direction.getOffsetY() * safeLook.y
                    + direction.getOffsetZ() * safeLook.z;
            if (dot > bestDot) {
                bestDot = dot;
                best = direction;
            }
        }
        if (best == null || bestDot <= 1.0E-6) {
            // Olhar perfeitamente alinhado com a normal da face (ou nulo) — usa o facing horizontal do jogador.
            Direction fallback = horizontalFacing != null && horizontalFacing.getAxis().isHorizontal()
                    ? horizontalFacing
                    : Direction.NORTH;
            if (excludeAxis != null && fallback.getAxis() == excludeAxis) {
                return rotateClockwise(fallback);
            }
            return fallback;
        }
        return best;
    }

    private static Direction resolveHorizontalDirection(Vec3d lookVec, Direction face, Direction horizontalFacing,
            UltPlaceRotationMode rotationMode) {
        return switch (rotationMode) {
            case FACE_LOCKED -> face != null && face.getAxis().isHorizontal()
                    ? face
                    : horizontalFacing == null ? Direction.NORTH : horizontalFacing;
            case PLAYER_FACING -> horizontalFacing == null ? Direction.NORTH : horizontalFacing;
            case AUTO -> snapToCardinal(lookVec, face != null && face.getAxis().isHorizontal() ? face : horizontalFacing);
        };
    }

    private static Direction resolveWallFace(Direction face, Vec3d lookVec, Direction horizontalFacing,
            UltPlaceRotationMode rotationMode) {
        if (face != null && face.getAxis().isHorizontal()) {
            return face;
        }
        return resolveHorizontalDirection(lookVec, face, horizontalFacing, rotationMode);
    }

    private static PlaneAxes resolvePlaneAxes(Direction face, Direction horizontalFacing,
            UltPlaceRotationMode rotationMode) {
        Direction safeFace = face == null ? Direction.UP : face;
        IntVector normal = vector(safeFace);

        if (safeFace.getAxis() == Direction.Axis.Y) {
            Direction forward = rotationMode == UltPlaceRotationMode.FACE_LOCKED
                    ? Direction.NORTH
                    : horizontalFacing == null ? Direction.NORTH : horizontalFacing;
            Direction right = rotateClockwise(forward);
            return new PlaneAxes(vector(right), vector(forward), normal);
        }

        if (safeFace.getAxis() == Direction.Axis.X) {
            return new PlaneAxes(new IntVector(0, 0, 1), new IntVector(0, 1, 0), normal);
        }
        return new PlaneAxes(new IntVector(1, 0, 0), new IntVector(0, 1, 0), normal);
    }

    private static Direction rotateClockwise(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private static IntVector vector(Direction direction) {
        Direction safe = direction == null ? Direction.NORTH : direction;
        return new IntVector(safe.getOffsetX(), safe.getOffsetY(), safe.getOffsetZ());
    }

    private static Direction snapToCardinal(Vec3d lookVec, Direction fallback) {
        double absX = Math.abs(lookVec.x);
        double absZ = Math.abs(lookVec.z);

        if (absX < 1.0E-6 && absZ < 1.0E-6) {
            return fallback != null && fallback.getAxis().isHorizontal() ? fallback : Direction.NORTH;
        }

        if (absX >= absZ) {
            return lookVec.x >= 0.0 ? Direction.EAST : Direction.WEST;
        }
        return lookVec.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int[] anchoredRange(int size, UltPlaceAnchorMode anchorMode) {
        int safeSize = Math.max(1, size);
        return switch (anchorMode) {
            case LEADING_EDGE -> new int[] { 0, safeSize - 1 };
            case TRAILING_EDGE -> new int[] { -(safeSize - 1), 0 };
            case CENTER -> centeredRange(safeSize);
        };
    }

    private static int[] centeredRange(int size) {
        int min = -(size / 2);
        int max = min + size - 1;
        return new int[] { min, max };
    }

    private record PlaneAxes(IntVector axisA, IntVector axisB, IntVector normal) {
    }

    private record IntVector(int x, int y, int z) {
    }
}
