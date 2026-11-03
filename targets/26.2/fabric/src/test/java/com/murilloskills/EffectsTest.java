package com.murilloskills;
import com.murilloskills.config.ModConfig;
import com.murilloskills.data.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectsTest {
    @Test void milestonesIncludeTheRealValuesWhileAnAbilityRuns(){
        var p=new PlayerProfile();p.selected.add(SkillType.WARRIOR);
        var e=SkillEvaluation.of(p,SkillType.WARRIOR,new ModConfig(),false,false,false,false,false);
        var first=SummaryService.milestoneEffects(new WarriorSkill().effects(),e,20);
        assertTrue(first.stream().anyMatch(text->text.startsWith("Durante ")&&text.contains("Dano corpo a corpo")&&text.contains("21,8%")));
        var upgraded=SummaryService.milestoneEffects(new WarriorSkill().effects(),e,40);
        assertTrue(upgraded.stream().anyMatch(text->text.startsWith("Durante ")&&text.contains("Dano corpo a corpo")&&text.endsWith("37,6%")));
        p.selected.clear();p.selected.add(SkillType.ARCHER);
        e=SkillEvaluation.of(p,SkillType.ARCHER,new ModConfig(),false,false,false,false,false);
        assertTrue(SummaryService.milestoneEffects(new ArcherSkill().effects(),e,20).stream().anyMatch(text->text.startsWith("Durante ")&&text.contains("Velocidade das flechas")&&text.contains("1,21×")));
        p.selected.clear();p.selected.add(SkillType.FISHER);
        e=SkillEvaluation.of(p,SkillType.FISHER,new ModConfig(),false,false,false,false,false);
        assertTrue(SummaryService.milestoneEffects(new FisherSkill().effects(),e,20).stream().anyMatch(text->text.startsWith("Durante ")&&text.contains("Sorte aplicada")&&text.endsWith("+1")));
    }
    @TempDir Path temp;
    private SkillEvaluation evaluate(SkillType type,int branch){var p=new PlayerProfile();p.selected.add(type);var s=p.stats(type);s.level=25;s.highestLevel=25;s.branch=branch;return SkillEvaluation.of(p,type,new ModConfig(),false,false,false,false,false);}
    @Test void firstBranchTierChangesRealValues(){
        assertEquals(.05,ArcherSkill.PRECISION.nominal(evaluate(SkillType.ARCHER,1))-ArcherSkill.PRECISION.nominal(evaluate(SkillType.ARCHER,0)),.000001);
        assertEquals(1,FisherSkill.LUCK.value(evaluate(SkillType.FISHER,2)),.000001);
        assertEquals(.05,BlacksmithSkill.ANVIL.value(evaluate(SkillType.BLACKSMITH,1)),.000001);
    }
    @Test void controlsDoNotEnableInactiveOrLockedSkills(){
        var e=evaluate(SkillType.MINER,1);e.profile().featureSettings.put(Feature.ORE_RADAR.id,true);
        assertEquals(0,MinerSkill.RADAR.value(e));
        e.profile().selected.clear();assertFalse(Feature.AUTO_TORCH.available(e.profile()));
        assertEquals(.4,MinerSkill.FORTUNE.limit());
    }
    @Test void oldPreferencesMigrateOnceWithOriginalBackup()throws Exception{
        var p=new PlayerProfile();p.toggles.add("area_planting");p.toggles.add("night_vision");
        var tree=ProfileCodec.JSON.toJsonTree(p).getAsJsonObject();tree.remove("featureSettings");String original=tree.toString();
        Path file=temp.resolve("player.json");Files.writeString(file,original);
        var loaded=ProfileCodec.read(file);assertTrue(Feature.HARVEST.enabled(loaded));assertTrue(Feature.NIGHT_VISION.enabled(loaded));
        assertTrue(Feature.REPAIR.enabled(loaded));assertFalse(Feature.ORE_RADAR.enabled(loaded));
        loaded.featureSettings.put(Feature.REPAIR.id,false);ProfileCodec.write(file,loaded);
        assertFalse(Feature.REPAIR.enabled(ProfileCodec.read(file)));
        assertEquals(original,Files.readString(file.resolveSibling("player.json.pre-2.1.bak")));
    }
    @Test void branchProjectionKeepsProgressAndShowsUnlockRequirements(){
        var e=evaluate(SkillType.ARCHER,1);var future=e.branchPreview(2,3);
        assertEquals(75,future.level());assertEquals(25,e.profile().stats(SkillType.ARCHER).level);
        assertEquals(1,EffectNumbers.area(e,2)>=1?1:0);
        assertEquals("12,5%",EffectDefinition.format(EffectDefinition.Unit.PERCENT,.125));
    }
    @Test void prestigeComparisonsKeepCurrentLevelAndEarnedBranchTiers(){
        var original=evaluate(SkillType.MINER,1);var p=original.profile();p.stats(SkillType.MINER).level=0;p.stats(SkillType.MINER).highestLevel=100;p.stats(SkillType.MINER).prestige=1;
        var e=SkillEvaluation.of(p,SkillType.MINER,new ModConfig(),false,false,false,false,false);
        var preview=e.branchPreview(1,3);
        assertEquals(0,preview.level());assertEquals(100,preview.highest());assertEquals(3,preview.branchTier());
        assertEquals(MinerSkill.SPEED.nominal(e),MinerSkill.SPEED.nominal(preview),.000001);
    }
}
