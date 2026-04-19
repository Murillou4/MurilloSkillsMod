package com.murilloskills.skills;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UltPlaceShapeCalculatorTest {

    @Test
    void lineLeadingEdgeStartsAtOriginAndExtendsForward() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.LINE,
                1,
                4,
                Direction.UP,
                new Vec3d(1.0, 0.0, 0.0),
                Direction.EAST,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.AUTO);

        assertEquals(List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(1, 64, 0),
                new BlockPos(2, 64, 0),
                new BlockPos(3, 64, 0)), blocks);
    }

    @Test
    void planeLeadingEdgeUsesPlayerFacingAxes() {
        BlockPos origin = new BlockPos(10, 70, 10);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.PLANE_NXN,
                3,
                1,
                Direction.UP,
                new Vec3d(0.0, 0.0, -1.0),
                Direction.EAST,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.PLAYER_FACING);

        assertEquals(9, blocks.size());
        assertTrue(blocks.contains(origin));
        assertTrue(blocks.contains(new BlockPos(12, 70, 12)));
        assertFalse(blocks.contains(new BlockPos(9, 70, 9)));
    }

    @Test
    void wallTrailingEdgeBuildsBackwardAndDown() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.WALL,
                3,
                2,
                Direction.UP,
                new Vec3d(1.0, 0.0, 0.0),
                Direction.EAST,
                0,
                UltPlaceAnchorMode.TRAILING_EDGE,
                UltPlaceRotationMode.PLAYER_FACING);

        assertEquals(6, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getX() == 0));
        assertTrue(blocks.contains(new BlockPos(0, 63, -2)));
        assertTrue(blocks.contains(origin));
        assertFalse(blocks.contains(new BlockPos(0, 65, 1)));
    }

    @Test
    void stairsFaceLockedFollowClickedFaceInsteadOfLookDirection() {
        BlockPos origin = new BlockPos(3, 40, 7);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.STAIRS,
                1,
                4,
                Direction.SOUTH,
                new Vec3d(-1.0, 0.0, 0.0),
                Direction.WEST,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.FACE_LOCKED);

        assertEquals(4, blocks.size());
        assertEquals(origin, blocks.getFirst());
        assertEquals(new BlockPos(3, 43, 10), blocks.getLast());
    }

    @Test
    void tunnelPlayerFacingAlignsSlicesToFacingDirection() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.TUNNEL_3X3,
                1,
                2,
                Direction.UP,
                new Vec3d(1.0, 0.0, 0.0),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.PLAYER_FACING);

        assertEquals(16, blocks.size());
        assertTrue(blocks.contains(new BlockPos(1, 64, 0)));
        assertTrue(blocks.contains(new BlockPos(0, 65, 0)));
        assertTrue(blocks.contains(new BlockPos(-1, 63, 0)));
        assertTrue(blocks.contains(new BlockPos(1, 64, -1)));
        assertFalse(blocks.contains(origin));
    }
}
