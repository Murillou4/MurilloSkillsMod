package com.murilloskills.utils;

public class SkillConfig {
    // --- GERAL ---
    public static final int MAX_LEVEL = 100;

    // --- MINERADOR (MINER) ---
    public static final float MINER_SPEED_PER_LEVEL = 0.03f; // 3%
    public static final float MINER_FORTUNE_PER_LEVEL = 0.03f; // 0.03 niveis

    // Perks
    public static final int MINER_NIGHT_VISION_LEVEL = 10;
    public static final int MINER_DURABILITY_LEVEL = 30;
    public static final int MINER_RADAR_LEVEL = 60;
    public static final int MINER_MASTER_LEVEL = 100;

    // Valores
    public static final float MINER_DURABILITY_CHANCE = 0.15f; // 15% chance de ignorar dano
    public static final long MINER_ABILITY_COOLDOWN = 20L * 60 * 60; // 1 Hora em ticks (72000)
    public static final int MINER_ABILITY_RADIUS = 30; // Raio do Master Miner
    public static final int MINER_ABILITY_DURATION = 200; // 3 segundos (20 * 3)

    // --- GUERREIRO (WARRIOR) ---
    public static final float WARRIOR_DAMAGE_PER_LEVEL = 0.05f;
    public static final float WARRIOR_LOOTING_PER_LEVEL = 0.02f;

    // Milestones (Warrior)
    public static final int RESISTANCE_UNLOCK_LEVEL = 25;
    public static final float RESISTANCE_REDUCTION = 0.85f;

    public static final int LIFESTEAL_UNLOCK_LEVEL = 75;
    public static final float LIFESTEAL_PERCENTAGE = 0.15f;

    // Master Skill (Warrior Berserk - Level 100)
    public static final int WARRIOR_MASTER_LEVEL = 100;
    public static final long WARRIOR_ABILITY_COOLDOWN = MINER_ABILITY_COOLDOWN; // Mesmo cooldown do Miner (1 hora)
    public static final int WARRIOR_BERSERK_DURATION = 200; // 10 segundos (20 ticks * 10)
    public static final float WARRIOR_BERSERK_LIFESTEAL = 0.50f; // 50% de roubo de vida durante Berserk
    public static final int WARRIOR_EXHAUSTION_DURATION = 100; // 5 segundos de debuff após Berserk
    public static final int WARRIOR_BERSERK_STRENGTH_AMPLIFIER = 3; // Força IV (amplifier 3)
    public static final int WARRIOR_BERSERK_RESISTANCE_AMPLIFIER = 1; // Resistência II

    // --- ARQUEIRO (ARCHER) ---
    public static final float ARCHER_DAMAGE_PER_LEVEL = 0.02f; // +2% dano à distância por nível

    // Perks
    public static final int ARCHER_FAST_ARROWS_LEVEL = 10; // Flechas voam mais rápido
    public static final int ARCHER_BONUS_DAMAGE_LEVEL = 25; // +5% dano à distância
    public static final int ARCHER_PENETRATION_LEVEL = 50; // Penetração na flecha
    public static final int ARCHER_STABLE_SHOT_LEVEL = 75; // Disparo mais estável
    public static final int ARCHER_MASTER_LEVEL = 100; // Master Ranger

    // Valores
    public static final float ARCHER_ARROW_SPEED_MULTIPLIER = 1.25f; // 25% mais rápido
    public static final float ARCHER_BONUS_DAMAGE_AMOUNT = 0.05f; // +5% de bônus no nível 25
    public static final float ARCHER_SPREAD_REDUCTION = 0.50f; // 50% menos spread
    public static final long ARCHER_ABILITY_COOLDOWN = 20L * 60 * 60; // 1 Hora em ticks (72000)
    public static final int ARCHER_MASTER_RANGER_DURATION = 1200; // 30 segundos (20 * 30)
}