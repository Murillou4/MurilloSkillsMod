package com.murilloskills;
import com.murilloskills.skills.*;
import com.murilloskills.data.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;

/** Activity model: production weights, explicit cadence and efficiency assumptions; native actions are verified separately. */
class BalanceSimulationTest {
    @Test void representativeAndEfficientPlayStayWithinThirtyToFiftyHours()throws Exception{
        Map<SkillType,Map<String,Double>> traces=new EnumMap<>(SkillType.class);
        traces.put(SkillType.MINER,Map.of("stone",14d,"ore",3d,"rare_ore",1d/3));
        traces.put(SkillType.WARRIOR,Map.of("melee_kill",4d));
        traces.put(SkillType.ARCHER,Map.of("ranged_kill",2d));
        traces.put(SkillType.FARMER,Map.of("harvest",12d));
        traces.put(SkillType.FISHER,Map.of("fish",1.2d,"treasure",0.1d));
        traces.put(SkillType.BLACKSMITH,Map.of("smelt",15d,"forge",1d));
        traces.put(SkillType.BUILDER,Map.of("place",30d));
        // 250 m/min on new ground: one travel reward per 20 m; occasional new biomes and loot.
        traces.put(SkillType.EXPLORER,Map.of("travel",12.5d,"biome",0.05d,"loot",0.2d));
        List<String> report=new ArrayList<>();report.add("skill,scenario,hours,levels,source");
        for(var type:SkillType.values()){
            double basePerMinute=traces.get(type).entrySet().stream().mapToDouble(e->ActionRewards.get(e.getKey()).xp()*e.getValue()).sum();
            for(double pace:new double[]{1,1.2,1.32}){
                var p=new PlayerProfile();p.selected.add(type);int seconds=0;
                while(p.stats(type).level<100&&seconds<100*3600){
                    // Modeled throughput at five gameplay bands; human pacing still needs campaign calibration.
                    int l=p.stats(type).level;
                    double passive=1+l/200d;
                    double area=(type==SkillType.MINER||type==SkillType.BUILDER||type==SkillType.FARMER)?l>=75?1.6:l>=50?1.4:l>=25?1.2:1:1;
                    ProgressionService.addXp(p,type,basePerMinute/60*pace*passive*area,1);seconds++;
                }
                double hours=seconds/3600d;report.add(type+","+(pace==1?"reference":pace==1.2?"efficient":"efficient_with_full_combo")+","+hours+",100,weighted-action-trace");
                assertTrue(hours>=30&&hours<=50,()->type+" "+pace+" pace took "+hours+"h");
            }
        }
        Path file=Path.of("artifacts/balance-simulation.csv");Files.createDirectories(file.getParent());Files.write(file,report);
        var catalog=new StringBuilder("# Catálogo gerado das definições da 2.0\n\n");
        for(var type:SkillType.values()){
            catalog.append("## ").append(type.title).append("\n\n").append(type.branchA).append(": ").append(MilestoneCatalog.branchDescription(type,1)).append("\n\n").append(type.branchB).append(": ").append(MilestoneCatalog.branchDescription(type,2)).append("\n\n| Nível | Marco | Efeito |\n|---:|---|---|\n");
            for(var m:MilestoneCatalog.milestones(type))catalog.append("| ").append(m.level()).append(" | ").append(m.title()).append(" | ").append(m.description()).append(" |\n");
            catalog.append("\n");
        }
        catalog.append("## Sinergias\n\n| Combinação | Profissões | Efeito | Bônus |\n|---|---|---|---:|\n");
        for(var s:SynergyService.ALL)catalog.append("| ").append(s.name()).append(" | ").append(s.a().title).append(" + ").append(s.b().title).append(" | ").append(s.effect()).append(" | ").append(Math.round(s.amount()*100)).append("% |\n");
        catalog.append("\n## Recompensas de ações\n\n| Ação | Profissão | XP base |\n|---|---|---:|\n");
        ActionRewards.all().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->catalog.append("| ").append(e.getKey()).append(" | ").append(e.getValue().skill().title).append(" | ").append(e.getValue().xp()).append(" |\n"));
        Files.writeString(Path.of("artifacts/catalog.md"),catalog);
    }
}

