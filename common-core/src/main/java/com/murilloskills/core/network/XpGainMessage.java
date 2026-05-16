package com.murilloskills.core.network;

import com.murilloskills.core.config.SkillType;

public final class XpGainMessage {
    private final SkillType skill;
    private final int xpAmount;
    private final String source;

    public XpGainMessage(SkillType skill, int xpAmount, String source) {
        this.skill = skill;
        this.xpAmount = xpAmount;
        this.source = source == null ? "" : source;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getXpAmount() {
        return xpAmount;
    }

    public String getSource() {
        return source;
    }
}
