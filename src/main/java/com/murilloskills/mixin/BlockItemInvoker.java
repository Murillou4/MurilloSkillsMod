package com.murilloskills.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockItem.class)
public interface BlockItemInvoker {

    @Invoker("getPlacementState")
    BlockState murilloskills$invokeGetPlacementState(ItemPlacementContext context);

    @Invoker("canPlace")
    boolean murilloskills$invokeCanPlace(ItemPlacementContext context, BlockState state);
}
