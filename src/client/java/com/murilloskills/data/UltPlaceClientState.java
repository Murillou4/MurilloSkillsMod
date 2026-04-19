package com.murilloskills.data;

import com.murilloskills.network.UltPlaceConfigC2SPayload;
import com.murilloskills.skills.UltPlaceShape;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side UltPlace selection and preview cache.
 */
public final class UltPlaceClientState {
    private static final Map<UltPlaceShape, ShapeSelection> SHAPE_SELECTIONS = new EnumMap<>(UltPlaceShape.class);

    private static UltPlaceShape selectedShape = UltPlaceShape.PLANE_NXN;
    private static boolean enabled = false;
    private static List<BlockPos> preview = List.of();

    static {
        resetSelections();
    }

    private UltPlaceClientState() {
    }

    public static void resetSelections() {
        SHAPE_SELECTIONS.clear();
        for (UltPlaceShape shape : UltPlaceShape.values()) {
            SHAPE_SELECTIONS.put(shape, createDefaultSelection(shape));
        }
        selectedShape = UltPlaceShape.PLANE_NXN;
        enabled = false;
        preview = List.of();
    }

    public static UltPlaceShape getSelectedShape() {
        return selectedShape;
    }

    public static void selectShape(UltPlaceShape shape) {
        selectedShape = shape == null ? UltPlaceShape.PLANE_NXN : shape;
        SHAPE_SELECTIONS.putIfAbsent(selectedShape, createDefaultSelection(selectedShape));
    }

    public static int getSize() {
        return getSelection(selectedShape).size;
    }

    public static int getLength() {
        return getSelection(selectedShape).length;
    }

    public static int getVariant() {
        return getSelection(selectedShape).variant;
    }

    public static void setSize(int size) {
        ShapeSelection selection = getSelection(selectedShape);
        selection.size = clampSize(selectedShape, size);
    }

    public static void setLength(int length) {
        ShapeSelection selection = getSelection(selectedShape);
        selection.length = clampLength(selectedShape, length);
    }

    public static void setVariant(int variant) {
        ShapeSelection selection = getSelection(selectedShape);
        int maxVariant = UltPlaceShape.getVariantCount(selectedShape) - 1;
        selection.variant = Math.max(0, Math.min(variant, maxVariant));
    }

    public static void adjustSize(int delta) {
        setSize(getSize() + delta);
    }

    public static void adjustLength(int delta) {
        setLength(getLength() + delta);
    }

    public static void adjustVariant(int delta) {
        int count = UltPlaceShape.getVariantCount(selectedShape);
        if (count <= 1) {
            return;
        }
        int current = getVariant();
        setVariant((current + delta + count) % count);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggleEnabled() {
        enabled = !enabled;
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

    public static UltPlaceConfigC2SPayload toPayload() {
        return new UltPlaceConfigC2SPayload(selectedShape, getSize(), getLength(), getVariant(), enabled);
    }

    private static ShapeSelection getSelection(UltPlaceShape shape) {
        return SHAPE_SELECTIONS.computeIfAbsent(shape, UltPlaceClientState::createDefaultSelection);
    }

    private static ShapeSelection createDefaultSelection(UltPlaceShape shape) {
        return new ShapeSelection(
                SkillConfig.getUltPlaceShapeDefaultSize(shape),
                SkillConfig.getUltPlaceShapeDefaultLength(shape),
                0);
    }

    private static int clampSize(UltPlaceShape shape, int value) {
        return Math.max(1, Math.min(value, SkillConfig.getUltPlaceShapeMaxSize(shape)));
    }

    private static int clampLength(UltPlaceShape shape, int value) {
        return Math.max(1, Math.min(value, SkillConfig.getUltPlaceShapeMaxLength(shape)));
    }

    private static final class ShapeSelection {
        private int size;
        private int length;
        private int variant;

        private ShapeSelection(int size, int length, int variant) {
            this.size = size;
            this.length = length;
            this.variant = variant;
        }
    }
}
