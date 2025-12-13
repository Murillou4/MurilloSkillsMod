package com.murilloskills.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ForgingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface to access protected fields from ForgingScreenHandler.
 * Used by AnvilScreenHandlerMixin to get player and input fields.
 */
@Mixin(ForgingScreenHandler.class)
public interface ForgingScreenHandlerAccessor {

    @Accessor("player")
    PlayerEntity getPlayer();

    @Accessor("input")
    Inventory getInput();
}
