package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
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
 * We hook into onSlotClick to detect when player takes output, and quickMove
 * for shift-click.
 */
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {

    @Unique
    private static final int GRINDSTONE_OUTPUT_SLOT = 2;

    /**
     * Hook into onSlotClick to detect when player clicks on the output slot.
     * This catches normal clicks on the output.
     */
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onGrindstoneSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player,
            CallbackInfo ci) {
        if (slotIndex != GRINDSTONE_OUTPUT_SLOT) {
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
     * Hook into quickMove to detect shift-click on the output slot.
     */
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void onGrindstoneQuickMove(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
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
     * Grants Blacksmith XP for using the grindstone.
     */
    @Unique
    private void grantGrindstoneXp(ServerPlayerEntity serverPlayer) {
        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Get XP for grindstone use
        int xp = BlacksmithXpGetter.getGrindstoneXp();

        // Add XP using the central method that handles paragon constraints
        boolean leveledUp = playerData.addXpToSkill(MurilloSkillsList.BLACKSMITH, xp);
        state.markDirty();

        // Notify player on level up
        if (leveledUp) {
            int newLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BLACKSMITH, newLevel);
        }

        // Sync skill data with client
        SkillsNetworkUtils.syncSkills(serverPlayer);
    }
}
