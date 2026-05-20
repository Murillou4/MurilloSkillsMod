package com.murilloskills.forge112.impl;

import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import com.murilloskills.forge112.data.PlayerRuntime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

import java.util.ArrayList;
import java.util.List;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.performHarvestMoon;
import static com.murilloskills.forge112.utils.Forge112EnvironmentEffects.accelerateNearbyPlants;
import static com.murilloskills.forge112.utils.Forge112MiningTools.isLoadedBlock;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.*;

public final class FarmerSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.FARMER;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        if (!isSelected(data, SkillType.FARMER)) {
            return;
        }
        int level = data.getSkill(SkillType.FARMER).getLevel();
        if (level >= FARMER_NATURES_VITALITY_LEVEL
                && isLoadedBlock(player.world, player.getPosition().down())
                && isNaturalGround(player.world.getBlockState(player.getPosition().down()))) {
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 60, 0, true, false));
        }
        if (level >= FARMER_SEED_MASTER_LEVEL) {
            player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 60, 0, true, false));
        }
    }

    @Override
    public void onTick(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        if (runtime.ticks % 80 == 0 && isSelected(data, SkillType.FARMER)) {
            accelerateNearbyPlants(player, data);
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, HARVEST_MOON_UNTIL, FARMER_ABILITY_DURATION_SECONDS, "Harvest Moon");
        int harvested = performHarvestMoon(player);
        addXp(player, SkillType.FARMER, harvested * 10, "harvest moon start");
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.PLAYERS, 0.80F, 1.00F);
        say(player, "MurilloSkills Farmer: Harvest Moon ativo. Colheita automatica iniciada.");
    }

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        if (CrossModCompatRules.isHarvestablePlantId(blockId)) {
            addXp(player, SkillType.FARMER, 25, "crop break " + blockId);
        }
    }

    @Override
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        if (!isSelected(data, SkillType.FARMER) || !CrossModCompatRules.isHarvestablePlantId(blockId)) {
            return;
        }
        double chance = getFarmerDoubleHarvestChance(data.getSkill(SkillType.FARMER));
        if (RANDOM.nextDouble() <= chance && !event.getDrops().isEmpty()) {
            List<ItemStack> extra = new ArrayList<ItemStack>();
            for (ItemStack stack : event.getDrops()) {
                if (!stack.isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(Math.max(1, stack.getCount()));
                    extra.add(copy);
                }
            }
            event.getDrops().addAll(extra);
            LOG.info("[MurilloSkills][1.12.2][Farmer] Extra harvest drops for {} at chance {}", player.getName(), chance);
        }
    }

    @Override
    public void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        EntityPlayer player = nearestSelectedPlayer(event.getWorld(), event.getPos(), SkillType.FARMER, 12.0D);
        if (player == null) {
            return;
        }
        double chance = getFarmerFertileGrowthChance(data(player).getSkill(SkillType.FARMER));
        if (RANDOM.nextDouble() < chance) {
            event.setResult(Result.ALLOW);
            if (player.ticksExisted % 200 == 0) {
                LOG.info("[MurilloSkills][1.12.2][Farmer] Allowed accelerated crop tick near {}", player.getName());
            }
        }
    }
}
