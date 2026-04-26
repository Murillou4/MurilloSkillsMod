package com.murilloskills.skills;

public enum MurilloSkillsList {
    FARMER(SkillClass.SUB),
    FISHER(SkillClass.SUB),
    ARCHER(SkillClass.MASTER),
    MINER(SkillClass.MASTER),
    BUILDER(SkillClass.SUB),
    BLACKSMITH(SkillClass.SUB),
    EXPLORER(SkillClass.SUB),
    WARRIOR(SkillClass.MASTER);

    private final SkillClass skillClass;

    MurilloSkillsList(SkillClass skillClass) {
        this.skillClass = skillClass;
    }

    public SkillClass getSkillClass() {
        return skillClass;
    }

    public boolean isMasterClass() {
        return skillClass == SkillClass.MASTER;
    }

    public boolean isSubClass() {
        return skillClass == SkillClass.SUB;
    }

    public enum SkillClass {
        MASTER,
        SUB
    }
}
