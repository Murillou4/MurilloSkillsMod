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
        Set<BlockPos> positions = new LinkedHashSet<>();
        UltPlaceShape safeShape = shape == null ? UltPlaceShape.PLANE_NXN : shape;
        int safeSize = Math.max(1, size);
        int safeLength = Math.max(1, length);
        Direction safeFace = face == null ? Direction.UP : face;
        Vec3d safeLook = lookVec == null ? Vec3d.of(safeFace.getVector()) : lookVec;

        switch (safeShape) {
            case PLANE_NXN -> addPlanar(origin, safeFace, safeSize, safeSize, 1, positions);
            case LINE -> addLine(origin, safeLength, safeLook, safeFace, positions);
            case WALL -> addWall(origin, safeSize, safeLength, safeFace, safeLook, positions);
            case STAIRS -> addStairs(origin, safeLength, safeLook, safeFace, variant == 1, positions);
            case COLUMN -> addColumn(origin, safeLength, variant == 1, positions);
            case TUNNEL_3X3 -> addTunnel(origin, safeLength, safeLook, safeFace, positions);
            case CIRCLE -> addCircle(origin, safeSize, positions);
            case SPHERE_SHELL -> addSphereShell(origin, safeSize, positions);
            case SINGLE -> positions.add(origin.toImmutable());
        }

        return new ArrayList<>(positions);
    }

    private static void addPlanar(BlockPos origin, Direction face, int width, int height, int depth, Set<BlockPos> out) {
        IntVector axisA;
        IntVector axisB;

        if (face.getAxis() == Direction.Axis.Y) {
            axisA = new IntVector(1, 0, 0);
            axisB = new IntVector(0, 0, 1);
        } else if (face.getAxis() == Direction.Axis.X) {
            axisA = new IntVector(0, 0, 1);
            axisB = new IntVector(0, 1, 0);
        } else {
            axisA = new IntVector(1, 0, 0);
            axisB = new IntVector(0, 1, 0);
        }

        IntVector normal = new IntVector(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ());
        int[] rangeA = centeredRange(width);
        int[] rangeB = centeredRange(height);

        for (int layer = 0; layer < depth; layer++) {
            for (int a = rangeA[0]; a <= rangeA[1]; a++) {
                for (int b = rangeB[0]; b <= rangeB[1]; b++) {
                    out.add(origin.add(
                            normal.x() * layer + axisA.x() * a + axisB.x() * b,
                            normal.y() * layer + axisA.y() * a + axisB.y() * b,
                            normal.z() * layer + axisA.z() * a + axisB.z() * b));
                }
            }
        }
    }

    private static void addLine(BlockPos origin, int length, Vec3d lookVec, Direction fallback, Set<BlockPos> out) {
        Direction direction = snapToDominantDirection(lookVec, fallback);
        for (int i = 0; i < length; i++) {
            out.add(origin.offset(direction, i).toImmutable());
        }
    }

    private static void addWall(BlockPos origin, int width, int height, Direction face, Vec3d lookVec,
            Set<BlockPos> out) {
        Direction wallFace = face.getAxis().isHorizontal() ? face : snapToCardinal(lookVec, Direction.NORTH);
        addPlanar(origin, wallFace, width, height, 1, out);
    }

    private static void addStairs(BlockPos origin, int length, Vec3d lookVec, Direction face, boolean goDown,
            Set<BlockPos> out) {
        Direction forward = snapToCardinal(lookVec, face.getAxis().isHorizontal() ? face : Direction.NORTH);
        int yStep = goDown ? -1 : 1;

        for (int i = 0; i < length; i++) {
            BlockPos step = origin.offset(forward, i);
            out.add(new BlockPos(step.getX(), origin.getY() + i * yStep, step.getZ()));
        }
    }

    private static void addColumn(BlockPos origin, int length, boolean downward, Set<BlockPos> out) {
        Direction direction = downward ? Direction.DOWN : Direction.UP;
        for (int i = 0; i < length; i++) {
            out.add(origin.offset(direction, i).toImmutable());
        }
    }

    private static void addTunnel(BlockPos origin, int length, Vec3d lookVec, Direction face, Set<BlockPos> out) {
        Direction forward = snapToCardinal(lookVec, face.getAxis().isHorizontal() ? face : Direction.NORTH);
        IntVector axisA = forward.getAxis() == Direction.Axis.X
                ? new IntVector(0, 0, 1)
                : new IntVector(1, 0, 0);
        IntVector axisB = new IntVector(0, 1, 0);

        for (int i = 0; i < length; i++) {
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

    private static Direction snapToDominantDirection(Vec3d lookVec, Direction fallback) {
        double absX = Math.abs(lookVec.x);
        double absY = Math.abs(lookVec.y);
        double absZ = Math.abs(lookVec.z);

        if (absX < 1.0E-6 && absY < 1.0E-6 && absZ < 1.0E-6) {
            return fallback == null ? Direction.NORTH : fallback;
        }

        if (absY >= absX && absY >= absZ) {
            return lookVec.y >= 0.0 ? Direction.UP : Direction.DOWN;
        }
        if (absX >= absZ) {
            return lookVec.x >= 0.0 ? Direction.EAST : Direction.WEST;
        }
        return lookVec.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
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

    private static int[] centeredRange(int size) {
        int min = -(size / 2);
        int max = min + size - 1;
        return new int[] { min, max };
    }

    private record IntVector(int x, int y, int z) {
    }
}
