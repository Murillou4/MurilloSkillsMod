package com.murilloskills.skills;
/** A short bonus for a productive session; breaks never remove earned XP or progress. */
public final class ActionCombo {
    public int actions;
    public long until;
    public double record(long now){actions=now>until?1:Math.min(6,actions+1);until=now+900;return multiplier();}
    public double multiplier(){return 1+Math.min(5,Math.max(0,actions-1))*0.02;}
}

