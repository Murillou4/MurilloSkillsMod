package com.murilloskills.forge112.api;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.forge112.data.PlayerRuntime;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;

public final class SkillRegistry {
    private static final Map<SkillType, AbstractSkill> SKILLS = new EnumMap<SkillType, AbstractSkill>(SkillType.class);

    private SkillRegistry() {
    }

    public static void register(AbstractSkill skill) {
        if (skill == null || skill.getSkillType() == null) {
            LOG.error("[MurilloSkills][1.12.2][SkillRegistry] Ignored null skill registration.");
            return;
        }
        if (SKILLS.containsKey(skill.getSkillType())) {
            LOG.warn("[MurilloSkills][1.12.2][SkillRegistry] Duplicate skill ignored: {}", skill.getSkillType());
            return;
        }
        SKILLS.put(skill.getSkillType(), skill);
    }

    public static AbstractSkill get(SkillType type) {
        AbstractSkill skill = SKILLS.get(type);
        if (skill == null) {
            LOG.warn("[MurilloSkills][1.12.2][SkillRegistry] Skill not registered: {}", type);
        }
        return skill;
    }

    public static Collection<AbstractSkill> values() {
        return Collections.unmodifiableCollection(new ArrayList<AbstractSkill>(SKILLS.values()));
    }

    public static void onPlayerJoin(EntityPlayer player, PlayerSkillDataCore data) {
        for (AbstractSkill skill : values()) {
            skill.onPlayerJoin(player, data);
        }
    }

    public static void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        for (AbstractSkill skill : values()) {
            skill.applyPassives(player, data);
        }
    }

    public static void tick(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        for (AbstractSkill skill : values()) {
            skill.onTick(player, data, runtime);
        }
    }

    public static double getFallDistanceReduction(PlayerSkillDataCore data) {
        double reduction = 0.0D;
        for (AbstractSkill skill : values()) {
            reduction += skill.getFallDistanceReduction(data);
        }
        return reduction;
    }

    public static int size() {
        return SKILLS.size();
    }

    public static void clear() {
        SKILLS.clear();
    }

    public static void logRegisteredSkills() {
        LOG.info("[MurilloSkills][1.12.2][SkillRegistry] Registered {} skill implementations.", SKILLS.size());
        for (Map.Entry<SkillType, AbstractSkill> entry : SKILLS.entrySet()) {
            LOG.info("[MurilloSkills][1.12.2][SkillRegistry] - {} -> {}", entry.getKey(), entry.getValue().getClass().getSimpleName());
        }
    }
}
