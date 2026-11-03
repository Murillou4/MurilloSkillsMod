package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.api.*;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method="getDamageAfterMagicAbsorb",at=@At("RETURN"),cancellable=true)
    private void damage(DamageSource source,float amount,CallbackInfoReturnable<Float> cir){
        if(!((LivingEntity)(Object)this).level().isClientSide())cir.setReturnValue(GameplayEvents.damage((LivingEntity)(Object)this,source,cir.getReturnValue()));
    }
    @Inject(method="getDamageAfterArmorAbsorb",at=@At("RETURN"),cancellable=true)
    private void penetration(DamageSource source,float amount,CallbackInfoReturnable<Float> cir){
        if(MurilloSkills.store!=null&&source.getDirectEntity() instanceof Projectile&&source.getEntity() instanceof ServerPlayer p){
            var c=GameplayEvents.context(p);var skill=(ArcherSkill)SkillRegistry.get(SkillType.ARCHER);
            float penetration=skill.armorPenetration(c);cir.setReturnValue(cir.getReturnValue()+Math.max(0,amount-cir.getReturnValue())*penetration);
        }
    }
}

