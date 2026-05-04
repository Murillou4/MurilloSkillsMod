package com.murilloskills.mixin;

import com.murilloskills.utils.BlacksmithMachineSpeedHelper;
import com.murilloskills.utils.BlacksmithXpGetter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for AbstractFurnaceBlockEntity to grant Blacksmith XP when smelting
 * ores and to speed up furnaces when a Blacksmith player is nearby.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow
    int cookingTimeSpent;

    @Shadow
    int cookingTotalTime;

    @Shadow
    int litTimeRemaining;

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

        ItemStack inputStack = input.getStackInSlot(0);
        if (inputStack.isEmpty()) {
            return;
        }

        BlacksmithXpGetter.getSmeltingXp(inputStack.getItem());
    }

    /**
     * Blacksmith Machine Mastery: speeds up nearby furnaces based on Blacksmith level.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private static void onFurnaceTick(ServerWorld world, BlockPos pos, BlockState state,
            AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        AbstractFurnaceBlockEntityMixin self = (AbstractFurnaceBlockEntityMixin) (Object) blockEntity;

        // Only boost if furnace is actively cooking (has fuel and progress)
        if (self.litTimeRemaining <= 0 || self.cookingTimeSpent <= 0 || self.cookingTimeSpent >= self.cookingTotalTime)
            return;

        int bestLevel = BlacksmithMachineSpeedHelper.getBestNearbyBlacksmithLevel(world, pos);
        int extraTicks = BlacksmithMachineSpeedHelper.getExtraProgressTicks(world, bestLevel);
        if (extraTicks <= 0)
            return;

        // Add extra cook time, capped at cookingTotalTime - 1 to let normal crafting logic handle completion
        self.cookingTimeSpent = Math.min(self.cookingTimeSpent + extraTicks, self.cookingTotalTime - 1);

        BlacksmithMachineSpeedHelper.spawnSpeedParticles(world, pos);
    }
}
