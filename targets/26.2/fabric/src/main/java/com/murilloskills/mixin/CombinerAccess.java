package com.murilloskills.mixin;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemCombinerMenu.class)
public interface CombinerAccess {
    @Accessor("player") Player murilloskills$player();
    @Accessor("resultSlots") net.minecraft.world.inventory.ResultContainer murilloskills$resultSlots();
}

