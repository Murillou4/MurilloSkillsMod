package com.murilloskills.mixin;

import com.murilloskills.utils.BlacksmithXpGetter;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for AbstractFurnaceBlockEntity to grant Blacksmith XP when smelting
 * ores.
 * Only grants XP for ore-to-ingot/material smelting (iron, gold, copper,
 * ancient debris).
 * 
 * Note: XP granting is currently handled via FurnaceOutputMixin which has
 * player context. This mixin validates ore smelting but cannot grant XP
 * directly due to static context limitations.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    /**
     * Validates when a smelting operation completes.
     * Actual XP granting is handled in FurnaceOutputMixin when player takes output.
     */
    @Inject(method = "craftRecipe", at = @At("HEAD"))
    private static void onSmeltComplete(
            DynamicRegistryManager registryManager,
            RecipeEntry<?> recipe,
            SingleStackRecipeInput input,
            DefaultedList<ItemStack> slots,
            int count,
            CallbackInfoReturnable<Boolean> cir) {

        // Get the input item being smelted (first slot in furnace)
        ItemStack inputStack = input.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            return;
        }

        // Validate this is an ore that would grant XP (logging only)
        BlacksmithXpGetter.getSmeltingXp(inputStack.getItem());
        // XP is granted in FurnaceOutputMixin when player retrieves the output
    }
}
