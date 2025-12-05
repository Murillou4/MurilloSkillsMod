package com.murilloskills.mixin;

import com.murilloskills.skills.FishingCatchHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for ItemEntity to detect when fishing loots are spawned.
 * This hooks into the ItemEntity constructor to detect items spawned by
 * fishing.
 */
@Mixin(ItemEntity.class)
public class ItemEntityFishingMixin {

    /**
     * After an ItemEntity is spawned, check if it was from fishing.
     * We use a heuristic: if there's a fishing bobber nearby owned by a player,
     * and the item was just spawned, it's likely a fishing catch.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void onItemEntityCreated(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (world.isClient()) {
            return;
        }

        // Look for nearby fishing bobbers to determine if this is a fishing catch
        ItemEntity self = (ItemEntity) (Object) this;

        for (Entity entity : world.getOtherEntities(self, self.getBoundingBox().expand(2.0))) {
            if (entity instanceof FishingBobberEntity bobber) {
                PlayerEntity player = bobber.getPlayerOwner();
                if (player != null) {
                    // This item was spawned near a fishing bobber - likely a catch!
                    FishingCatchHandler.handle(player, stack);
                    break;
                }
            }
        }
    }
}
