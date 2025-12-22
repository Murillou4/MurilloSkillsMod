package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for EnchantmentScreenHandler to grant Blacksmith XP when enchanting
 * items.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {

    /**
     * Grant XP when player successfully enchants an item.
     * Injects into onButtonClick which handles enchantment selection.
     */
    @Inject(method = "onButtonClick", at = @At("RETURN"))
    private void onBlacksmithEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        // Only proceed if enchanting was successful
        if (!cir.getReturnValue()) {
            return;
        }

        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Track enchanting for daily challenges (before skill check - challenges are
        // for all players)
        com.murilloskills.events.ChallengeEventsHandler.onItemEnchanted(serverPlayer);

        var playerData = serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Get XP based on enchantment slot (id is 0, 1, or 2 for level 1, 2, 3)
        int xp = BlacksmithXpGetter.getEnchantXp(id);

        // Add XP using the central method that handles paragon constraints
        PlayerSkillData.XpAddResult xpResult = playerData.addXpToSkill(MurilloSkillsList.BLACKSMITH, xp);

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Ferreiro", xpResult);

        // Notify player on level up
        if (xpResult.leveledUp()) {
            int newLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BLACKSMITH, newLevel);
        }

        // Sync skill data with client
        SkillsNetworkUtils.syncSkills(serverPlayer);

        // Track enchantments for Master Enchanter achievement
        com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                serverPlayer, MurilloSkillsList.BLACKSMITH,
                com.murilloskills.utils.AchievementTracker.KEY_ITEMS_ENCHANTED, 1);
    }
}
