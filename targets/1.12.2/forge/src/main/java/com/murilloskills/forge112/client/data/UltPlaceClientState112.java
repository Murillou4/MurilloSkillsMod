package com.murilloskills.forge112.client.data;

import java.util.EnumMap;
import java.util.Map;

public final class UltPlaceClientState112 {
    private static final Map<UltPlaceShape112, Selection> SELECTIONS =
            new EnumMap<UltPlaceShape112, Selection>(UltPlaceShape112.class);
    private static UltPlaceShape112 selectedShape = UltPlaceShape112.PLANE_NXN;
    private static boolean enabled;
    private static boolean previewEnabled = true;

    static {
        reset();
    }

    private UltPlaceClientState112() {
    }

    public static void reset() {
        SELECTIONS.clear();
        for (UltPlaceShape112 shape : UltPlaceShape112.values()) {
            SELECTIONS.put(shape, defaults(shape));
        }
        selectedShape = UltPlaceShape112.PLANE_NXN;
        enabled = false;
        previewEnabled = true;
    }

    public static UltPlaceShape112 getSelectedShape() {
        return selectedShape;
    }

    public static void setSelectedShape(UltPlaceShape112 shape) {
        selectedShape = shape == null ? UltPlaceShape112.PLANE_NXN : shape;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean toggleEnabled() {
        enabled = !enabled;
        return enabled;
    }

    public static boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public static boolean togglePreview() {
        previewEnabled = !previewEnabled;
        return previewEnabled;
    }

    public static int getSize() {
        return selection().size;
    }

    public static int getLength() {
        return selection().length;
    }

    public static int getHeight() {
        return selection().height;
    }

    public static int getSpacing() {
        return selection().spacing;
    }

    public static int getVariant() {
        return selection().variant;
    }

    public static void adjustSize(int delta) {
        selection().size = clamp(selection().size + delta, 1, 64);
    }

    public static void adjustLength(int delta) {
        selection().length = clamp(selection().length + delta, 1, 128);
    }

    public static void adjustHeight(int delta) {
        selection().height = clamp(selection().height + delta, 1, 32);
    }

    public static void adjustSpacing(int delta) {
        selection().spacing = clamp(selection().spacing + delta, 1, 16);
    }

    public static void adjustVariant() {
        selection().variant = selectedShape.supportsVariant() ? (selection().variant + 1) % 2 : 0;
    }

    public static String summary() {
        Selection selection = selection();
        return selectedShape.label() + " size " + selection.size + " length " + selection.length
                + " height " + selection.height + " spacing " + selection.spacing;
    }

    private static Selection selection() {
        Selection selection = SELECTIONS.get(selectedShape);
        if (selection == null) {
            selection = defaults(selectedShape);
            SELECTIONS.put(selectedShape, selection);
        }
        return selection;
    }

    private static Selection defaults(UltPlaceShape112 shape) {
        if (shape == UltPlaceShape112.LINE) {
            return new Selection(1, 12, 1, 1, 0);
        }
        if (shape == UltPlaceShape112.WALL) {
            return new Selection(5, 5, 3, 1, 0);
        }
        if (shape == UltPlaceShape112.TUNNEL_3X3) {
            return new Selection(3, 16, 3, 1, 0);
        }
        if (shape == UltPlaceShape112.SINGLE) {
            return new Selection(1, 1, 1, 1, 0);
        }
        return new Selection(5, 5, shape.supportsHeight() ? 3 : 1, 1, 0);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Selection {
        private int size;
        private int length;
        private int height;
        private int spacing;
        private int variant;

        private Selection(int size, int length, int height, int spacing, int variant) {
            this.size = size;
            this.length = length;
            this.height = height;
            this.spacing = spacing;
            this.variant = variant;
        }
    }
}
