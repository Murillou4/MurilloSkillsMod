package com.murilloskills.core.platform;

import com.murilloskills.core.config.SkillType;

public interface AttributePort<P> {
    void applySkillAttributes(P player, SkillType skill, int level, int prestige);

    void clearSkillAttributes(P player, SkillType skill);
}
