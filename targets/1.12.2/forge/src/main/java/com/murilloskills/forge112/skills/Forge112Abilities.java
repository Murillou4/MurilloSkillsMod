package com.murilloskills.forge112.skills;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import com.murilloskills.forge112.api.SkillRegistry;
import net.minecraft.entity.player.EntityPlayer;

import static com.murilloskills.forge112.MurilloSkillsForge112.CONFIG;
import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;
import static com.murilloskills.forge112.MurilloSkillsForge112.STORE;
import static com.murilloskills.forge112.utils.Forge112Notifications.ability;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.say;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.skillName;

public final class Forge112Abilities {
    private Forge112Abilities() {
    }

    public static void triggerAbility(EntityPlayer player, SkillType requested) {
        PlayerSkillDataCore data = com.murilloskills.forge112.utils.Forge112PlayerServices.data(player);
        SkillType skill = requested != null ? requested : activeAbilitySkill(data);
        if (skill == null) {
            say(player, "MurilloSkills: selecione uma skill/paragon antes de usar habilidade.");
            return;
        }
        SkillStatsCore stats = data.getSkill(skill);
        if (stats.getLevel() < CONFIG.getMaxLevel()) {
            say(player, "MurilloSkills: " + skillName(skill) + " precisa estar no level 100.");
            LOG.info("[MurilloSkills][1.12.2][Ability] Reject {} for {} level={}", skill, player.getName(), stats.getLevel());
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = abilityCooldownMillis(stats.getPrestige());
        if (stats.getLastAbilityUse() > 0L && now - stats.getLastAbilityUse() < cooldown) {
            say(player, "MurilloSkills: habilidade em cooldown por " + ((cooldown - (now - stats.getLastAbilityUse())) / 1000L) + "s.");
            return;
        }
        AbstractSkill implementation = SkillRegistry.get(skill);
        if (implementation == null) {
            say(player, "MurilloSkills: skill sem implementacao ativa: " + skillName(skill));
            return;
        }
        stats.setLastAbilityUse(now);
        LOG.info("[MurilloSkills][1.12.2][Ability] Activate {} for {} level={} prestige={}",
                skill, player.getName(), stats.getLevel(), stats.getPrestige());
        implementation.onActiveAbility(player, data, stats);
        ability(player, skill, "Activated");
        STORE.save(player.getUniqueID());
    }

    public static long abilityCooldownMillis(int prestige) {
        double reduction = Math.min(CONFIG.getMaxPrestigeCooldownReduction(),
                prestige * CONFIG.getPrestigeCooldownReductionPerLevel());
        return (long) (60_000L * (1.0D - reduction));
    }

    public static SkillType activeAbilitySkill(PlayerSkillDataCore data) {
        SkillType active = data.getActiveParagonSkill();
        if (active != null) {
            return active;
        }
        return data.getSelectedSkills().isEmpty() ? null : data.getSelectedSkills().get(0);
    }
}
