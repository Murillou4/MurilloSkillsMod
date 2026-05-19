package com.murilloskills.forge112.impl;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.isTimedActive;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.addXp;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.isSelected;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.say;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getArcherDamageMultiplier;

public final class ArcherSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.ARCHER;
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, MASTER_RANGER_UNTIL, ARCHER_MASTER_RANGER_DURATION_SECONDS, "Master Ranger");
        data.setToggle(SkillType.ARCHER, "focus", true);
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, ARCHER_MASTER_RANGER_DURATION_SECONDS * 20, 1, false, true));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.85F, 0.65F);
        say(player, "MurilloSkills Archer: Master Ranger ativo. Flechas ganham velocidade, dano e foco.");
    }

    @Override
    public void onOutgoingDamage(LivingHurtEvent event, EntityPlayer attacker, PlayerSkillDataCore data, boolean arrow) {
        if (!arrow || !isSelected(data, SkillType.ARCHER)) {
            return;
        }
        float next = (float) (event.getAmount() * getArcherDamageMultiplier(data.getSkill(SkillType.ARCHER)));
        if (isTimedActive(attacker, MASTER_RANGER_UNTIL)) {
            next *= 1.75F;
        }
        if (data.getToggle(SkillType.ARCHER, "focus", false)) {
            next *= 1.5F;
            data.setToggle(SkillType.ARCHER, "focus", false);
            say(attacker, "MurilloSkills Archer Focus consumed.");
        }
        event.setAmount(next);
        addXp(attacker, SkillType.ARCHER, 18, "arrow hit");
    }

    @Override
    public void onLivingDeath(LivingDeathEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityPlayer || !(event.getSource().getImmediateSource() instanceof EntityArrow)) {
            return;
        }
        EntityArrow arrow = (EntityArrow) event.getSource().getImmediateSource();
        if (arrow.shootingEntity instanceof EntityPlayer) {
            addXp((EntityPlayer) arrow.shootingEntity, SkillType.ARCHER, 40, "arrow kill");
        }
    }

    @Override
    public void onArrowJoin(EntityJoinWorldEvent event, EntityArrow arrow, EntityPlayer shooter, PlayerSkillDataCore data) {
        if (!isSelected(data, SkillType.ARCHER)) {
            return;
        }
        int level = data.getSkill(SkillType.ARCHER).getLevel();
        double mult = level >= ARCHER_FAST_ARROWS_LEVEL ? 1.25D : 1.0D;
        if (isTimedActive(shooter, MASTER_RANGER_UNTIL)) {
            mult *= 1.20D;
            arrow.setIsCritical(true);
        }
        arrow.motionX *= mult;
        arrow.motionY *= mult;
        arrow.motionZ *= mult;
        LOG.debug("[MurilloSkills][1.12.2][Archer] Arrow speed multiplier {} for {}", mult, shooter.getName());
    }
}
