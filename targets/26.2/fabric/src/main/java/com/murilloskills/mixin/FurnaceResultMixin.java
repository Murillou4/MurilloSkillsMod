package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.api.*;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
abstract class FurnaceResultMixin {
    @Shadow @Final private Player player;
    @Shadow private int removeCount;
    @Inject(method="checkTakeAchievements",at=@At("HEAD"))
    private void smelted(ItemStack stack,CallbackInfo ci){
        if(player instanceof ServerPlayer p&&MurilloSkills.store!=null&&removeCount>0){var c=GameplayEvents.context(p);((BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH)).forged(c,true,removeCount);}
    }
}

