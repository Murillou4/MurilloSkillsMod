package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MinerFortuneHandler {
    private MinerFortuneHandler() {
    }

    public static int addSkillFortuneToVanillaLevel(int originalLevel, LootContext context) {
        if (context == null) {
            return originalLevel;
        }

        Entity entity = context.get(LootContextParameters.THIS_ENTITY);
        if (!(entity instanceof ServerPlayerEntity player)) {
            return originalLevel;
        }

        BlockState blockState = context.get(LootContextParameters.BLOCK_STATE);
        String blockId = blockState == null ? null : Registries.BLOCK.getId(blockState.getBlock()).toString();
        ItemStack tool = context.get(LootContextParameters.TOOL);

        PlayerSkillData playerData = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        var minerStats = playerData.getSkill(MurilloSkillsList.MINER);
        int bonusFortune = getSkillFortuneBonus(minerStats.level, minerStats.prestige, blockId, tool);
        return bonusFortune > 0 ? originalLevel + bonusFortune : originalLevel;
    }

    public static int getSkillFortuneBonus(int minerLevel, int prestige, String blockId) {
        return getSkillFortuneBonus(minerLevel, prestige, blockId, null);
    }

    public static int getSkillFortuneBonus(int minerLevel, int prestige, String blockId, ItemStack tool) {
        if (!shouldApplySkillFortune(blockId, minerLevel, prestige, tool)) {
            return 0;
        }

        float prestigeMultiplier = PrestigeManager.getPassiveMultiplier(prestige);
        return Math.max(0, (int) (minerLevel * SkillConfig.getMinerFortunePerLevel() * prestigeMultiplier
                + prestige * SkillConfig.getMinerFortunePerPrestige()));
    }

    public static boolean shouldApplySkillFortune(String blockId, int minerLevel, int prestige) {
        return shouldApplySkillFortune(blockId, minerLevel, prestige, null);
    }

    public static boolean shouldApplySkillFortune(String blockId, int minerLevel, int prestige, ItemStack tool) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        if (MinerXpGetter.isLikelyOreId(blockId)) {
            return true;
        }
        if (isGlowstoneId(blockId)) {
            return true;
        }
        if (isLeavesBlockId(blockId)) {
            return isAxeTool(tool);
        }
        return minerLevel >= SkillConfig.getMinerResourceFortuneLevel() || prestige > 0;
    }

    private static boolean isGlowstoneId(String blockId) {
        return "minecraft:glowstone".equals(blockId);
    }

    static boolean isLeavesBlockId(String blockId) {
        if (blockId == null) {
            return false;
        }
        String path = blockId;
        int sep = blockId.indexOf(':');
        if (sep >= 0 && sep < blockId.length() - 1) {
            path = blockId.substring(sep + 1);
        }
        return path.endsWith("_leaves") || path.equals("leaves");
    }

    private static boolean isAxeTool(ItemStack tool) {
        return tool != null && !tool.isEmpty() && tool.getItem() instanceof AxeItem;
    }
}
