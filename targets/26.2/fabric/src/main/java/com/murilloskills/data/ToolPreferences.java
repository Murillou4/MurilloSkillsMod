package com.murilloskills.data;
import java.util.*;

public final class ToolPreferences {
    public String miningShape = "VEIN", placementShape = "PLANE";
    public int size = 3, depth = 3, spacing = 1;
    public boolean inventoryDrops = true, directXp = false, oresOnly = false, hollow = false;
    public Set<String> trash = new HashSet<>(), oreFilter = new HashSet<>(), classicFilter = new HashSet<>();
}

