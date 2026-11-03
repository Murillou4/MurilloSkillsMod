package com.murilloskills.skills;
import java.util.*;
/** Stable catalog available before the first network snapshot. */
public final class ProfessionInfo {
    private static final Map<SkillType,String> ROLES=Map.of(
        SkillType.MINER,"Minere com eficiência e encontre recursos.",
        SkillType.WARRIOR,"Resista aos golpes e vença no corpo a corpo.",
        SkillType.ARCHER,"Domine disparos, precisão e perfuração.",
        SkillType.FARMER,"Cuide de cultivos e aproveite cada colheita.",
        SkillType.FISHER,"Pesque melhor e explore debaixo d'água.",
        SkillType.BLACKSMITH,"Repare, encante e trabalhe com fornalhas.",
        SkillType.BUILDER,"Construa com formas, alcance e precisão.",
        SkillType.EXPLORER,"Viaje, descubra lugares e encontre tesouros.");
    private ProfessionInfo(){}
    public static String role(SkillType type){return ROLES.get(type);}
}
