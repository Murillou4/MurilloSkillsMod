package com.murilloskills.skills;
import java.util.*;
/** Stable unlock IDs shared by gameplay, descriptions and the profession trail. */
public enum SkillPerk {
    MINING_PACE(SkillType.MINER,0,"Ritmo de mineração"),
    MINER_VISION(SkillType.MINER,1,"Visão noturna e tochas"),
    ULTMINE(SkillType.MINER,2,"Ultmine inicial"),
    TOOL_CONSERVATION(SkillType.MINER,3,"Conservação da picareta"),
    ORE_FILTER(SkillType.MINER,4,"Filtro de minérios"),
    RESOURCE_FORTUNE(SkillType.MINER,5,"Fortuna de recursos"),
    EXCAVATION(SkillType.MINER,6,"Escavação ampliada"),
    MELTING(SkillType.MINER,7,"Fusão de minérios"),
    MELEE_TRAINING(SkillType.WARRIOR,0,"Treino de combate"),
    VITALITY(SkillType.WARRIOR,1,"Vitalidade"),
    GUARD(SkillType.WARRIOR,2,"Guarda firme"),
    RESILIENCE(SkillType.WARRIOR,3,"Resistência"),
    VICTORY_RECOVERY(SkillType.WARRIOR,4,"Recuperação após vitória"),
    EXPERIENCED_STRIKES(SkillType.WARRIOR,5,"Golpes experientes"),
    LIFESTEAL(SkillType.WARRIOR,6,"Vampirismo controlado"),
    WARRIOR_HEART(SkillType.WARRIOR,7,"Coração de guerreiro"),
    ARCHERY_TRAINING(SkillType.ARCHER,0,"Treino de pontaria"),
    FAST_ARROWS(SkillType.ARCHER,1,"Flechas velozes"),
    STEADY_SHOT(SkillType.ARCHER,2,"Disparo estável"),
    HEADSHOTS(SkillType.ARCHER,3,"Acertos na cabeça"),
    DRAW_MOBILITY(SkillType.ARCHER,4,"Mobilidade com arco"),
    PENETRATION(SkillType.ARCHER,5,"Penetração de armadura"),
    ARROW_ECONOMY(SkillType.ARCHER,6,"Flecha econômica"),
    MASTER_PRECISION(SkillType.ARCHER,7,"Precisão de mestre"),
    EARTH_HANDS(SkillType.FARMER,0,"Mãos da terra"),
    REPLANTING(SkillType.FARMER,1,"Replantio inteligente"),
    AREA_PLANTING(SkillType.FARMER,2,"Plantio em área"),
    SEED_SAVING(SkillType.FARMER,3,"Conservação de sementes"),
    FERTILE_GROUND(SkillType.FARMER,4,"Solo fértil"),
    AREA_HARVEST(SkillType.FARMER,5,"Colheita ampliada"),
    LARGE_CULTIVATION(SkillType.FARMER,6,"Cultivo em área maior"),
    GOLDEN_CROP(SkillType.FARMER,7,"Colheita dourada"),
    FISHING_PACE(SkillType.FISHER,0,"Ritmo de pesca"),
    SHORT_WAIT(SkillType.FISHER,1,"Espera reduzida"),
    AQUATIC_FISHER(SkillType.FISHER,2,"Respiração aquática"),
    FISHING_TREASURE(SkillType.FISHER,3,"Tesouros adicionais"),
    OCEAN_GRACE(SkillType.FISHER,4,"Graça do oceano"),
    FISHING_BUNDLE(SkillType.FISHER,5,"Lotes de pesca"),
    SEA_LUCK(SkillType.FISHER,6,"Sorte dos mares"),
    SEA_BLESSING(SkillType.FISHER,7,"Bênção marítima"),
    FORGE_CRAFT(SkillType.BLACKSMITH,0,"Ofício da forja"),
    IRON_SKIN(SkillType.BLACKSMITH,1,"Pele de ferro"),
    VANILLA_FURNACES(SkillType.BLACKSMITH,2,"Fornalhas eficientes"),
    EFFICIENT_ANVIL(SkillType.BLACKSMITH,3,"Bigorna econômica"),
    FIRE_MASTERY(SkillType.BLACKSMITH,4,"Resistência ao fogo"),
    REPAIR_AURA(SkillType.BLACKSMITH,5,"Aura de reparo"),
    EFFICIENT_ENCHANTING(SkillType.BLACKSMITH,6,"Mesa de encantamento econômica"),
    OVERENCHANTING(SkillType.BLACKSMITH,7,"Sobre-encantamento"),
    BUILDING_PACE(SkillType.BUILDER,0,"Ritmo de construção"),
    EXTENDED_REACH(SkillType.BUILDER,1,"Alcance ampliado"),
    ULTPLACE(SkillType.BUILDER,2,"UltPlace inicial"),
    SAFE_LANDING(SkillType.BUILDER,3,"Pouso seguro"),
    SCAFFOLDING(SkillType.BUILDER,4,"Andaimes rápidos"),
    MATERIAL_ECONOMY(SkillType.BUILDER,5,"Economia de materiais"),
    LARGE_BUILDING(SkillType.BUILDER,6,"Construção ampliada"),
    MASTER_REACH(SkillType.BUILDER,7,"Alcance de mestre"),
    LIGHT_FEET(SkillType.EXPLORER,0,"Passos leves"),
    STEP_ASSIST(SkillType.EXPLORER,1,"Subida automática"),
    AQUATIC_EXPLORER(SkillType.EXPLORER,2,"Fôlego aquático"),
    EXPLORER_VISION(SkillType.EXPLORER,3,"Visão noturna"),
    PATHFINDER(SkillType.EXPLORER,4,"Orientação e caminho"),
    FEATHER_FEET(SkillType.EXPLORER,5,"Pés de pluma"),
    NETHER_WALKER(SkillType.EXPLORER,6,"Caminhante do Nether"),
    DISTANT_TREASURES(SkillType.EXPLORER,7,"Tesouros distantes");
    public String description(){return switch(this){
        case MINING_PACE->"Aumenta sua velocidade de mineração conforme a profissão evolui.";
        case MINER_VISION->"Permite ligar visão noturna e colocar suas tochas automaticamente em locais escuros.";
        case ULTMINE->"Desbloqueia mineração em veios e formatos, com desgaste da ferramenta e drops reais.";
        case TOOL_CONSERVATION->"Dá chance de preservar durabilidade ao minerar recursos naturais.";
        case ORE_FILTER->"Permite escolher quais minérios aparecem no pulso de prospecção.";
        case RESOURCE_FORTUNE->"Minérios naturais podem render uma unidade extra de recurso.";
        case EXCAVATION->"Amplia a capacidade das formas de mineração.";
        case MELTING->"Permite converter automaticamente ferro, ouro e cobre brutos em lingotes.";
        case MELEE_TRAINING->"Aprimora o dano corpo a corpo enquanto você desenvolve a profissão.";
        case VITALITY->"Aumenta sua vida máxima de acordo com o nível e a especialização.";
        case GUARD->"Reduz parte do dano recebido sem substituir a proteção da sua armadura.";
        case RESILIENCE->"Reforça sua resistência em combates prolongados.";
        case VICTORY_RECOVERY->"Recupera um pouco de vida ao vencer criaturas hostis em combate corpo a corpo.";
        case EXPERIENCED_STRIKES->"Adiciona potência aos seus golpes corpo a corpo.";
        case LIFESTEAL->"Golpes efetivos em criaturas hostis recuperam uma pequena quantidade de vida, com intervalo.";
        case WARRIOR_HEART->"Concede vitalidade adicional para enfrentar os últimos níveis.";
        case ARCHERY_TRAINING->"Aumenta o dano dos projéteis disparados por você.";
        case FAST_ARROWS->"Aumenta a velocidade das flechas, facilitando tiros a distância.";
        case STEADY_SHOT->"Reduz a dispersão dos disparos para melhorar a precisão.";
        case HEADSHOTS->"Acertar a parte superior de criaturas causa dano adicional.";
        case DRAW_MOBILITY->"Ajuda a se mover enquanto prepara o disparo.";
        case PENETRATION->"Seus projéteis ignoram uma parte da proteção de armadura do alvo.";
        case ARROW_ECONOMY->"Dá chance de recuperar uma flecha realmente consumida pelo arco.";
        case MASTER_PRECISION->"Aprimora o bônus de acerto na cabeça.";
        case EARTH_HANDS->"Colheitas maduras podem render produtos e sementes adicionais.";
        case REPLANTING->"Permite replantar usando sementes do seu inventário após colher.";
        case AREA_PLANTING->"Permite plantar várias sementes em uma área de uma só vez.";
        case SEED_SAVING->"Dá chance de conservar sementes ao plantar.";
        case FERTILE_GROUND->"Acelera gradualmente plantações próximas enquanto você trabalha na região.";
        case AREA_HARVEST->"Permite colher plantações maduras adjacentes com o modo de cultivo em área.";
        case LARGE_CULTIVATION->"Amplia o raio disponível para o plantio em área.";
        case GOLDEN_CROP->"Colheitas maduras podem produzir uma cenoura dourada adicional.";
        case FISHING_PACE->"Aprimora o ritmo da pesca enquanto você evolui.";
        case SHORT_WAIT->"Reduz a espera até a próxima fisgada.";
        case AQUATIC_FISHER->"Ajuda a respirar debaixo d'água enquanto esta profissão está escolhida.";
        case FISHING_TREASURE->"Melhora a sorte aplicada às capturas.";
        case OCEAN_GRACE->"Facilita a movimentação dentro d'água.";
        case FISHING_BUNDLE->"Dá chance de obter mais peixes em uma captura.";
        case SEA_LUCK->"Concede sorte enquanto você mantém esta profissão escolhida.";
        case SEA_BLESSING->"Dá chance de reduzir o desgaste da vara ao recolher a linha.";
        case FORGE_CRAFT->"A prática da forja passa a render experiência adicional conforme seu nível.";
        case IRON_SKIN->"Reduz parte do dano recebido.";
        case VANILLA_FURNACES->"Acelera fornalhas, altos-fornos e defumadores próximos a você.";
        case EFFICIENT_ANVIL->"Reduz o custo de experiência nos trabalhos de bigorna.";
        case FIRE_MASTERY->"Reforça sua proteção contra fogo e explosões.";
        case REPAIR_AURA->"Recupera lentamente a durabilidade de seus equipamentos.";
        case EFFICIENT_ENCHANTING->"Reduz os níveis de experiência realmente consumidos ao encantar.";
        case OVERENCHANTING->"Permite combinar encantamentos máximos iguais para superar o limite vanilla.";
        case BUILDING_PACE->"Aumenta seu alcance de trabalho inicial.";
        case EXTENDED_REACH->"Seu alcance de colocação cresce com a profissão.";
        case ULTPLACE->"Desbloqueia formas de construção que consomem a pilha de blocos na mão e oferecem desfazer.";
        case SAFE_LANDING->"Aumenta a tolerância a quedas e reduz o impacto ao construir.";
        case SCAFFOLDING->"Aprimora a subida em andaimes e escadas.";
        case MATERIAL_ECONOMY->"Dá chance de conservar materiais de blocos decorativos elegíveis e não reversíveis.";
        case LARGE_BUILDING->"Amplia a capacidade das formas do UltPlace.";
        case MASTER_REACH->"Concede alcance adicional para obras avançadas.";
        case LIGHT_FEET->"Aprimora seu deslocamento e reduz o consumo de fome.";
        case STEP_ASSIST->"Permite ligar a subida automática de pequenos desníveis.";
        case AQUATIC_EXPLORER->"Ajuda a respirar debaixo d'água.";
        case EXPLORER_VISION->"Permite ligar visão noturna durante a exploração.";
        case PATHFINDER->"Permite mostrar coordenadas e direção no HUD.";
        case FEATHER_FEET->"Reduz o impacto das quedas e aumenta a distância segura.";
        case NETHER_WALKER->"Reforça a proteção contra os perigos do fogo e da lava.";
        case DISTANT_TREASURES->"Amplia o alcance da busca de tesouros.";
    };}
    public final SkillType skill;
    public final int index,level;
    public final String title;
    SkillPerk(SkillType skill,int index,String title){this.skill=skill;this.index=index;this.level=MilestoneCatalog.utilityLevel(index);this.title=title;}
    public static SkillPerk at(SkillType skill,int index){return Arrays.stream(values()).filter(p->p.skill==skill&&p.index==index).findFirst().orElseThrow();}
}

