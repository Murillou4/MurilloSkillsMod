package com.murilloskills.skills.effects;

import com.murilloskills.data.PlayerProfile;
import com.murilloskills.skills.SkillType;
import java.util.*;

/** Functional controls. Attribute bonuses are automatic; utilities are deliberately configurable. */
public enum Feature {
    NIGHT_VISION("night_vision","Visão noturna","Ilumina a visão; o Pescador fornece o efeito apenas submerso.",false,Map.of(SkillType.MINER,10,SkillType.EXPLORER,30,SkillType.FISHER,35)),
    AUTO_TORCH("auto_torch","Tochas automáticas","Usa tochas do inventário em locais escuros.",false,Map.of(SkillType.MINER,10)),
    MELTING("melting","Toque de fusão","Converte ferro, ouro e cobre brutos em lingotes.",false,Map.of(SkillType.MINER,65)),
    ORE_RADAR("ore_radar","Radar sonoro","Avisa quando há recursos valiosos próximos; respeita seu filtro.",false,Map.of(SkillType.MINER,60)),
    SPEED("speed_boost","Velocidade extra","Controla o deslocamento extra de exploração, preparo do arco e Fúria.",true,Map.of(SkillType.EXPLORER,5,SkillType.ARCHER,35,SkillType.WARRIOR,20)),
    STEP("step_assist","Subida automática","Permite atravessar desníveis de um bloco.",false,Map.of(SkillType.EXPLORER,10)),
    PLANT("area_planting","Plantio em área","Planta usando as sementes da pilha na mão.",false,Map.of(SkillType.FARMER,15)),
    BONEMEAL("area_bonemeal","Bone meal em área","Aplica bone meal em cultivos vizinhos, consumindo cada uso real.",false,Map.of(SkillType.FARMER,25)),
    HARVEST("area_harvest","Colheita adjacente","Colhe apenas cultivos maduros junto ao primeiro.",false,Map.of(SkillType.FARMER,45)),
    REPLANT("replant","Replantio","Repõe o cultivo usando uma semente do inventário.",false,Map.of(SkillType.FARMER,10)),
    GROWTH("auto_growth","Crescimento automático","Favorece cultivos próximos enquanto trabalha na região.",false,Map.of(SkillType.FARMER,35)),
    FARM_REGEN("farm_regen","Vitalidade do campo","Recupera vida sobre terra arada ou grama.",false,Map.of(SkillType.FARMER,35)),
    BREATH("aquatic_breath","Respiração aquática","Mantém respiração aquática enquanto estiver submerso.",true,Map.of(SkillType.FISHER,15,SkillType.EXPLORER,15)),
    SWIM("swim_assist","Graça dos golfinhos","Melhora a travessia na água com o efeito vanilla.",true,Map.of(SkillType.FISHER,35)),
    REPAIR("repair_aura","Reparo automático","Repara itens na mão e equipamentos, sem reparar o mesmo item duas vezes.",false,Map.of(SkillType.BLACKSMITH,45)),
    FURNACE("furnace_boost","Fornalhas rápidas","Acelera fornalhas vanilla próximas, com combustível e receitas normais.",true,Map.of(SkillType.BLACKSMITH,15)),
    REFLECT("damage_reflect","Reflexão de dano","Reage a ataques diretos; não causa cadeias de reflexão.",false,Map.of(SkillType.BLACKSMITH,75)),
    FIRE_WARD("fire_ward","Proteção contra fogo","Concede resistência ao fogo do Ferreiro; outros perigos continuam normais.",false,Map.of(SkillType.BLACKSMITH,35)),
    BUILD_HASTE("build_haste","Vigor construtor","Concede Pressa I; pode ser desligado para preservar o ritmo vanilla.",false,Map.of(SkillType.BUILDER,35)),
    CLIMB("climb_assist","Escalada assistida","Ajuda a subir escadas e andaimes.",true,Map.of(SkillType.BUILDER,35)),
    SLOW_FALL("slow_fall","Descida segura","Queda lenta ao agachar em altura durante uma construção.",false,Map.of(SkillType.BUILDER,60)),
    RECOVERY("swift_recovery","Recuperação fora de combate","Recupera vida após sair do combate.",false,Map.of(SkillType.EXPLORER,55)),
    NETHER("nether_assist","Travessia do Nether","Evita dano de magma e lentidão da areia das almas; auxilia na lava.",false,Map.of(SkillType.EXPLORER,55)),
    PATHFINDER("pathfinder","Coordenadas e direção","Mostra orientação no HUD de exploração.",false,Map.of(SkillType.EXPLORER,35));

    public final String id,title,description;
    public final boolean defaultOn;
    public final Map<SkillType,Integer> sources;
    Feature(String id,String title,String description,boolean defaultOn,Map<SkillType,Integer> sources){this.id=id;this.title=title;this.description=description;this.defaultOn=defaultOn;this.sources=sources;}
    public boolean enabled(PlayerProfile p){return p.featureSettings.getOrDefault(id,defaultOn);}
    public boolean available(PlayerProfile p){return sources.entrySet().stream().anyMatch(e->p.unlocked(e.getKey(),e.getValue()));}
    public int level(SkillType type){return sources.getOrDefault(type,1000);}
    public boolean supplied(PlayerProfile p,SkillType type){return p.unlocked(type,level(type))&&enabled(p);}
    public static Optional<Feature> find(String id){return Arrays.stream(values()).filter(f->f.id.equals(id)).findFirst();}
    public static void migrate(PlayerProfile p){
        for(var f:values())p.featureSettings.put(f.id,f.defaultOn);
        for(String id:List.of("night_vision","auto_torch","melting","step_assist","area_planting","replant","pathfinder"))p.featureSettings.put(id,p.toggles.contains(id));
        p.featureSettings.put(HARVEST.id,p.toggles.contains(PLANT.id));
        p.featureSettings.put(GROWTH.id,true);p.featureSettings.put(REPAIR.id,true);p.featureSettings.put(NETHER.id,true);
    }
}
