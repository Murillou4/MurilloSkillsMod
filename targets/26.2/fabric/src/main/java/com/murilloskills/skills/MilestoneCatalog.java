package com.murilloskills.skills;
import com.murilloskills.data.SkillStats;
import java.util.*;

public final class MilestoneCatalog {
    private static final int COOLDOWN_STEP=5,DURATION_STEP=2;
    public record Milestone(int level,String title,String description) {}
    private static final EnumMap<SkillType,String[]> BRANCHES=new EnumMap<>(SkillType.class);
    static {
        BRANCHES.put(SkillType.MINER,new String[]{"Mais alcance no Pulso e fortuna de minérios.","Mais velocidade, blocos no Ultmine e conservação da ferramenta."});
        BRANCHES.put(SkillType.WARRIOR,new String[]{"Mais vida e redução de dano.","Mais dano e recuperação nas vitórias."});
        BRANCHES.put(SkillType.ARCHER,new String[]{"Menos dispersão desde 25; melhora headshots e penetração quando desbloqueados.","Flechas mais rápidas e mobilidade durante o disparo."});
        BRANCHES.put(SkillType.FARMER,new String[]{"Plantio e bone meal maiores, crescimento mais eficiente e conservação de sementes.","Mais produtos por colheita madura."});
        BRANCHES.put(SkillType.FISHER,new String[]{"Menor espera e mais peixes por captura.","Mais chance de tesouros e sorte."});
        BRANCHES.put(SkillType.BLACKSMITH,new String[]{"Menor custo em reparos e encantamentos.","Fornalhas mais rápidas e maior proteção."});
        BRANCHES.put(SkillType.BUILDER,new String[]{"Mais alcance e maior janela para desfazer.","Maior capacidade do UltPlace e economia de materiais."});
        BRANCHES.put(SkillType.EXPLORER,new String[]{"Maior velocidade e menor consumo de fome.","Maior alcance da busca de tesouros."});
    }
    private MilestoneCatalog() {}
    public static String branchDescription(SkillType type,int branch){return BRANCHES.get(type)[Math.clamp(branch,1,2)-1]+" Melhora nos níveis 25, 50 e 75.";}
    public static List<Milestone> milestones(SkillType type) {
        List<Milestone> list=new ArrayList<>();int utility=0;
        for(int level=5;level<=100;level+=5){
            if(level%20==0)list.add(new Milestone(level,level==100?"Domínio e Paragon":type.ability+" · "+(level/20),
                level==100?"Versão final da ativa. Escolha esta profissão como Paragon para seu efeito exclusivo.":"Ativa de patamar "+(level/20)+": mais potência, duração ou alcance."));
            else if(level==25||level==50||level==75)list.add(new Milestone(level,level==25?"Escolha seu ramo":"Especialização · "+(level==50?2:3),type.branchA+" ou "+type.branchB+". Os recursos básicos existem nos dois ramos."));
            else if(utility<8){var perk=SkillPerk.at(type,utility++);list.add(new Milestone(level,perk.title,perk.description()));}
            else {String title=level==70?"Ritmo veterano":level==85?"Técnica refinada":level==90?"Eficiência superior":"Preparação para o domínio";
                list.add(new Milestone(level,title,(level==70||level==90)?"Recarga da ativa reduzida em mais "+COOLDOWN_STEP+" segundos.":"Duração da ativa aumentada em mais "+DURATION_STEP+" segundos."));}
        }
        return List.copyOf(list);
    }
    public static int utilityLevel(int index) {
        int current=0;for(int level=5;level<=100;level+=5)if(level%20!=0&&level!=25&&level!=50&&level!=75){if(current++==index)return level;}return 95;
    }
    public static int next(int level){return Math.min(100,(level/5+1)*5);}
    public static int cooldownReduction(int highest){return (highest>=70?COOLDOWN_STEP:0)+(highest>=90?COOLDOWN_STEP:0);}
    public static int durationBonus(int highest){return (highest>=85?DURATION_STEP:0)+(highest>=95?DURATION_STEP:0);}
    public static String progress(SkillType type,SkillStats s){return s.level+" / 100 · "+type.branch(s.branch)+" · P"+s.prestige;}
}

