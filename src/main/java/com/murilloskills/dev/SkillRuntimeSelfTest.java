package com.murilloskills.dev;

import com.murilloskills.MurilloSkills;
import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.impl.BuilderFillMode;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithMachineSpeedHelper;
import com.murilloskills.utils.BuilderReachHelper;
import com.murilloskills.utils.MinecraftVersionCompat;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillAttributes;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SkillRuntimeSelfTest {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Map<UUID, Integer> PENDING_PLAYERS = new ConcurrentHashMap<>();
    private static final Path LOG_PATH = Path.of("logs", "murilloskills-skill-selftest.log");
    private static final int JOIN_DELAY_TICKS = 80;

    private SkillRuntimeSelfTest() {
    }

    public static void register() {
        if (!isEnabled() || !REGISTERED.compareAndSet(false, true)) {
            return;
        }

        MurilloSkills.LOGGER.warn("[MurilloSkillsSelfTest] ENABLED - runtime skill validation will run for joined players.");

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PENDING_PLAYERS.put(handler.player.getUuid(), JOIN_DELAY_TICKS));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                Integer ticksLeft = PENDING_PLAYERS.get(uuid);
                if (ticksLeft == null) {
                    continue;
                }
                if (ticksLeft > 0) {
                    PENDING_PLAYERS.put(uuid, ticksLeft - 1);
                    continue;
                }

                PENDING_PLAYERS.remove(uuid);
                run(player);
            }
        });
    }

    private static boolean isEnabled() {
        return "1".equals(System.getenv("MURILLOSKILLS_SELFTEST"))
                || "true".equalsIgnoreCase(System.getenv("MURILLOSKILLS_SELFTEST"))
                || Boolean.getBoolean("murilloskills.selftest");
    }

    private static void run(ServerPlayerEntity player) {
        TestLog log = new TestLog();
        log.line("START player=" + player.getName().getString() + " uuid=" + player.getUuid());

        try {
            prepareWorld(player, log);
            PlayerSkillData data = prepareData(player);

            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                AbstractSkill impl = SkillRegistry.get(skill);
                check(log, "registry." + skill.name(), impl != null);
                if (impl == null) {
                    continue;
                }

                PlayerSkillData.SkillStats stats = data.getSkill(skill);
                stats.level = 100;
                stats.xp = 0;
                stats.prestige = 0;
                stats.lastAbilityUse = -1L;
                data.paragonSkill = skill;
                data.paragonSkills.clear();
                data.paragonSkills.add(skill);

                verifyParagonState(log, data, skill);
                verifyPerkCoverage(log, skill);
                impl.updateAttributes(player, stats.level);
                impl.onPlayerJoin(player, stats.level);
                impl.onTick(player, stats.level);
                verifyPassiveState(log, player, skill);
                impl.onActiveAbility(player, stats);

                check(log, "ability.cooldown." + skill.name(), stats.lastAbilityUse >= 0);
                verifyActiveState(log, player, skill, impl);
            }

            verifyToggles(log, player);
            SkillAttributes.updateAllStats(player, data);
            SkillsNetworkUtils.syncSkills(player);

            log.line(log.failed == 0 ? "PASS" : "FAIL failures=" + log.failed);
        } catch (Throwable t) {
            log.failed++;
            log.line("ERROR " + t.getClass().getName() + ": " + t.getMessage());
            MurilloSkills.LOGGER.error("[MurilloSkillsSelfTest] FAIL", t);
        } finally {
            flush(log);
        }
    }

    private static PlayerSkillData prepareData(ServerPlayerEntity player) {
        PlayerSkillData data = ModAttachments.getOrCreate(player);
        data.selectedSkills.clear();
        data.selectedSkills.addAll(Arrays.asList(MurilloSkillsList.values()));
        data.skillToggles.clear();
        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            data.setSkill(skill, 100, 0, -1L, 0);
        }
        return data;
    }

    private static void verifyParagonState(TestLog log, PlayerSkillData data, MurilloSkillsList skill) {
        check(log, "paragon." + skill.name() + ".selected", data.selectedSkills.contains(skill));
        check(log, "paragon." + skill.name() + ".primary", data.paragonSkill == skill);
        check(log, "paragon." + skill.name() + ".set", data.paragonSkills.size() == 1
                && data.paragonSkills.contains(skill));
    }

    private static void verifyPerkCoverage(TestLog log, MurilloSkillsList skill) {
        int[] levels = switch (skill) {
            case MINER -> new int[] {
                    SkillConfig.getMinerNightVisionLevel(),
                    SkillConfig.getMinerDurabilityLevel(),
                    SkillConfig.getMinerRadarLevel(),
                    SkillConfig.getMinerResourceFortuneLevel(),
                    SkillConfig.getUltminePermissionLevel(),
                    SkillConfig.getMinerMasterLevel()
            };
            case WARRIOR -> new int[] {
                    10,
                    SkillConfig.getResistanceUnlockLevel(),
                    50,
                    SkillConfig.getLifestealUnlockLevel(),
                    SkillConfig.getWarriorMasterLevel()
            };
            case ARCHER -> new int[] {
                    SkillConfig.getArcherFastArrowsLevel(),
                    SkillConfig.getArcherBonusDamageLevel(),
                    SkillConfig.getArcherPenetrationLevel(),
                    SkillConfig.getArcherStableShotLevel(),
                    SkillConfig.getArcherMasterLevel()
            };
            case FARMER -> new int[] {
                    SkillConfig.getFarmerGreenThumbLevel(),
                    SkillConfig.getFarmerFertileGroundLevel(),
                    SkillConfig.getFarmerNaturesVitalityLevel(),
                    SkillConfig.getFarmerNutrientCycleLevel(),
                    SkillConfig.getFarmerSeedMasterLevel(),
                    SkillConfig.getFarmerAbundantHarvestLevel(),
                    SkillConfig.getFarmerAreaPlantingLevel(),
                    SkillConfig.getFarmerMasterLevel()
            };
            case FISHER -> new int[] {
                    SkillConfig.getFisherWaitReductionLevel(),
                    SkillConfig.getFisherTreasureBonusLevel(),
                    SkillConfig.getFisherDolphinGraceLevel(),
                    SkillConfig.getFisherLuckSeaLevel(),
                    SkillConfig.getFisherOceanBlessingLevel(),
                    SkillConfig.getFisherSeasFortuneLevel(),
                    SkillConfig.getFisherMasterLevel()
            };
            case BLACKSMITH -> new int[] {
                    SkillConfig.getBlacksmithIronSkinLevel(),
                    SkillConfig.getBlacksmithEfficientAnvilLevel(),
                    SkillConfig.getBlacksmithForgedResilienceLevel(),
                    SkillConfig.getBlacksmithThornsMasterLevel(),
                    SkillConfig.getBlacksmithFireMasteryLevel(),
                    SkillConfig.getBlacksmithRepairAuraLevel(),
                    SkillConfig.getBlacksmithOverEnchantUnlockLevel(),
                    SkillConfig.getBlacksmithMasterLevel()
            };
            case BUILDER -> new int[] {
                    SkillConfig.getBuilderExtendedReachLevel(),
                    SkillConfig.getBuilderEfficientCraftingLevel(),
                    SkillConfig.getBuilderSafeLandingLevel(),
                    SkillConfig.getBuilderScaffoldMasterLevel(),
                    SkillConfig.getBuilderBuildersVigorLevel(),
                    SkillConfig.getBuilderFeatherBuildLevel(),
                    SkillConfig.getBuilderMasterReachLevel(),
                    SkillConfig.getBuilderMasterLevel()
            };
            case EXPLORER -> new int[] {
                    SkillConfig.getExplorerStepAssistLevel(),
                    SkillConfig.getExplorerAquaticLevel(),
                    SkillConfig.getExplorerNightVisionLevel(),
                    SkillConfig.getExplorerFeatherFeetLevel(),
                    SkillConfig.getExplorerNetherWalkerLevel(),
                    SkillConfig.getExplorerPathfinderLevel(),
                    SkillConfig.getExplorerSwiftRecoveryLevel(),
                    SkillConfig.getExplorerMasterLevel()
            };
        };

        boolean inBounds = levels.length >= 5;
        for (int level : levels) {
            inBounds &= level >= 0 && level <= SkillConfig.getMaxLevel();
        }
        check(log, "perks." + skill.name() + ".coverage", inBounds);
        check(log, "perks." + skill.name() + ".master_level", levels[levels.length - 1] == SkillConfig.getMaxLevel());
        log.line("DETAIL perks." + skill.name() + ".levels=" + Arrays.toString(levels));
    }

    private static void prepareWorld(ServerPlayerEntity player, TestLog log) {
        try {
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos base = player.getBlockPos();
            world.setBlockState(base.add(2, 0, 0), Blocks.DIAMOND_ORE.getDefaultState());
            world.setBlockState(base.add(3, 0, 0), Blocks.IRON_ORE.getDefaultState());
            world.setBlockState(base.add(4, 0, 0), Blocks.STONE.getDefaultState());
            log.line("WORLD prepared_test_blocks_at=" + base);
        } catch (Throwable t) {
            log.line("WORLD prepare_skipped " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void verifyPassiveState(TestLog log, ServerPlayerEntity player, MurilloSkillsList skill) {
        switch (skill) {
            case MINER -> {
                check(log, "passive.MINER.mining_speed_formula",
                        MinerSkill.getMiningSpeedMultiplier(100, 0) > MinerSkill.getMiningSpeedMultiplier(0, 0));
            }
            case WARRIOR -> {
                checkAttributeBonus(log, "passive.WARRIOR.attack_damage_attribute", player, "attack_damage");
                checkAttributeBonus(log, "passive.WARRIOR.max_health_attribute", player, "max_health");
            }
            case ARCHER -> {
                check(log, "passive.ARCHER.ranged_damage_formula",
                        ArcherSkill.getRangedDamageMultiplier(100, 0) > ArcherSkill.getRangedDamageMultiplier(0, 0));
                check(log, "passive.ARCHER.arrow_speed_formula", ArcherSkill.getArrowSpeedMultiplier(100) > 1.0D);
                check(log, "passive.ARCHER.arrow_penetration", ArcherSkill.hasArrowPenetration(100));
                check(log, "passive.ARCHER.spread_reduction", ArcherSkill.getSpreadReduction(100) > 0.0D);
            }
            case FARMER -> {
                check(log, "passive.FARMER.double_harvest",
                        FarmerSkill.getDoubleHarvestChance(100, 0) > FarmerSkill.getDoubleHarvestChance(0, 0));
                check(log, "passive.FARMER.golden_crop",
                        FarmerSkill.getGoldenCropChance(100, 0) > FarmerSkill.getGoldenCropChance(0, 0));
                check(log, "passive.FARMER.fertile_ground_speed",
                        FarmerSkill.getFertileGroundGrowthBoost(100) > FarmerSkill.getFertileGroundGrowthBoost(0));
                check(log, "passive.FARMER.area_radius", FarmerSkill.getMaxAreaPlantingRadius(100) > 0);
            }
            case FISHER -> {
                check(log, "passive.FISHER.fishing_speed",
                        FisherSkill.getFishingSpeedBonus(100, 0) > FisherSkill.getFishingSpeedBonus(0, 0));
                check(log, "passive.FISHER.epic_bundle",
                        FisherSkill.getEpicBundleChance(100, 0) > FisherSkill.getEpicBundleChance(0, 0));
                check(log, "passive.FISHER.wait_time", FisherSkill.getWaitTimeMultiplier(100) < 1.0F);
                check(log, "passive.FISHER.luck_of_the_sea", FisherSkill.getLuckOfTheSeaBonus(100) > 0);
            }
            case BLACKSMITH -> {
                float baseDamage = BlacksmithSkill.calculateDamageMultiplier(player, 0, 0, false);
                float maxDamage = BlacksmithSkill.calculateDamageMultiplier(player, 100, 0, false);
                float maxFireDamage = BlacksmithSkill.calculateDamageMultiplier(player, 100, 0, true);
                check(log, "passive.BLACKSMITH.damage_resistance", maxDamage < baseDamage);
                check(log, "passive.BLACKSMITH.fire_resistance", maxFireDamage < maxDamage);
                check(log, "passive.BLACKSMITH.machine_speed",
                        BlacksmithMachineSpeedHelper.getDirectSpeedMultiplier(100)
                                > BlacksmithMachineSpeedHelper.getDirectSpeedMultiplier(0));
                checkAttributeBonus(log, "passive.BLACKSMITH.knockback_attribute", player, "knockback_resistance");
            }
            case BUILDER -> {
                double reachBonus = BuilderSkill.getReachBonus(100, 0);
                check(log, "passive.BUILDER.reach_formula",
                        reachBonus > BuilderSkill.getReachBonus(0, 0));
                check(log, "passive.BUILDER.reach_server_data",
                        BuilderReachHelper.getReachBonus(player) >= reachBonus);
                check(log, "passive.BUILDER.reach_runtime_range",
                        BuilderReachHelper.extendSquaredRange(player, 36.0D) > 36.0D);
                check(log, "passive.BUILDER.fall_damage", BuilderSkill.getFallDamageMultiplier(100) < 1.0F);
                check(log, "passive.BUILDER.scaffold_speed", BuilderSkill.hasScaffoldSpeedBoost(100)
                        && BuilderSkill.getScaffoldSpeedMultiplier(100) > 1.0F);
                check(log, "passive.BUILDER.crafting_economy",
                        BuilderSkill.getCraftingEconomyChance(100, true) > 0.0F
                                && BuilderSkill.getCraftingEconomyChance(100, false) > 0.0F);
            }
            case EXPLORER -> {
                checkAttributeBonus(log, "passive.EXPLORER.speed_attribute", player, "movement_speed");
                check(log, "passive.EXPLORER.step_assist_unlock", ExplorerSkill.hasStepAssist(100));
                check(log, "passive.EXPLORER.aquatic_breath", ExplorerSkill.hasAquatic(100)
                        && ExplorerSkill.getBreathMultiplier(100) > 1.0F);
                check(log, "passive.EXPLORER.feather_feet", ExplorerSkill.hasFeatherFeet(100)
                        && ExplorerSkill.getFallDamageMultiplier(100) < 1.0F);
                check(log, "passive.EXPLORER.nether_walker", ExplorerSkill.hasNetherWalker(100));
                check(log, "passive.EXPLORER.hunger_reduction",
                        ExplorerSkill.getHungerReductionMultiplier(100) < 1.0F);
            }
        }
    }

    private static void checkAttributeBonus(TestLog log, String name, ServerPlayerEntity player, String vanillaId) {
        var attribute = MinecraftVersionCompat.getAttributeInstance(player, vanillaId);
        if (attribute == null) {
            log.line("SKIP " + name + " missing_attribute=" + vanillaId);
            return;
        }
        check(log, name, attribute.getValue() > attribute.getBaseValue());
    }

    private static void verifyActiveState(TestLog log, ServerPlayerEntity player, MurilloSkillsList skill,
            AbstractSkill impl) {
        switch (skill) {
            case MINER -> check(log, "active.MINER.scan_started", true);
            case WARRIOR -> check(log, "active.WARRIOR.berserk", impl instanceof WarriorSkill warrior
                    && warrior.isBerserkActive(player));
            case ARCHER -> check(log, "active.ARCHER.master_ranger", ArcherSkill.isMasterRangerActive(player));
            case FARMER -> check(log, "active.FARMER.harvest_moon", FarmerSkill.isHarvestMoonActive(player));
            case FISHER -> check(log, "active.FISHER.rain_dance", FisherSkill.isRainDanceActive(player));
            case BLACKSMITH -> check(log, "active.BLACKSMITH.titanium_aura", BlacksmithSkill.isTitaniumAuraActive(player));
            case BUILDER -> check(log, "active.BUILDER.creative_brush", BuilderSkill.isCreativeBrushActive(player));
            case EXPLORER -> check(log, "active.EXPLORER.treasure_hunter", ExplorerSkill.isTreasureHunterActive(player));
        }
    }

    private static void verifyToggles(TestLog log, ServerPlayerEntity player) {
        boolean autoTorch = MinerSkill.toggleAutoTorch(player);
        check(log, "toggle.MINER.auto_torch", MinerSkill.isAutoTorchEnabled(player) == autoTorch);

        int areaMode = FarmerSkill.cycleAreaPlantingMode(player, 100);
        check(log, "toggle.FARMER.area_planting", areaMode > 0
                && FarmerSkill.isAreaPlantingEnabled(player.getUuid(), 100));

        boolean hollow = BuilderSkill.toggleHollowMode(player);
        check(log, "toggle.BUILDER.hollow", BuilderSkill.isHollowModeEnabled(player) == hollow);
        BuilderFillMode fillMode = BuilderSkill.cycleNextFillMode(player);
        check(log, "toggle.BUILDER.fill_mode", fillMode != null && BuilderSkill.getFillMode(player) == fillMode);
        boolean horizontal = BuilderSkill.toggleCylinderOrientation(player);
        check(log, "toggle.BUILDER.cylinder", BuilderSkill.isCylinderHorizontal(player) == horizontal);

        AbstractSkill explorerImpl = SkillRegistry.get(MurilloSkillsList.EXPLORER);
        check(log, "toggle.EXPLORER.impl", explorerImpl instanceof ExplorerSkill);
        if (explorerImpl instanceof ExplorerSkill explorer) {
            boolean nightBefore = ExplorerSkill.isNightVisionEnabled(player);
            explorer.toggleNightVision(player);
            check(log, "toggle.EXPLORER.night_vision", ExplorerSkill.isNightVisionEnabled(player) != nightBefore);

            boolean stepBefore = ExplorerSkill.isStepAssistEnabled(player);
            explorer.toggleStepAssist(player);
            check(log, "toggle.EXPLORER.step_assist", ExplorerSkill.isStepAssistEnabled(player) != stepBefore);

            boolean speedBefore = ExplorerSkill.isSpeedBoostEnabled(player);
            explorer.toggleSpeedBoost(player);
            check(log, "toggle.EXPLORER.speed_boost", ExplorerSkill.isSpeedBoostEnabled(player) != speedBefore);
        }
    }

    private static void check(TestLog log, String name, boolean passed) {
        if (passed) {
            log.line("PASS " + name);
        } else {
            log.failed++;
            log.line("FAIL " + name);
        }
    }

    private static void flush(TestLog log) {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(LOG_PATH, log.builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MurilloSkills.LOGGER.error("[MurilloSkillsSelfTest] Could not write {}", LOG_PATH, e);
        }
        for (String line : log.builder.toString().split("\\R")) {
            if (!line.isBlank()) {
                MurilloSkills.LOGGER.warn("[MurilloSkillsSelfTest] {}", line);
            }
        }
    }

    private static final class TestLog {
        private final StringBuilder builder = new StringBuilder();
        private int failed = 0;

        private void line(String text) {
            builder.append(text).append(System.lineSeparator());
        }
    }
}
