package com.murilloskills.skills.warrior;

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

public class WarriorAbilityHandler {

    private static final Identifier BERSERK_KNOCKBACK_RESISTANCE_ID = Identifier.of("murilloskills", "berserk_knockback_resistance");

    // Mapa para rastrear jogadores em modo Berserk (UUID -> timestamp de início)
    private static final Map<UUID, Long> berserkPlayers = new HashMap<>();

    /**
     * Verifica se o jogador está atualmente em modo Berserk
     */
    public static boolean isBerserkActive(ServerPlayerEntity player) {
        if (!berserkPlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = berserkPlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.WARRIOR_BERSERK_DURATION;
    }

    /**
     * Chamado a cada Tick do Jogador (Event Listener)
     */
    public static void onPlayerTick(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return; // Executa apenas 1 vez por segundo

        // Verifica se o jogador estava em Berserk e o tempo acabou
        if (berserkPlayers.containsKey(player.getUuid())) {
            long startTime = berserkPlayers.get(player.getUuid());
            long currentTime = player.getEntityWorld().getTime();
            long elapsed = currentTime - startTime;

            // Se o Berserk acabou
            if (elapsed >= SkillConfig.WARRIOR_BERSERK_DURATION) {
                endBerserk(player);
            }
        }
    }

    /**
     * Chamado quando aperta a tecla da Habilidade (Level 100)
     */
    public static void triggerActiveAbility(ServerPlayerEntity player) {
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        var stats = state.getPlayerData(player).getSkill(MurilloSkillsList.WARRIOR);

        // 1. Verifica Nível
        if (stats.level < SkillConfig.WARRIOR_MASTER_LEVEL) {
            player.sendMessage(Text.of("§cVocê precisa ser Nível 100 de Guerreiro!"), true);
            return;
        }

        // 2. Verifica se já está em Berserk
        if (isBerserkActive(player)) {
            player.sendMessage(Text.of("§cVocê já está em modo Berserk!"), true);
            return;
        }

        // 3. Verifica Cooldown
        long worldTime = player.getEntityWorld().getTime();
        long timeSinceUse = worldTime - stats.lastAbilityUse;

        if (timeSinceUse < SkillConfig.WARRIOR_ABILITY_COOLDOWN) {
            long minutesLeft = (SkillConfig.WARRIOR_ABILITY_COOLDOWN - timeSinceUse) / 20 / 60;
            player.sendMessage(Text.of("§cHabilidade em recarga: " + minutesLeft + " minutos."), true);
            return;
        }

        // 4. Ativa o Berserk
        stats.lastAbilityUse = worldTime;
        state.markDirty();

        startBerserk(player);
    }

    /**
     * Inicia o modo Berserk
     */
    private static void startBerserk(ServerPlayerEntity player) {
        // Registra o início do Berserk
        berserkPlayers.put(player.getUuid(), player.getEntityWorld().getTime());

        // Aplica Força (Strength)
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.STRENGTH,
                SkillConfig.WARRIOR_BERSERK_DURATION,
                SkillConfig.WARRIOR_BERSERK_STRENGTH_AMPLIFIER,
                false, true, true
        ));

        // Aplica Resistência
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                SkillConfig.WARRIOR_BERSERK_DURATION,
                SkillConfig.WARRIOR_BERSERK_RESISTANCE_AMPLIFIER,
                false, true, true
        ));

        // Aplica Knockback Resistance (imunidade total)
        var knockbackAttr = player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(BERSERK_KNOCKBACK_RESISTANCE_ID);
            knockbackAttr.addTemporaryModifier(new EntityAttributeModifier(
                    BERSERK_KNOCKBACK_RESISTANCE_ID,
                    1.0, // 100% de resistência a knockback
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        // Som de ativação (rugido)
        player.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);

        // Mensagem
        player.sendMessage(Text.literal("⚔ MODO BERSERK ATIVADO! ⚔").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Força aumentada, resistência máxima, lifesteal absoluto!").formatted(Formatting.RED), true);
    }

    /**
     * Finaliza o modo Berserk e aplica exaustão
     */
    private static void endBerserk(ServerPlayerEntity player) {
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
                SkillConfig.WARRIOR_EXHAUSTION_DURATION,
                1, // Slowness II
                false, true, true
        ));

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WEAKNESS,
                SkillConfig.WARRIOR_EXHAUSTION_DURATION,
                1, // Weakness II
                false, true, true
        ));

        // Som de fim
        player.playSound(SoundEvents.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);

        // Mensagem
        player.sendMessage(Text.literal("A fúria passou... Você está exausto.").formatted(Formatting.GRAY), true);
    }
}

