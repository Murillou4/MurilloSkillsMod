package com.murilloskills.models;

public class SkillReceptorResult {

    private final boolean gainedXp;     // ganhou XP?
    private final int xpAmount;         // quanto de XP ganhou

    public SkillReceptorResult(boolean gainedXp, int xpAmount) {
        this.gainedXp = gainedXp;
        this.xpAmount = xpAmount;
    }


    public boolean didGainXp() {
        return gainedXp;
    }

    public int getXpAmount() {
        return xpAmount;
    }

    @Override
    public String toString() {
        return "SkillReceptorResult{" +
                "gainedXp=" + gainedXp +
                ", xpAmount=" + xpAmount +
                '}';
    }
}
