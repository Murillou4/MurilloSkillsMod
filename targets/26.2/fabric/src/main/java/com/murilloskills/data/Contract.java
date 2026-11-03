package com.murilloskills.data;
import com.murilloskills.skills.SkillType;

public final class Contract {
    public SkillType skill;
    public String activity;
    public double progress, target;
    public boolean rewarded;
    public Contract() {}
    public Contract(SkillType skill, String activity, double target) { this.skill=skill; this.activity=activity; this.target=target; }
    public boolean complete() { return progress >= target; }
}

