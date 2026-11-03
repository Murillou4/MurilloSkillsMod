package com.murilloskills.api;
import com.murilloskills.skills.SkillType;
import java.util.*;

public final class SkillRegistry {
    private static final EnumMap<SkillType,AbstractSkill> SKILLS=new EnumMap<>(SkillType.class);
    private SkillRegistry(){}
    public static void register(AbstractSkill skill){if(SKILLS.putIfAbsent(skill.type(),skill)!=null)throw new IllegalStateException("Duplicate skill "+skill.type());}
    public static AbstractSkill get(SkillType skill){return Objects.requireNonNull(SKILLS.get(skill),"Missing "+skill);}
    public static Collection<AbstractSkill> all(){return Collections.unmodifiableCollection(SKILLS.values());}
    public static void validate(){for(var skill:SkillType.values())get(skill);}
}

