package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import com.murilloskills.utils.SkillSynergyManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

        // [New Feature] Master Crafter Synergy (Builder + Blacksmith)
        // Chance to duplicate the crafted item
        float duplicateChance = SkillSynergyManager.getTotalBonus(serverPlayer,
                SkillSynergyManager.SynergyType.CRAFTING_EFFICIENCY);

        if (duplicateChance > 0 && serverPlayer.getRandom().nextFloat() < duplicateChance) {
            ItemStack bonusStack = stack.copy();
            if (!serverPlayer.getInventory().insertStack(bonusStack)) {
                serverPlayer.dropItem(bonusStack, false);
            }
            serverPlayer.sendMessage(Text.translatable("murilloskills.synergy.master_crafter.proc")
                    .formatted(Formatting.GOLD), true);
        }
    }
}
