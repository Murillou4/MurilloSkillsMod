package com.murilloskills.mixin;

import com.murilloskills.accessor.AnvilCostSyncAccessor;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.events.ChallengeEventsHandler;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithOverEnchanting;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for AnvilScreenHandler to grant Blacksmith XP and apply perks.
 * - XP when taking output from anvil
 * - Level 25: large XP cost reduction
 * - Tracks item repairs for daily challenges
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin implements AnvilCostSyncAccessor {

    @Unique
    private static final int MURILLOSKILLS_BLACKSMITH_MASTERY_MAX_ANVIL_COST = 25;

    // Shadow the levelCost property from AnvilScreenHandler
    @Shadow
    @Final
    private Property levelCost;

    @Unique
    private int murilloskills$vanillaLevelCost;

    @Unique
    private final Property murilloskills$originalLevelCost = Property.create();

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At("TAIL"))
    private void murilloskills$registerOriginalCostProperty(int syncId, PlayerInventory inventory,
            ScreenHandlerContext context, CallbackInfo ci) {
        ((ScreenHandlerInvoker) (Object) this).murilloskills$invokeAddProperty(this.murilloskills$originalLevelCost);
    }

    /**
     * Apply Blacksmith XP cost discount after vanilla calculates the cost.
     *
     * We run at TAIL for compatibility and push an extra content sync whenever
     * our adjustments change the final cost/output so the client UI stays in
     * lockstep with what is actually charged on take.
     *
     * Discount scales with level:
     * - Level 25: 40% discount (base)
     * - Level 100: 65% discount
     * - Level 99+: cost is additionally capped for Master Enchanter operations
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void applyBlacksmithAnvilDiscount(CallbackInfo ci) {
        this.murilloskills$vanillaLevelCost = this.levelCost.get();
        boolean changed = this.murilloskills$refreshFinalAnvilCost();
        if (changed) {
            ((ScreenHandler) (Object) this).sendContentUpdates();
        }
    }

    @Unique
    private boolean murilloskills$refreshFinalAnvilCost() {
        // Use accessor to get player from parent class
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        PlayerEntity player = accessor.getPlayer();

        // Only apply on server side
        if (player == null || player.getEntityWorld().isClient()) {
            return false;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        var playerData = serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        Inventory input = accessor.getInput();
        Inventory output = accessor.getOutput();
        if (input == null || output == null) {
            return false;
        }

        ItemStack firstInput = input.getStack(0);
        ItemStack secondInput = input.getStack(1);
        ItemStack vanillaOutput = output.getStack(0);

        int baseCost = this.murilloskills$vanillaLevelCost > 0 ? this.murilloskills$vanillaLevelCost : this.levelCost.get();
        int safeBaseCost = Math.max(0, baseCost);

        boolean changed = false;
        if (this.murilloskills$originalLevelCost.get() != safeBaseCost) {
            this.murilloskills$originalLevelCost.set(safeBaseCost);
            changed = true;
        }

        // Only apply discount if player has BLACKSMITH selected and meets level
        // requirement
        boolean blacksmithSelected = playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH);
        if (!blacksmithSelected) {
            if (this.levelCost.get() != safeBaseCost) {
                this.levelCost.set(safeBaseCost);
                changed = true;
            }
            return changed;
        }

        int level = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        boolean masterEnchanterUnlocked = BlacksmithOverEnchanting.isUnlocked(level);

        if (safeBaseCost <= 0 && !masterEnchanterUnlocked) {
            if (this.levelCost.get() != 0) {
                this.levelCost.set(0);
                changed = true;
            }
            return changed;
        }

        int finalCost = safeBaseCost > 0 ? safeBaseCost : 1;
        if (safeBaseCost > 0 && level >= SkillConfig.getBlacksmithEfficientAnvilLevel()) {
            float discount = SkillConfig.getBlacksmithAnvilDiscount(level);
            finalCost = Math.max(1, Math.round(safeBaseCost * (1.0f - discount)));
        }

        if (masterEnchanterUnlocked) {
            var override = BlacksmithOverEnchanting.tryApply(
                    firstInput,
                    secondInput,
                    vanillaOutput,
                    finalCost);

            // Rebuild over-enchant results from the two inputs when vanilla blocks or
            // clears the output (too expensive or invalid intermediate states).
            if (override == null && vanillaOutput.isEmpty()) {
                override = BlacksmithOverEnchanting.tryApply(
                        firstInput,
                        secondInput,
                        ItemStack.EMPTY,
                        finalCost);
            }

            if (override != null) {
                ItemStack overrideStack = override.stack();
                if (!ItemStack.areEqual(output.getStack(0), overrideStack)) {
                    output.setStack(0, overrideStack);
                    changed = true;
                }
                finalCost = override.levelCost();
            } else if (safeBaseCost <= 0) {
                finalCost = 0;
            }

            // Master Enchanter should never hit "Too Expensive" and should feel cheap.
            if (finalCost > 0) {
                finalCost = Math.max(1, Math.min(finalCost, MURILLOSKILLS_BLACKSMITH_MASTERY_MAX_ANVIL_COST));
            }
        }

        if (this.levelCost.get() != finalCost) {
            this.levelCost.set(finalCost);
            changed = true;
        }

        return changed;
    }

    /**
     * Grant XP when player takes item from anvil output.
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void onBlacksmithAnvilUse(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
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

    @Override
    public int murilloskills$getSyncedOriginalLevelCost() {
        return this.murilloskills$originalLevelCost.get();
    }
}
