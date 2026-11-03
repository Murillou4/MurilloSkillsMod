package com.murilloskills.utils;
import com.murilloskills.MurilloSkills;
import com.murilloskills.api.SkillContext;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.*;
public final class Attributes {
    private Attributes(){}
    public static void capMovement(SkillContext c){
        var attribute=net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
        var instance=c.player().getAttribute(attribute);if(instance==null)return;
        instance.removeModifier(MurilloSkills.id("cap/speed"));
        double factor=1;
        for(var mod:instance.getModifiers())if(mod.id().getNamespace().equals(MurilloSkills.ID)&&mod.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)factor*=1+mod.amount();
        if(factor>1+c.store().config.maxMoveBonus)set(c,attribute,"cap/speed",(1+c.store().config.maxMoveBonus)/factor-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    public static void set(SkillContext c,Holder<Attribute> attribute,String key,double value,AttributeModifier.Operation operation){
        var instance=c.player().getAttribute(attribute);if(instance==null)return;
        var id=MurilloSkills.id(key);var old=instance.getModifier(id);
        if(old!=null&&old.amount()==value&&old.operation()==operation)return;
        instance.removeModifier(id);
        if(value!=0)instance.addTransientModifier(new AttributeModifier(id,value,operation));
    }
    public static void clear(SkillContext c,String prefix){
        for(var attribute:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE){
            var instance=c.player().getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));if(instance==null)continue;
            for(var modifier:java.util.List.copyOf(instance.getModifiers()))if(modifier.id().getNamespace().equals(MurilloSkills.ID)&&modifier.id().getPath().startsWith(prefix))instance.removeModifier(modifier.id());
        }
        if(c.player().getHealth()>c.player().getMaxHealth())c.player().setHealth(c.player().getMaxHealth());
    }
}

