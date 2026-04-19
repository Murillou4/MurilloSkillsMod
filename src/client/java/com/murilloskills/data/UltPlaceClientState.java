package com.murilloskills.data;

import com.murilloskills.network.UltPlaceConfigC2SPayload;
import com.murilloskills.skills.UltPlaceAnchorMode;
import com.murilloskills.skills.UltPlacePlanner;
import com.murilloskills.skills.UltPlaceRotationMode;
import com.murilloskills.skills.UltPlaceSelection;
import com.murilloskills.skills.UltPlaceShape;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side UltPlace selection and preview cache.
 */
public final class UltPlaceClientState {
    private static final Map<UltPlaceShape, ShapeSelection> SHAPE_SELECTIONS = new EnumMap<>(UltPlaceShape.class);

    private static UltPlaceShape selectedShape = UltPlaceShape.PLANE_NXN;
    private static boolean enabled = false;
    private static List<UltPlacePlanner.PreviewBlock> preview = List.of();
    private static Set<BlockPos> validatedPreviewPositions = Set.of();
    private static long activeValidationRequestKey = 0L;
    private static long appliedValidationRequestKey = 0L;
    private static String previewFallbackReason = null;

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
        validatedPreviewPositions = Set.of();
        activeValidationRequestKey = 0L;
        appliedValidationRequestKey = 0L;
        previewFallbackReason = null;
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

    public static UltPlaceAnchorMode getAnchorMode() {
        return getSelection(selectedShape).anchorMode;
    }

    public static UltPlaceRotationMode getRotationMode() {
        return getSelection(selectedShape).rotationMode;
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

    public static void setAnchorMode(UltPlaceAnchorMode anchorMode) {
        getSelection(selectedShape).anchorMode = UltPlaceAnchorMode.normalize(selectedShape, anchorMode);
    }

    public static void setRotationMode(UltPlaceRotationMode rotationMode) {
        getSelection(selectedShape).rotationMode = UltPlaceRotationMode.normalize(selectedShape, rotationMode);
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

    public static void adjustAnchorMode(int delta) {
        if (!selectedShape.supportsAnchorMode()) {
            setAnchorMode(UltPlaceAnchorMode.CENTER);
            return;
        }
        UltPlaceAnchorMode[] values = UltPlaceAnchorMode.values();
        int current = getAnchorMode().ordinal();
        setAnchorMode(values[(current + delta + values.length) % values.length]);
    }

    public static void adjustRotationMode(int delta) {
        if (!selectedShape.supportsRotationMode()) {
            setRotationMode(UltPlaceRotationMode.AUTO);
            return;
        }
        UltPlaceRotationMode[] values = UltPlaceRotationMode.values();
        int current = getRotationMode().ordinal();
        setRotationMode(values[(current + delta + values.length) % values.length]);
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

    public static void updateSpeculativePreview(List<UltPlacePlanner.PreviewBlock> blocks, String fallbackReason) {
        if (blocks == null || blocks.isEmpty()) {
            preview = List.of();
        } else {
            preview = Collections.unmodifiableList(new ArrayList<>(blocks));
        }
        previewFallbackReason = fallbackReason;
        validatedPreviewPositions = Set.of();
        activeValidationRequestKey = 0L;
        appliedValidationRequestKey = 0L;
    }

    public static void beginValidation(long requestKey) {
        activeValidationRequestKey = requestKey;
        appliedValidationRequestKey = 0L;
        validatedPreviewPositions = Set.of();
    }

    public static void applyValidatedPreview(long requestKey, List<BlockPos> positions) {
        if (requestKey != activeValidationRequestKey) {
            return;
        }
        if (positions == null || positions.isEmpty()) {
            validatedPreviewPositions = Set.of();
        } else {
            validatedPreviewPositions = Collections.unmodifiableSet(new LinkedHashSet<>(positions));
        }
        appliedValidationRequestKey = requestKey;
    }

    public static List<UltPlacePlanner.PreviewBlock> getPreview() {
        if (preview.isEmpty()) {
            return preview;
        }
        if (activeValidationRequestKey == 0L || appliedValidationRequestKey != activeValidationRequestKey) {
            return preview;
        }

        List<UltPlacePlanner.PreviewBlock> filtered = new ArrayList<>();
        for (UltPlacePlanner.PreviewBlock block : preview) {
            if (validatedPreviewPositions.contains(block.pos())) {
                filtered.add(block);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public static BlockPos getPrimaryPreviewPos() {
        for (UltPlacePlanner.PreviewBlock block : getPreview()) {
            if (block.anchor()) {
                return block.pos();
            }
        }
        return null;
    }

    public static String getPreviewFallbackReason() {
        return previewFallbackReason;
    }

    public static void clearPreview() {
        preview = List.of();
        validatedPreviewPositions = Set.of();
        activeValidationRequestKey = 0L;
        appliedValidationRequestKey = 0L;
        previewFallbackReason = null;
    }

    public static UltPlaceConfigC2SPayload toPayload() {
        return new UltPlaceConfigC2SPayload(selectedShape, getSize(), getLength(), getVariant(),
                getAnchorMode(), getRotationMode(), enabled);
    }

    public static UltPlaceSelection toSelection() {
        return new UltPlaceSelection(selectedShape, getSize(), getLength(), getVariant(), getAnchorMode(),
                getRotationMode());
    }

    private static ShapeSelection getSelection(UltPlaceShape shape) {
        return SHAPE_SELECTIONS.computeIfAbsent(shape, UltPlaceClientState::createDefaultSelection);
    }

    private static ShapeSelection createDefaultSelection(UltPlaceShape shape) {
        return new ShapeSelection(
                SkillConfig.getUltPlaceShapeDefaultSize(shape),
                SkillConfig.getUltPlaceShapeDefaultLength(shape),
                0,
                UltPlaceAnchorMode.CENTER,
                UltPlaceRotationMode.AUTO);
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
        private UltPlaceAnchorMode anchorMode;
        private UltPlaceRotationMode rotationMode;

        private ShapeSelection(int size, int length, int variant,
                UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode) {
            this.size = size;
            this.length = length;
            this.variant = variant;
            this.anchorMode = anchorMode;
            this.rotationMode = rotationMode;
        }
    }
}
