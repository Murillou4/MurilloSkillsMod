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
    void lineAutoOnFloorStaysHorizontalEvenWhenLookingDown() {
        // Regressão: olhando ~70° pra baixo no chão, a LINE não pode descer
        // verticalmente para fora da superfície — precisa correr ao longo da face clicada.
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.LINE,
                1,
                4,
                Direction.UP,
                new Vec3d(0.0, -0.94, -0.34),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.AUTO);

        assertTrue(blocks.stream().allMatch(pos -> pos.getY() == 64),
                "linha em chão deve permanecer no Y do origin, não atravessá-lo");
        assertEquals(List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(0, 64, -1),
                new BlockPos(0, 64, -2),
                new BlockPos(0, 64, -3)), blocks);
    }

    @Test
    void lineAutoOnCeilingExtendsAlongCeilingNotUpward() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.LINE,
                1,
                3,
                Direction.DOWN,
                new Vec3d(0.0, 0.94, 0.34),
                Direction.SOUTH,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.AUTO);

        assertTrue(blocks.stream().allMatch(pos -> pos.getY() == 64));
        assertEquals(List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(0, 64, 1),
                new BlockPos(0, 64, 2)), blocks);
    }

    @Test
    void planeWithSpacingTwoPlacesEveryOtherBlock() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.PLANE_NXN,
                5,
                1,
                Direction.UP,
                new Vec3d(0.0, 0.0, -1.0),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.CENTER,
                UltPlaceRotationMode.AUTO,
                2);

        // 5x5 com spacing 2 e center anchor → range [-2, 2] em ambos eixos,
        // só índices pares: -2, 0, 2 → grid 3x3 = 9 blocos espaçados.
        assertEquals(9, blocks.size());
        assertTrue(blocks.contains(origin));
        assertTrue(blocks.contains(new BlockPos(2, 64, -2)));
        assertTrue(blocks.contains(new BlockPos(-2, 64, 2)));
        assertFalse(blocks.contains(new BlockPos(1, 64, 0)));
        assertFalse(blocks.contains(new BlockPos(0, 64, -1)));
    }

    @Test
    void wallWithSpacingMatchesTrapdoorPattern() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.WALL,
                5,
                5,
                Direction.SOUTH,
                new Vec3d(0.0, 0.0, 1.0),
                Direction.SOUTH,
                0,
                UltPlaceAnchorMode.CENTER,
                UltPlaceRotationMode.AUTO,
                2);

        // wall 5x5 (face SOUTH, axisA=X, axisB=Y, normal=Z) com spacing 2
        // → 9 blocos coplanares em z = origin.z.
        assertEquals(9, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getZ() == 0));
        assertTrue(blocks.contains(origin));
        assertTrue(blocks.contains(new BlockPos(2, 66, 0)));
        assertFalse(blocks.contains(new BlockPos(0, 65, 0)));
    }

    @Test
    void wallAutoBuildsInFrontOfPlayerEvenWhenClickedSideIsHorizontal() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.WALL,
                3,
                3,
                Direction.EAST,
                new Vec3d(0.0, 0.0, -1.0),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.CENTER,
                UltPlaceRotationMode.AUTO);

        assertEquals(9, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getZ() == 0),
                "wall AUTO deve ficar no plano frontal do jogador, não no plano lateral da face clicada");
        assertTrue(blocks.contains(new BlockPos(-1, 63, 0)));
        assertTrue(blocks.contains(new BlockPos(1, 65, 0)));
        assertFalse(blocks.contains(new BlockPos(0, 64, -1)));
    }

    @Test
    void wallFaceLockedStillUsesClickedHorizontalFace() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.WALL,
                3,
                3,
                Direction.EAST,
                new Vec3d(0.0, 0.0, -1.0),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.CENTER,
                UltPlaceRotationMode.FACE_LOCKED);

        assertEquals(9, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getX() == 0));
        assertTrue(blocks.contains(new BlockPos(0, 63, -1)));
        assertTrue(blocks.contains(new BlockPos(0, 65, 1)));
        assertFalse(blocks.contains(new BlockPos(-1, 64, 0)));
    }

    @Test
    void lineWithSpacingSkipsBetweenBlocks() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.LINE,
                1,
                8,
                Direction.UP,
                new Vec3d(0.0, 0.0, -1.0),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.AUTO,
                3);

        // length 8 leading edge → range [0, 7], spacing 3 → 0, 3, 6 = 3 blocos.
        assertEquals(3, blocks.size());
        assertEquals(origin, blocks.get(0));
        assertEquals(new BlockPos(0, 64, -3), blocks.get(1));
        assertEquals(new BlockPos(0, 64, -6), blocks.get(2));
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
    void horizontalBoxUsesWidthHeightLengthOnHorizontalPlane() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.HORIZONTAL_BOX,
                3,
                4,
                2,
                Direction.UP,
                new Vec3d(0.0, 0.0, -1.0),
                Direction.NORTH,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.PLAYER_FACING);

        assertEquals(24, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getY() == 64 || pos.getY() == 65));
        assertTrue(blocks.contains(origin));
        assertTrue(blocks.contains(new BlockPos(2, 65, -3)));
        assertFalse(blocks.contains(new BlockPos(-1, 64, 0)));
        assertFalse(blocks.contains(new BlockPos(0, 64, 1)));
    }

    @Test
    void horizontalBoxOnCeilingBuildsDownward() {
        BlockPos origin = new BlockPos(0, 64, 0);

        List<BlockPos> blocks = UltPlaceShapeCalculator.getShapeBlocks(
                origin,
                UltPlaceShape.HORIZONTAL_BOX,
                1,
                2,
                3,
                Direction.DOWN,
                new Vec3d(0.0, 0.0, 1.0),
                Direction.SOUTH,
                0,
                UltPlaceAnchorMode.LEADING_EDGE,
                UltPlaceRotationMode.PLAYER_FACING);

        assertEquals(6, blocks.size());
        assertTrue(blocks.stream().allMatch(pos -> pos.getY() >= 62 && pos.getY() <= 64));
        assertTrue(blocks.contains(origin));
        assertTrue(blocks.contains(new BlockPos(0, 62, 1)));
        assertFalse(blocks.contains(new BlockPos(0, 65, 0)));
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
