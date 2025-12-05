package com.murilloskills.utils;

public class SkillConfig {
    // --- GERAL ---
    public static final int MAX_LEVEL = 100;
    public static final int TICKS_PER_SECOND = 20; // Minecraft roda a 20 ticks por segundo

    // --- MINERADOR (MINER) ---
    public static final float MINER_SPEED_PER_LEVEL = 0.03f; // 3%
    public static final float MINER_FORTUNE_PER_LEVEL = 0.03f; // 0.03 niveis

    // Perks
    public static final int MINER_NIGHT_VISION_LEVEL = 10;
    public static final int MINER_DURABILITY_LEVEL = 30;
    public static final int MINER_RADAR_LEVEL = 60;
    public static final int MINER_MASTER_LEVEL = 100;

    // Valores (tempos em SEGUNDOS - converter com toTicks() quando necessário)
    public static final float MINER_DURABILITY_CHANCE = 0.15f; // 15% chance de ignorar dano
    public static final int MINER_ABILITY_COOLDOWN_SECONDS = 3600; // 1 hora (3600 segundos)
    public static final int MINER_ABILITY_RADIUS = 30; // Raio do Master Miner
    public static final int MINER_ABILITY_DURATION_SECONDS = 10; // 10 segundos

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
    public static final int WARRIOR_ABILITY_COOLDOWN_SECONDS = 3600; // 1 hora (3600 segundos)
    public static final int WARRIOR_BERSERK_DURATION_SECONDS = 10; // 10 segundos
    public static final float WARRIOR_BERSERK_LIFESTEAL = 0.50f; // 50% de roubo de vida durante Berserk
    public static final int WARRIOR_EXHAUSTION_DURATION_SECONDS = 5; // 5 segundos de debuff após Berserk
    public static final int WARRIOR_BERSERK_STRENGTH_AMPLIFIER = 3; // Força IV (amplifier 3)
    public static final int WARRIOR_BERSERK_RESISTANCE_AMPLIFIER = 1; // Resistência II

    // --- ARQUEIRO (ARCHER) ---
    public static final float ARCHER_DAMAGE_PER_LEVEL = 0.03f; // +3% dano à distância por nível

    // Perks
    public static final int ARCHER_FAST_ARROWS_LEVEL = 10; // Flechas voam mais rápido
    public static final int ARCHER_BONUS_DAMAGE_LEVEL = 25; // +5% dano à distância
    public static final int ARCHER_PENETRATION_LEVEL = 50; // Penetração na flecha
    public static final int ARCHER_STABLE_SHOT_LEVEL = 75; // Disparo mais estável
    public static final int ARCHER_MASTER_LEVEL = 100; // Master Ranger

    // Valores (tempos em SEGUNDOS)
    public static final float ARCHER_ARROW_SPEED_MULTIPLIER = 1.25f; // 25% mais rápido
    public static final float ARCHER_BONUS_DAMAGE_AMOUNT = 0.05f; // +5% de bônus no nível 25
    public static final float ARCHER_SPREAD_REDUCTION = 0.50f; // 50% menos spread
    public static final int ARCHER_ABILITY_COOLDOWN_SECONDS = 3600; // 1 hora (3600 segundos)
    public static final int ARCHER_MASTER_RANGER_DURATION_SECONDS = 30; // 30 segundos

    // --- FARMER (AGRICULTOR) ---
    // Bônus por Nível
    public static final float FARMER_DOUBLE_HARVEST_PER_LEVEL = 0.005f; // 0.5% por nível (máx 50% nv100)
    public static final float FARMER_GOLDEN_CROP_PER_LEVEL = 0.0015f; // 0.15% por nível (máx 15% nv100)

    // Perks por Nível
    public static final int FARMER_GREEN_THUMB_LEVEL = 10; // +5% colheita extra, 10% semente não consumida
    public static final int FARMER_FERTILE_GROUND_LEVEL = 25; // Crescimento 25% mais rápido
    public static final int FARMER_NUTRIENT_CYCLE_LEVEL = 50; // 2x Bone Meal, 5% sementes extras
    public static final int FARMER_ABUNDANT_HARVEST_LEVEL = 75; // +15% colheita, 10% colheita adjacente
    public static final int FARMER_MASTER_LEVEL = 100; // Harvest Moon

    // Valores de Chance
    public static final float FARMER_GREEN_THUMB_EXTRA = 0.05f; // +5% colheita extra
    public static final float FARMER_GREEN_THUMB_SEED_SAVE = 0.10f; // 10% chance semente não consumida
    public static final float FARMER_FERTILE_GROUND_SPEED = 0.25f; // 25% crescimento mais rápido
    public static final float FARMER_NUTRIENT_SEED_CHANCE = 0.05f; // 5% sementes extras
    public static final float FARMER_ABUNDANT_EXTRA = 0.15f; // +15% colheita extra
    public static final float FARMER_ABUNDANT_ADJACENT = 0.10f; // 10% chance colheita adjacente

    // Habilidade Ativa (Harvest Moon)
    public static final int FARMER_ABILITY_RADIUS = 8; // Raio do Harvest Moon
    public static final int FARMER_ABILITY_DURATION_SECONDS = 10; // 10 segundos de efeito
    public static final int FARMER_ABILITY_COOLDOWN_SECONDS = 120; // 2 minutos

    // --- MÉTODOS UTILITÁRIOS ---

    // Habilidade Passiva Toggle (Plantio em Área 3x3)
    public static final int FARMER_AREA_PLANTING_LEVEL = 25; // Nível para desbloquear
    public static final int FARMER_AREA_PLANTING_RADIUS = 1; // 3x3 = centro ± 1

    // --- FISHER (PESCADOR) ---
    // Bônus por Nível
    public static final float FISHER_SPEED_PER_LEVEL = 0.005f; // 0.5% velocidade de pesca por nível
    public static final float FISHER_EPIC_BUNDLE_PER_LEVEL = 0.003f; // 0.3% chance de Bundle Épico (máx 30% nv100)

    // Perks por Nível
    public static final int FISHER_WAIT_REDUCTION_LEVEL = 10; // -25% tempo de espera
    public static final int FISHER_TREASURE_BONUS_LEVEL = 25; // +10% tesouro + 10% XP
    public static final int FISHER_DOLPHIN_GRACE_LEVEL = 50; // Velocidade na água
    public static final int FISHER_LUCK_SEA_LEVEL = 75; // Luck of the Sea passivo
    public static final int FISHER_MASTER_LEVEL = 100; // Rain Dance

    // Valores de Chance e Bônus
    public static final float FISHER_WAIT_REDUCTION = 0.25f; // -25% tempo de espera entre fisgadas
    public static final float FISHER_TREASURE_BONUS = 0.10f; // +10% chance de tesouros raros
    public static final float FISHER_XP_BONUS = 0.10f; // +10% bônus de XP no nível 25

    // Habilidade Ativa (Rain Dance)
    public static final int FISHER_ABILITY_DURATION_SECONDS = 60; // 60 segundos de efeito
    public static final int FISHER_ABILITY_COOLDOWN_SECONDS = 1800; // 30 minutos (1800 segundos)
    public static final float FISHER_RAIN_DANCE_SPEED_BONUS = 0.50f; // +50% velocidade de pesca
    public static final float FISHER_RAIN_DANCE_TREASURE_BONUS = 0.30f; // +30% chance de tesouro
    public static final int FISHER_RAIN_DANCE_BUNDLE_MULTIPLIER = 3; // Chance tripla de Bundle Épico

    /** Converte segundos para ticks */
    public static int toTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /** Converte segundos para ticks (long) */
    public static long toTicksLong(int seconds) {
        return (long) seconds * TICKS_PER_SECOND;
    }
}