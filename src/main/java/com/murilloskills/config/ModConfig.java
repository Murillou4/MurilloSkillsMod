package com.murilloskills.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.murilloskills.MurilloSkills;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * External configuration system for MurilloSkills.
 * Loads settings from config/murilloskills.json and provides defaults if
 * missing.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "murilloskills.json";

    private static ConfigData config;

    /**
     * Main configuration data class.
     * All values have sensible defaults matching the original hardcoded values.
     */
    public static class ConfigData {
        // --- GENERAL ---
        public GeneralConfig general = new GeneralConfig();

        // --- SKILLS ---
        public MinerConfig miner = new MinerConfig();
        public WarriorConfig warrior = new WarriorConfig();
        public ArcherConfig archer = new ArcherConfig();
        public FarmerConfig farmer = new FarmerConfig();
        public FisherConfig fisher = new FisherConfig();
        public BlacksmithConfig blacksmith = new BlacksmithConfig();
        public BuilderConfig builder = new BuilderConfig();
        public ExplorerConfig explorer = new ExplorerConfig();

        // --- MILESTONES ---
        public MilestoneConfig milestones = new MilestoneConfig();
    }

    public static class GeneralConfig {
        public int maxLevel = 100;
        public float cooldownReductionPerLevel = 0.005f;
    }

    public static class MinerConfig {
        public float speedPerLevel = 0.03f;
        public float fortunePerLevel = 0.03f;
        public int nightVisionLevel = 10;
        public int durabilityLevel = 30;
        public int radarLevel = 60;
        public int masterLevel = 100;
        public float durabilityChance = 0.15f;
        public int abilityCooldownSeconds = 1200;
        public int abilityRadius = 30;
        public int abilityDurationSeconds = 10;
    }

    public static class WarriorConfig {
        public float damagePerLevel = 0.05f;
        public float lootingPerLevel = 0.02f;
        public int resistanceUnlockLevel = 25;
        public float resistanceReduction = 0.85f;
        public int lifestealUnlockLevel = 75;
        public float lifestealPercentage = 0.15f;
        public int masterLevel = 100;
        public int abilityCooldownSeconds = 1200;
        public int berserkDurationSeconds = 10;
        public float berserkLifesteal = 0.50f;
        public int exhaustionDurationSeconds = 5;
        public int berserkStrengthAmplifier = 3;
        public int berserkResistanceAmplifier = 1;
    }

    public static class ArcherConfig {
        public float damagePerLevel = 0.03f;
        public int fastArrowsLevel = 10;
        public int bonusDamageLevel = 25;
        public int penetrationLevel = 50;
        public int stableShotLevel = 75;
        public int masterLevel = 100;
        public float arrowSpeedMultiplier = 1.25f;
        public float bonusDamageAmount = 0.05f;
        public float spreadReduction = 0.50f;
        public int abilityCooldownSeconds = 1200;
        public int masterRangerDurationSeconds = 30;
        public float headshotDamageBonus = 0.30f;
    }

    public static class FarmerConfig {
        public float doubleHarvestPerLevel = 0.005f;
        public float goldenCropPerLevel = 0.0015f;
        public int greenThumbLevel = 10;
        public int fertileGroundLevel = 25;
        public int nutrientCycleLevel = 50;
        public int abundantHarvestLevel = 75;
        public int masterLevel = 100;
        public float greenThumbExtra = 0.05f;
        public float greenThumbSeedSave = 0.10f;
        public float fertileGroundSpeed = 0.25f;
        public float nutrientSeedChance = 0.05f;
        public float abundantExtra = 0.15f;
        public float abundantAdjacent = 0.10f;
        public int fertileGroundRadius = 32;
        public int abilityRadius = 8;
        public int abilityDurationSeconds = 10;
        public int abilityCooldownSeconds = 120;
        public int areaPlantingLevel = 25;
        public int areaPlantingRadius = 1;
    }

    public static class FisherConfig {
        public float speedPerLevel = 0.005f;
        public float epicBundlePerLevel = 0.001f;
        public int waitReductionLevel = 10;
        public int treasureBonusLevel = 25;
        public int dolphinGraceLevel = 50;
        public int luckSeaLevel = 75;
        public int masterLevel = 100;
        public float waitReduction = 0.25f;
        public float treasureBonus = 0.10f;
        public float xpBonus = 0.10f;
        public int abilityDurationSeconds = 60;
        public int abilityCooldownSeconds = 900;
        public float rainDanceSpeedBonus = 0.50f;
        public float rainDanceTreasureBonus = 0.30f;
        public int rainDanceBundleMultiplier = 2;
    }

    public static class BlacksmithConfig {
        public float resistancePerLevel = 0.02f;
        public int ironSkinLevel = 10;
        public int efficientAnvilLevel = 25;
        public int forgedResilienceLevel = 50;
        public int thornsMasterLevel = 75;
        public int masterLevel = 100;
        public float ironSkinBonus = 0.05f;
        public float anvilXpDiscount = 0.25f;
        public float anvilMaterialSave = 0.10f;
        public float fireExplosionResist = 0.10f;
        public float thornsChance = 0.20f;
        public float thornsReflect = 0.25f;
        public float knockbackReduction = 0.50f;
        public float superEnchantChance = 0.25f;
        public int abilityDurationSeconds = 15;
        public int abilityCooldownSeconds = 1200;
        public float titaniumResistance = 0.30f;
        public float titaniumRegen = 1.0f;
    }

    public static class BuilderConfig {
        public float reachPerLevel = 0.05f;
        public int extendedReachLevel = 10;
        public int efficientCraftingLevel = 15;
        public int safeLandingLevel = 25;
        public int scaffoldMasterLevel = 50;
        public int masterReachLevel = 75;
        public int masterLevel = 100;
        public float level10Reach = 1.0f;
        public float level75Reach = 5.0f;
        public float decorativeEconomy = 0.20f;
        public float structuralEconomy = 0.50f;
        public float fallDamageReduction = 0.25f;
        public float scaffoldSpeedMultiplier = 1.5f;
        public int abilityDurationSeconds = 120;
        public int abilityCooldownSeconds = 600;
        public int brushMaxDistance = 6;
        public int highBuildYThreshold = 100;
    }

    public static class ExplorerConfig {
        public float speedPerLevel = 0.002f;
        public int luckInterval = 20;
        public float hungerReductionPerLevel = 0.005f;
        public int stepAssistLevel = 10;
        public int aquaticLevel = 20;
        public int nightVisionLevel = 35;
        public int featherFeetLevel = 65;
        public int netherWalkerLevel = 80;
        public int masterLevel = 100;
        public float breathMultiplier = 1.5f;
        public float fallDamageReduction = 0.40f;
        public int treasureRadius = 128;
        public float stepHeight = 1.0f;
        public int xpBiome = 500;
        public int xpStructure = 200;
        public int xpLootChest = 300;
        public int xpMapComplete = 2000;
        public int xpWanderingTrade = 400;
        public double distanceThreshold = 50.0;
        public int xpPerDistance = 35;
    }

    public static class MilestoneConfig {
        public int[] skillMilestones = { 10, 25, 50, 75, 100 };
        public int xpLevel10 = 10;
        public int xpLevel25 = 25;
        public int xpLevel50 = 50;
        public int xpLevel75 = 75;
        public int xpLevel100 = 150;
    }

    /**
     * Loads config from file or creates default if not exists.
     */
    public static void load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                config = GSON.fromJson(json, ConfigData.class);
                MurilloSkills.LOGGER.info("Loaded config from {}", configPath);
            } catch (IOException e) {
                MurilloSkills.LOGGER.error("Failed to load config, using defaults", e);
                config = new ConfigData();
                save();
            }
        } else {
            MurilloSkills.LOGGER.info("Config not found, creating default config");
            config = new ConfigData();
            save();
        }
    }

    /**
     * Saves current config to file.
     */
    public static void save() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(config));
            MurilloSkills.LOGGER.info("Saved config to {}", configPath);
        } catch (IOException e) {
            MurilloSkills.LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Reloads config from file.
     */
    public static void reload() {
        load();
        MurilloSkills.LOGGER.info("Config reloaded");
    }

    /**
     * Gets the config data. Load must be called first.
     */
    public static ConfigData get() {
        if (config == null) {
            load();
        }
        return config;
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}
