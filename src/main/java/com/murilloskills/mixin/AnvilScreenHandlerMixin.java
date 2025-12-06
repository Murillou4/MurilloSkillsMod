package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
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

        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Calculate XP based on action type (simplified - any anvil output gives XP)
        int xp = BlacksmithXpGetter.getAnvilXp(true, false, false);

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
