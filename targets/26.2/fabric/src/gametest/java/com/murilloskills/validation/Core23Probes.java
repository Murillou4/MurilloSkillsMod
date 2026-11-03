package com.murilloskills.validation;
import com.murilloskills.api.*;
import com.murilloskills.data.PlayerProfile;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.Feature;
import com.murilloskills.utils.OwnedEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import static com.murilloskills.validation.Checks.check;

final class Core23Probes {
    static void run(ServerPlayer player){
        var c=GameplayEvents.context(player);var p=new PlayerProfile();c.store().replace(player,p);
        p.selected.addAll(java.util.List.of(SkillType.MINER,SkillType.FISHER,SkillType.EXPLORER,SkillType.BUILDER));
        for(var type:p.selected){p.stats(type).level=100;p.stats(type).highestLevel=100;}
        OwnedEffects.clear(c);
        for(var effect:java.util.List.of(MobEffects.NIGHT_VISION,MobEffects.WATER_BREATHING,MobEffects.FIRE_RESISTANCE,MobEffects.SLOW_FALLING))player.removeEffect(effect);
        check(!player.isInWater()&&!player.isInLava()&&!player.isShiftKeyDown(),"v223.effects.dry_fixture");
        OwnedEffects.grant(c,SkillType.MINER,Feature.NIGHT_VISION,MobEffects.NIGHT_VISION,240,0);
        OwnedEffects.grant(c,SkillType.FISHER,Feature.NIGHT_VISION,MobEffects.NIGHT_VISION,240,0);
        SkillRegistry.get(SkillType.FISHER).fastTick(c);
        check(player.hasEffect(MobEffects.NIGHT_VISION),"v223.effects.fisher_exit_keeps_miner_vision");
        OwnedEffects.clearSource(c,SkillType.MINER,Feature.NIGHT_VISION);
        check(!player.hasEffect(MobEffects.NIGHT_VISION),"v223.effects.only_remaining_source_was_miner");
        OwnedEffects.grant(c,SkillType.FISHER,Feature.BREATH,MobEffects.WATER_BREATHING,60,0);
        OwnedEffects.grant(c,SkillType.EXPLORER,Feature.BREATH,MobEffects.WATER_BREATHING,60,0);
        SkillRegistry.get(SkillType.FISHER).fastTick(c);
        check(player.hasEffect(MobEffects.WATER_BREATHING),"v223.effects.shared_breath_source_preserved");
        SkillRegistry.get(SkillType.EXPLORER).fastTick(c);
        check(!player.hasEffect(MobEffects.WATER_BREATHING),"v223.effects.dry_explorer_removes_breath");
        OwnedEffects.grant(c,SkillType.EXPLORER,Feature.NETHER,MobEffects.FIRE_RESISTANCE,60,0);
        SkillRegistry.get(SkillType.EXPLORER).fastTick(c);
        check(!player.hasEffect(MobEffects.FIRE_RESISTANCE),"v223.effects.leaving_lava_removes_own_resistance");
        OwnedEffects.grant(c,SkillType.BUILDER,Feature.SLOW_FALL,MobEffects.SLOW_FALLING,30,0);
        SkillRegistry.get(SkillType.BUILDER).fastTick(c);
        check(!player.hasEffect(MobEffects.SLOW_FALLING),"v223.effects.released_crouch_removes_own_slow_fall");
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,600,0));
        OwnedEffects.grant(c,SkillType.FISHER,Feature.NIGHT_VISION,MobEffects.NIGHT_VISION,240,0);
        SkillRegistry.get(SkillType.FISHER).fastTick(c);
        check(player.hasEffect(MobEffects.NIGHT_VISION)&&player.getEffect(MobEffects.NIGHT_VISION).getDuration()==600,"v223.effects.vanilla_potion_preserved");
        player.removeEffect(MobEffects.NIGHT_VISION);OwnedEffects.clear(c);
    }
}
