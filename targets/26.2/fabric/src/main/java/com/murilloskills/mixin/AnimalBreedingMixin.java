package com.murilloskills.mixin;
import com.murilloskills.MurilloSkills;
import com.murilloskills.events.GameplayEvents;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
@Mixin(Animal.class)
abstract class AnimalBreedingMixin {
    @Inject(method="finalizeSpawnChildFromBreeding",at=@At("HEAD"))
    private void before(ServerLevel world,Animal mate,AgeableMob child,CallbackInfo ci,@Share("owner") LocalRef<ServerPlayer> owner){
        var self=(Animal)(Object)this;owner.set(self.getLoveCause()!=null?self.getLoveCause():mate.getLoveCause());
    }
    @Inject(method="finalizeSpawnChildFromBreeding",at=@At("RETURN"))
    private void after(ServerLevel world,Animal mate,AgeableMob child,CallbackInfo ci,@Share("owner") LocalRef<ServerPlayer> owner){
        if(child!=null&&owner.get()!=null&&MurilloSkills.store!=null)GameplayEvents.context(owner.get()).xp("breed",1);
    }
}

