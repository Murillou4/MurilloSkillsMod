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
        public XpConfig xp = new XpConfig();
        public PrestigeConfig prestige = new PrestigeConfig();
        public SynergiesConfig synergies = new SynergiesConfig();
        public NotificationConfig notifications = new NotificationConfig();
        public DailyChallengeConfig dailyChallenges = new DailyChallengeConfig();
        public XpStreakConfig xpStreak = new XpStreakConfig();
        public EpicBundleConfig epicBundle = new EpicBundleConfig();

        // --- SKILLS ---
        public MinerConfig miner = new MinerConfig();
        public MinerSourceConfig minerSource = new MinerSourceConfig(); // Separated for cleaner JSON

        public WarriorConfig warrior = new WarriorConfig();
        public WarriorSourceConfig warriorSource = new WarriorSourceConfig();

        public ArcherConfig archer = new ArcherConfig();
        public ArcherSourceConfig archerSource = new ArcherSourceConfig();

        public FarmerConfig farmer = new FarmerConfig();
        public FarmerSourceConfig farmerSource = new FarmerSourceConfig();

        public FisherConfig fisher = new FisherConfig();
        public FisherSourceConfig fisherSource = new FisherSourceConfig();

        public BlacksmithConfig blacksmith = new BlacksmithConfig();
        public BlacksmithSourceConfig blacksmithSource = new BlacksmithSourceConfig();

        public BuilderConfig builder = new BuilderConfig();
        public BuilderSourceConfig builderSource = new BuilderSourceConfig();

        public ExplorerConfig explorer = new ExplorerConfig();
        public VeinMinerConfig veinMiner = new VeinMinerConfig();

        // --- MILESTONES ---
        public MilestoneConfig milestones = new MilestoneConfig();
    }

    public static class GeneralConfig {
        public int maxLevel = 100;
        public float cooldownReductionPerLevel = 0.005f;
    }

    public static class XpConfig {
        public int base = 60;
        public int multiplier = 15;
        public int exponent = 2;
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
        public int scanLimit = 5000;
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

    public static class VeinMinerConfig {
        public int maxBlocks = 64;
        public boolean dropsToInventory = false;
    }

    public static class MilestoneConfig {
        public int[] skillMilestones = { 10, 25, 50, 75, 100 };
        public int xpLevel10 = 10;
        public int xpLevel25 = 25;
        public int xpLevel50 = 50;
        public int xpLevel75 = 75;
        public int xpLevel100 = 150;
    }

    public static class PrestigeConfig {
        public float xpBonus = 0.05f;
        public float passiveBonus = 0.02f;
        public int maxPrestigeLevel = 10;
        public String[] prestigeSymbols = {
                "⚔", "⚔⚔", "★", "★★", "✦", "✦✦", "✦✦✦", "♦", "♦♦", "👑"
        };
        // Hex colors: Green, Green, Cyan, Cyan, Yellow, Yellow, Magenta, Magenta, Gold,
        // Gold
        public int[] prestigeColors = {
                0xFF88FF88, 0xFF88FF88,
                0xFF88FFFF, 0xFF88FFFF,
                0xFFFFFF88, 0xFFFFFF88,
                0xFFFF88FF, 0xFFFF88FF,
                0xFFFFDD00, 0xFFFFDD00
        };
    }

    public static class NotificationConfig {
        public float levelUpVolume = 1.0f;
        public float levelUpPitch = 1.0f;
    }

    public static class DailyChallengeConfig {
        public int challengesPerDay = 3;
        public int baseXpReward = 800;
        public int bonusXpAllComplete = 1500;
        // Reset interval in game ticks (24000 = 1 Minecraft day = ~20 real minutes)
        public int resetIntervalTicks = 24000;
        // Whether to scale difficulty based on player skill level
        public boolean difficultyScalingEnabled = true;
        // Multipliers for target amounts based on average skill level (0-100)
        public float minTargetMultiplier = 0.5f; // At skill level 0
        public float maxTargetMultiplier = 2.0f; // At skill level 100
    }

    public static class XpStreakConfig {
        public int streakTimeoutMs = 5000;
        public int maxStreak = 10;
        public float streakBonusPerLevel = 0.10f;
    }

    // --- XP SOURCES ---
    public static class MinerSourceConfig {
        public int xpStone = 2;
        public int xpCoal = 5;
        public int xpCopper = 5;
        public int xpIron = 10;
        public int xpGold = 15;
        public int xpLapis = 10;
        public int xpRedstone = 10;
        public int xpDiamond = 60;
        public int xpEmerald = 100;
        public int xpAncientDebris = 150;
        public int xpNetherQuartz = 10;
        public int xpNetherGold = 10;
    }

    public static class WarriorSourceConfig {
        public int xpEnderDragon = 1000;
        public int xpWither = 500;
        public int xpWarden = 500;
        public int xpEnderman = 20;
        public int xpBlaze = 25;
        public int xpMonsterDefault = 15;
    }

    public static class ArcherSourceConfig {
        public int xpHitBase = 5;
        public int xpHitHostile = 10;
        public int xpKillBase = 15;
        public int xpKillHostile = 25;
        public int longRangeTier1 = 20;
        public int longRangeTier2 = 40;
        public int longRangeTier3 = 60;
        public double longRangeMultiplier1 = 1.5;
        public double longRangeMultiplier2 = 2.0;
        public double longRangeMultiplier3 = 2.0;
    }

    public static class FarmerSourceConfig {
        public int xpWheat = 3;
        public int xpCarrot = 3;
        public int xpPotato = 3;
        public int xpBeetroot = 3;
        public int xpMelon = 8;
        public int xpPumpkin = 8;
        public int xpNetherWart = 5;
        public int xpSweetBerry = 2;
        public int xpCocoa = 4;
    }

    public static class FisherSourceConfig {
        public int xpTreasure = 50;
        public int xpFish = 15;
        public int xpJunk = 5;
    }

    public static class BlacksmithSourceConfig {
        public int xpAnvilRepair = 80;
        public int xpAnvilRename = 50;
        public int xpAnvilEnchantCombine = 100;
        public int xpEnchantLevel1 = 40;
        public int xpEnchantLevel2 = 70;
        public int xpEnchantLevel3 = 100;
        public int xpSmeltIron = 4;
        public int xpSmeltGold = 6;
        public int xpSmeltCopper = 3;
        public int xpSmeltAncientDebris = 80;
        public int xpGrindstoneUse = 30;
    }

    public static class BuilderSourceConfig {
        public int xpStructural = 15;
        public int xpDecorative = 10;
        public int xpBasic = 3;
        public int xpPremium = 25;
        public int xpCraftStructural = 20;
        public int xpCraftDecorative = 12;
    }

    public static class EpicBundleConfig {
        public int weightEnchantedBook = 40;
        public int weightGoldBlock = 25;
        public int weightDiamondBlock = 20;
        public int weightTrident = 10;
        public int weightHeartOfSea = 4;
        public int weightGoldenApple = 1;

        public int levelDiamondBlock = 25;
        public int levelTrident = 50;
        public int levelHeartOfSea = 75;
        public int levelGoldenApple = 90;
    }

    public static class SynergiesConfig {
        public float ironWill = 0.10f;
        public float forgeMaster = 0.15f;
        public float ranger = 0.20f;
        public float naturesBounty = 0.10f;
        public float treasureHunter = 0.25f;
        public float combatMaster = 0.10f;
        public float masterCrafter = 0.30f;
        // New synergies
        public float survivor = 0.15f; // Warrior + Explorer
        public float industrial = 0.20f; // Miner + Builder
        public float seaWarrior = 0.12f; // Warrior + Fisher
        public float greenArcher = 0.15f; // Farmer + Archer
        public float prospector = 0.20f; // Miner + Warrior
        public float adventurer = 0.18f; // Builder + Explorer
        public float hermit = 0.15f; // Farmer + Builder
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
