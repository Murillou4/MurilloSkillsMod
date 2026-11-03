package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.api.*;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method="getBlockSpeedFactor",at=@At("RETURN"),cancellable=true)
    private void soulSand(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> ci){
        if((Object)this instanceof ServerPlayer p&&MurilloSkills.store!=null){
            var c=GameplayEvents.context(p);
            if(com.murilloskills.impl.ExplorerSkill.NETHER.value(c,SkillType.EXPLORER)>0&&
                (p.level().getBlockState(p.blockPosition()).is(net.minecraft.world.level.block.Blocks.SOUL_SAND)||p.level().getBlockState(p.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.SOUL_SAND)))ci.setReturnValue(Math.max(1f,ci.getReturnValue()));
        }
    }
    @ModifyVariable(method="causeFoodExhaustion",at=@At("HEAD"),argsOnly=true)
    private float hunger(float amount){
        if((Object)this instanceof ServerPlayer p&&MurilloSkills.store!=null)return amount*((ExplorerSkill)SkillRegistry.get(SkillType.EXPLORER)).hungerMultiplier(GameplayEvents.context(p));
        return amount;
    }
}

