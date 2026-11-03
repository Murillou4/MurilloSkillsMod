package com.murilloskills.utils;
import com.murilloskills.api.SkillContext;
import com.murilloskills.skills.SkillType;
import com.murilloskills.skills.effects.Feature;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import java.util.*;

/** Leases identify both profession and feature, preserving vanilla potions and unrelated utilities. */
public final class OwnedEffects {
    private record Source(SkillType skill,String feature){}
    public static final class Lease {
        public MobEffectInstance instance;public int maxDuration,amplifier;private final Set<Source> owners=new HashSet<>();
        Lease(MobEffectInstance instance,int duration,int amplifier){this.instance=instance;maxDuration=duration;this.amplifier=amplifier;}
    }
    private OwnedEffects(){}
    public static void grant(SkillContext c,SkillType skill,Holder<MobEffect> effect,int duration,int amplifier){grant(c,skill,null,effect,duration,amplifier);}
    public static void grant(SkillContext c,SkillType skill,Feature feature,Holder<MobEffect> effect,int duration,int amplifier){
        var leases=c.runtime().effects;var existing=c.player().getEffect(effect);var lease=leases.get(effect);
        if(existing!=null&&(lease==null||existing!=lease.instance||existing.getDuration()>lease.maxDuration||existing.getAmplifier()!=lease.amplifier)){leases.remove(effect);return;}
        c.player().addEffect(new MobEffectInstance(effect,duration,amplifier,true,false));
        var active=c.player().getEffect(effect);
        if(lease==null){lease=new Lease(active,duration,amplifier);leases.put(effect,lease);}
        lease.instance=active;lease.maxDuration=duration;lease.amplifier=amplifier;lease.owners.add(new Source(skill,feature==null?"passive":feature.id));
    }
    private static void remove(SkillContext c,java.util.function.Predicate<Source> predicate){
        for(var entry:List.copyOf(c.runtime().effects.entrySet())){
            var lease=entry.getValue();lease.owners.removeIf(predicate);if(!lease.owners.isEmpty())continue;
            var current=c.player().getEffect(entry.getKey());
            if(current==lease.instance&&current.getDuration()<=lease.maxDuration&&current.getAmplifier()==lease.amplifier)c.player().removeEffect(entry.getKey());
            c.runtime().effects.remove(entry.getKey());
        }
    }
    public static void clearType(SkillContext c,SkillType skill){remove(c,s->s.skill==skill);}
    public static void clearSource(SkillContext c,SkillType skill,Feature feature){remove(c,s->s.skill==skill&&s.feature.equals(feature.id));}
    public static void clearFeature(SkillContext c,Feature feature){remove(c,s->s.feature.equals(feature.id));}
    public static void clear(SkillContext c){remove(c,s->true);}
}

