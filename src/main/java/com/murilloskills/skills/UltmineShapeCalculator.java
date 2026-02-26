package com.murilloskills.skills;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure shape calculator used by both preview and server execution.
 */
public final class UltmineShapeCalculator {

    private UltmineShapeCalculator() {
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltmineShape shape, int depth, int length,
            Direction face) {
        return getShapeBlocks(origin, shape, depth, length, face, Vec3d.of(face.getVector()));
    }

    public static List<BlockPos> getShapeBlocks(BlockPos origin, UltmineShape shape, int depth, int length,
            Direction face, Vec3d lookVector) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        int safeDepth = Math.max(1, depth);
        int safeLength = Math.max(1, length);

        switch (shape) {
            case S_3x3 -> addPlanar(origin, face, 3, 3, safeDepth, positions);
            case R_2x1 -> addPlanar(origin, face, 2, 1, safeDepth, positions);
            case LEGACY -> positions.add(origin);
            case LINE -> addLine(origin, safeLength, face, lookVector, positions);
            case STAIRS -> addStairs(origin, safeDepth, face, lookVector, positions);
            case SQUARE_20x20_D1 -> addHorizontalSquare(origin, 20, safeDepth, positions);
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

        IntVector normal = new IntVector(-face.getOffsetX(), -face.getOffsetY(), -face.getOffsetZ());
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

    private static void addLine(BlockPos origin, int length, Direction face, Vec3d lookVector, Set<BlockPos> out) {
        Vec3d direction = safeDirection(lookVector, Vec3d.of(face.getVector()));
        out.addAll(traceRayBlocks(origin, direction, length));
    }

    private static void addStairs(BlockPos origin, int depth, Direction face, Vec3d lookVector, Set<BlockPos> out) {
        Vec3d fallback = new Vec3d(face.getOffsetX(), 0.0, face.getOffsetZ());
        Vec3d horizontal = safeDirection(new Vec3d(lookVector.x, 0.0, lookVector.z), fallback);
        List<BlockPos> path = traceRayBlocks(origin, new Vec3d(horizontal.x, 0.0, horizontal.z), depth);
        for (int i = 0; i < path.size(); i++) {
            BlockPos step = path.get(i);
            out.add(new BlockPos(step.getX(), origin.getY() + i, step.getZ()));
        }
    }

    private static void addHorizontalSquare(BlockPos origin, int size, int depth, Set<BlockPos> out) {
        int[] range = centeredRange(size);
        for (int layer = 0; layer < depth; layer++) {
            int y = origin.getY() - layer;
            for (int dx = range[0]; dx <= range[1]; dx++) {
                for (int dz = range[0]; dz <= range[1]; dz++) {
                    out.add(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz));
                }
            }
        }
    }

    private static int[] centeredRange(int size) {
        int min = -(size / 2);
        int max = min + size - 1;
        return new int[] { min, max };
    }

    private static Vec3d safeDirection(Vec3d vector, Vec3d fallback) {
        if (vector.lengthSquared() > 1.0E-6) {
            return vector.normalize();
        }
        if (fallback.lengthSquared() > 1.0E-6) {
            return fallback.normalize();
        }
        return new Vec3d(1.0, 0.0, 0.0);
    }

    /**
     * Voxel traversal (Amanatides-Woo) to guarantee exactly {@code length} unique
     * blocks in ray direction.
     */
    private static List<BlockPos> traceRayBlocks(BlockPos origin, Vec3d direction, int length) {
        List<BlockPos> result = new ArrayList<>(length);
        Vec3d dir = safeDirection(direction, new Vec3d(1.0, 0.0, 0.0));

        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        result.add(origin);
        if (length == 1) {
            return result;
        }

        int stepX = (int) Math.signum(dir.x);
        int stepY = (int) Math.signum(dir.y);
        int stepZ = (int) Math.signum(dir.z);

        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.y);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z);

        double centerX = x + 0.5;
        double centerY = y + 0.5;
        double centerZ = z + 0.5;

        double nextBoundaryX = stepX > 0 ? (x + 1.0) : x;
        double nextBoundaryY = stepY > 0 ? (y + 1.0) : y;
        double nextBoundaryZ = stepZ > 0 ? (z + 1.0) : z;

        double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs((nextBoundaryX - centerX) / dir.x);
        double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs((nextBoundaryY - centerY) / dir.y);
        double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs((nextBoundaryZ - centerZ) / dir.z);

        while (result.size() < length) {
            double min = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));

            if (Double.compare(tMaxX, min) == 0) {
                x += stepX;
                tMaxX += tDeltaX;
            }
            if (Double.compare(tMaxY, min) == 0) {
                y += stepY;
                tMaxY += tDeltaY;
            }
            if (Double.compare(tMaxZ, min) == 0) {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }

            result.add(new BlockPos(x, y, z));
        }

        return result;
    }

    private record IntVector(int x, int y, int z) {
    }
}
