package com.murilloskills.mixin;
import com.murilloskills.MurilloSkills;
import com.murilloskills.events.GameplayEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ComposterBlock.class)
abstract class ComposterMixin {
    @Inject(method="extractProduce",at=@At("RETURN"))
    private static void extra(Entity entity,BlockState state,Level level,BlockPos pos,CallbackInfoReturnable<BlockState> ci){
        if(entity instanceof ServerPlayer p&&MurilloSkills.store!=null&&state.getValue(ComposterBlock.LEVEL)==8&&GameplayEvents.context(p).has(com.murilloskills.skills.SkillType.FARMER,50))p.drop(new ItemStack(Items.BONE_MEAL),false);
    }
}
