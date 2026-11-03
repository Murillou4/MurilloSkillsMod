package com.murilloskills.skills;
import java.util.*;

/** Only server-observed, completed activities may use this table. */
public final class ActionRewards {
    public record Reward(SkillType skill, double xp, String contract) {}
    private static final Map<String,Reward> TABLE=Map.ofEntries(
        Map.entry("stone",new Reward(SkillType.MINER,7,"mine")),
        Map.entry("ore",new Reward(SkillType.MINER,22,"mine")),
        Map.entry("rare_ore",new Reward(SkillType.MINER,45,"mine")),
        Map.entry("melee_kill",new Reward(SkillType.WARRIOR,10,"combat")),
        Map.entry("ranged_kill",new Reward(SkillType.ARCHER,25,"combat")),
        Map.entry("harvest",new Reward(SkillType.FARMER,10,"harvest")),
        Map.entry("plant",new Reward(SkillType.FARMER,1,"plant")),
        Map.entry("breed",new Reward(SkillType.FARMER,20,"breed")),
        Map.entry("fish",new Reward(SkillType.FISHER,10,"fish")),
        Map.entry("treasure",new Reward(SkillType.FISHER,30,"fish")),
        Map.entry("smelt",new Reward(SkillType.BLACKSMITH,2,"forge")),
        Map.entry("forge",new Reward(SkillType.BLACKSMITH,20,"forge")),
        Map.entry("place",new Reward(SkillType.BUILDER,6,"build")),
        Map.entry("travel",new Reward(SkillType.EXPLORER,8,"explore")),
        Map.entry("biome",new Reward(SkillType.EXPLORER,200,"discover")),
        Map.entry("structure",new Reward(SkillType.EXPLORER,150,"discover")),
        Map.entry("loot",new Reward(SkillType.EXPLORER,50,"discover"))
    );
    private ActionRewards() {}
    public static Reward get(String activity) {
        var reward=TABLE.get(activity);if(reward==null)throw new IllegalArgumentException(activity);return reward;
    }
    public static Map<String,Reward> all(){return TABLE;}
}

