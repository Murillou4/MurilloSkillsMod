package com.murilloskills.forge112.impl;

import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112Passives.applyReach;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.creativeBrushFill;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.isTimedActive;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;

public final class BuilderSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.BUILDER;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        applyReach(player, data);
        if (!isSelected(data, SkillType.BUILDER)) {
            return;
        }
        int level = data.getSkill(SkillType.BUILDER).getLevel();
        if (level >= BUILDER_BUILDERS_VIGOR_LEVEL) {
            player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 60, 0, true, false));
        }
        if (level >= BUILDER_FEATHER_BUILD_LEVEL && player.isSneaking() && player.posY >= 100.0D) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 60, 0, true, false));
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        if (isTimedActive(player, CREATIVE_BRUSH_UNTIL)) {
            CREATIVE_BRUSH_UNTIL.remove(player.getUniqueID());
            say(player, "MurilloSkills Builder: Creative Brush desativado.");
        } else {
            activateTimed(player, CREATIVE_BRUSH_UNTIL, BUILDER_ABILITY_DURATION_SECONDS, "Creative Brush");
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, BUILDER_ABILITY_DURATION_SECONDS * 20, 0, false, true));
            say(player, "MurilloSkills Builder: Creative Brush ativo. Coloque um bloco para preencher 3x3.");
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 0.80F, 1.00F);
    }

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        if (CrossModCompatRules.builderCategory(blockId) != CrossModCompatRules.BuilderCategory.NONE) {
            addXp(player, SkillType.BUILDER, 8, "building block break " + blockId);
        }
    }

    @Override
    public void onBlockPlace(BlockEvent.PlaceEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        int xp = 8 + CrossModCompatRules.builderCategory(blockId).ordinal() * 4;
        addXp(player, SkillType.BUILDER, xp, "place " + blockId);
        if (isSelected(data, SkillType.BUILDER) && isTimedActive(player, CREATIVE_BRUSH_UNTIL)) {
            int placed = creativeBrushFill(player, event.getWorld(), event.getPos(), event.getPlacedBlock());
            if (placed > 0) {
                addXp(player, SkillType.BUILDER, placed * 2, "creative brush " + blockId);
                LOG.info("[MurilloSkills][1.12.2][Builder] Creative Brush filled {} blocks for {}", placed, player.getName());
            }
        }
    }

    @Override
    public void onIncomingDamage(LivingHurtEvent event, EntityPlayer player, PlayerSkillDataCore data, DamageSource source) {
        if (source == DamageSource.FALL
                && isSelected(data, SkillType.BUILDER)
                && data.getSkill(SkillType.BUILDER).getLevel() >= BUILDER_SAFE_LANDING_LEVEL) {
            event.setAmount(event.getAmount() * 0.50F);
        }
    }

    @Override
    public double getFallDistanceReduction(PlayerSkillDataCore data) {
        return isSelected(data, SkillType.BUILDER)
                ? passiveScale(data, SkillType.BUILDER, 0.008D, 0.030D)
                : 0.0D;
    }

    @Override
    public void onCraft(ItemCraftedEvent event, String itemId) {
        if (CrossModCompatRules.builderCategory(itemId) != CrossModCompatRules.BuilderCategory.NONE) {
            addXp(event.player, SkillType.BUILDER, 25, "craft " + itemId);
        }
    }
}
