package com.murilloskills.core.data;

public final class XpAddResult {
    public static final XpAddResult NO_CHANGE = new XpAddResult(false, 0, 0);

    private final boolean leveledUp;
    private final int oldLevel;
    private final int newLevel;

    public XpAddResult(boolean leveledUp, int oldLevel, int newLevel) {
        this.leveledUp = leveledUp;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public boolean isLeveledUp() {
        return leveledUp;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
