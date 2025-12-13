package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.events.ChallengeEventsHandler;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for AnvilScreenHandler to grant Blacksmith XP and apply perks.
 * - XP when taking output from anvil
 * - Level 25: 25% XP discount (level cost reduction)
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    // Shadow the input field from ForgingScreenHandler (parent class)
    @Shadow
    protected Inventory input;

    @Shadow
    @Final
    private Property levelCost;

    /**
     * Grant XP when player takes item from anvil output.
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void onBlacksmithAnvilUse(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Check if this was actually a repair operation
        // Get the first input slot item to check if it was damageable and damaged
        ItemStack inputItem = this.input.getStack(0);
        boolean wasRepair = false;

        // An item is considered "repaired" if:
        // 1. The input item is damageable (tools, armor, etc.)
        // 2. The input item has damage (durability lost)
        // 3. OR there's a second item that could be used for repair
        if (inputItem.isDamageable() && inputItem.isDamaged()) {
            wasRepair = true;
        } else {
            // Also check if there's a material in slot 1 that could repair this item
            ItemStack repairMaterial = this.input.getStack(1);
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

        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Calculate XP based on action type (simplified - any anvil output gives XP)
        int xp = BlacksmithXpGetter.getAnvilXp(wasRepair, false, false);

        // Add XP using the central method that handles paragon constraints
        SkillGlobalState.XpAddResult xpResult = playerData.addXpToSkill(MurilloSkillsList.BLACKSMITH, xp);
        state.markDirty();

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Ferreiro", xpResult);

        // Notify player on level up
        if (xpResult.leveledUp()) {
            int newLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BLACKSMITH, newLevel);
        }

        // Sync skill data with client
        SkillsNetworkUtils.syncSkills(serverPlayer);

        // Apply 25% XP discount at level 25+ (reduce the cost shown)
        int level = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
        if (level >= SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL) {
            int currentCost = this.levelCost.get();
            if (currentCost > 0) {
                int discountedCost = Math.max(1, (int) (currentCost * (1 - SkillConfig.BLACKSMITH_ANVIL_XP_DISCOUNT)));
                this.levelCost.set(discountedCost);
            }
        }
    }
}
