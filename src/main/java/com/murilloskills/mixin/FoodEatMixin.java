package com.murilloskills.mixin;

import com.murilloskills.events.ChallengeEventsHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to track when players eat food for daily challenge progress.
 * Targets ItemStack.finishUsing which is called when an item finishes being
 * used (including eating).
 */
@Mixin(ItemStack.class)
public abstract class FoodEatMixin {

    /**
     * Called when an item finishes being used.
     * For food items, this is when eating is complete.
     * 
     * IMPORTANT: We inject at HEAD because after finishUsing completes,
     * the item stack count may have decreased and food component checks may fail.
     */
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsing(World world, net.minecraft.entity.LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient()) {
            return;
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;

        // Check if the item is food BEFORE it's consumed (at HEAD, not RETURN)
        // Using get() != null is more reliable than contains()
        if (self.get(net.minecraft.component.DataComponentTypes.FOOD) != null) {
            // Track eating for daily challenges
            ChallengeEventsHandler.onFoodEaten(serverPlayer);
        }
    }
}
