package com.murilloskills.skills;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UltmineShapeCalculatorTest {

    @Test
    void shape3x3OnHorizontalSurfaceReturnsNineBlocks() {
        BlockPos origin = new BlockPos(0, 64, 0);
        List<BlockPos> blocks = UltmineShapeCalculator.getShapeBlocks(origin, UltmineShape.S_3x3, 1, 1, Direction.UP);

        assertEquals(9, blocks.size());
        assertTrue(blocks.contains(new BlockPos(-1, 64, -1)));
        assertTrue(blocks.contains(new BlockPos(1, 64, 1)));
        assertTrue(blocks.contains(origin));
    }

    @Test
    void shape3x3OnWallUsesVerticalPlane() {
        BlockPos origin = new BlockPos(0, 64, 0);
        List<BlockPos> blocks = UltmineShapeCalculator.getShapeBlocks(origin, UltmineShape.S_3x3, 1, 1, Direction.EAST);

        assertEquals(9, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getX() == 0));
    }

    @Test
    void lineUsesExactLengthOnDiagonalLook() {
        BlockPos origin = new BlockPos(0, 64, 0);
        List<BlockPos> blocks = UltmineShapeCalculator.getShapeBlocks(
                origin, UltmineShape.LINE, 1, 8, Direction.NORTH, new Vec3d(1.0, 0.0, 1.0));

        assertEquals(8, blocks.size());
        assertEquals(new BlockPos(7, 64, 7), blocks.get(7));
    }

    @Test
    void stairsDecreaseHeightByOnePerStep() {
        BlockPos origin = new BlockPos(10, 40, 10);
        List<BlockPos> blocks = UltmineShapeCalculator.getShapeBlocks(
                origin, UltmineShape.STAIRS, 5, 1, Direction.NORTH, new Vec3d(0.0, 0.0, -1.0));

        assertEquals(5, blocks.size());
        assertEquals(new BlockPos(10, 40, 10), blocks.getFirst());
        assertEquals(new BlockPos(10, 39, 9), blocks.get(1));
        assertEquals(new BlockPos(10, 36, 6), blocks.get(4));
    }

    @Test
    void square20x20Depth1ReturnsFourHundredBlocks() {
        BlockPos origin = new BlockPos(0, 70, 0);
        List<BlockPos> blocks = UltmineShapeCalculator.getShapeBlocks(
                origin, UltmineShape.SQUARE_20x20_D1, 1, 1, Direction.UP);

        assertEquals(400, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getY() == 70));
    }
}
