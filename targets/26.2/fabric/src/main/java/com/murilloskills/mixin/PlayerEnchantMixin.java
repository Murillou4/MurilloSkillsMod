package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
abstract class PlayerEnchantMixin {
    @org.spongepowered.asm.mixin.Unique private int murilloskills$oldLevel;
    @ModifyVariable(method="onEnchantmentPerformed",at=@At("HEAD"),argsOnly=true)
    private int discount(int cost){
        murilloskills$oldLevel=((Player)(Object)this).experienceLevel;
        if((Object)this instanceof ServerPlayer p&&MurilloSkills.store!=null){var c=GameplayEvents.context(p);return ((BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH)).enchantingCost(c,cost);}
        return cost;
    }
    @Inject(method="onEnchantmentPerformed",at=@At("TAIL"))
    private void enchanted(ItemStack stack,int cost,CallbackInfo ci){if((Object)this instanceof ServerPlayer p&&MurilloSkills.store!=null){var c=GameplayEvents.context(p);c.xp("forge",1);int paid=Math.max(0,murilloskills$oldLevel-p.experienceLevel);if(c.profile().selected(SkillType.BLACKSMITH))com.murilloskills.network.Wire.notice(c,"smith:enchant","result","Encantamento",paid+" "+(paid==1?"nível consumido":"níveis consumidos"),1,60);}}
}

