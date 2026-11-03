package com.murilloskills.mixin;
import com.murilloskills.MurilloSkills;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractArrow.class)
abstract class ArrowSkillMixin {
    @Unique private int murilloskills$original=-1;
    @Inject(method="onHitEntity",at=@At("HEAD"))
    private void hit(net.minecraft.world.phys.EntityHitResult hit,CallbackInfo ci){
        var arrow=(AbstractArrow)(Object)this;if(MurilloSkills.store!=null&&arrow.getOwner() instanceof ServerPlayer p)GameplayEvents.context(p).runtime().impacts.put(arrow.getUUID(),hit.getLocation());
    }
    @Inject(method="onHitEntity",at=@At("RETURN"))
    private void clearHit(net.minecraft.world.phys.EntityHitResult hit,CallbackInfo ci){
        var arrow=(AbstractArrow)(Object)this;if(MurilloSkills.store!=null&&arrow.getOwner() instanceof ServerPlayer p)GameplayEvents.context(p).runtime().impacts.remove(arrow.getUUID());
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void skill(CallbackInfo ci){
        var arrow=(AbstractArrow)(Object)this;
        if(MurilloSkills.store==null||!(arrow.getOwner() instanceof ServerPlayer p))return;
        if(murilloskills$original<0)murilloskills$original=arrow.getPierceLevel();
        var c=GameplayEvents.context(p);var skill=(ArcherSkill)SkillRegistry.get(SkillType.ARCHER);
        ((ArrowAccess)arrow).murilloskills$pierce((byte)Math.max(murilloskills$original,skill.piercing(c)));
        skill.guide(c,arrow);
    }
}
