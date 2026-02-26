package com.murilloskills.data;

import com.murilloskills.skills.UltmineShape;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side cache for Ultmine selection and latest preview.
 */
public final class UltmineClientState {
    private static UltmineShape selectedShape = UltmineShape.S_3x3;
    private static int depth = 1;
    private static int length = SkillConfig.getUltmineLineLengthDefault();
    private static List<BlockPos> preview = List.of();

    private UltmineClientState() {
    }

    public static UltmineShape getSelectedShape() {
        return selectedShape;
    }

    public static int getDepth() {
        return depth;
    }

    public static int getLength() {
        return length;
    }

    public static void setSelection(UltmineShape shape, int newDepth, int newLength) {
        selectedShape = shape == null ? UltmineShape.S_3x3 : shape;
        depth = Math.max(1, newDepth);
        length = Math.max(1, newLength);
    }

    public static void applyShapeDefaults(UltmineShape shape) {
        int newDepth = switch (shape) {
            case STAIRS -> SkillConfig.getUltmineStairsDepthDefault();
            case SQUARE_20x20_D1, S_3x3, R_2x1, LINE -> shape.getDefaultDepth();
        };
        int newLength = shape == UltmineShape.LINE ? SkillConfig.getUltmineLineLengthDefault() : shape.getWidth();
        setSelection(shape, newDepth, newLength);
    }

    public static void updatePreview(List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            preview = List.of();
            return;
        }
        preview = Collections.unmodifiableList(new ArrayList<>(positions));
    }

    public static List<BlockPos> getPreview() {
        return preview;
    }

    public static void clearPreview() {
        preview = List.of();
    }
}
