package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
abstract class FurnaceMixin {
    @Shadow private int cookingTimer,cookingTotalTime,litTimeRemaining;
    @Unique private double murilloskills$fraction;
    @Inject(method="serverTick",at=@At("TAIL"))
    private static void boost(ServerLevel world,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
        if(MurilloSkills.store==null||world.getGameTime()%4!=0)return;
        var mixin=(FurnaceMixin)(Object)furnace;
        if(mixin.cookingTimer<=0||mixin.cookingTimer>=mixin.cookingTotalTime-1||mixin.litTimeRemaining<=0)return;
        double best=1;
        for(var player:world.players())if(player.blockPosition().distSqr(pos)<=64){
            var c=GameplayEvents.context(player);best=Math.max(best,((BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH)).furnaceMultiplier(c));
        }
        mixin.murilloskills$fraction+=(best-1)*4;
        int extra=(int)mixin.murilloskills$fraction;mixin.murilloskills$fraction-=extra;
        mixin.cookingTimer=Math.min(mixin.cookingTotalTime-1,mixin.cookingTimer+extra);
    }
}

