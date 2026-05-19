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
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.List;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.utils.Forge112MiningTools.*;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getMinerSpeedBonus;

public final class MinerSkill extends AbstractSkill {
    @Override
    public SkillType getSkillType() {
        return SkillType.MINER;
    }

    @Override
    public void applyPassives(EntityPlayer player, PlayerSkillDataCore data) {
        if (!isSelected(data, SkillType.MINER)) {
            return;
        }
        int level = data.getSkill(SkillType.MINER).getLevel();
        int amplifier = level >= 50 ? 1 : 0;
        player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 60, amplifier, true, false));
        if (level >= MINER_NIGHT_VISION_LEVEL && player.posY < 55.0D && !player.world.canBlockSeeSky(player.getPosition())) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 300, 0, true, false));
        }
        if (level >= MINER_AUTO_TORCH_LEVEL
                && data.getToggle(SkillType.MINER, "auto_torch", false)
                && player.ticksExisted % 40 == 0) {
            placeAutoTorch(player);
        }
    }

    @Override
    public void onActiveAbility(EntityPlayer player, PlayerSkillDataCore data, SkillStatsCore stats) {
        activateTimed(player, MINER_VISION_UNTIL, MINER_ABILITY_DURATION_SECONDS, "Master Miner");
        List<BlockPos> ores = scanOrePositions(player, MINER_ABILITY_RADIUS, 512);
        MINER_VISIBLE_ORES.put(player.getUniqueID(), ores);
        player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, MINER_ABILITY_DURATION_SECONDS * 20, 0, false, true));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.60F, 1.35F);
        say(player, "MurilloSkills Miner: Master Miner ativo. " + ores.size() + " minerios revelados.");
    }

    @Override
    public void onBreakSpeed(PlayerEvent.BreakSpeed event, EntityPlayer player, PlayerSkillDataCore data) {
        if (!isSelected(data, SkillType.MINER)) {
            return;
        }
        double bonus = getMinerSpeedBonus(data.getSkill(SkillType.MINER));
        float next = (float) (event.getOriginalSpeed() * (1.0D + bonus));
        event.setNewSpeed(next);
        if (player.ticksExisted % 100 == 0) {
            LOG.info("[MurilloSkills][1.12.2][Miner] BreakSpeed {} -> {} for {}", event.getOriginalSpeed(), next, player.getName());
        }
    }

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, EntityPlayer player, PlayerSkillDataCore data, String blockId) {
        if (CrossModCompatRules.isOreResourceId(blockId)) {
            addXp(player, SkillType.MINER, 45, "ore break " + blockId);
        } else {
            addXp(player, SkillType.MINER, 12, "block break " + blockId);
        }
        if (isSelected(data, SkillType.MINER) && isUltmineHeld(player) && !ULTMINE_RUNNING.contains(player.getUniqueID())) {
            int mined = runUltmine(player, event.getPos(), event.getState());
            if (mined > 0) {
                addXp(player, SkillType.MINER, mined * 6, "ultmine " + blockId);
                LOG.info("[MurilloSkills][1.12.2][Ultmine] {} mined {} extra blocks.", player.getName(), mined);
            }
        }
    }
}
