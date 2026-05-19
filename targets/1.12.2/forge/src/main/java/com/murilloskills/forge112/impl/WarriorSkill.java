package com.murilloskills.forge112.impl;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112Passives.applyAttribute;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.isTimedActive;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.addXp;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.isSelected;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.say;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getWarriorDamageBonus;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getWarriorHealthBonus;

public final class WarriorSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.WARRIOR;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        boolean selected = isSelected(data, SkillType.WARRIOR);
        SkillStatsCore stats = data.getSkill(SkillType.WARRIOR);
        applyAttribute(player, SharedMonsterAttributes.ATTACK_DAMAGE, WARRIOR_DAMAGE, "MurilloSkills warrior damage",
                selected ? getWarriorDamageBonus(stats) : 0.0D, 0);
        applyAttribute(player, SharedMonsterAttributes.MAX_HEALTH, WARRIOR_HEALTH, "MurilloSkills warrior health",
                selected ? getWarriorHealthBonus(stats) : 0.0D, 0);
        applyAttribute(player, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, WARRIOR_BERSERK_KNOCKBACK,
                "MurilloSkills berserk knockback", isTimedActive(player, BERSERK_UNTIL) ? 1.0D : 0.0D, 0);
        if (selected && stats.getLevel() >= 75) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 80, 0, true, false));
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, BERSERK_UNTIL, WARRIOR_BERSERK_DURATION_SECONDS, "Berserk");
        player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, WARRIOR_BERSERK_DURATION_SECONDS * 20, 3, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, WARRIOR_BERSERK_DURATION_SECONDS * 20, 1, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, WARRIOR_BERSERK_DURATION_SECONDS * 20, 1, false, true));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.65F, 1.25F);
        say(player, "MurilloSkills Warrior: Berserk ativo.");
    }

    @Override
    public void onIncomingDamage(LivingHurtEvent event, EntityPlayer player, PlayerSkillDataCore data, DamageSource source) {
        if (!isSelected(data, SkillType.WARRIOR)) {
            return;
        }
        double reduction = data.getSkill(SkillType.WARRIOR).getLevel() >= 25 ? 0.15D : 0.0D;
        if (isTimedActive(player, BERSERK_UNTIL)) {
            reduction += 0.40D;
        }
        float amount = event.getAmount() * (float) Math.max(0.45D, 1.0D - reduction);
        if (amount != event.getAmount()) {
            event.setAmount(amount);
            LOG.info("[MurilloSkills][1.12.2][Warrior] Reduced damage for {} to {}", player.getName(), amount);
        }
    }

    @Override
    public void onOutgoingDamage(LivingHurtEvent event, EntityPlayer attacker, PlayerSkillDataCore data, boolean arrow) {
        if (!isSelected(data, SkillType.WARRIOR) || (arrow && isSelected(data, SkillType.ARCHER))) {
            return;
        }
        event.setAmount((float) (event.getAmount() + getWarriorDamageBonus(data.getSkill(SkillType.WARRIOR))));
        if (data.getSkill(SkillType.WARRIOR).getLevel() >= 75 || isTimedActive(attacker, BERSERK_UNTIL)) {
            float heal = event.getAmount() * (isTimedActive(attacker, BERSERK_UNTIL) ? 0.50F : 0.15F);
            attacker.heal(Math.max(0.5F, heal));
        }
        addXp(attacker, SkillType.WARRIOR, 12, "melee hit");
    }

    @Override
    public void onLivingDeath(LivingDeathEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            addXp(player, SkillType.WARRIOR, 35, "kill " + event.getEntityLiving().getName());
        }
    }

    @Override
    public void onAttackEntity(AttackEntityEvent event, EntityPlayer player) {
        if (event.getTarget() instanceof EntityLivingBase) {
            addXp(player, SkillType.WARRIOR, 4, "attack");
        }
    }
}
