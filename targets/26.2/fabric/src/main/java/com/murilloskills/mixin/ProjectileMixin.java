package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.api.*;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Projectile.class)
abstract class ProjectileMixin {
    @ModifyVariable(method="shoot",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private float spread(float value){
        if((Object)this instanceof AbstractArrow arrow&&arrow.getOwner() instanceof ServerPlayer p&&MurilloSkills.store!=null){
            var c=GameplayEvents.context(p);return value*((ArcherSkill)SkillRegistry.get(SkillType.ARCHER)).spreadMultiplier(c);
        }
        return value;
    }
    @Inject(method="shoot",at=@At("TAIL"))
    private void velocity(double x,double y,double z,float speed,float spread,CallbackInfo ci){
        if((Object)this instanceof AbstractArrow arrow&&arrow.getOwner() instanceof ServerPlayer p&&MurilloSkills.store!=null){
            var c=GameplayEvents.context(p);arrow.setDeltaMovement(arrow.getDeltaMovement().scale(((ArcherSkill)SkillRegistry.get(SkillType.ARCHER)).arrowSpeed(c)));
        }
    }
}

