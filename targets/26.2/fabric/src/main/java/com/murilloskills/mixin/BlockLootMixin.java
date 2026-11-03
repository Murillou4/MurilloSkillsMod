package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.events.GameplayEvents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
abstract class BlockLootMixin {
    @Inject(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",at=@At("RETURN"),cancellable=true)
    private static void drops(BlockState state,ServerLevel level,BlockPos pos,BlockEntity be,Entity entity,ItemInstance tool,CallbackInfoReturnable<List<ItemStack>> cir){
        if(MurilloSkills.store==null||!(entity instanceof ServerPlayer p))return;
        var c=GameplayEvents.context(p);
        if(!c.store().natural(p,pos)&&!(state.getBlock() instanceof CropBlock))return;
        var drops=new ArrayList<ItemStack>();for(var s:cir.getReturnValue())drops.add(s.copy());
        for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))skill.drops(c,state,drops);
        cir.setReturnValue(drops);
    }
}

