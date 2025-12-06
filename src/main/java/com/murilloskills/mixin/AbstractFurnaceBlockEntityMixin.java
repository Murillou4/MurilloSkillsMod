package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithXpGetter;
import com.murilloskills.utils.SkillNotifier;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
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
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    /**
     * Grant XP when a smelting operation completes successfully.
     * Injects into craftRecipe which is called when an item finishes smelting.
     * 
     * Method signature for MC 1.21.1:
     * private static boolean craftRecipe(DynamicRegistryManager registryManager,
     * RecipeEntry<?> recipe,
     * SingleStackRecipeInput input, DefaultedList<ItemStack> slots, int count)
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

        // Check if this is an ore that grants XP
        var xpResult = BlacksmithXpGetter.getSmeltingXp(inputStack.getItem());
        if (!xpResult.didGainXp()) {
            return;
        }

        // Note: In a static context without world access, we cannot easily find the
        // player
        // This mixin will be simplified - furnace smelting XP will be handled
        // differently
        // For now, this mixin is disabled for smelting XP
        // A better approach would be to track who placed items in furnaces via a
        // separate system
    }
}
