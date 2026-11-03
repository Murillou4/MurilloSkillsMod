package com.murilloskills.mixin;
import com.murilloskills.events.GameplayEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    @Inject(method="place",at=@At("HEAD"))
    private void before(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> cir,@Share("count") LocalIntRef count){count.set(context.getItemInHand().getCount());}
    @Inject(method="place",at=@At("RETURN"))
    private void placed(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> cir,@Share("count") LocalIntRef count){
        if(cir.getReturnValue().consumesAction()&&context.getPlayer() instanceof ServerPlayer p&&!context.getLevel().getBlockState(context.getClickedPos()).isAir()){
            boolean first=GameplayEvents.placed(p,context.getClickedPos(),context.getLevel().getBlockState(context.getClickedPos()));
            if(com.murilloskills.MurilloSkills.store!=null&&first&&count.get()==context.getItemInHand().getCount()+1&&!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof net.minecraft.world.level.block.EntityBlock)){
                var c=GameplayEvents.context(p);var skill=(com.murilloskills.impl.BuilderSkill)com.murilloskills.api.SkillRegistry.get(com.murilloskills.skills.SkillType.BUILDER);
                if(com.murilloskills.utils.WorldTools.economicBlock(context.getLevel().getBlockState(context.getClickedPos()))&&p.getRandom().nextDouble()<skill.economy(c))context.getItemInHand().grow(1);
            }
            if(com.murilloskills.MurilloSkills.store!=null&&count.get()==context.getItemInHand().getCount()+1&&context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof net.minecraft.world.level.block.CropBlock){
                var c=GameplayEvents.context(p);if(p.getRandom().nextDouble()<((com.murilloskills.impl.FarmerSkill)com.murilloskills.api.SkillRegistry.get(com.murilloskills.skills.SkillType.FARMER)).seedConservation(c))context.getItemInHand().grow(1);
            }
        }
    }
}

