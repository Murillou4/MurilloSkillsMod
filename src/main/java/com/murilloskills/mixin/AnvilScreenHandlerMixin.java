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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    private ItemStack murilloskills$baselineFirstInput = ItemStack.EMPTY;

    @Unique
    private ItemStack murilloskills$baselineSecondInput = ItemStack.EMPTY;

    @Unique
    private ItemStack murilloskills$baselineVanillaOutput = ItemStack.EMPTY;

    @Unique
    private boolean murilloskills$hasBaselineSnapshot;

    @Unique
    private boolean murilloskills$stickyBlacksmithSelected;

    @Unique
    private int murilloskills$stickyBlacksmithLevel;

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
     * - Level 100: up to 90% discount (configurable)
     * - Level 99+: cost is additionally capped for Master Enchanter operations
     */
    @Inject(method = "updateResult", at = @At("TAIL"), order = 2000)
    private void applyBlacksmithAnvilDiscount(CallbackInfo ci) {
        this.murilloskills$vanillaLevelCost = this.levelCost.get();
        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) this;
        this.murilloskills$captureVanillaBaseline(accessor.getInput(), accessor.getOutput());
        boolean changed = this.murilloskills$refreshFinalAnvilCost();
        if (changed) {
            ((ScreenHandler) (Object) this).sendContentUpdates();
        }
    }

    /**
     * Re-evaluate final anvil cost right before vanilla checks level gate.
     *
     * This protects against third-party flow changes that can reapply vanilla cost
     * after updateResult and before the output-take check.
     */
    @Inject(method = "canTakeOutput", at = @At("HEAD"), order = 2000)
    private void murilloskills$refreshBeforeCanTake(
            PlayerEntity player,
            boolean present,
            CallbackInfoReturnable<Boolean> cir) {
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
        boolean usingBaselineSnapshot = this.murilloskills$hasBaselineSnapshot
                && this.murilloskills$matchesBaselineInputs(firstInput, secondInput);
        if (usingBaselineSnapshot) {
            vanillaOutput = this.murilloskills$baselineVanillaOutput;
        }

        int baseCost = this.murilloskills$vanillaLevelCost > 0 ? this.murilloskills$vanillaLevelCost : this.levelCost.get();
        if (baseCost <= 0 && this.murilloskills$originalLevelCost.get() > 0) {
            baseCost = this.murilloskills$originalLevelCost.get();
        }
        int safeBaseCost = Math.max(0, baseCost);

        boolean changed = false;
        if (this.murilloskills$originalLevelCost.get() != safeBaseCost) {
            this.murilloskills$originalLevelCost.set(safeBaseCost);
            changed = true;
        }

        boolean blacksmithSelected = playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH);
        int level = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        boolean qualifiesByLevel = level >= SkillConfig.getBlacksmithEfficientAnvilLevel();
        boolean blacksmithActiveForAnvil = blacksmithSelected || qualifiesByLevel;
        if (blacksmithActiveForAnvil) {
            this.murilloskills$stickyBlacksmithSelected = true;
            if (level > this.murilloskills$stickyBlacksmithLevel) {
                this.murilloskills$stickyBlacksmithLevel = level;
            }
        } else if (this.murilloskills$stickyBlacksmithSelected) {
            // Keep Blacksmith status stable for the opened anvil handler to avoid
            // transient attachment reads from disabling perks mid-operation.
            blacksmithActiveForAnvil = true;
            level = Math.max(level, this.murilloskills$stickyBlacksmithLevel);
        }

        if (!blacksmithActiveForAnvil) {
            if (this.levelCost.get() != safeBaseCost) {
                this.levelCost.set(safeBaseCost);
                changed = true;
            }
            return changed;
        }

        level = Math.max(level, this.murilloskills$stickyBlacksmithLevel);
        boolean masterEnchanterUnlocked = BlacksmithOverEnchanting.isUnlocked(level);

        if (safeBaseCost <= 0 && !masterEnchanterUnlocked) {
            if (this.levelCost.get() != 0) {
                this.levelCost.set(0);
                changed = true;
            }
            return changed;
        }

        boolean canApplyDiscount = safeBaseCost > 0 && level >= SkillConfig.getBlacksmithEfficientAnvilLevel();
        float discount = canApplyDiscount ? SkillConfig.getBlacksmithAnvilDiscount(level) : 0f;
        int preDiscountCost = safeBaseCost > 0 ? safeBaseCost : 1;

        if (masterEnchanterUnlocked) {
            var override = BlacksmithOverEnchanting.tryApply(
                    firstInput,
                    secondInput,
                    vanillaOutput,
                    preDiscountCost);

            // Rebuild over-enchant results from the two inputs when vanilla blocks or
            // clears the output (too expensive or invalid intermediate states).
            if (override == null && vanillaOutput.isEmpty()) {
                override = BlacksmithOverEnchanting.tryApply(
                        firstInput,
                        secondInput,
                        ItemStack.EMPTY,
                        preDiscountCost);
            }

            if (override != null) {
                ItemStack overrideStack = override.stack();
                if (!ItemStack.areEqual(output.getStack(0), overrideStack)) {
                    output.setStack(0, overrideStack);
                    changed = true;
                }
                preDiscountCost = override.levelCost();
            } else if (safeBaseCost <= 0) {
                preDiscountCost = 0;
            }

            // Master Enchanter should never hit "Too Expensive" and should feel cheap.
            if (preDiscountCost > 0) {
                preDiscountCost = Math.max(1,
                        Math.min(preDiscountCost, MURILLOSKILLS_BLACKSMITH_MASTERY_MAX_ANVIL_COST));
            }
        }

        int finalCost = preDiscountCost;
        if (canApplyDiscount && finalCost > 0) {
            finalCost = Math.max(1, Math.round(finalCost * (1.0f - discount)));
        }

        // Blacksmith perk should never make an operation more expensive than vanilla.
        if (safeBaseCost > 0 && finalCost > safeBaseCost) {
            finalCost = safeBaseCost;
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

        // Keep charged level cost in sync right before vanilla consumes XP.
        if (this.murilloskills$refreshFinalAnvilCost()) {
            ((ScreenHandler) (Object) this).sendContentUpdates();
        }

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
        this.murilloskills$applyTakeOutputMasterEnchanterFallback(playerData, input, stack);

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

    @Unique
    private void murilloskills$captureVanillaBaseline(Inventory input, Inventory output) {
        if (input == null || output == null) {
            this.murilloskills$hasBaselineSnapshot = false;
            this.murilloskills$baselineFirstInput = ItemStack.EMPTY;
            this.murilloskills$baselineSecondInput = ItemStack.EMPTY;
            this.murilloskills$baselineVanillaOutput = ItemStack.EMPTY;
            return;
        }

        this.murilloskills$baselineFirstInput = input.getStack(0).copy();
        this.murilloskills$baselineSecondInput = input.getStack(1).copy();
        this.murilloskills$baselineVanillaOutput = output.getStack(0).copy();
        this.murilloskills$hasBaselineSnapshot = true;
    }

    @Unique
    private boolean murilloskills$matchesBaselineInputs(ItemStack firstInput, ItemStack secondInput) {
        if (!this.murilloskills$hasBaselineSnapshot) {
            return false;
        }
        return ItemStack.areEqual(firstInput, this.murilloskills$baselineFirstInput)
                && ItemStack.areEqual(secondInput, this.murilloskills$baselineSecondInput);
    }

    @Unique
    private void murilloskills$applyTakeOutputMasterEnchanterFallback(
            PlayerSkillData playerData,
            Inventory input,
            ItemStack takenStack) {
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        int level = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        if (!BlacksmithOverEnchanting.isUnlocked(level)) {
            return;
        }

        ItemStack firstInput = input.getStack(0);
        ItemStack secondInput = input.getStack(1);
        if (firstInput.isEmpty() || secondInput.isEmpty() || takenStack.isEmpty()) {
            return;
        }

        int currentCost = Math.max(1, this.levelCost.get());
        var override = BlacksmithOverEnchanting.tryApply(firstInput, secondInput, takenStack, currentCost);
        if (override == null) {
            return;
        }

        ItemStack overrideStack = override.stack();
        if (!ItemStack.areEqual(takenStack, overrideStack)) {
            takenStack.applyComponentsFrom(overrideStack.getComponents());
            takenStack.setCount(overrideStack.getCount());
        }

        int cappedCost = Math.max(1, Math.min(override.levelCost(), MURILLOSKILLS_BLACKSMITH_MASTERY_MAX_ANVIL_COST));
        if (this.levelCost.get() != cappedCost) {
            this.levelCost.set(cappedCost);
        }
    }
}
