package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.TreasureHunterS2CPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Explorer skill implementation with discovery-based XP and terrain mastery.
 * Features:
 * - Passive: +0.2% movement speed per level (max 20%)
 * - Passive: +1 Luck every 20 levels
 * - Passive: Reduced hunger from walking
 * - Level 10: Step Assist (auto-step up blocks)
 * - Level 20: Aquatic (50% more breath + normal underwater mining)
 * - Level 35: Night Vision (permanent, toggleable)
 * - Level 65: Feather Feet (40% fall damage reduction)
 * - Level 80: Nether Walker (magma immunity + soul sand normal speed)
 * - Level 100: Treasure Hunter (glowing chests/spawners in 32 block radius)
 */
public class ExplorerSkill extends AbstractSkill {

    private static final Identifier EXPLORER_SPEED_ID = Identifier.of("murilloskills", "explorer_speed_bonus");
    private static final Identifier EXPLORER_LUCK_ID = Identifier.of("murilloskills", "explorer_luck_bonus");
    private static final Identifier EXPLORER_STEP_HEIGHT_ID = Identifier.of("murilloskills", "explorer_step_height");

    // Map to track players with Night Vision enabled (UUID → enabled)
    private static final Map<UUID, Boolean> nightVisionEnabled = new HashMap<>();

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.EXPLORER;
    }

    // Map to track Treasure Hunter ability state (UUID → end time in world ticks)
    private static final Map<UUID, Long> treasureHunterActive = new HashMap<>();
    private static final Map<UUID, Long> lastAbilityUse = new HashMap<>();

    // Duration and cooldown for Treasure Hunter ability
    private static final int TREASURE_HUNTER_DURATION_SECONDS = 60;
    private static final int TREASURE_HUNTER_COOLDOWN_SECONDS = 300; // 5 minutes

    @Override
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        // Level 100: Treasure Hunter ability
        if (stats.level >= SkillConfig.EXPLORER_MASTER_LEVEL) {
            activateTreasureHunter(player, stats);
        }
        // Level 35-99: Toggle Night Vision
        else if (stats.level >= SkillConfig.EXPLORER_NIGHT_VISION_LEVEL) {
            toggleNightVision(player);
        } else {
            sendMessage(player, "Você precisa ser Nível 35 de Explorador para usar habilidades!", Formatting.RED,
                    true);
        }
    }

    /**
     * Activates the Treasure Hunter ability with cooldown
     */
    private void activateTreasureHunter(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        UUID uuid = player.getUuid();
        long worldTime = player.getEntityWorld().getTime();
        long cooldownTicks = SkillConfig.toTicksLong(TREASURE_HUNTER_COOLDOWN_SECONDS);

        // Check cooldown
        Long lastUse = lastAbilityUse.get(uuid);
        if (lastUse != null && lastUse != Long.MIN_VALUE) {
            long timeSinceUse = worldTime - lastUse;
            if (timeSinceUse < cooldownTicks) {
                long remainingSeconds = (cooldownTicks - timeSinceUse) / 20;
                long minutes = remainingSeconds / 60;
                long seconds = remainingSeconds % 60;
                sendMessage(player, String.format("⏳ Sexto Sentido em recarga: %d:%02d", minutes, seconds),
                        Formatting.YELLOW, true);
                return;
            }
        }

        // Activate ability
        int durationTicks = SkillConfig.toTicks(TREASURE_HUNTER_DURATION_SECONDS);
        treasureHunterActive.put(uuid, worldTime + durationTicks);
        lastAbilityUse.put(uuid, worldTime);

        sendMessage(player, "🔍 Sexto Sentido ATIVADO! Baús e Spawners revelados por "
                + TREASURE_HUNTER_DURATION_SECONDS + " segundos!", Formatting.AQUA, false);

        // Immediately trigger a scan
        handleTreasureHunter(player);
    }

    /**
     * Checks if Treasure Hunter ability is currently active for player
     */
    public static boolean isTreasureHunterActive(ServerPlayerEntity player) {
        Long endTime = treasureHunterActive.get(player.getUuid());
        if (endTime == null)
            return false;
        return player.getEntityWorld().getTime() < endTime;
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Execute only once per second

            // --- LEVEL 20: AQUATIC (Water Breathing when underwater) ---
            if (meetsLevelRequirement(level, SkillConfig.EXPLORER_AQUATIC_LEVEL)) {
                if (player.isSubmergedInWater()) {
                    applyWaterBreathing(player);
                }
            }

            // --- LEVEL 35: NIGHT VISION ---
            if (meetsLevelRequirement(level, SkillConfig.EXPLORER_NIGHT_VISION_LEVEL)) {
                if (isNightVisionEnabled(player)) {
                    applyNightVision(player);
                }
            }

            // --- LEVEL 100: TREASURE HUNTER (only when ability is active) ---
            if (meetsLevelRequirement(level, SkillConfig.EXPLORER_MASTER_LEVEL)) {
                if (isTreasureHunterActive(player)) {
                    // Scan every 2 seconds while ability is active
                    if (player.age % 40 == 0) {
                        handleTreasureHunter(player);
                    }
                }
            }

        } catch (Exception e) {
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Explorador para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // --- MOVEMENT SPEED ---
            double speedBonus = level * SkillConfig.EXPLORER_SPEED_PER_LEVEL;
            var speedAttribute = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

            if (speedAttribute != null) {
                speedAttribute.removeModifier(EXPLORER_SPEED_ID);
                if (speedBonus > 0) {
                    speedAttribute.addTemporaryModifier(new EntityAttributeModifier(
                            EXPLORER_SPEED_ID,
                            speedBonus,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }

            // --- LUCK (every 20 levels) ---
            int luckBonus = level / SkillConfig.EXPLORER_LUCK_INTERVAL;
            var luckAttribute = player.getAttributeInstance(EntityAttributes.LUCK);

            if (luckAttribute != null) {
                luckAttribute.removeModifier(EXPLORER_LUCK_ID);
                if (luckBonus > 0) {
                    luckAttribute.addTemporaryModifier(new EntityAttributeModifier(
                            EXPLORER_LUCK_ID,
                            luckBonus,
                            EntityAttributeModifier.Operation.ADD_VALUE));
                }
            }

            // --- STEP HEIGHT (Level 10+) ---
            var stepHeightAttribute = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (stepHeightAttribute != null) {
                stepHeightAttribute.removeModifier(EXPLORER_STEP_HEIGHT_ID);
                if (level >= SkillConfig.EXPLORER_STEP_ASSIST_LEVEL) {
                    stepHeightAttribute.addTemporaryModifier(new EntityAttributeModifier(
                            EXPLORER_STEP_HEIGHT_ID,
                            SkillConfig.EXPLORER_STEP_HEIGHT,
                            EntityAttributeModifier.Operation.ADD_VALUE));
                }
            }

            LOGGER.debug("Updated Explorer attributes for {} - Speed: {}%, Luck: +{}, StepAssist: {}",
                    player.getName().getString(),
                    String.format("%.1f", speedBonus * 100),
                    luckBonus,
                    level >= SkillConfig.EXPLORER_STEP_ASSIST_LEVEL);

        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos do Explorador para " + player.getName().getString(), e);
        }
    }

    /**
     * Toggles night vision for the player
     */
    public void toggleNightVision(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean currentlyEnabled = nightVisionEnabled.getOrDefault(uuid, true); // Default: enabled
        boolean newState = !currentlyEnabled;
        nightVisionEnabled.put(uuid, newState);

        if (newState) {
            sendMessage(player, "👁️ Visão Noturna: ATIVADA", Formatting.GREEN, true);
            applyNightVision(player);
        } else {
            sendMessage(player, "👁️ Visão Noturna: DESATIVADA", Formatting.GRAY, true);
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    /**
     * Checks if night vision is enabled for player
     */
    public static boolean isNightVisionEnabled(ServerPlayerEntity player) {
        return nightVisionEnabled.getOrDefault(player.getUuid(), true); // Default: enabled
    }

    /**
     * Applies night vision effect
     */
    private void applyNightVision(ServerPlayerEntity player) {
        // Apply for 15 seconds (reapplied every second via tick)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                300, // 15 seconds
                0, // Level 1
                true, // Ambient - less particles
                false, // No particles
                true // Show icon
        ));
    }

    /**
     * Applies water breathing effect for Aquatic perk
     */
    private void applyWaterBreathing(ServerPlayerEntity player) {
        // Apply Water Breathing for 3 seconds (reapplied every second via tick)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WATER_BREATHING,
                60, // 3 seconds
                0, // Level 1
                true, // Ambient - less particles
                false, // No particles
                true // Show icon
        ));
    }

    /**
     * Handles the Treasure Hunter passive - finds and highlights chests/spawners
     */
    private void handleTreasureHunter(ServerPlayerEntity player) {
        List<BlockPos> treasurePositions = new ArrayList<>();
        BlockPos center = player.getBlockPos();
        int radius = SkillConfig.EXPLORER_TREASURE_RADIUS;

        // Get server world
        net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld) player.getEntityWorld();

        // Scan for chests and spawners
        for (BlockPos pos : BlockPos.iterate(
                center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius))) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof MobSpawnerBlockEntity) {
                treasurePositions.add(pos.toImmutable());
            }
        }

        // Send to client for rendering
        if (!treasurePositions.isEmpty()) {
            ServerPlayNetworking.send(player, new TreasureHunterS2CPayload(treasurePositions));
        }
    }

    // =====================================================
    // STATIC HELPER METHODS FOR MIXINS
    // =====================================================

    /**
     * Check if player has Step Assist perk (Level 10+)
     */
    public static boolean hasStepAssist(int level) {
        return level >= SkillConfig.EXPLORER_STEP_ASSIST_LEVEL;
    }

    /**
     * Check if player has Aquatic perk (Level 20+)
     */
    public static boolean hasAquatic(int level) {
        return level >= SkillConfig.EXPLORER_AQUATIC_LEVEL;
    }

    /**
     * Get breath multiplier for Aquatic perk
     */
    public static float getBreathMultiplier(int level) {
        if (level >= SkillConfig.EXPLORER_AQUATIC_LEVEL) {
            return SkillConfig.EXPLORER_BREATH_MULTIPLIER;
        }
        return 1.0f;
    }

    /**
     * Check if player has Feather Feet perk (Level 65+)
     */
    public static boolean hasFeatherFeet(int level) {
        return level >= SkillConfig.EXPLORER_FEATHER_FEET_LEVEL;
    }

    /**
     * Get fall damage multiplier for Feather Feet perk
     */
    public static float getFallDamageMultiplier(int level) {
        if (level >= SkillConfig.EXPLORER_FEATHER_FEET_LEVEL) {
            return 1.0f - SkillConfig.EXPLORER_FALL_DAMAGE_REDUCTION;
        }
        return 1.0f;
    }

    /**
     * Check if player has Nether Walker perk (Level 80+)
     */
    public static boolean hasNetherWalker(int level) {
        return level >= SkillConfig.EXPLORER_NETHER_WALKER_LEVEL;
    }

    /**
     * Check if player has Treasure Hunter passive (Level 100)
     */
    public static boolean hasTreasureHunter(int level) {
        return level >= SkillConfig.EXPLORER_MASTER_LEVEL;
    }

    /**
     * Get hunger reduction multiplier based on level
     */
    public static float getHungerReductionMultiplier(int level) {
        float reduction = level * SkillConfig.EXPLORER_HUNGER_REDUCTION_PER_LEVEL;
        return Math.max(0.5f, 1.0f - reduction); // Cap at 50% reduction
    }
}
