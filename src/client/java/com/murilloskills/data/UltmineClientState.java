package com.murilloskills.data;

import com.murilloskills.client.config.UltmineClientConfig;
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
    private static int depth = SkillConfig.getUltmineShapeDefaultDepth(UltmineShape.S_3x3);
    private static int length = SkillConfig.getUltmineShapeDefaultLength(UltmineShape.S_3x3);
    private static int variant = 0;
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

    public static int getVariant() {
        return variant;
    }

    public static void setSelection(UltmineShape shape, int newDepth, int newLength) {
        selectedShape = shape == null ? UltmineShape.S_3x3 : shape;
        depth = Math.max(1, Math.min(newDepth, SkillConfig.getUltmineShapeMaxDepth(selectedShape)));
        length = Math.max(1, Math.min(newLength, SkillConfig.getUltmineShapeMaxLength(selectedShape)));
    }

    public static void setVariant(int newVariant) {
        int maxVariant = UltmineShape.getVariantCount(selectedShape) - 1;
        variant = Math.max(0, Math.min(newVariant, maxVariant));
    }

    public static void applyShapeDefaults(UltmineShape shape) {
        // Use per-shape config if the user customized depth/length, otherwise server defaults
        int configDepth = UltmineClientConfig.getShapeDepth(shape);
        int configLength = UltmineClientConfig.getShapeLength(shape);
        int newDepth = configDepth > 0 ? configDepth : SkillConfig.getUltmineShapeDefaultDepth(shape);
        int newLength = configLength > 0 ? configLength : SkillConfig.getUltmineShapeDefaultLength(shape);
        setSelection(shape, newDepth, newLength);
        // Read variant from persistent client config instead of hardcoding to 0
        variant = UltmineClientConfig.getShapeVariant(shape);
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
