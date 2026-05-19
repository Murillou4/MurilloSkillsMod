package com.murilloskills.forge112.client.data;

public enum UltPlaceShape112 {
    PLANE_NXN("Plane"),
    HORIZONTAL_BOX("Box"),
    LINE("Line"),
    WALL("Wall"),
    STAIRS("Stairs"),
    COLUMN("Column"),
    TUNNEL_3X3("Tunnel"),
    CIRCLE("Circle"),
    SPHERE_SHELL("Sphere"),
    SINGLE("Single");

    private final String label;

    UltPlaceShape112(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean supportsHeight() {
        return this == HORIZONTAL_BOX;
    }

    public boolean supportsSpacing() {
        return this == PLANE_NXN || this == HORIZONTAL_BOX || this == WALL || this == LINE || this == COLUMN;
    }

    public boolean supportsVariant() {
        return this == STAIRS || this == COLUMN;
    }

    public UltPlaceShape112 next() {
        UltPlaceShape112[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
