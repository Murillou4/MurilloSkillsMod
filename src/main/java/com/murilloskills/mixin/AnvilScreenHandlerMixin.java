package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.events.ChallengeEventsHandler;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithOverEnchanting;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for AnvilScreenHandler to grant Blacksmith XP and apply perks.
 * - XP when taking output from anvil
 * - Level 25: large XP cost reduction
 * - Tracks item repairs for daily challenges
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    // Shadow the levelCost property from AnvilScreenHandler
    @Shadow
    @Final
    private Property levelCost;

    @Unique
    private int murilloskills$vanillaLevelCost;

    /**
     * Apply Blacksmith XP cost discount after vanilla calculates the cost.
     * Injects at the end of updateResult() to modify the cost before it's
     * displayed.
     *
     * Discount scales with level:
     * - Level 25: 40% discount (base)
     * - Level 100: 65% discount
     * Also raises the "Too Expensive!" threshold from 40 to 55 for Blacksmiths.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void applyBlacksmithAnvilDiscount(CallbackInfo ci) {
        this.murilloskills$vanillaLevelCost = this.levelCost.get();
        this.murilloskills$refreshFinalAnvilCost();
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"))
    private void murilloskills$refreshFinalCostBeforeTake(PlayerEntity player, boolean present,
            CallbackInfoReturnable<Boolean> cir) {
        this.murilloskills$refreshFinalAnvilCost();
    }

    @Unique
    private void murilloskills$refreshFinalAnvilCost() {
        // Use accessor to get player from parent class
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        PlayerEntity player = accessor.getPlayer();

        // Only apply on server side
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        var playerData = serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        Inventory input = accessor.getInput();
        Inventory output = accessor.getOutput();
        if (input == null || output == null || output.getStack(0).isEmpty()) {
            return;
        }

        int baseCost = this.murilloskills$vanillaLevelCost > 0 ? this.murilloskills$vanillaLevelCost : this.levelCost.get();
        if (baseCost <= 0) {
            return;
        }

        // Only apply discount if player has BLACKSMITH selected and meets level
        // requirement
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            if (this.levelCost.get() != baseCost) {
                this.levelCost.set(baseCost);
            }
            return;
        }

        int level = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        int finalCost = baseCost;
        if (level >= SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL) {
            float discount = SkillConfig.getBlacksmithAnvilDiscount(level);
            finalCost = Math.max(1, (int) (baseCost * (1.0f - discount)));
        }

        if (!BlacksmithOverEnchanting.isUnlocked(level)) {
            if (this.levelCost.get() != finalCost) {
                this.levelCost.set(finalCost);
            }
            return;
        }

        var override = BlacksmithOverEnchanting.tryApply(
                input.getStack(0),
                input.getStack(1),
                output.getStack(0),
                finalCost);

        if (override != null) {
            output.setStack(0, override.stack());
            finalCost = override.levelCost();
        }

        if (this.levelCost.get() != finalCost) {
            this.levelCost.set(finalCost);
        }
    }

    /**
     * Grant XP when player takes item from anvil output.
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void onBlacksmithAnvilUse(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        this.murilloskills$refreshFinalAnvilCost();

        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Use accessor to get input from parent class
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        Inventory input = accessor.getInput();

        // Check if this was actually a repair operation
        ItemStack inputItem = input.getStack(0);
        boolean wasRepair = false;

        // An item is considered "repaired" if:
        // 1. The input item is damageable (tools, armor, etc.)
        // 2. The input item has damage (durability lost)
        // 3. OR there's a second item that could be used for repair
        if (inputItem.isDamageable() && inputItem.isDamaged()) {
            wasRepair = true;
        } else {
            // Also check if there's a material in slot 1 that could repair this item
            ItemStack repairMaterial = input.getStack(1);
            if (!repairMaterial.isEmpty() && inputItem.isDamageable()) {
                // If there's a repair material and the item is damageable, it's likely a repair
                // (Could be combining two of the same item, or using repair material)
                wasRepair = true;
            }
        }

        // Track repair for daily challenges only if it was actually a repair
        if (wasRepair) {
            ChallengeEventsHandler.onItemRepaired(serverPlayer);
        }

        var playerData = serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Calculate XP based on action type (simplified - any anvil output gives XP)
        int xp = BlacksmithXpGetter.getAnvilXp(wasRepair, false, false);

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

        // Grant "First Forge" advancement (first anvil use)
        com.murilloskills.utils.AdvancementGranter.grantFirstForge(serverPlayer);
    }
}
