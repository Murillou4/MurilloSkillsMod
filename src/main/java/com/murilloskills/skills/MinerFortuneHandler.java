package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
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

        PlayerSkillData playerData = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        var minerStats = playerData.getSkill(MurilloSkillsList.MINER);
        int bonusFortune = getSkillFortuneBonus(minerStats.level, minerStats.prestige, blockId);
        return bonusFortune > 0 ? originalLevel + bonusFortune : originalLevel;
    }

    public static int getSkillFortuneBonus(int minerLevel, int prestige, String blockId) {
        if (!shouldApplySkillFortune(blockId, minerLevel, prestige)) {
            return 0;
        }

        float prestigeMultiplier = PrestigeManager.getPassiveMultiplier(prestige);
        return Math.max(0, (int) (minerLevel * SkillConfig.getMinerFortunePerLevel() * prestigeMultiplier
                + prestige * SkillConfig.getMinerFortunePerPrestige()));
    }

    public static boolean shouldApplySkillFortune(String blockId, int minerLevel, int prestige) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        if (MinerXpGetter.isLikelyOreId(blockId)) {
            return true;
        }
        return minerLevel >= SkillConfig.getMinerResourceFortuneLevel() || prestige > 0;
    }
}
