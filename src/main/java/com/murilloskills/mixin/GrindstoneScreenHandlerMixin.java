package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for GrindstoneScreenHandler to grant Blacksmith XP when using
 * grindstone.
 * 
 * The grindstone output slot is slot index 2.
 * We hook into quickMove (shift-click) and onContentChanged to detect when
 * output is taken.
 */
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {

    @Unique
    private static final int GRINDSTONE_OUTPUT_SLOT = 2;

    @Unique
    private ItemStack murilloskills$lastOutputStack = ItemStack.EMPTY;

    @Unique
    private PlayerEntity murilloskills$currentPlayer = null;

    /**
     * Hook into quickMove to detect shift-click on the output slot.
     */
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void onGrindstoneQuickMove(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        // Track the player for later use
        this.murilloskills$currentPlayer = player;

        if (slot != GRINDSTONE_OUTPUT_SLOT) {
            return;
        }

        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GrindstoneScreenHandler handler = (GrindstoneScreenHandler) (Object) this;
        ItemStack outputStack = handler.getSlot(GRINDSTONE_OUTPUT_SLOT).getStack();

        // Only grant XP if there's something in the output to take
        if (outputStack.isEmpty()) {
            return;
        }

        grantGrindstoneXp(serverPlayer);
    }

    /**
     * Hook into onContentChanged to track when output is taken.
     * This detects when an item disappears from the output slot.
     */
    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void onGrindstoneContentChanged(Inventory inventory, CallbackInfo ci) {
        GrindstoneScreenHandler handler = (GrindstoneScreenHandler) (Object) this;
        ItemStack currentOutput = handler.getSlot(GRINDSTONE_OUTPUT_SLOT).getStack();

        // If there was an output item and now it's gone, player took it
        if (!this.murilloskills$lastOutputStack.isEmpty() && currentOutput.isEmpty()) {
            // Try to find the player who took the item
            PlayerEntity player = this.murilloskills$currentPlayer;
            if (player instanceof ServerPlayerEntity serverPlayer && !serverPlayer.getEntityWorld().isClient()) {
                grantGrindstoneXp(serverPlayer);
            }
        }

        this.murilloskills$lastOutputStack = currentOutput.copy();
    }

    /**
     * Grants Blacksmith XP for using the grindstone.
     */
    @Unique
    private void grantGrindstoneXp(ServerPlayerEntity serverPlayer) {
        var playerData = serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Get XP for grindstone use
        int xp = BlacksmithXpGetter.getGrindstoneXp();

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
    }
}
