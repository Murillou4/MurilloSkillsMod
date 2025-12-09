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
    public static final float ARCHER_HEADSHOT_DAMAGE_BONUS = 0.30f; // 30% extra damage on headshots

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

    // --- BLACKSMITH (FERREIRO) ---
    // Bônus por Nível
    public static final float BLACKSMITH_RESISTANCE_PER_LEVEL = 0.02f; // 2% resistência física por nível

    // Perks por Nível
    public static final int BLACKSMITH_IRON_SKIN_LEVEL = 10; // +5% resistência física passiva
    public static final int BLACKSMITH_EFFICIENT_ANVIL_LEVEL = 25; // 25% desconto XP, 10% material save
    public static final int BLACKSMITH_FORGED_RESILIENCE_LEVEL = 50; // +10% fire/explosion, +1 Protection
    public static final int BLACKSMITH_THORNS_MASTER_LEVEL = 75; // 20% reflect, 50% knockback reduction
    public static final int BLACKSMITH_MASTER_LEVEL = 100; // Titanium Aura

    // Valores de Perks
    public static final float BLACKSMITH_IRON_SKIN_BONUS = 0.05f; // +5% resistência física
    public static final float BLACKSMITH_ANVIL_XP_DISCOUNT = 0.25f; // 25% desconto de XP
    public static final float BLACKSMITH_ANVIL_MATERIAL_SAVE = 0.10f; // 10% chance de não consumir material
    public static final float BLACKSMITH_FIRE_EXPLOSION_RESIST = 0.10f; // +10% resistência fogo/explosão
    public static final float BLACKSMITH_THORNS_CHANCE = 0.20f; // 20% chance de refletir
    public static final float BLACKSMITH_THORNS_REFLECT = 0.25f; // 25% do dano refletido
    public static final float BLACKSMITH_KNOCKBACK_REDUCTION = 0.50f; // 50% redução de knockback
    public static final float BLACKSMITH_SUPER_ENCHANT_CHANCE = 0.25f; // 25% chance encantamentos acima do limite

    // Habilidade Ativa (Titanium Aura)
    public static final int BLACKSMITH_ABILITY_DURATION_SECONDS = 15; // 15 segundos
    public static final int BLACKSMITH_ABILITY_COOLDOWN_SECONDS = 3600; // 1 hora
    public static final float BLACKSMITH_TITANIUM_RESISTANCE = 0.30f; // +30% resistência a todo dano
    public static final float BLACKSMITH_TITANIUM_REGEN = 1.0f; // 0.5 coração/seg = 1 HP/seg

    // --- BUILDER (CONSTRUTOR) ---
    // Bônus por Nível
    public static final float BUILDER_REACH_PER_LEVEL = 0.05f; // +0.05 blocos por nível (máx +5 nv100)

    // Perks por Nível
    public static final int BUILDER_EXTENDED_REACH_LEVEL = 10; // +1 bloco alcance + Quick Hands
    public static final int BUILDER_EFFICIENT_CRAFTING_LEVEL = 15; // 20% economia decorativos
    public static final int BUILDER_SAFE_LANDING_LEVEL = 25; // 25% menos dano de queda
    public static final int BUILDER_SCAFFOLD_MASTER_LEVEL = 50; // Velocidade scaffolding + 50% economia estruturais
    public static final int BUILDER_MASTER_REACH_LEVEL = 75; // +5 blocos alcance
    public static final int BUILDER_MASTER_LEVEL = 100; // Creative Brush

    // Valores de Perks
    public static final float BUILDER_LEVEL_10_REACH = 1.0f; // +1 bloco no nível 10
    public static final float BUILDER_LEVEL_75_REACH = 5.0f; // +5 blocos no nível 75
    public static final float BUILDER_DECORATIVE_ECONOMY = 0.20f; // 20% economia blocos decorativos
    public static final float BUILDER_STRUCTURAL_ECONOMY = 0.50f; // 50% economia blocos estruturais
    public static final float BUILDER_FALL_DAMAGE_REDUCTION = 0.25f; // 25% redução dano de queda
    public static final float BUILDER_SCAFFOLD_SPEED_MULTIPLIER = 1.5f; // 50% mais rápido no scaffolding

    // Habilidade Ativa (Creative Brush)
    public static final int BUILDER_ABILITY_DURATION_SECONDS = 120; // 20 segundos
    public static final int BUILDER_ABILITY_COOLDOWN_SECONDS = 600; // 10 minutos
    public static final int BUILDER_BRUSH_MAX_DISTANCE = 6; // Distância máxima do brush

    // --- EXPLORER (EXPLORADOR) ---
    // Bônus por Nível
    public static final float EXPLORER_SPEED_PER_LEVEL = 0.002f; // 0.2% velocidade por nível (máx 20%)
    public static final int EXPLORER_LUCK_INTERVAL = 20; // +1 Luck a cada 20 níveis
    public static final float EXPLORER_HUNGER_REDUCTION_PER_LEVEL = 0.005f; // 0.5% menos fome ao andar

    // Perks por Nível
    public static final int EXPLORER_STEP_ASSIST_LEVEL = 10; // Subir blocos automaticamente
    public static final int EXPLORER_AQUATIC_LEVEL = 20; // 50% mais respiração + mineração normal submerso
    public static final int EXPLORER_NIGHT_VISION_LEVEL = 35; // Visão noturna permanente (toggle)
    public static final int EXPLORER_FEATHER_FEET_LEVEL = 65; // 40% menos dano de queda
    public static final int EXPLORER_NETHER_WALKER_LEVEL = 80; // Imunidade magma + soul sand normal
    public static final int EXPLORER_MASTER_LEVEL = 100; // Treasure Hunter

    // Valores de Perks
    public static final float EXPLORER_BREATH_MULTIPLIER = 1.5f; // 50% mais tempo submerso
    public static final float EXPLORER_FALL_DAMAGE_REDUCTION = 0.40f; // 40% redução
    public static final int EXPLORER_TREASURE_RADIUS = 128; // Raio do Sexto Sentido
    public static final float EXPLORER_STEP_HEIGHT = 1.0f; // Altura automática de subida

    // XP Values
    public static final int EXPLORER_XP_BIOME = 500; // Novo bioma (reduzido de 5000)
    public static final int EXPLORER_XP_STRUCTURE = 200; // Nova estrutura (reduzido de 1000)
    public static final int EXPLORER_XP_LOOT_CHEST = 300; // Abrir baú de loot pela primeira vez
    public static final int EXPLORER_XP_MAP_COMPLETE = 2000; // Completar mapa
    public static final int EXPLORER_XP_WANDERING_TRADE = 400; // Trade com Wandering Trader
    public static final double EXPLORER_DISTANCE_THRESHOLD = 50.0; // Distância para ganhar XP
    public static final int EXPLORER_XP_PER_DISTANCE = 35; // XP ganho por atingir o threshold

    // --- MÉTODOS UTILITÁRIOS ---

    // Habilidade Passiva Toggle (Plantio em Área 3x3)
    public static final int FARMER_AREA_PLANTING_LEVEL = 25; // Nível para desbloquear
    public static final int FARMER_AREA_PLANTING_RADIUS = 1; // 3x3 = centro ± 1

    // --- FISHER (PESCADOR) ---
    // Bônus por Nível
    public static final float FISHER_SPEED_PER_LEVEL = 0.005f; // 0.5% velocidade de pesca por nível
    public static final float FISHER_EPIC_BUNDLE_PER_LEVEL = 0.001f; // 0.1% chance de Bundle Épico (máx 10% nv100) -
                                                                     // REBALANCED

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
    public static final int FISHER_RAIN_DANCE_BUNDLE_MULTIPLIER = 2; // Chance dobrada de Bundle Épico (reduzido de 3x)

    /** Converte segundos para ticks */
    public static int toTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /** Converte segundos para ticks (long) */
    public static long toTicksLong(int seconds) {
        return (long) seconds * TICKS_PER_SECOND;
    }

    // --- MILESTONE VANILLA XP REWARDS ---
    /** Níveis de skill onde recompensas de XP vanilla são concedidas */
    public static final int[] SKILL_MILESTONES = { 10, 25, 50, 75, 100 };

    /**
     * XP vanilla progressivo por milestone (em níveis de experiência do Minecraft)
     */
    public static final int MILESTONE_XP_LEVEL_10 = 10; // ~ 8 níveis
    public static final int MILESTONE_XP_LEVEL_25 = 25; // ~ 22 níveis
    public static final int MILESTONE_XP_LEVEL_50 = 50; // ~ 45 níveis (encantamento máximo+)
    public static final int MILESTONE_XP_LEVEL_75 = 75; // ~ 75 níveis
    public static final int MILESTONE_XP_LEVEL_100 = 150; // ~ 150 níveis (recompensa máxima)

    /**
     * Retorna a quantidade de níveis de XP vanilla para um milestone específico.
     * 
     * @param level O nível do milestone
     * @return Quantidade de níveis de XP vanilla, ou 0 se não for um milestone
     */
    public static int getMilestoneVanillaXpLevels(int level) {
        return switch (level) {
            case 10 -> MILESTONE_XP_LEVEL_10;
            case 25 -> MILESTONE_XP_LEVEL_25;
            case 50 -> MILESTONE_XP_LEVEL_50;
            case 75 -> MILESTONE_XP_LEVEL_75;
            case 100 -> MILESTONE_XP_LEVEL_100;
            default -> 0;
        };
    }
}
