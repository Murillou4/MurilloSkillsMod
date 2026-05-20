package com.murilloskills.forge112.impl;

import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.AbstractSkill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.murilloskills.forge112.MurilloSkillsForge112.*;
import static com.murilloskills.forge112.skills.Forge112TimedEffects.activateTimed;
import static com.murilloskills.forge112.utils.Forge112MiningTools.*;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.*;
import static com.murilloskills.forge112.utils.Forge112SkillMath.*;

public final class MinerSkill extends AbstractSkill {
    private static final double FORTUNE_PER_LEVEL = 0.03D;
    private static final double FORTUNE_PER_PRESTIGE = 0.5D;
    private static final int RESOURCE_FORTUNE_LEVEL = 75;

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
    }

    @Override
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event, EntityPlayer player, PlayerSkillDataCore data,
            String blockId) {
        if (!isSelected(data, SkillType.MINER) || event.isSilkTouching()) {
            return;
        }
        SkillStatsCore stats = data.getSkill(SkillType.MINER);
        ItemStack tool = player.getHeldItemMainhand();
        int bonusFortune = getSkillFortuneBonus(stats, blockId, tool);
        if (bonusFortune <= 0 || event.getDrops().isEmpty()) {
            return;
        }

        int vanillaFortune = Math.max(0, event.getFortuneLevel());
        int combinedFortune = vanillaFortune + bonusFortune;
        List<ItemStack> boostedDrops = event.getState().getBlock().getDrops(
                event.getWorld(), event.getPos(), event.getState(), combinedFortune);
        List<ItemStack> extraDrops = differenceByItem(boostedDrops, event.getDrops());

        if (extraDrops.isEmpty() && !boostedDrops.isEmpty()) {
            ItemStack guaranteed = boostedDrops.get(0).copy();
            guaranteed.setCount(Math.max(1, Math.min(guaranteed.getCount(), bonusFortune)));
            extraDrops.add(guaranteed);
        }
        if (extraDrops.isEmpty()) {
            return;
        }

        event.getDrops().addAll(extraDrops);
        event.setDropChance(1.0F);
        if (player.ticksExisted % 40 == 0) {
            LOG.info("[MurilloSkills][1.12.2][Miner] Fortune bonus +{} on {} produced {} extra stacks for {}",
                    bonusFortune, blockId, extraDrops.size(), player.getName());
        }
    }

    public static int getSkillFortuneBonus(SkillStatsCore stats, String blockId, ItemStack tool) {
        if (stats == null || !shouldApplySkillFortune(blockId, stats.getLevel(), stats.getPrestige(), tool)) {
            return 0;
        }
        return Math.max(0, (int) (stats.getLevel() * FORTUNE_PER_LEVEL * prestigePassiveMultiplier(stats)
                + stats.getPrestige() * FORTUNE_PER_PRESTIGE));
    }

    private static boolean shouldApplySkillFortune(String blockId, int minerLevel, int prestige, ItemStack tool) {
        if (blockId == null || blockId.trim().isEmpty()) {
            return false;
        }
        if (CrossModCompatRules.isOreResourceId(blockId) || "minecraft:glowstone".equalsIgnoreCase(blockId)) {
            return true;
        }
        if (isLeavesBlockId(blockId)) {
            return tool != null && !tool.isEmpty() && tool.getItem() instanceof ItemAxe;
        }
        return minerLevel >= RESOURCE_FORTUNE_LEVEL || prestige > 0;
    }

    private static boolean isLeavesBlockId(String blockId) {
        String path = CrossModCompatRules.path(blockId);
        return path.endsWith("_leaves") || "leaves".equals(path) || "leaves2".equals(path);
    }

    private static List<ItemStack> differenceByItem(List<ItemStack> boosted, List<ItemStack> current) {
        Map<String, Integer> currentCounts = countByItem(current);
        Map<String, Integer> boostedCounts = countByItem(boosted);
        java.util.ArrayList<ItemStack> extra = new java.util.ArrayList<ItemStack>();
        for (ItemStack stack : boosted) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String id = itemId(stack);
            int missing = boostedCounts.containsKey(id) ? boostedCounts.get(id) - safeCount(currentCounts, id) : 0;
            if (missing <= 0) {
                continue;
            }
            ItemStack copy = stack.copy();
            copy.setCount(Math.min(stack.getCount(), missing));
            extra.add(copy);
            currentCounts.put(id, safeCount(currentCounts, id) + copy.getCount());
        }
        return extra;
    }

    private static Map<String, Integer> countByItem(List<ItemStack> stacks) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String id = itemId(stack);
            counts.put(id, Integer.valueOf(safeCount(counts, id) + stack.getCount()));
        }
        return counts;
    }

    private static int safeCount(Map<String, Integer> counts, String id) {
        Integer count = counts.get(id);
        return count == null ? 0 : count.intValue();
    }
}
