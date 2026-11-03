package com.murilloskills.skills.effects;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import java.util.*;
import static com.murilloskills.skills.effects.SkillSummary.*;

/** All values and conditions are evaluated on the server using the gameplay definitions. */
public final class SummaryService {
    private SummaryService(){}
    public static List<SkillSummary> build(SkillContext c){return Arrays.stream(SkillType.values()).map(t->build(c,t)).toList();}
    private static String next(EffectDefinition d,SkillEvaluation e){
        var base=e.withActive(false);
        for(int level=e.level()+1;level<=100;level++){
            var projection=e.preview(level,e.branch());
            if(projection.has(d.level())&&(!e.has(d.level())||Math.abs(d.nominal(projection)-d.nominal(base))>.00001))return "Nv. "+level+": "+d.format(d.nominal(projection));
        }
        return e.level()==100?"Domínio":"Patamar mantido nesta jornada";
    }
    /** Milestones describe both the ordinary value and the value while the existing ability runs. */
    public static List<String> milestoneEffects(List<EffectDefinition> defs,SkillEvaluation evaluation,int level){
        var after=evaluation.atMilestone(level,evaluation.branch());
        var before=evaluation.atMilestone(Math.max(0,level-1),evaluation.branch());
        var running=after.withActive(true);var previousRunning=before.withActive(true);
        List<String> benefits=new ArrayList<>();
        for(var d:defs){
            var shown=d.activeOnly()?running:after;var previous=d.activeOnly()?previousRunning:before;
            if(d.level()==level)benefits.add(d.title()+": "+d.format(d.nominal(shown))+". "+d.condition());
            else if(d.level()<level&&Math.abs(d.nominal(shown)-d.nominal(previous))>.00001)benefits.add(d.title()+": "+d.format(d.nominal(previous))+" → "+d.format(d.nominal(shown)));
            if(d.activeOnly()||!after.has(20)||!after.has(d.level()))continue;
            boolean affected=Math.abs(d.nominal(running)-d.nominal(after))>.00001||before.has(20)&&Math.abs(d.nominal(previousRunning)-d.nominal(before))>.00001;
            if(!affected)continue;
            if(level==20||d.level()==level)benefits.add("Durante "+evaluation.type().ability+": "+d.title()+" · "+d.format(d.nominal(running)));
            else if(Math.abs(d.nominal(running)-d.nominal(previousRunning))>.00001)benefits.add("Durante "+evaluation.type().ability+": "+d.title()+" · "+d.format(d.nominal(previousRunning))+" → "+d.format(d.nominal(running)));
        }
        return benefits;
    }
    public static SkillSummary build(SkillContext c,SkillType type){
        var e=c.evaluate(type);var stats=c.stats(type);var defs=SkillRegistry.get(type).effects();
        var effects=defs.stream().map(d->{
            State state=!d.unlocked(e)?State.LOCKED:!d.enabled(e)?State.DISABLED:!d.applies().test(e)?State.CONDITIONAL:State.APPLIED;
            var group=d.activeOnly()?Group.ACTIVE:d.feature()!=null?Group.UTILITY:Group.PASSIVE;
            return new EffectView(d.id(),d.title(),d.format(d.nominal(e)),next(d,e),d.condition(),d.level(),d.unlocked(e),d.enabled(e),d.limit()>=0?d.format(d.limit()):"",d.nominal(e),d.value(e),d.unit(),group,state);
        }).toList();
        List<MilestoneView> milestones=new ArrayList<>();
        for(var old:MilestoneCatalog.milestones(type)){
            int level=old.level();var after=e.atMilestone(level,stats.branch);
            List<String> benefits=milestoneEffects(defs,e,level);
            for(var f:Feature.values())if(f.level(type)==level&&defs.stream().noneMatch(d->d.feature()==f&&d.level()==level))benefits.add(f.title+": "+f.description);
            if(type==SkillType.MINER&&level==SkillPerk.ORE_FILTER.level)benefits.add("Filtro de minérios: escolha os recursos revelados pelo Pulso.");
            if(level%20==0)benefits.add(type.ability+" · patamar "+level/20+" · "+EffectNumbers.duration(after)/20+" s · recarga "+EffectNumbers.cooldown(after)/20+" s");
            if(level==25||level==50||level==75)benefits.add("Especialização "+level/25+": "+type.branchA+" / "+type.branchB+".");
            if(level==70||level==90)benefits.add("Recarga da ativa: "+EffectNumbers.cooldown(after)/20+" s.");
            if(level==85||level==95)benefits.add("Duração da ativa: "+EffectNumbers.duration(after)/20+" s.");
            if(benefits.isEmpty())benefits.add("Nível "+level+" conquistado. As ferramentas e marcos anteriores permanecem disponíveis.");
            String title=defs.stream().filter(d->d.level()==level).findFirst().map(EffectDefinition::title).orElse(old.title());
            milestones.add(new MilestoneView(level,title,List.copyOf(benefits),stats.unlockLevel()>=level));
        }
        var controls=Arrays.stream(Feature.values()).filter(f->f.sources.containsKey(type)).map(f->new ControlView(f.id,f.title,f.description,f.level(type),c.profile().unlocked(type,f.level(type)),f.enabled(c.profile()))).toList();
        List<BranchPreview> branches=new ArrayList<>();
        for(int choice=1;choice<=2;choice++){
            int rank=stats.unlockLevel()>=75?3:stats.unlockLevel()>=50?2:1;
            var comparisons=compare(defs,e,choice,rank);
            List<TierPreview> tiers=new ArrayList<>();
            for(int tier=1;tier<=3;tier++){
                var projection=e.branchPreview(choice,tier);var base=e.branchPreview(0,tier);List<String> gains=new ArrayList<>();
                for(var d:defs)if(Math.abs(d.nominal(projection)-d.nominal(base))>.00001)gains.add(d.title()+": "+d.format(d.nominal(base))+" → "+d.format(d.nominal(projection))+(d.level()>tier*25?" (libera no "+d.level()+")":""));
                tiers.add(new TierPreview(tier*25,projection.level(),List.copyOf(gains),compare(defs,e,choice,tier)));
            }
            branches.add(new BranchPreview(choice,type.branch(choice),MilestoneCatalog.branchDescription(type,choice),comparisons,List.copyOf(tiers)));
        }
        var active=e.withActive(true);var inactive=e.withActive(false);List<String> activeBenefits=new ArrayList<>();
        for(var d:defs)if(active.has(d.level())){
            double change=d.nominal(active)-d.nominal(inactive);
            if(d.activeOnly()&&d.nominal(active)>0)activeBenefits.add(d.title()+": "+d.format(d.nominal(active)));
            else if(change>.00001)activeBenefits.add(d.title()+": "+d.increase(change));
            else if(d.level()==20)activeBenefits.add(d.title()+": "+d.format(d.nominal(active)));
        }
        if(type==SkillType.BUILDER)activeBenefits.add("Pincel por dois pontos · marcação, prévia e confirmação.");
        List<String> limits=List.of(
            "Com sua build: corpo a corpo +"+EffectDefinition.number(PowerComposition.damageBonus(c,false,false)*100)+"%; projéteis +"+EffectDefinition.number(PowerComposition.damageBonus(c,true,false)*100)+"%; com headshot +"+EffectDefinition.number(PowerComposition.damageBonus(c,true,true)*100)+"%.",
            "Proteção comum aplicada: "+EffectDefinition.number(PowerComposition.defense(c)*100)+"%; movimento adicional: "+EffectDefinition.number(PowerComposition.speed(c)*100)+"%.",
            "Limites: dano +"+EffectDefinition.number(c.store().config.maxDamageBonus*100)+"% / "+EffectDefinition.format(EffectDefinition.Unit.PERCENT,PowerComposition.ACTIVE_DAMAGE_MULTIPLIER-1)+" com ativa; defesa "+EffectDefinition.number(c.store().config.maxSkillResistance*100)+"%; movimento +"+EffectDefinition.number(c.store().config.maxMoveBonus*100)+"%.",
            "Valores da profissão são contribuições potenciais. Condições, sinergias e limites formam o resultado combinado acima.");
        return new SkillSummary(type,e.selected(),stats.level,stats.highestLevel,stats.prestige,type.ability,e.tier(),EffectNumbers.duration(e),EffectNumbers.cooldown(e),effects,List.copyOf(milestones),controls,List.copyOf(branches),limits,List.copyOf(activeBenefits));
    }
    private static List<Comparison> compare(List<EffectDefinition> defs,SkillEvaluation e,int choice,int rank){
        var after=e.branchPreview(choice,rank);var current=e.branchPreview(e.branch(),rank);var a=e.branchPreview(1,rank);var b=e.branchPreview(2,rank);
        List<Comparison> values=new ArrayList<>();
        for(var d:defs)if(Math.abs(d.nominal(a)-d.nominal(b))>.00001)values.add(new Comparison(d.title(),d.format(d.nominal(current)),d.format(d.nominal(after)),"Conquista necessária: "+Math.max(d.level(),rank*25)+". "+d.condition()));
        return List.copyOf(values);
    }
    public static String catalog(SkillContext c){
        var out=new StringBuilder("# MurilloSkills "+com.murilloskills.MurilloSkills.VERSION+" — catálogo avaliado no servidor\n\nValores potenciais no nível 100, ramo A, sem ativa. Condições e custos continuam valendo.\n\n");
        for(var type:SkillType.values()){
            out.append("## ").append(type.title).append("\n\n| Libera | Efeito | Referência | Condição |\n|---:|---|---|---|\n");
            var e=c.evaluate(type).preview(100,1);
            for(var d:SkillRegistry.get(type).effects())out.append("| ").append(d.level()).append(" | ").append(d.title()).append(" | ").append(d.format(d.nominal(e))).append(" | ").append(d.condition()).append(" |\n");
            out.append("\n");for(var m:build(c,type).milestones())out.append("- ").append(m.level()).append(": ").append(String.join("; ",m.benefits())).append("\n");
        }return out.toString();
    }
}
