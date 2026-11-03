package com.murilloskills.skills;

public enum SkillType {
    MINER("Minerador", "Prospecção", "Escavação", "Pulso de prospecção", 0xffd6ad66, 18),
    WARRIOR("Guerreiro", "Guardião", "Berserker", "Fúria de combate", 0xffd87c70, 4),
    ARCHER("Arqueiro", "Precisão", "Mobilidade", "Foco do arqueiro", 0xffb6c583, 5),
    FARMER("Fazendeiro", "Cultivo", "Colheita", "Surto de crescimento", 0xff88b98a, 12),
    FISHER("Pescador", "Pescaria", "Tesouros", "Dança da chuva", 0xff7bbcc8, 1.5),
    BLACKSMITH("Ferreiro", "Artesão", "Forjador", "Aura de titânio", 0xffb2a4cc, 5),
    BUILDER("Construtor", "Arquiteto", "Mestre de Obras", "Pincel construtor", 0xffd6b18b, 18),
    EXPLORER("Explorador", "Desbravador", "Caçador de Tesouros", "Busca de tesouros", 0xff8faede, 12);

    public final String title, branchA, branchB, ability;
    public final int color;
    /** Reference meaningful actions/minute, before unlocked efficiency gains. */
    public final double referenceRate;
    SkillType(String title, String a, String b, String ability, int color, double rate) {
        this.title=title; branchA=a; branchB=b; this.ability=ability; this.color=color; referenceRate=rate;
    }
    public String branch(int branch) { return branch == 1 ? branchA : branch == 2 ? branchB : "Sem ramo"; }
}

