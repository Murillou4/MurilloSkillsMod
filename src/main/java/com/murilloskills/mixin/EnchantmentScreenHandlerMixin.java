package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.data.ModAttachments;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithOverEnchanting;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * Handles Blacksmith enchanting-table perks.
 *
 * The enchanting preview is generated from vanilla {@code enchantmentPower}, so
 * we keep that array untouched on the server and only relax the level gate when
 * the player qualifies for the Blacksmith discount. This keeps the preview and
 * the applied enchantment in sync while preserving XP refund and bonus logic.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {

    @Shadow
    @Final
    private Inventory inventory;

    @Shadow
    @Final
    private int[] enchantmentPower;

    @Shadow
    @Final
    private Property seed;

    @Unique
    private int murilloskills$pendingEnchantButtonId = -1;

    @Unique
    private PlayerEntity murilloskills$pendingEnchantPlayer;

    @Unique
    private boolean murilloskills$pendingTableBonusRoll;

    @Inject(method = "onButtonClick", at = @At("HEAD"))
    private void murilloskills$capturePendingEnchantButton(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.murilloskills$pendingEnchantButtonId = id;
        this.murilloskills$pendingEnchantPlayer = player;
        this.murilloskills$pendingTableBonusRoll = true;
    }

    @Inject(method = "onButtonClick", at = @At("RETURN"))
    private void murilloskills$clearPendingEnchantButton(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.murilloskills$pendingEnchantButtonId = -1;
        this.murilloskills$pendingEnchantPlayer = null;
        this.murilloskills$pendingTableBonusRoll = false;
    }

    @Redirect(
            method = "onButtonClick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;experienceLevel:I",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 1))
    private int murilloskills$useDiscountedRequirementForLevelGate(PlayerEntity player) {
        int actualLevel = player.experienceLevel;
        int discountedRequirement = this.murilloskills$getDiscountedRequirement(player, this.murilloskills$pendingEnchantButtonId);
        if (discountedRequirement <= 0 || actualLevel < discountedRequirement) {
            return actualLevel;
        }

        int originalRequirement = this.murilloskills$getOriginalRequirement(this.murilloskills$pendingEnchantButtonId);
        return Math.max(actualLevel, originalRequirement);
    }

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

        var playerData = serverPlayer.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        int blacksmithLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        if (!serverPlayer.isCreative()) {
            int spentLevels = Math.max(1, id + 1);
            int refundedLevels = SkillConfig.getBlacksmithEnchantingTableRefundLevels(blacksmithLevel, spentLevels);
            if (refundedLevels > 0) {
                serverPlayer.addExperienceLevels(refundedLevels);
            }
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

    @Inject(method = "generateEnchantments", at = @At("RETURN"), cancellable = true)
    private void murilloskills$applyDeterministicTableBonus(
            DynamicRegistryManager registryManager,
            ItemStack stack,
            int slot,
            int level,
            CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        if (!this.murilloskills$pendingTableBonusRoll || slot != this.murilloskills$pendingEnchantButtonId) {
            return;
        }

        this.murilloskills$pendingTableBonusRoll = false;

        if (!(this.murilloskills$pendingEnchantPlayer instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        List<EnchantmentLevelEntry> generated = cir.getReturnValue();
        if (generated == null || generated.isEmpty()) {
            return;
        }

        var playerData = serverPlayer.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        int blacksmithLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        if (!BlacksmithOverEnchanting.isUnlocked(blacksmithLevel)) {
            return;
        }

        cir.setReturnValue(BlacksmithOverEnchanting.applyDeterministicEnchantingTableBonus(
                generated,
                this.seed.get(),
                slot));
    }

    @Unique
    private int murilloskills$getOriginalRequirement(int id) {
        if (id < 0 || id >= this.enchantmentPower.length) {
            return 0;
        }
        return this.enchantmentPower[id];
    }

    @Unique
    private int murilloskills$getDiscountedRequirement(PlayerEntity player, int id) {
        int originalRequirement = this.murilloskills$getOriginalRequirement(id);
        if (originalRequirement <= 0 || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return originalRequirement;
        }

        var playerData = serverPlayer.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return originalRequirement;
        }

        int level = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        if (level < SkillConfig.getBlacksmithEfficientAnvilLevel()) {
            return originalRequirement;
        }

        return SkillConfig.getBlacksmithEnchantingTableRequirement(level, originalRequirement);
    }
}
