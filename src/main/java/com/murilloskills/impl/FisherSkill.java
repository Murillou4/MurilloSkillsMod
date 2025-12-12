package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.network.RainDanceS2CPayload;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fisher skill implementation with all fisher-specific logic.
 * Features:
 * - Passive: +0.5% fishing speed per level
 * - Passive: +0.1% Epic Bundle chance per level (max 10% at level 100)
 * [REBALANCED]
 * - Level 10: -25% wait time between bites
 * - Level 25: +10% treasure chance + 10% more XP
 * - Level 50: Light Dolphin's Grace (increased water speed)
 * - Level 75: Passive Luck of the Sea level 1 (invisible but active)
 * - Level 100: Rain Dance (active) - Visual rain for 60s with fishing buffs
 */
public class FisherSkill extends AbstractSkill {

    // Map to track players with Rain Dance active (UUID → start timestamp)
    private static final Map<UUID, Long> rainDancePlayers = new HashMap<>();

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.FISHER;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // 1. Check Level
            // 1. Verifica Nível (permite se level >= 100 OU se já prestigiou)
            boolean hasReachedMaster = stats.level >= SkillConfig.FISHER_MASTER_LEVEL || stats.prestige > 0;
            if (!hasReachedMaster) {
                player.sendMessage(Text.translatable("murilloskills.error.level_required", 100,
                        Text.translatable("murilloskills.skill.name.fisher")).formatted(Formatting.RED), true);
                return;
            }

            // 2. Check if already active
            if (isRainDanceActive(player)) {
                player.sendMessage(
                        Text.translatable("murilloskills.error.already_active", "Rain Dance").formatted(Formatting.RED),
                        true);
                return;
            }

            // 3. Check Cooldown (skip if never used: lastAbilityUse == -1)
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            long cooldownTicks = SkillConfig.toTicksLong(SkillConfig.FISHER_ABILITY_COOLDOWN_SECONDS);
            if (stats.lastAbilityUse >= 0 && timeSinceUse < cooldownTicks) {
                long secondsLeft = (cooldownTicks - timeSinceUse) / 20;
                long minutesLeft = secondsLeft / 60;
                player.sendMessage(Text.translatable("murilloskills.error.cooldown_minutes", minutesLeft)
                        .formatted(Formatting.RED), true);
                return;
            }

            // 4. Activate Rain Dance
            stats.lastAbilityUse = worldTime;
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            state.markDirty();

            startRainDance(player);

            LOGGER.info("Player {} ativou Rain Dance", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Error executing Fisher active ability for " + player.getName().getString(), e);
            player.sendMessage(Text.translatable("murilloskills.error.ability_error").formatted(Formatting.RED), false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Execute only once per second

            // Level 50: Apply Dolphin's Grace when in water
            if (level >= SkillConfig.FISHER_DOLPHIN_GRACE_LEVEL && player.isTouchingWater()) {
                applyDolphinGrace(player);
            }

            // Check if Rain Dance is active and handle it
            if (rainDancePlayers.containsKey(player.getUuid())) {
                long startTime = rainDancePlayers.get(player.getUuid());
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;

                // Rain Dance still active - apply effects
                if (elapsed < SkillConfig.toTicks(SkillConfig.FISHER_ABILITY_DURATION_SECONDS)) {
                    applyRainDanceEffects(player);
                } else {
                    endRainDance(player);
                }
            }

        } catch (Exception e) {
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Pescador para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        // Fisher bonuses are applied via onTick and mixins
        LOGGER.debug("Updated Fisher attributes for {} - Level: {}",
                player.getName().getString(), level);
    }

    /**
     * Checks if Rain Dance is currently active for the player
     */
    public static boolean isRainDanceActive(ServerPlayerEntity player) {
        if (!rainDancePlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = rainDancePlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.toTicks(SkillConfig.FISHER_ABILITY_DURATION_SECONDS);
    }

    /**
     * Calculates the fishing speed bonus based on level.
     * +0.5% per level = 50% at level 100
     * 
     * @param level    The player's fisher level
     * @param prestige The player's prestige level for passive bonus
     */
    public static float getFishingSpeedBonus(int level, int prestige) {
        // Get prestige passive multiplier (+2% per prestige level)
        float prestigeMultiplier = com.murilloskills.utils.PrestigeManager.getPassiveMultiplier(prestige);

        float bonus = level * SkillConfig.FISHER_SPEED_PER_LEVEL * prestigeMultiplier;
        return Math.min(bonus, 0.60f); // Cap at 60% (slightly higher with prestige)
    }

    /**
     * Calculates the Epic Bundle chance based on level.
     * +0.3% per level = 30% at level 100
     * 
     * @param level    The player's fisher level
     * @param prestige The player's prestige level for passive bonus
     */
    public static float getEpicBundleChance(int level, int prestige) {
        // Get prestige passive multiplier (+2% per prestige level)
        float prestigeMultiplier = com.murilloskills.utils.PrestigeManager.getPassiveMultiplier(prestige);

        return level * SkillConfig.FISHER_EPIC_BUNDLE_PER_LEVEL * prestigeMultiplier;
    }

    /**
     * Gets the wait time reduction factor for level 10+ fishers.
     * Returns 0.75 (25% reduction) if level >= 10, otherwise 1.0 (no reduction)
     */
    public static float getWaitTimeMultiplier(int level) {
        if (level >= SkillConfig.FISHER_WAIT_REDUCTION_LEVEL) {
            return 1.0f - SkillConfig.FISHER_WAIT_REDUCTION;
        }
        return 1.0f;
    }

    /**
     * Gets the Luck of the Sea bonus level for level 75+ fishers.
     * Returns 1 if level >= 75, otherwise 0
     */
    public static int getLuckOfTheSeaBonus(int level) {
        if (level >= SkillConfig.FISHER_LUCK_SEA_LEVEL) {
            return 1;
        }
        return 0;
    }

    /**
     * Applies Dolphin's Grace effect (light version - short duration, reapplied
     * each tick)
     */
    private void applyDolphinGrace(ServerPlayerEntity player) {
        // Apply for 2 seconds (reapplied every second via tick)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.DOLPHINS_GRACE,
                40, // 2 seconds
                0, // Level 1 (amplifier 0)
                true, // Ambient - less particles
                false, // No particles
                false // No icon
        ));
    }

    /**
     * Starts Rain Dance ability
     */
    private void startRainDance(ServerPlayerEntity player) {
        rainDancePlayers.put(player.getUuid(), player.getEntityWorld().getTime());

        // Send visual rain effect to client
        int durationTicks = SkillConfig.toTicks(SkillConfig.FISHER_ABILITY_DURATION_SECONDS);
        ServerPlayNetworking.send(player, new RainDanceS2CPayload(true, durationTicks));

        // Sound effect
        player.playSound(SoundEvents.WEATHER_RAIN, 1.0f, 1.0f);
        player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);

        // Message
        player.sendMessage(
                Text.translatable("murilloskills.fisher.rain_dance_activated").formatted(Formatting.AQUA,
                        Formatting.BOLD),
                false);
        player.sendMessage(
                Text.translatable("murilloskills.fisher.rain_dance_description")
                        .formatted(Formatting.DARK_AQUA),
                true);
    }

    /**
     * Applies Rain Dance ongoing effects
     */
    private void applyRainDanceEffects(ServerPlayerEntity player) {
        // Apply Conduit Power when in water
        if (player.isTouchingWater()) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.CONDUIT_POWER,
                    40, // 2 seconds (reapplied every second)
                    0, // Level 1
                    true, // Ambient
                    true, // Show particles
                    true // Show icon
            ));
        }

        // Additional effects could be added here (e.g., visual rain particles via
        // network packet)
    }

    /**
     * Ends Rain Dance ability
     */
    private void endRainDance(ServerPlayerEntity player) {
        rainDancePlayers.remove(player.getUuid());

        // Send stop rain effect to client
        ServerPlayNetworking.send(player, new RainDanceS2CPayload(false, 0));

        player.playSound(SoundEvents.WEATHER_RAIN_ABOVE, 0.5f, 0.5f);
        player.sendMessage(Text.translatable("murilloskills.fisher.rain_dance_ended").formatted(Formatting.GRAY), true);

        LOGGER.debug("Player {} saiu do Rain Dance", player.getName().getString());
    }
}
