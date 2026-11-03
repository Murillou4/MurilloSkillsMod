package com.murilloskills.api;
import com.murilloskills.skills.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public abstract class AbstractSkill {
    public abstract SkillType type();
    public java.util.List<com.murilloskills.skills.effects.EffectDefinition> effects(){return java.util.List.of();}
    public void cancel(SkillContext c){c.runtime().activeUntil.remove(type());}
    public abstract void activate(SkillContext context);
    public void tick(SkillContext context){}
    public void fastTick(SkillContext context){}
    public void updateAttributes(SkillContext context){}
    public void clear(SkillContext context){
        com.murilloskills.utils.OwnedEffects.clearType(context,type());context.runtime().activeUntil.remove(type());
        context.runtime().combos.remove(type());
        String scan=type()==com.murilloskills.skills.SkillType.MINER?"ore":type()==com.murilloskills.skills.SkillType.EXPLORER?"treasure":null;
        if(scan!=null&&context.runtime().scans.remove(scan)!=null)com.murilloskills.network.Wire.markers(context.player(),java.util.List.of(),scan,1);
    }
    public void broken(SkillContext context,BlockPos pos,BlockState state,boolean natural){}
    public void placed(SkillContext context,BlockPos pos,BlockState state,boolean first){}
    public void killed(SkillContext context,LivingEntity victim,DamageSource source){}
    public boolean useBlock(SkillContext context,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){return false;}
    public float incoming(SkillContext context,DamageSource source,float damage){return damage;}
    public float outgoing(SkillContext context,LivingEntity victim,DamageSource source,float damage){return damage;}
    public void drops(SkillContext context,BlockState state,List<ItemStack> drops){}
    public int cooldown(SkillContext c){return com.murilloskills.skills.effects.EffectNumbers.cooldown(c.evaluate(type()));}
    public int duration(SkillContext c){return com.murilloskills.skills.effects.EffectNumbers.duration(c.evaluate(type()));}
    protected void begin(SkillContext c){c.runtime().activeUntil.put(type(),c.now()+duration(c));}
}

