package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for CraftingResultSlot to track item crafting for daily challenges.
 */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultMixin {

    /**
     * Track when player crafts an item
     */
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onCraft(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        int craftedAmount = stack.getCount();
        ChallengeEventsHandler.onItemCrafted(serverPlayer, craftedAmount);
    }
}
