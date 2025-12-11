package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Blacksmith skill implementation with focus on forging, resistance, and armor.
 * Features:
 * - Passive: 2% physical damage resistance per level (max 200% at level 100)
 * - Level 10: Iron Skin (+5% physical resistance)
 * - Level 25: Efficient Anvil (25% XP discount, 10% material save)
 * - Level 50: Forged Resilience (+10% fire/explosion resistance, +1 Protection)
 * - Level 75: Thorns Master (20% reflect 25% damage, 50% knockback reduction)
 * - Level 100: Titanium Aura (active ability)
 */
public class BlacksmithSkill extends AbstractSkill {

    private static final Identifier BLACKSMITH_KNOCKBACK_RESISTANCE_ID = Identifier.of("murilloskills",
            "blacksmith_knockback_resistance");
    private static final Identifier TITANIUM_KNOCKBACK_RESISTANCE_ID = Identifier.of("murilloskills",
            "titanium_knockback_resistance");

    // Map to track players with Titanium Aura active (UUID → start timestamp)
    private static final Map<UUID, Long> titaniumAuraPlayers = new HashMap<>();

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.BLACKSMITH;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // 1. Check Level
            if (stats.level < SkillConfig.BLACKSMITH_MASTER_LEVEL) {
                player.sendMessage(
                        Text.translatable("murilloskills.error.level_required", 100,
                                Text.translatable("murilloskills.skill.name.blacksmith")).formatted(Formatting.RED),
                        true);
                return;
            }

            // 2. Check if already active
            if (isTitaniumAuraActive(player)) {
                player.sendMessage(Text.translatable("murilloskills.error.already_active", Text.translatable("murilloskills.perk.name.blacksmith.master"))
                        .formatted(Formatting.RED), true);
                return;
            }

            // 3. Check Cooldown
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            long cooldownTicks = SkillConfig.toTicksLong(SkillConfig.BLACKSMITH_ABILITY_COOLDOWN_SECONDS);
            if (stats.lastAbilityUse >= 0 && timeSinceUse < cooldownTicks) {
                long minutesLeft = (cooldownTicks - timeSinceUse) / 20 / 60;
                player.sendMessage(Text.translatable("murilloskills.error.cooldown_minutes", minutesLeft)
                        .formatted(Formatting.RED), true);
                return;
            }

            // 4. Activate Titanium Aura
            stats.lastAbilityUse = worldTime;
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            state.markDirty();

            startTitaniumAura(player);

            LOGGER.info("Player {} ativou Titanium Aura", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Error executing Blacksmith active ability for " + player.getName().getString(), e);
            player.sendMessage(Text.translatable("murilloskills.error.ability_error").formatted(Formatting.RED), false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Execute only once per second

            // Check if player has Titanium Aura active
            if (titaniumAuraPlayers.containsKey(player.getUuid())) {
                long startTime = titaniumAuraPlayers.get(player.getUuid());
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;

                // If Titanium Aura ended
                if (elapsed >= SkillConfig.toTicks(SkillConfig.BLACKSMITH_ABILITY_DURATION_SECONDS)) {
                    endTitaniumAura(player);
                } else {
                    // Apply regeneration (0.5 heart/sec = 1 HP/sec)
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.heal(SkillConfig.BLACKSMITH_TITANIUM_REGEN);
                    }
                }
            }

        } catch (Exception e) {
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Ferreiro para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // Apply knockback resistance at level 75+
            double knockbackResistance = 0;

            if (level >= SkillConfig.BLACKSMITH_THORNS_MASTER_LEVEL) {
                knockbackResistance = SkillConfig.BLACKSMITH_KNOCKBACK_REDUCTION;
            }

            var knockbackAttr = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                knockbackAttr.removeModifier(BLACKSMITH_KNOCKBACK_RESISTANCE_ID);
                if (knockbackResistance > 0) {
                    knockbackAttr.addTemporaryModifier(new EntityAttributeModifier(
                            BLACKSMITH_KNOCKBACK_RESISTANCE_ID, knockbackResistance,
                            EntityAttributeModifier.Operation.ADD_VALUE));
                }
            }

            LOGGER.debug("Updated blacksmith attributes for {} - Knockback Resistance: {}",
                    player.getName().getString(), knockbackResistance);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos do Ferreiro para " + player.getName().getString(), e);
        }
    }

    /**
     * Check if Titanium Aura is currently active for a player
     */
    public static boolean isTitaniumAuraActive(ServerPlayerEntity player) {
        if (!titaniumAuraPlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = titaniumAuraPlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.toTicks(SkillConfig.BLACKSMITH_ABILITY_DURATION_SECONDS);
    }

    /**
     * Start Titanium Aura ability
     */
    private void startTitaniumAura(ServerPlayerEntity player) {
        // Register aura start
        titaniumAuraPlayers.put(player.getUuid(), player.getEntityWorld().getTime());

        // Apply Resistance effect (damage reduction)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                SkillConfig.toTicks(SkillConfig.BLACKSMITH_ABILITY_DURATION_SECONDS),
                1, // Resistance II (~40% reduction, combined with passive for ~70%)
                false, true, true));

        // Apply full knockback immunity
        var knockbackAttr = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(TITANIUM_KNOCKBACK_RESISTANCE_ID);
            knockbackAttr.addTemporaryModifier(new EntityAttributeModifier(
                    TITANIUM_KNOCKBACK_RESISTANCE_ID,
                    1.0, // 100% knockback resistance
                    EntityAttributeModifier.Operation.ADD_VALUE));
        }

        // Activation sound (anvil)
        player.playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0f, 0.8f);

        // Messages
        player.sendMessage(
                Text.translatable("murilloskills.blacksmith.titanium_activated").formatted(Formatting.AQUA,
                        Formatting.BOLD),
                false);
        player.sendMessage(
                Text.translatable("murilloskills.blacksmith.titanium_description")
                        .formatted(Formatting.DARK_AQUA),
                true);
    }

    /**
     * End Titanium Aura and cleanup
     */
    private void endTitaniumAura(ServerPlayerEntity player) {
        // Remove from map
        titaniumAuraPlayers.remove(player.getUuid());

        // Remove full knockback resistance (keep level 75 base if applicable)
        var knockbackAttr = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(TITANIUM_KNOCKBACK_RESISTANCE_ID);
        }

        // End sound
        player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.5f, 1.2f);

        // Message
        player.sendMessage(Text.translatable("murilloskills.blacksmith.titanium_ended").formatted(Formatting.GRAY),
                true);

        LOGGER.debug("Player {} saiu do modo Titanium Aura", player.getName().getString());
    }

    /**
     * Calculate the total damage resistance multiplier for a blacksmith.
     * This is used by the damage mixin.
     * 
     * @param player            The player
     * @param level             Current blacksmith level
     * @param isFireOrExplosion Whether the damage is from fire or explosion
     * @return Damage multiplier (e.g., 0.7 means 30% reduction)
     */
    public static float calculateDamageMultiplier(ServerPlayerEntity player, int level, boolean isFireOrExplosion) {
        float multiplier = 1.0f;

        // Base resistance: 0.5% per level (max 50% at level 100)
        // Note: skill.md says 2% but that would give 200% which is impossible
        // Using 0.5% for balanced progression
        float baseResistance = Math.min(level * 0.005f, 0.50f);
        multiplier -= baseResistance;

        // Iron Skin (level 10+): +5%
        if (level >= SkillConfig.BLACKSMITH_IRON_SKIN_LEVEL) {
            multiplier -= SkillConfig.BLACKSMITH_IRON_SKIN_BONUS;
        }

        // Forged Resilience (level 50+): +10% for fire/explosion
        if (level >= SkillConfig.BLACKSMITH_FORGED_RESILIENCE_LEVEL && isFireOrExplosion) {
            multiplier -= SkillConfig.BLACKSMITH_FIRE_EXPLOSION_RESIST;
        }

        // Titanium Aura: +30% all damage resistance
        if (isTitaniumAuraActive(player)) {
            multiplier -= SkillConfig.BLACKSMITH_TITANIUM_RESISTANCE;
        }

        // Ensure minimum damage (at least 5% damage goes through)
        return Math.max(multiplier, 0.05f);
    }

    /**
     * Check if thorns should reflect damage.
     * 
     * @param level Current blacksmith level
     * @return true if thorns should trigger (20% chance at level 75+)
     */
    public static boolean shouldReflectDamage(int level) {
        if (level < SkillConfig.BLACKSMITH_THORNS_MASTER_LEVEL) {
            return false;
        }
        return Math.random() < SkillConfig.BLACKSMITH_THORNS_CHANCE;
    }

    /**
     * Get the amount of damage to reflect (25% of original damage).
     * 
     * @param originalDamage The damage received
     * @return Damage to reflect back to attacker
     */
    public static float getReflectedDamage(float originalDamage) {
        return originalDamage * SkillConfig.BLACKSMITH_THORNS_REFLECT;
    }
}
