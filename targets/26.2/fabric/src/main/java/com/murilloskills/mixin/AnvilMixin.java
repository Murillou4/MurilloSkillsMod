package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.api.*;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilMenu.class)
abstract class AnvilMixin {
    @Shadow @Final private DataSlot cost;
    @Unique private ItemStack murilloskills$material=ItemStack.EMPTY;
    @Unique private int murilloskills$count;
    @Unique private int murilloskills$charged;
    @Unique private int murilloskills$oldLevel;
    @Inject(method="createResult",at=@At("TAIL"))
    private void discount(CallbackInfo ci){
        var self=(AnvilMenu)(Object)this;var owner=self.getSlot(0).container;
        Player player=((CombinerAccess)self).murilloskills$player();
        if(player instanceof ServerPlayer p&&MurilloSkills.store!=null&&cost.get()>0){
            var c=GameplayEvents.context(p);double discount=((BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH)).discount(c);
            cost.set(Math.max(1,(int)Math.ceil(cost.get()*(1-discount))));
        }
        if(player instanceof ServerPlayer p&&MurilloSkills.store!=null){
            var c=GameplayEvents.context(p);var left=self.getSlot(0).getItem();var right=self.getSlot(1).getItem();
            var original=((CombinerAccess)self).murilloskills$resultSlots().getItem(0);
            var enhanced=com.murilloskills.utils.BlacksmithCrafting.enhance(c,left,right,original);
            if(enhanced!=original){((CombinerAccess)self).murilloskills$resultSlots().setItem(0,enhanced);cost.set(Math.min(39,Math.max(8,cost.get())+8));self.broadcastChanges();}
        }
    }
    @Inject(method="onTake",at=@At("HEAD"))
    private void taken(Player player,ItemStack stack,CallbackInfo ci){
        murilloskills$charged=cost.get();
        murilloskills$oldLevel=player.experienceLevel;
        var self=(AnvilMenu)(Object)this;var left=self.getSlot(0).getItem();var right=self.getSlot(1).getItem();
        murilloskills$material=left.isDamaged()&&!right.isDamageableItem()&&!right.is(Items.ENCHANTED_BOOK)?right.copyWithCount(1):ItemStack.EMPTY;murilloskills$count=right.getCount();
        if(player instanceof ServerPlayer p&&MurilloSkills.store!=null&& !((AnvilMenu)(Object)this).getSlot(1).getItem().isEmpty())GameplayEvents.context(p).xp("forge",1);
    }
    @Inject(method="onTake",at=@At("RETURN"))
    private void material(Player player,ItemStack stack,CallbackInfo ci){
        if(player instanceof ServerPlayer p&&MurilloSkills.store!=null&&GameplayEvents.context(p).profile().selected(SkillType.BLACKSMITH)){
            int paid=Math.max(0,murilloskills$oldLevel-player.experienceLevel);
            com.murilloskills.network.Wire.notice(GameplayEvents.context(p),"smith:anvil","result","Trabalho de bigorna",paid+" "+(paid==1?"nível consumido":"níveis consumidos")+" · serviço concluído",1,60);
        }
        if(player instanceof ServerPlayer p&&MurilloSkills.store!=null&&!murilloskills$material.isEmpty()&&p.gameMode.isSurvival()&&((AnvilMenu)(Object)this).getSlot(1).getItem().getCount()<murilloskills$count){
            var c=GameplayEvents.context(p);if(c.chance(BlacksmithSkill.MATERIAL.value(c,SkillType.BLACKSMITH)))c.give(murilloskills$material.copy());
        }
        murilloskills$material=ItemStack.EMPTY;
    }
}

