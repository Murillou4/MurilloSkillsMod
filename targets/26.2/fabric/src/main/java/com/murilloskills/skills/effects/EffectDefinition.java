package com.murilloskills.skills.effects;
import com.murilloskills.api.SkillContext;
import java.util.function.*;
import java.util.Locale;

public record EffectDefinition(String id,String title,Unit unit,int level,Feature feature,String condition,
        double limit,ToDoubleFunction<SkillEvaluation> calculation,Predicate<SkillEvaluation> applies,boolean activeOnly) {
    public enum Unit { PERCENT,MULTIPLIER,HEARTS,BLOCKS,SECONDS,LEVEL,ITEMS,DURABILITY,STATE,TARGETS,LEVELS }
    public static EffectDefinition of(String id,String title,Unit unit,int level,Feature feature,String condition,double limit,ToDoubleFunction<SkillEvaluation> calculation){
        return new EffectDefinition(id,title,unit,level,feature,condition,limit,calculation,e->true,false);
    }
    public EffectDefinition when(Predicate<SkillEvaluation> predicate){return new EffectDefinition(id,title,unit,level,feature,condition,limit,calculation,predicate,activeOnly);}
    public EffectDefinition onlyWhileActive(){return new EffectDefinition(id,title,unit,level,feature,condition,limit,calculation,SkillEvaluation::active,true);}
    public double nominal(SkillEvaluation e){
        double v=calculation.applyAsDouble(e);if(!Double.isFinite(v))return 0;
        return Math.max(0,limit>=0?Math.min(limit,v):v);
    }
    public boolean unlocked(SkillEvaluation e){return e.has(level);}
    public boolean enabled(SkillEvaluation e){return feature==null||e.feature(feature);}
    public double value(SkillEvaluation e){return unlocked(e)&&enabled(e)&&applies.test(e)?nominal(e):0;}
    public double value(SkillContext c,com.murilloskills.skills.SkillType type){return value(c.evaluate(type));}
    public String format(double value){return format(unit,value);}
    public String increase(double value){return (unit==Unit.LEVEL?"":"+")+format(value);}
    public static String format(Unit unit,double value){
        return switch(unit){
            case PERCENT->number(value*100)+"%";
            case MULTIPLIER->number(value,4)+"×";
            case HEARTS->number(value/2)+" "+(value<=2?"coração":"corações");
            case BLOCKS->number(value)+(value==1?" bloco":" blocos");
            case SECONDS->number(value)+" s";
            case LEVEL->"+"+number(value);
            case ITEMS->number(value)+(value==1?" item":" itens");
            case DURABILITY->number(value)+" durabilidade";
            case STATE->value>0?"Disponível":"Indisponível";
            case TARGETS->number(value)+(value==1?" alvo":" alvos");
            case LEVELS->number(value)+(value==1?" nível":" níveis");
        };
    }
    public static String number(double v){return number(v,2);}
    private static String number(double v,int decimals){String s=String.format(Locale.ROOT,"%."+decimals+"f",v);while(s.endsWith("0"))s=s.substring(0,s.length()-1);if(s.endsWith("."))s=s.substring(0,s.length()-1);return s.replace('.',',');}
}
