package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ResultSlot.class)
abstract class CraftingMixin {
    @Inject(method="onTake",at=@At("TAIL"))
    private void decoration(Player owner,ItemStack result,CallbackInfo ci){
        if(owner instanceof ServerPlayer p&&MurilloSkills.store!=null)((com.murilloskills.impl.BuilderSkill)com.murilloskills.api.SkillRegistry.get(com.murilloskills.skills.SkillType.BUILDER)).crafted(GameplayEvents.context(p),result);
    }
    @Inject(method="onTake",at=@At("HEAD"))
    private void crafted(Player owner,ItemStack result,CallbackInfo ci){
        if(owner instanceof ServerPlayer p&&MurilloSkills.store!=null&&result.isDamageableItem())GameplayEvents.context(p).xp("forge",1);
    }
}

