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
 * Warrior skill implementation with all warrior-specific logic.
 * Features:
 * - Passive: Attack damage bonus per level
 * - Level 10, 50, 100: Health bonuses (milestone rewards)
 * - Level 25: Damage resistance
 * - Level 75: Lifesteal on attacks
 * - Level 100: Berserk mode (active ability)
 */
public class WarriorSkill extends AbstractSkill {

    private static final Identifier WARRIOR_DAMAGE_ID = Identifier.of("murilloskills", "warrior_damage");
    private static final Identifier WARRIOR_HEALTH_ID = Identifier.of("murilloskills", "warrior_health");
    private static final Identifier BERSERK_KNOCKBACK_RESISTANCE_ID = Identifier.of("murilloskills",
            "berserk_knockback_resistance");

    // Mapa para rastrear jogadores em modo Berserk (UUID → timestamp de início)
    private static final Map<UUID, Long> berserkPlayers = new HashMap<>();

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.WARRIOR;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // 1. Verifica Nível
            if (stats.level < SkillConfig.WARRIOR_MASTER_LEVEL) {
                sendMessage(player, "Você precisa ser Nível 100 de Guerreiro!", Formatting.RED, true);
                return;
            }

            // 2. Verifica se já está em Berserk
            if (isBerserkActive(player)) {
                sendMessage(player, "Você já está em modo Berserk!", Formatting.RED, true);
                return;
            }

            // 3. Verifica Cooldown (pula se nunca usou: lastAbilityUse == -1)
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            long cooldownTicks = SkillConfig.toTicksLong(SkillConfig.WARRIOR_ABILITY_COOLDOWN_SECONDS);
            if (stats.lastAbilityUse >= 0 && timeSinceUse < cooldownTicks) {
                long minutesLeft = (cooldownTicks - timeSinceUse) / 20 / 60;
                sendMessage(player, "Habilidade em recarga: " + minutesLeft + " minutos.", Formatting.RED, true);
                return;
            }

            // 4. Ativa o Berserk
            stats.lastAbilityUse = worldTime;
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            state.markDirty();

            startBerserk(player);

            LOGGER.info("Player {} ativou modo Berserk", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Erro ao executar habilidade ativa do Guerreiro para " + player.getName().getString(), e);
            sendMessage(player, "Erro ao ativar habilidade. Contate o admin.", Formatting.RED, false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Executa apenas 1 vez por segundo

            // Verifica se o jogador estava em Berserk e o tempo acabou
            if (berserkPlayers.containsKey(player.getUuid())) {
                long startTime = berserkPlayers.get(player.getUuid());
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;

                // Se o Berserk acabou
                if (elapsed >= SkillConfig.toTicks(SkillConfig.WARRIOR_BERSERK_DURATION_SECONDS)) {
                    endBerserk(player);
                }
            }

        } catch (Exception e) {
            // Log with limited frequency to avoid spamming console on tick errors
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Guerreiro para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // --- 1. DANO (por level) ---
            double damageBonus = level * SkillConfig.WARRIOR_DAMAGE_PER_LEVEL;

            var damageAttr = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.removeModifier(WARRIOR_DAMAGE_ID);
                if (damageBonus > 0) {
                    damageAttr.addTemporaryModifier(new EntityAttributeModifier(
                            WARRIOR_DAMAGE_ID, damageBonus, EntityAttributeModifier.Operation.ADD_VALUE));
                }
            }

            // --- 2. VIDA EXTRA (Milestones) ---
            double healthBonus = 0;

            // Nível 10: +1 Coração (+2 HP)
            if (level >= 10)
                healthBonus += 2.0;
            // Nível 50: +1 Coração (Total +4 HP)
            if (level >= 50)
                healthBonus += 2.0;
            // Nível 100: +3 Corações (Total +10 HP = 5 Corações)
            if (level >= 100)
                healthBonus += 6.0;

            var healthAttr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.removeModifier(WARRIOR_HEALTH_ID);
                if (healthBonus > 0) {
                    healthAttr.addTemporaryModifier(new EntityAttributeModifier(
                            WARRIOR_HEALTH_ID, healthBonus, EntityAttributeModifier.Operation.ADD_VALUE));

                    // Cura o player para preencher a vida nova se ele estiver full
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
            }

            LOGGER.debug("Updated warrior attributes for {} - Damage: {}, Health: {}",
                    player.getName().getString(), damageBonus, healthBonus);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos do Guerreiro para " + player.getName().getString(), e);
        }
    }

    /**
     * Verifica se o jogador está atualmente em modo Berserk
     */
    public boolean isBerserkActive(ServerPlayerEntity player) {
        if (!berserkPlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = berserkPlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.toTicks(SkillConfig.WARRIOR_BERSERK_DURATION_SECONDS);
    }

    /**
     * Inicia o modo Berserk
     */
    private void startBerserk(ServerPlayerEntity player) {
        // Registra o início do Berserk
        berserkPlayers.put(player.getUuid(), player.getEntityWorld().getTime());

        // Aplica Força (Strength)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.STRENGTH,
                SkillConfig.toTicks(SkillConfig.WARRIOR_BERSERK_DURATION_SECONDS),
                SkillConfig.WARRIOR_BERSERK_STRENGTH_AMPLIFIER,
                false, true, true));

        // Aplica Resistência
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                SkillConfig.toTicks(SkillConfig.WARRIOR_BERSERK_DURATION_SECONDS),
                SkillConfig.WARRIOR_BERSERK_RESISTANCE_AMPLIFIER,
                false, true, true));

        // Aplica Knockback Resistance (imunidade total)
        var knockbackAttr = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(BERSERK_KNOCKBACK_RESISTANCE_ID);
            knockbackAttr.addTemporaryModifier(new EntityAttributeModifier(
                    BERSERK_KNOCKBACK_RESISTANCE_ID,
                    1.0, // 100% de resistência a knockback
                    EntityAttributeModifier.Operation.ADD_VALUE));
        }

        // Som de ativação (rugido)
        player.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);

        // Mensagem
        player.sendMessage(Text.literal("⚔ MODO BERSERK ATIVADO! ⚔").formatted(Formatting.DARK_RED, Formatting.BOLD),
                false);
        player.sendMessage(
                Text.literal("Força aumentada, resistência máxima, lifesteal absoluto!").formatted(Formatting.RED),
                true);
    }

    /**
     * Finaliza o modo Berserk e aplica exaustão
     */
    private void endBerserk(ServerPlayerEntity player) {
        // Remove do mapa
        berserkPlayers.remove(player.getUuid());

        // Remove Knockback Resistance
        var knockbackAttr = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(BERSERK_KNOCKBACK_RESISTANCE_ID);
        }

        // Aplica Exaustão (Slowness e Weakness)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                SkillConfig.toTicks(SkillConfig.WARRIOR_EXHAUSTION_DURATION_SECONDS),
                1, // Slowness II
                false, true, true));

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WEAKNESS,
                SkillConfig.toTicks(SkillConfig.WARRIOR_EXHAUSTION_DURATION_SECONDS),
                1, // Weakness II
                false, true, true));

        // Som de fim
        player.playSound(SoundEvents.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);

        // Mensagem
        sendMessage(player, "A fúria passou... Você está exausto.", Formatting.GRAY, true);

        LOGGER.debug("Player {} saiu do modo Berserk", player.getName().getString());
    }
}