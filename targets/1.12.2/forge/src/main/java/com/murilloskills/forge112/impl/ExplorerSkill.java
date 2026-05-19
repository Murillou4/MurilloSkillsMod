package com.murilloskills.forge112.impl;

import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import com.murilloskills.forge112.data.PlayerRuntime;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

import java.util.List;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112Passives.applyAttribute;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.utils.Forge112EnvironmentEffects.awardExplorerMovement;
import static com.murilloskills.forge112.utils.Forge112EnvironmentEffects.scanTreasurePositions;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getExplorerSpeedBonus;

public final class ExplorerSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.EXPLORER;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        boolean selected = isSelected(data, SkillType.EXPLORER);
        SkillStatsCore stats = data.getSkill(SkillType.EXPLORER);
        applyAttribute(player, SharedMonsterAttributes.MOVEMENT_SPEED, EXPLORER_SPEED, "MurilloSkills explorer speed",
                selected && data.getToggle(SkillType.EXPLORER, "speed_boost", true)
                        ? getExplorerSpeedBonus(stats, player.isSprinting())
                        : 0.0D, 2);
        applyAttribute(player, SharedMonsterAttributes.LUCK, EXPLORER_LUCK, "MurilloSkills explorer luck",
                selected ? stats.getLevel() / 20 : 0.0D, 0);
        if (!selected) {
            player.stepHeight = 0.6F;
            return;
        }
        if (data.getToggle(SkillType.EXPLORER, "night_vision", false)) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
        if (stats.getLevel() >= EXPLORER_AQUATIC_LEVEL && player.isInWater()) {
            player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 100, 0, true, false));
        }
        if (stats.getLevel() >= EXPLORER_SWIFT_RECOVERY_LEVEL && player.getHealth() < player.getMaxHealth() * 0.5F) {
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 60, 0, true, false));
        }
        if (stats.getLevel() >= EXPLORER_NETHER_WALKER_LEVEL && player.dimension == -1) {
            player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
        }
        player.stepHeight = stats.getLevel() >= EXPLORER_STEP_ASSIST_LEVEL
                && data.getToggle(SkillType.EXPLORER, "step_assist", true) ? 1.0F : 0.6F;
    }

    @Override
    public void onTick(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        if (runtime.ticks % 80 == 0) {
            awardExplorerMovement(player, data, runtime);
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, TREASURE_HUNTER_UNTIL, EXPLORER_TREASURE_DURATION_SECONDS, "Treasure Hunter");
        List<BlockPos> treasures = scanTreasurePositions(player, EXPLORER_TREASURE_RADIUS, 128);
        TREASURE_VISIBLE_TARGETS.put(player.getUniqueID(), treasures);
        player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, EXPLORER_TREASURE_DURATION_SECONDS * 20, 0, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, EXPLORER_TREASURE_DURATION_SECONDS * 20, 1, false, true));
        player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, EXPLORER_TREASURE_DURATION_SECONDS * 20, 0, false, true));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.80F, 0.75F);
        say(player, "MurilloSkills Explorer: Treasure Hunter ativo. " + treasures.size() + " alvos encontrados.");
    }

    @Override
    public void onIncomingDamage(LivingHurtEvent event, EntityPlayer player, PlayerSkillDataCore data, DamageSource source) {
        if (source == DamageSource.FALL
                && isSelected(data, SkillType.EXPLORER)
                && data.getSkill(SkillType.EXPLORER).getLevel() >= EXPLORER_FEATHER_FEET_LEVEL) {
            event.setAmount(event.getAmount() * 0.40F);
        }
    }

    @Override
    public double getFallDistanceReduction(PlayerSkillDataCore data) {
        return isSelected(data, SkillType.EXPLORER)
                ? passiveScale(data, SkillType.EXPLORER, 0.004D, 0.020D)
                : 0.0D;
    }

    @Override
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        if (CrossModCompatRules.isLootContainerId(blockId)) {
            addXp(player, SkillType.EXPLORER, 18, "open/interact " + blockId);
        }
    }

    @Override
    public void onDimensionChanged(PlayerChangedDimensionEvent event) {
        addXp(event.player, SkillType.EXPLORER, 150, "dimension change");
    }
}
