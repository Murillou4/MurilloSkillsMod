package com.murilloskills.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
@Mixin(BlockItem.class)
public interface BlockItemAccess {
    @Invoker("getPlacementState") BlockState murilloskills$state(BlockPlaceContext context);
    @Invoker("canPlace") boolean murilloskills$canPlace(BlockPlaceContext context,BlockState state);
}

