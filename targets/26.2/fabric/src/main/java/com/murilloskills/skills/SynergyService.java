package com.murilloskills.skills;
import com.murilloskills.data.PlayerProfile;
import java.util.*;
public final class SynergyService {
    public static final double MAX_BONUS=.10;
    public enum Effect { DEFENSE,DAMAGE,ORE,DROPS,ECONOMY,SPEED,FINDS }
    public record Synergy(String name,SkillType a,SkillType b,Effect effect,double amount){}
    public static final List<Synergy> ALL=List.of(
        new Synergy("Vontade de ferro",SkillType.WARRIOR,SkillType.BLACKSMITH,Effect.DEFENSE,.04),
        new Synergy("Mestre da forja",SkillType.MINER,SkillType.BLACKSMITH,Effect.ORE,.04),
        new Synergy("Patrulheiro",SkillType.ARCHER,SkillType.EXPLORER,Effect.SPEED,.03),
        new Synergy("Dádiva natural",SkillType.FARMER,SkillType.FISHER,Effect.DROPS,.04),
        new Synergy("Caça ao tesouro",SkillType.MINER,SkillType.EXPLORER,Effect.FINDS,.04),
        new Synergy("Mestre do combate",SkillType.WARRIOR,SkillType.ARCHER,Effect.DAMAGE,.04),
        new Synergy("Artesão mestre",SkillType.BUILDER,SkillType.BLACKSMITH,Effect.ECONOMY,.03),
        new Synergy("Sobrevivente",SkillType.WARRIOR,SkillType.EXPLORER,Effect.DEFENSE,.03),
        new Synergy("Industrioso",SkillType.MINER,SkillType.BUILDER,Effect.ECONOMY,.03),
        new Synergy("Guerreiro do mar",SkillType.WARRIOR,SkillType.FISHER,Effect.DAMAGE,.03),
        new Synergy("Arqueiro verde",SkillType.FARMER,SkillType.ARCHER,Effect.SPEED,.02),
        new Synergy("Prospector",SkillType.MINER,SkillType.WARRIOR,Effect.ORE,.03),
        new Synergy("Aventureiro",SkillType.BUILDER,SkillType.EXPLORER,Effect.FINDS,.03),
        new Synergy("Ermitão",SkillType.FARMER,SkillType.BUILDER,Effect.ECONOMY,.03));
    private SynergyService(){}
    public static List<Synergy> active(PlayerProfile p){return ALL.stream().filter(s->p.unlocked(s.a,10)&&p.unlocked(s.b,10)).toList();}
    public static double bonus(PlayerProfile p,Effect e){return Math.min(MAX_BONUS,active(p).stream().filter(s->s.effect==e).mapToDouble(Synergy::amount).sum());}
}

