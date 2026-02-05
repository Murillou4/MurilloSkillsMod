package com.murilloskills.utils;

import com.murilloskills.config.ModConfig;
import com.murilloskills.data.XpDataManager;

/**
 * Skill configuration values - now backed by external config file.
 * Maintains backward-compatible API with static getters reading from ModConfig.
 */
public class SkillConfig {
    // --- CONSTANTS (never change) ---
    public static final int TICKS_PER_SECOND = 20;

    // --- XP CALCULATION ---
    // --- XP CALCULATION ---
    // Formula: Base + (Level * Multiplier) + (Exponent * Level^2)
    public static int getXpForLevel(int level) {
        return XpDataManager.getCurve().getXpForLevel(level);
    }

    // --- GENERAL ---
    public static int getMaxLevel() {
        return ModConfig.get().general.maxLevel;
    }

    public static final int MAX_LEVEL = 100; // Keep for backward compat, prefer getMaxLevel()

    // --- MINER ---
    public static float getMinerSpeedPerLevel() {
        return ModConfig.get().miner.speedPerLevel;
    }

    public static float getMinerFortunePerLevel() {
        return ModConfig.get().miner.fortunePerLevel;
    }

    public static int getMinerNightVisionLevel() {
        return ModConfig.get().miner.nightVisionLevel;
    }

    public static int getMinerDurabilityLevel() {
        return ModConfig.get().miner.durabilityLevel;
    }

    public static int getMinerRadarLevel() {
        return ModConfig.get().miner.radarLevel;
    }

    public static int getMinerMasterLevel() {
        return ModConfig.get().miner.masterLevel;
    }

    public static float getMinerDurabilityChance() {
        return ModConfig.get().miner.durabilityChance;
    }

    public static int getMinerAbilityCooldownSeconds() {
        return ModConfig.get().miner.abilityCooldownSeconds;
    }

    public static int getMinerAbilityRadius() {
        return ModConfig.get().miner.abilityRadius;
    }

    public static int getMinerAbilityDurationSeconds() {
        return ModConfig.get().miner.abilityDurationSeconds;
    }

    public static int getMinerScanLimit() {
        return ModConfig.get().miner.scanLimit;
    }

    // --- VEIN MINER ---
    public static int getVeinMinerMaxBlocks() {
        return ModConfig.get().veinMiner.maxBlocks;
    }

    public static boolean getVeinMinerDropsToInventory() {
        return ModConfig.get().veinMiner.dropsToInventory;
    }

    // Legacy constants for backward compatibility
    public static final float MINER_SPEED_PER_LEVEL = 0.03f;
    public static final float MINER_FORTUNE_PER_LEVEL = 0.03f;
    public static final int MINER_NIGHT_VISION_LEVEL = 10;
    public static final int MINER_DURABILITY_LEVEL = 30;
    public static final int MINER_RADAR_LEVEL = 60;
    public static final int MINER_MASTER_LEVEL = 100;
    public static final float MINER_DURABILITY_CHANCE = 0.15f;
    public static final int MINER_ABILITY_COOLDOWN_SECONDS = 1200;
    public static final int MINER_ABILITY_RADIUS = 30;
    public static final int MINER_ABILITY_DURATION_SECONDS = 30; // Aumentado de 10s para 30s

    // --- WARRIOR ---
    public static float getWarriorDamagePerLevel() {
        return ModConfig.get().warrior.damagePerLevel;
    }

    public static float getWarriorLootingPerLevel() {
        return ModConfig.get().warrior.lootingPerLevel;
    }

    public static int getResistanceUnlockLevel() {
        return ModConfig.get().warrior.resistanceUnlockLevel;
    }

    public static float getResistanceReduction() {
        return ModConfig.get().warrior.resistanceReduction;
    }

    public static int getLifestealUnlockLevel() {
        return ModConfig.get().warrior.lifestealUnlockLevel;
    }

    public static float getLifestealPercentage() {
        return ModConfig.get().warrior.lifestealPercentage;
    }

    public static int getWarriorMasterLevel() {
        return ModConfig.get().warrior.masterLevel;
    }

    public static int getWarriorAbilityCooldownSeconds() {
        return ModConfig.get().warrior.abilityCooldownSeconds;
    }

    public static int getWarriorBerserkDurationSeconds() {
        return ModConfig.get().warrior.berserkDurationSeconds;
    }

    public static float getWarriorBerserkLifesteal() {
        return ModConfig.get().warrior.berserkLifesteal;
    }

    public static int getWarriorExhaustionDurationSeconds() {
        return ModConfig.get().warrior.exhaustionDurationSeconds;
    }

    public static int getWarriorBerserkStrengthAmplifier() {
        return ModConfig.get().warrior.berserkStrengthAmplifier;
    }

    public static int getWarriorBerserkResistanceAmplifier() {
        return ModConfig.get().warrior.berserkResistanceAmplifier;
    }

    // Legacy constants
    public static final float WARRIOR_DAMAGE_PER_LEVEL = 0.05f;
    public static final float WARRIOR_LOOTING_PER_LEVEL = 0.02f;
    public static final int RESISTANCE_UNLOCK_LEVEL = 25;
    public static final float RESISTANCE_REDUCTION = 0.85f;
    public static final int LIFESTEAL_UNLOCK_LEVEL = 75;
    public static final float LIFESTEAL_PERCENTAGE = 0.15f;
    public static final int WARRIOR_MASTER_LEVEL = 100;
    public static final int WARRIOR_ABILITY_COOLDOWN_SECONDS = 1200;
    public static final int WARRIOR_BERSERK_DURATION_SECONDS = 10;
    public static final float WARRIOR_BERSERK_LIFESTEAL = 0.50f;
    public static final int WARRIOR_EXHAUSTION_DURATION_SECONDS = 5;
    public static final int WARRIOR_BERSERK_STRENGTH_AMPLIFIER = 3;
    public static final int WARRIOR_BERSERK_RESISTANCE_AMPLIFIER = 1;

    // --- ARCHER ---
    public static float getArcherDamagePerLevel() {
        return ModConfig.get().archer.damagePerLevel;
    }

    public static int getArcherFastArrowsLevel() {
        return ModConfig.get().archer.fastArrowsLevel;
    }

    public static int getArcherBonusDamageLevel() {
        return ModConfig.get().archer.bonusDamageLevel;
    }

    public static int getArcherPenetrationLevel() {
        return ModConfig.get().archer.penetrationLevel;
    }

    public static int getArcherStableShotLevel() {
        return ModConfig.get().archer.stableShotLevel;
    }

    public static int getArcherMasterLevel() {
        return ModConfig.get().archer.masterLevel;
    }

    public static float getArcherArrowSpeedMultiplier() {
        return ModConfig.get().archer.arrowSpeedMultiplier;
    }

    public static float getArcherBonusDamageAmount() {
        return ModConfig.get().archer.bonusDamageAmount;
    }

    public static float getArcherSpreadReduction() {
        return ModConfig.get().archer.spreadReduction;
    }

    public static int getArcherAbilityCooldownSeconds() {
        return ModConfig.get().archer.abilityCooldownSeconds;
    }

    public static int getArcherMasterRangerDurationSeconds() {
        return ModConfig.get().archer.masterRangerDurationSeconds;
    }

    public static float getArcherHeadshotDamageBonus() {
        return ModConfig.get().archer.headshotDamageBonus;
    }

    // Legacy constants
    public static final float ARCHER_DAMAGE_PER_LEVEL = 0.03f;
    public static final int ARCHER_FAST_ARROWS_LEVEL = 10;
    public static final int ARCHER_BONUS_DAMAGE_LEVEL = 25;
    public static final int ARCHER_PENETRATION_LEVEL = 50;
    public static final int ARCHER_STABLE_SHOT_LEVEL = 75;
    public static final int ARCHER_MASTER_LEVEL = 100;
    public static final float ARCHER_ARROW_SPEED_MULTIPLIER = 1.25f;
    public static final float ARCHER_BONUS_DAMAGE_AMOUNT = 0.05f;
    public static final float ARCHER_SPREAD_REDUCTION = 0.50f;
    public static final int ARCHER_ABILITY_COOLDOWN_SECONDS = 1200;
    public static final int ARCHER_MASTER_RANGER_DURATION_SECONDS = 30;
    public static final float ARCHER_HEADSHOT_DAMAGE_BONUS = 0.30f;

    // --- FARMER ---
    public static float getFarmerDoubleHarvestPerLevel() {
        return ModConfig.get().farmer.doubleHarvestPerLevel;
    }

    public static float getFarmerGoldenCropPerLevel() {
        return ModConfig.get().farmer.goldenCropPerLevel;
    }

    public static int getFarmerGreenThumbLevel() {
        return ModConfig.get().farmer.greenThumbLevel;
    }

    public static int getFarmerFertileGroundLevel() {
        return ModConfig.get().farmer.fertileGroundLevel;
    }

    public static int getFarmerNutrientCycleLevel() {
        return ModConfig.get().farmer.nutrientCycleLevel;
    }

    public static int getFarmerAbundantHarvestLevel() {
        return ModConfig.get().farmer.abundantHarvestLevel;
    }

    public static int getFarmerMasterLevel() {
        return ModConfig.get().farmer.masterLevel;
    }

    public static float getFarmerGreenThumbExtra() {
        return ModConfig.get().farmer.greenThumbExtra;
    }

    public static float getFarmerGreenThumbSeedSave() {
        return ModConfig.get().farmer.greenThumbSeedSave;
    }

    public static float getFarmerFertileGroundSpeed() {
        return ModConfig.get().farmer.fertileGroundSpeed;
    }

    public static float getFarmerNutrientSeedChance() {
        return ModConfig.get().farmer.nutrientSeedChance;
    }

    public static float getFarmerAbundantExtra() {
        return ModConfig.get().farmer.abundantExtra;
    }

    public static float getFarmerAbundantAdjacent() {
        return ModConfig.get().farmer.abundantAdjacent;
    }

    public static int getFarmerFertileGroundRadius() {
        return ModConfig.get().farmer.fertileGroundRadius;
    }

    public static int getFarmerAbilityRadius() {
        return ModConfig.get().farmer.abilityRadius;
    }

    public static int getFarmerAbilityDurationSeconds() {
        return ModConfig.get().farmer.abilityDurationSeconds;
    }

    public static int getFarmerAbilityCooldownSeconds() {
        return ModConfig.get().farmer.abilityCooldownSeconds;
    }

    public static int getFarmerAreaPlantingLevel() {
        return ModConfig.get().farmer.areaPlantingLevel;
    }

    public static int getFarmerAreaPlantingRadius() {
        return ModConfig.get().farmer.areaPlantingRadius;
    }

    // Legacy constants
    public static final float FARMER_DOUBLE_HARVEST_PER_LEVEL = 0.005f;
    public static final float FARMER_GOLDEN_CROP_PER_LEVEL = 0.0015f;
    public static final int FARMER_GREEN_THUMB_LEVEL = 10;
    public static final int FARMER_FERTILE_GROUND_LEVEL = 25;
    public static final int FARMER_NUTRIENT_CYCLE_LEVEL = 50;
    public static final int FARMER_ABUNDANT_HARVEST_LEVEL = 75;
    public static final int FARMER_MASTER_LEVEL = 100;
    public static final float FARMER_GREEN_THUMB_EXTRA = 0.05f;
    public static final float FARMER_GREEN_THUMB_SEED_SAVE = 0.10f;
    public static final float FARMER_FERTILE_GROUND_SPEED = 0.25f;
    public static final float FARMER_NUTRIENT_SEED_CHANCE = 0.05f;
    public static final float FARMER_ABUNDANT_EXTRA = 0.15f;
    public static final float FARMER_ABUNDANT_ADJACENT = 0.10f;
    public static final int FARMER_FERTILE_GROUND_RADIUS = 32;
    public static final int FARMER_ABILITY_RADIUS = 8;
    public static final int FARMER_ABILITY_DURATION_SECONDS = 10;
    public static final int FARMER_ABILITY_COOLDOWN_SECONDS = 120;
    public static final int FARMER_AREA_PLANTING_LEVEL = 25;
    public static final int FARMER_AREA_PLANTING_RADIUS = 1;

    // --- FISHER ---
    public static float getFisherSpeedPerLevel() {
        return ModConfig.get().fisher.speedPerLevel;
    }

    public static float getFisherEpicBundlePerLevel() {
        return ModConfig.get().fisher.epicBundlePerLevel;
    }

    public static int getFisherWaitReductionLevel() {
        return ModConfig.get().fisher.waitReductionLevel;
    }

    public static int getFisherTreasureBonusLevel() {
        return ModConfig.get().fisher.treasureBonusLevel;
    }

    public static int getFisherDolphinGraceLevel() {
        return ModConfig.get().fisher.dolphinGraceLevel;
    }

    public static int getFisherLuckSeaLevel() {
        return ModConfig.get().fisher.luckSeaLevel;
    }

    public static int getFisherMasterLevel() {
        return ModConfig.get().fisher.masterLevel;
    }

    public static float getFisherWaitReduction() {
        return ModConfig.get().fisher.waitReduction;
    }

    public static float getFisherTreasureBonus() {
        return ModConfig.get().fisher.treasureBonus;
    }

    public static float getFisherXpBonus() {
        return ModConfig.get().fisher.xpBonus;
    }

    public static int getFisherAbilityDurationSeconds() {
        return ModConfig.get().fisher.abilityDurationSeconds;
    }

    public static int getFisherAbilityCooldownSeconds() {
        return ModConfig.get().fisher.abilityCooldownSeconds;
    }

    public static float getFisherRainDanceSpeedBonus() {
        return ModConfig.get().fisher.rainDanceSpeedBonus;
    }

    public static float getFisherRainDanceTreasureBonus() {
        return ModConfig.get().fisher.rainDanceTreasureBonus;
    }

    public static int getFisherRainDanceBundleMultiplier() {
        return ModConfig.get().fisher.rainDanceBundleMultiplier;
    }

    // Legacy constants
    public static final float FISHER_SPEED_PER_LEVEL = 0.005f;
    public static final float FISHER_EPIC_BUNDLE_PER_LEVEL = 0.001f;
    public static final int FISHER_WAIT_REDUCTION_LEVEL = 10;
    public static final int FISHER_TREASURE_BONUS_LEVEL = 25;
    public static final int FISHER_DOLPHIN_GRACE_LEVEL = 50;
    public static final int FISHER_LUCK_SEA_LEVEL = 75;
    public static final int FISHER_MASTER_LEVEL = 100;
    public static final float FISHER_WAIT_REDUCTION = 0.25f;
    public static final float FISHER_TREASURE_BONUS = 0.10f;
    public static final float FISHER_XP_BONUS = 0.10f;
    public static final int FISHER_ABILITY_DURATION_SECONDS = 60;
    public static final int FISHER_ABILITY_COOLDOWN_SECONDS = 900;
    public static final float FISHER_RAIN_DANCE_SPEED_BONUS = 0.50f;
    public static final float FISHER_RAIN_DANCE_TREASURE_BONUS = 0.30f;
    public static final int FISHER_RAIN_DANCE_BUNDLE_MULTIPLIER = 2;

    // --- BLACKSMITH ---
    public static float getBlacksmithResistancePerLevel() {
        return ModConfig.get().blacksmith.resistancePerLevel;
    }

    public static int getBlacksmithIronSkinLevel() {
        return ModConfig.get().blacksmith.ironSkinLevel;
    }

    public static int getBlacksmithEfficientAnvilLevel() {
        return ModConfig.get().blacksmith.efficientAnvilLevel;
    }

    public static int getBlacksmithForgedResilienceLevel() {
        return ModConfig.get().blacksmith.forgedResilienceLevel;
    }

    public static int getBlacksmithThornsMasterLevel() {
        return ModConfig.get().blacksmith.thornsMasterLevel;
    }

    public static int getBlacksmithMasterLevel() {
        return ModConfig.get().blacksmith.masterLevel;
    }

    public static float getBlacksmithIronSkinBonus() {
        return ModConfig.get().blacksmith.ironSkinBonus;
    }

    public static float getBlacksmithAnvilXpDiscount() {
        return ModConfig.get().blacksmith.anvilXpDiscount;
    }

    public static float getBlacksmithAnvilMaterialSave() {
        return ModConfig.get().blacksmith.anvilMaterialSave;
    }

    public static float getBlacksmithFireExplosionResist() {
        return ModConfig.get().blacksmith.fireExplosionResist;
    }

    public static float getBlacksmithThornsChance() {
        return ModConfig.get().blacksmith.thornsChance;
    }

    public static float getBlacksmithThornsReflect() {
        return ModConfig.get().blacksmith.thornsReflect;
    }

    public static float getBlacksmithKnockbackReduction() {
        return ModConfig.get().blacksmith.knockbackReduction;
    }

    public static float getBlacksmithSuperEnchantChance() {
        return ModConfig.get().blacksmith.superEnchantChance;
    }

    public static int getBlacksmithAbilityDurationSeconds() {
        return ModConfig.get().blacksmith.abilityDurationSeconds;
    }

    public static int getBlacksmithAbilityCooldownSeconds() {
        return ModConfig.get().blacksmith.abilityCooldownSeconds;
    }

    public static float getBlacksmithTitaniumResistance() {
        return ModConfig.get().blacksmith.titaniumResistance;
    }

    public static float getBlacksmithTitaniumRegen() {
        return ModConfig.get().blacksmith.titaniumRegen;
    }

    // Legacy constants
    public static final float BLACKSMITH_RESISTANCE_PER_LEVEL = 0.02f;
    public static final int BLACKSMITH_IRON_SKIN_LEVEL = 10;
    public static final int BLACKSMITH_EFFICIENT_ANVIL_LEVEL = 25;
    public static final int BLACKSMITH_FORGED_RESILIENCE_LEVEL = 50;
    public static final int BLACKSMITH_THORNS_MASTER_LEVEL = 75;
    public static final int BLACKSMITH_MASTER_LEVEL = 100;
    public static final float BLACKSMITH_IRON_SKIN_BONUS = 0.05f;
    public static final float BLACKSMITH_ANVIL_XP_DISCOUNT = 0.25f;
    public static final float BLACKSMITH_ANVIL_MATERIAL_SAVE = 0.10f;
    public static final float BLACKSMITH_FIRE_EXPLOSION_RESIST = 0.10f;
    public static final float BLACKSMITH_THORNS_CHANCE = 0.20f;
    public static final float BLACKSMITH_THORNS_REFLECT = 0.25f;
    public static final float BLACKSMITH_KNOCKBACK_REDUCTION = 0.50f;
    public static final float BLACKSMITH_SUPER_ENCHANT_CHANCE = 0.25f;
    public static final int BLACKSMITH_ABILITY_DURATION_SECONDS = 15;
    public static final int BLACKSMITH_ABILITY_COOLDOWN_SECONDS = 1200;
    public static final float BLACKSMITH_TITANIUM_RESISTANCE = 0.30f;
    public static final float BLACKSMITH_TITANIUM_REGEN = 1.0f;

    // --- BUILDER ---
    public static float getBuilderReachPerLevel() {
        return ModConfig.get().builder.reachPerLevel;
    }

    public static int getBuilderExtendedReachLevel() {
        return ModConfig.get().builder.extendedReachLevel;
    }

    public static int getBuilderEfficientCraftingLevel() {
        return ModConfig.get().builder.efficientCraftingLevel;
    }

    public static int getBuilderSafeLandingLevel() {
        return ModConfig.get().builder.safeLandingLevel;
    }

    public static int getBuilderScaffoldMasterLevel() {
        return ModConfig.get().builder.scaffoldMasterLevel;
    }

    public static int getBuilderMasterReachLevel() {
        return ModConfig.get().builder.masterReachLevel;
    }

    public static int getBuilderMasterLevel() {
        return ModConfig.get().builder.masterLevel;
    }

    public static float getBuilderLevel10Reach() {
        return ModConfig.get().builder.level10Reach;
    }

    public static float getBuilderLevel75Reach() {
        return ModConfig.get().builder.level75Reach;
    }

    public static float getBuilderDecorativeEconomy() {
        return ModConfig.get().builder.decorativeEconomy;
    }

    public static float getBuilderStructuralEconomy() {
        return ModConfig.get().builder.structuralEconomy;
    }

    public static float getBuilderFallDamageReduction() {
        return ModConfig.get().builder.fallDamageReduction;
    }

    public static float getBuilderScaffoldSpeedMultiplier() {
        return ModConfig.get().builder.scaffoldSpeedMultiplier;
    }

    public static int getBuilderAbilityDurationSeconds() {
        return ModConfig.get().builder.abilityDurationSeconds;
    }

    public static int getBuilderAbilityCooldownSeconds() {
        return ModConfig.get().builder.abilityCooldownSeconds;
    }

    public static int getBuilderBrushMaxDistance() {
        return ModConfig.get().builder.brushMaxDistance;
    }

    public static int getBuilderHighBuildYThreshold() {
        return ModConfig.get().builder.highBuildYThreshold;
    }

    // Legacy constants
    public static final float BUILDER_REACH_PER_LEVEL = 0.05f;
    public static final int BUILDER_EXTENDED_REACH_LEVEL = 10;
    public static final int BUILDER_EFFICIENT_CRAFTING_LEVEL = 15;
    public static final int BUILDER_SAFE_LANDING_LEVEL = 25;
    public static final int BUILDER_SCAFFOLD_MASTER_LEVEL = 50;
    public static final int BUILDER_MASTER_REACH_LEVEL = 75;
    public static final int BUILDER_MASTER_LEVEL = 100;
    public static final float BUILDER_LEVEL_10_REACH = 1.0f;
    public static final float BUILDER_LEVEL_75_REACH = 5.0f;
    public static final float BUILDER_DECORATIVE_ECONOMY = 0.20f;
    public static final float BUILDER_STRUCTURAL_ECONOMY = 0.50f;
    public static final float BUILDER_FALL_DAMAGE_REDUCTION = 0.25f;
    public static final float BUILDER_SCAFFOLD_SPEED_MULTIPLIER = 1.5f;
    public static final int BUILDER_ABILITY_DURATION_SECONDS = 120;
    public static final int BUILDER_ABILITY_COOLDOWN_SECONDS = 600;
    public static final int BUILDER_BRUSH_MAX_DISTANCE = 6;
    public static final int BUILDER_HIGH_BUILD_Y_THRESHOLD = 100;

    // --- EXPLORER ---
    public static float getExplorerSpeedPerLevel() {
        return ModConfig.get().explorer.speedPerLevel;
    }

    public static int getExplorerLuckInterval() {
        return ModConfig.get().explorer.luckInterval;
    }

    public static float getExplorerHungerReductionPerLevel() {
        return ModConfig.get().explorer.hungerReductionPerLevel;
    }

    public static int getExplorerStepAssistLevel() {
        return ModConfig.get().explorer.stepAssistLevel;
    }

    public static int getExplorerAquaticLevel() {
        return ModConfig.get().explorer.aquaticLevel;
    }

    public static int getExplorerNightVisionLevel() {
        return ModConfig.get().explorer.nightVisionLevel;
    }

    public static int getExplorerFeatherFeetLevel() {
        return ModConfig.get().explorer.featherFeetLevel;
    }

    public static int getExplorerNetherWalkerLevel() {
        return ModConfig.get().explorer.netherWalkerLevel;
    }

    public static int getExplorerMasterLevel() {
        return ModConfig.get().explorer.masterLevel;
    }

    public static float getExplorerBreathMultiplier() {
        return ModConfig.get().explorer.breathMultiplier;
    }

    public static float getExplorerFallDamageReduction() {
        return ModConfig.get().explorer.fallDamageReduction;
    }

    public static int getExplorerTreasureRadius() {
        return ModConfig.get().explorer.treasureRadius;
    }

    public static float getExplorerStepHeight() {
        return ModConfig.get().explorer.stepHeight;
    }

    public static int getExplorerXpBiome() {
        return XpDataManager.getValues().explorer.discoveries.biome;
    }

    public static int getExplorerXpStructure() {
        return XpDataManager.getValues().explorer.discoveries.structure;
    }

    public static int getExplorerXpLootChest() {
        return XpDataManager.getValues().explorer.loot.chest;
    }

    public static int getExplorerXpMapComplete() {
        return XpDataManager.getValues().explorer.loot.mapComplete;
    }

    public static int getExplorerXpWanderingTrade() {
        return XpDataManager.getValues().explorer.loot.wanderingTrade;
    }

    public static double getExplorerDistanceThreshold() {
        return XpDataManager.getValues().explorer.travel.distanceThreshold;
    }

    public static int getExplorerXpPerDistance() {
        return XpDataManager.getValues().explorer.travel.xpPerDistance;
    }

    // Legacy constants
    public static final float EXPLORER_SPEED_PER_LEVEL = 0.002f;
    public static final int EXPLORER_LUCK_INTERVAL = 20;
    public static final float EXPLORER_HUNGER_REDUCTION_PER_LEVEL = 0.005f;
    public static final int EXPLORER_STEP_ASSIST_LEVEL = 10;
    public static final int EXPLORER_AQUATIC_LEVEL = 20;
    public static final int EXPLORER_NIGHT_VISION_LEVEL = 35;
    public static final int EXPLORER_FEATHER_FEET_LEVEL = 65;
    public static final int EXPLORER_NETHER_WALKER_LEVEL = 80;
    public static final int EXPLORER_MASTER_LEVEL = 100;
    public static final float EXPLORER_BREATH_MULTIPLIER = 1.5f;
    public static final float EXPLORER_FALL_DAMAGE_REDUCTION = 0.40f;
    public static final int EXPLORER_TREASURE_RADIUS = 128;
    public static final float EXPLORER_STEP_HEIGHT = 1.0f;
    public static final int EXPLORER_XP_BIOME = 500;
    public static final int EXPLORER_XP_STRUCTURE = 200;
    public static final int EXPLORER_XP_LOOT_CHEST = 300;
    public static final int EXPLORER_XP_MAP_COMPLETE = 2000;
    public static final int EXPLORER_XP_WANDERING_TRADE = 400;
    public static final double EXPLORER_DISTANCE_THRESHOLD = 50.0;
    public static final int EXPLORER_XP_PER_DISTANCE = 35;

    // --- MILESTONES ---
    public static int[] getSkillMilestones() {
        return ModConfig.get().milestones.skillMilestones;
    }

    public static int getMilestoneXpLevel10() {
        return ModConfig.get().milestones.xpLevel10;
    }

    public static int getMilestoneXpLevel25() {
        return ModConfig.get().milestones.xpLevel25;
    }

    public static int getMilestoneXpLevel50() {
        return ModConfig.get().milestones.xpLevel50;
    }

    public static int getMilestoneXpLevel75() {
        return ModConfig.get().milestones.xpLevel75;
    }

    public static int getMilestoneXpLevel100() {
        return ModConfig.get().milestones.xpLevel100;
    }

    // Legacy constants
    public static final int[] SKILL_MILESTONES = { 10, 25, 50, 75, 100 };
    public static final int MILESTONE_XP_LEVEL_10 = 10;
    public static final int MILESTONE_XP_LEVEL_25 = 25;
    public static final int MILESTONE_XP_LEVEL_50 = 50;
    public static final int MILESTONE_XP_LEVEL_75 = 75;
    public static final int MILESTONE_XP_LEVEL_100 = 150;

    // --- COOLDOWN ---
    public static float getCooldownReductionPerLevel() {
        return ModConfig.get().general.cooldownReductionPerLevel;
    }

    public static final float COOLDOWN_REDUCTION_PER_LEVEL = 0.005f;

    /** Converte segundos para ticks */
    public static int toTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /** Converte segundos para ticks (long) */
    public static long toTicksLong(int seconds) {
        return (long) seconds * TICKS_PER_SECOND;
    }

    /**
     * Retorna a quantidade de níveis de XP vanilla para um milestone específico.
     * 
     * @param level O nível do milestone
     * @return Quantidade de níveis de XP vanilla, ou 0 se não for um milestone
     */
    public static int getMilestoneVanillaXpLevels(int level) {
        return switch (level) {
            case 10 -> getMilestoneXpLevel10();
            case 25 -> getMilestoneXpLevel25();
            case 50 -> getMilestoneXpLevel50();
            case 75 -> getMilestoneXpLevel75();
            case 100 -> getMilestoneXpLevel100();
            default -> 0;
        };
    }

    /**
     * Calcula o cooldown reduzido baseado no nível do jogador.
     * Aplica redução de 0.5% por nível (máximo 50% no nível 100).
     * 
     * @param baseCooldownSeconds Cooldown base em segundos
     * @param level               Nível atual da skill do jogador
     * @return Cooldown reduzido em segundos
     */
    public static int getDynamicCooldown(int baseCooldownSeconds, int level) {
        float reduction = Math.min(level * getCooldownReductionPerLevel(), 0.50f);
        return (int) (baseCooldownSeconds * (1.0f - reduction));
    }

    /**
     * Calcula o cooldown reduzido em ticks.
     * 
     * @param baseCooldownSeconds Cooldown base em segundos
     * @param level               Nível atual da skill do jogador
     * @return Cooldown reduzido em ticks
     */
    public static long getDynamicCooldownTicks(int baseCooldownSeconds, int level) {
        return toTicksLong(getDynamicCooldown(baseCooldownSeconds, level));
    }

    // --- PRESTIGE ---
    public static float getPrestigeXpBonus() {
        return ModConfig.get().prestige.xpBonus;
    }

    public static float getPrestigePassiveBonus() {
        return ModConfig.get().prestige.passiveBonus;
    }

    public static int getMaxPrestigeLevel() {
        return ModConfig.get().prestige.maxPrestigeLevel;
    }

    public static String[] getPrestigeSymbols() {
        return ModConfig.get().prestige.prestigeSymbols;
    }

    public static int[] getPrestigeColors() {
        return ModConfig.get().prestige.prestigeColors;
    }

    // --- NOTIFICATIONS ---
    public static float getLevelUpVolume() {
        return ModConfig.get().notifications.levelUpVolume;
    }

    public static float getLevelUpPitch() {
        return ModConfig.get().notifications.levelUpPitch;
    }

    // --- DAILY CHALLENGES ---
    public static int getChallengesPerDay() {
        return ModConfig.get().dailyChallenges.challengesPerDay;
    }

    public static int getBaseXpReward() {
        return ModConfig.get().dailyChallenges.baseXpReward;
    }

    public static int getBonusXpAllComplete() {
        return ModConfig.get().dailyChallenges.bonusXpAllComplete;
    }

    public static int getChallengeResetIntervalTicks() {
        return ModConfig.get().dailyChallenges.resetIntervalTicks;
    }

    public static boolean isDifficultyScalingEnabled() {
        return ModConfig.get().dailyChallenges.difficultyScalingEnabled;
    }

    public static float getChallengeMinTargetMultiplier() {
        return ModConfig.get().dailyChallenges.minTargetMultiplier;
    }

    public static float getChallengeMaxTargetMultiplier() {
        return ModConfig.get().dailyChallenges.maxTargetMultiplier;
    }

    // --- XP STREAK ---
    public static int getStreakTimeoutMs() {
        return ModConfig.get().xpStreak.streakTimeoutMs;
    }

    public static int getMaxStreak() {
        return ModConfig.get().xpStreak.maxStreak;
    }

    public static float getStreakBonusPerLevel() {
        return ModConfig.get().xpStreak.streakBonusPerLevel;
    }

    // --- MINER SOURCE ---
    public static int getMinerXpStone() {
        return XpDataManager.getValues().miner.blocks.stone;
    }

    public static int getMinerXpCoal() {
        return XpDataManager.getValues().miner.blocks.coal;
    }

    public static int getMinerXpCopper() {
        return XpDataManager.getValues().miner.blocks.copper;
    }

    public static int getMinerXpIron() {
        return XpDataManager.getValues().miner.blocks.iron;
    }

    public static int getMinerXpGold() {
        return XpDataManager.getValues().miner.blocks.gold;
    }

    public static int getMinerXpLapis() {
        return XpDataManager.getValues().miner.blocks.lapis;
    }

    public static int getMinerXpRedstone() {
        return XpDataManager.getValues().miner.blocks.redstone;
    }

    public static int getMinerXpDiamond() {
        return XpDataManager.getValues().miner.blocks.diamond;
    }

    public static int getMinerXpEmerald() {
        return XpDataManager.getValues().miner.blocks.emerald;
    }

    public static int getMinerXpAncientDebris() {
        return XpDataManager.getValues().miner.blocks.ancientDebris;
    }

    public static int getMinerXpNetherQuartz() {
        return XpDataManager.getValues().miner.blocks.netherQuartz;
    }

    public static int getMinerXpNetherGold() {
        return XpDataManager.getValues().miner.blocks.netherGold;
    }

    // --- WARRIOR SOURCE ---
    public static int getWarriorXpEnderDragon() {
        return XpDataManager.getValues().warrior.mobs.enderDragon;
    }

    public static int getWarriorXpWither() {
        return XpDataManager.getValues().warrior.mobs.wither;
    }

    public static int getWarriorXpWarden() {
        return XpDataManager.getValues().warrior.mobs.warden;
    }

    public static int getWarriorXpEnderman() {
        return XpDataManager.getValues().warrior.mobs.enderman;
    }

    public static int getWarriorXpBlaze() {
        return XpDataManager.getValues().warrior.mobs.blaze;
    }

    public static int getWarriorXpMonsterDefault() {
        return XpDataManager.getValues().warrior.mobs.defaultMob;
    }

    // --- ARCHER SOURCE ---
    public static int getArcherXpHitBase() {
        return XpDataManager.getValues().archer.hits.base;
    }

    public static int getArcherXpHitHostile() {
        return XpDataManager.getValues().archer.hits.hostile;
    }

    public static int getArcherXpKillBase() {
        return XpDataManager.getValues().archer.kills.base;
    }

    public static int getArcherXpKillHostile() {
        return XpDataManager.getValues().archer.kills.hostile;
    }

    public static int getArcherLongRangeTier1() {
        return XpDataManager.getValues().archer.longRange.tier1;
    }

    public static int getArcherLongRangeTier2() {
        return XpDataManager.getValues().archer.longRange.tier2;
    }

    public static int getArcherLongRangeTier3() {
        return XpDataManager.getValues().archer.longRange.tier3;
    }

    public static double getArcherLongRangeMultiplier1() {
        return XpDataManager.getValues().archer.longRange.multiplier1;
    }

    public static double getArcherLongRangeMultiplier2() {
        return XpDataManager.getValues().archer.longRange.multiplier2;
    }

    public static double getArcherLongRangeMultiplier3() {
        return XpDataManager.getValues().archer.longRange.multiplier3;
    }

    // --- FARMER SOURCE ---
    public static int getFarmerXpWheat() {
        return XpDataManager.getValues().farmer.crops.wheat;
    }

    public static int getFarmerXpCarrot() {
        return XpDataManager.getValues().farmer.crops.carrot;
    }

    public static int getFarmerXpPotato() {
        return XpDataManager.getValues().farmer.crops.potato;
    }

    public static int getFarmerXpBeetroot() {
        return XpDataManager.getValues().farmer.crops.beetroot;
    }

    public static int getFarmerXpMelon() {
        return XpDataManager.getValues().farmer.crops.melon;
    }

    public static int getFarmerXpPumpkin() {
        return XpDataManager.getValues().farmer.crops.pumpkin;
    }

    public static int getFarmerXpNetherWart() {
        return XpDataManager.getValues().farmer.crops.netherWart;
    }

    public static int getFarmerXpSweetBerry() {
        return XpDataManager.getValues().farmer.crops.sweetBerry;
    }

    public static int getFarmerXpCocoa() {
        return XpDataManager.getValues().farmer.crops.cocoa;
    }

    // --- FISHER SOURCE ---
    public static int getFisherXpTreasure() {
        return XpDataManager.getValues().fisher.categories.treasure;
    }

    public static int getFisherXpFish() {
        return XpDataManager.getValues().fisher.categories.fish;
    }

    public static int getFisherXpJunk() {
        return XpDataManager.getValues().fisher.categories.junk;
    }

    // --- BLACKSMITH SOURCE ---
    public static int getBlacksmithXpAnvilRepair() {
        return XpDataManager.getValues().blacksmith.anvil.repair;
    }

    public static int getBlacksmithXpAnvilRename() {
        return XpDataManager.getValues().blacksmith.anvil.rename;
    }

    public static int getBlacksmithXpAnvilEnchantCombine() {
        return XpDataManager.getValues().blacksmith.anvil.enchantCombine;
    }

    public static int getBlacksmithXpEnchantLevel1() {
        return XpDataManager.getValues().blacksmith.enchanting.level1;
    }

    public static int getBlacksmithXpEnchantLevel2() {
        return XpDataManager.getValues().blacksmith.enchanting.level2;
    }

    public static int getBlacksmithXpEnchantLevel3() {
        return XpDataManager.getValues().blacksmith.enchanting.level3;
    }

    public static int getBlacksmithXpSmeltIron() {
        return XpDataManager.getValues().blacksmith.smelting.iron;
    }

    public static int getBlacksmithXpSmeltGold() {
        return XpDataManager.getValues().blacksmith.smelting.gold;
    }

    public static int getBlacksmithXpSmeltCopper() {
        return XpDataManager.getValues().blacksmith.smelting.copper;
    }

    public static int getBlacksmithXpSmeltAncientDebris() {
        return XpDataManager.getValues().blacksmith.smelting.ancientDebris;
    }

    public static int getBlacksmithXpGrindstoneUse() {
        return XpDataManager.getValues().blacksmith.grindstoneXp;
    }

    // --- BUILDER SOURCE ---
    public static int getBuilderXpStructural() {
        return XpDataManager.getValues().builder.placement.structural;
    }

    public static int getBuilderXpDecorative() {
        return XpDataManager.getValues().builder.placement.decorative;
    }

    public static int getBuilderXpBasic() {
        return XpDataManager.getValues().builder.placement.basic;
    }

    public static int getBuilderXpPremium() {
        return XpDataManager.getValues().builder.placement.premium;
    }

    public static int getBuilderXpCraftStructural() {
        return XpDataManager.getValues().builder.crafting.structural;
    }

    public static int getBuilderXpCraftDecorative() {
        return XpDataManager.getValues().builder.crafting.decorative;
    }

    // --- EPIC BUNDLE ---
    public static int getEpicBundleWeightEnchantedBook() {
        return ModConfig.get().epicBundle.weightEnchantedBook;
    }

    public static int getEpicBundleWeightGoldBlock() {
        return ModConfig.get().epicBundle.weightGoldBlock;
    }

    public static int getEpicBundleWeightDiamondBlock() {
        return ModConfig.get().epicBundle.weightDiamondBlock;
    }

    public static int getEpicBundleWeightTrident() {
        return ModConfig.get().epicBundle.weightTrident;
    }

    public static int getEpicBundleWeightHeartOfSea() {
        return ModConfig.get().epicBundle.weightHeartOfSea;
    }

    public static int getEpicBundleWeightGoldenApple() {
        return ModConfig.get().epicBundle.weightGoldenApple;
    }

    public static int getEpicBundleLevelDiamondBlock() {
        return ModConfig.get().epicBundle.levelDiamondBlock;
    }

    public static int getEpicBundleLevelTrident() {
        return ModConfig.get().epicBundle.levelTrident;
    }

    public static int getEpicBundleLevelHeartOfSea() {
        return ModConfig.get().epicBundle.levelHeartOfSea;
    }

    public static int getEpicBundleLevelGoldenApple() {
        return ModConfig.get().epicBundle.levelGoldenApple;
    }

    // --- SYNERGIES ---
    public static float getSynergyIronWill() {
        return ModConfig.get().synergies.ironWill;
    }

    public static float getSynergyForgeMaster() {
        return ModConfig.get().synergies.forgeMaster;
    }

    public static float getSynergyRanger() {
        return ModConfig.get().synergies.ranger;
    }

    public static float getSynergyNaturesBounty() {
        return ModConfig.get().synergies.naturesBounty;
    }

    public static float getSynergyTreasureHunter() {
        return ModConfig.get().synergies.treasureHunter;
    }

    public static float getSynergyCombatMaster() {
        return ModConfig.get().synergies.combatMaster;
    }

    public static float getSynergyMasterCrafter() {
        return ModConfig.get().synergies.masterCrafter;
    }

    // --- NEW SYNERGIES ---
    public static float getSynergySurvivor() {
        return ModConfig.get().synergies.survivor;
    }

    public static float getSynergyIndustrial() {
        return ModConfig.get().synergies.industrial;
    }

    public static float getSynergySeaWarrior() {
        return ModConfig.get().synergies.seaWarrior;
    }

    public static float getSynergyGreenArcher() {
        return ModConfig.get().synergies.greenArcher;
    }

    public static float getSynergyProspector() {
        return ModConfig.get().synergies.prospector;
    }

    public static float getSynergyAdventurer() {
        return ModConfig.get().synergies.adventurer;
    }

    public static float getSynergyHermit() {
        return ModConfig.get().synergies.hermit;
    }
}
