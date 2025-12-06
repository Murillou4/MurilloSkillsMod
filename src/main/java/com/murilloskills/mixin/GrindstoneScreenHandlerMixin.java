package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for GrindstoneScreenHandler to grant Blacksmith XP when using
 * grindstone.
 */
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {

    /**
     * Grant XP when player takes item from grindstone output.
     * Uses a method injection into the anonymous Slot class's onTakeItem.
     */
    @Inject(method = "method_24922", at = @At("HEAD"))
    private void onBlacksmithGrindstoneUse(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Skip if stack is empty (nothing was taken)
        if (stack.isEmpty()) {
            return;
        }

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
