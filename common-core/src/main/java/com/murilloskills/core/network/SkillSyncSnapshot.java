package com.murilloskills.core.network;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SkillSyncSnapshot {
    private final EnumMap<SkillType, SkillStatsCore> skills;
    private final SkillType activeParagonSkill;
    private final List<SkillType> paragonSkills;
    private final List<SkillType> selectedSkills;
    private final int maxSelectedSkills;

    public SkillSyncSnapshot(PlayerSkillDataCore data, int maxSelectedSkills) {
        data.normalizeParagonState();
        this.skills = new EnumMap<SkillType, SkillStatsCore>(SkillType.class);
        for (Map.Entry<SkillType, SkillStatsCore> entry : data.mutableSkills().entrySet()) {
            SkillStatsCore stats = entry.getValue();
            this.skills.put(entry.getKey(), new SkillStatsCore(
                    stats.getLevel(), stats.getXp(), stats.getLastAbilityUse(), stats.getPrestige()));
        }
        this.activeParagonSkill = data.getActiveParagonSkill();
        this.paragonSkills = new ArrayList<SkillType>(data.getParagonSkills());
        this.selectedSkills = new ArrayList<SkillType>(data.getSelectedSkills());
        this.maxSelectedSkills = maxSelectedSkills;
    }

    public EnumMap<SkillType, SkillStatsCore> getSkills() {
        return skills;
    }

    public SkillType getActiveParagonSkill() {
        return activeParagonSkill;
    }

    public List<SkillType> getParagonSkills() {
        return paragonSkills;
    }

    public List<SkillType> getSelectedSkills() {
        return selectedSkills;
    }

    public int getMaxSelectedSkills() {
        return maxSelectedSkills;
    }
}
