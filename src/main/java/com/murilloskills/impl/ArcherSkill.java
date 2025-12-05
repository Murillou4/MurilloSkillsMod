package com.murilloskills.impl;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Archer skill implementation with all archer-specific logic.
 * Features:
 * - Passive: +2% ranged damage per level
 * - Level 10: Arrows fly faster
 * - Level 25: +5% ranged damage bonus
 * - Level 50: Arrow penetration (piercing)
 * - Level 75: More stable shots (less spread)
 * - Level 100: Master Ranger - arrows pierce multiple targets and home on last
 * damaged enemy
 */
public class ArcherSkill extends AbstractSkill {

    // Mapa para rastrear jogadores com Master Ranger ativo (UUID → timestamp de
    // início)
    private static final Map<UUID, Long> masterRangerPlayers = new HashMap<>();

    // Mapa para rastrear o último inimigo que o jogador deu dano
    private static final Map<UUID, UUID> lastDamagedEnemy = new HashMap<>();

    @Override
    public MurilloSkillsList getSkillType() {
        return MurilloSkillsList.ARCHER;
    }

    @Override
    public void onActiveAbility(ServerPlayerEntity player, SkillGlobalState.SkillStats stats) {
        try {
            // 1. Verifica Nível
            if (stats.level < SkillConfig.ARCHER_MASTER_LEVEL) {
                sendMessage(player, "Você precisa ser Nível 100 de Arqueiro!", Formatting.RED, true);
                return;
            }

            // 2. Verifica se já está com Master Ranger ativo
            if (isMasterRangerActive(player)) {
                sendMessage(player, "Master Ranger já está ativo!", Formatting.RED, true);
                return;
            }

            // 3. Verifica Cooldown (pula se nunca usou: lastAbilityUse == -1)
            long worldTime = player.getEntityWorld().getTime();
            long timeSinceUse = worldTime - stats.lastAbilityUse;

            if (stats.lastAbilityUse >= 0 && timeSinceUse < SkillConfig.ARCHER_ABILITY_COOLDOWN) {
                long minutesLeft = (SkillConfig.ARCHER_ABILITY_COOLDOWN - timeSinceUse) / 20 / 60;
                sendMessage(player, "Habilidade em recarga: " + minutesLeft + " minutos.", Formatting.RED, true);
                return;
            }

            // 4. Ativa o Master Ranger
            stats.lastAbilityUse = worldTime;
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            state.markDirty();

            startMasterRanger(player);

            LOGGER.info("Player {} ativou modo Master Ranger", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Erro ao executar habilidade ativa do Arqueiro para " + player.getName().getString(), e);
            sendMessage(player, "Erro ao ativar habilidade. Contate o admin.", Formatting.RED, false);
        }
    }

    @Override
    public void onTick(ServerPlayerEntity player, int level) {
        try {
            if (player.age % 20 != 0)
                return; // Executa apenas 1 vez por segundo

            // Verifica se o jogador estava com Master Ranger e o tempo acabou
            if (masterRangerPlayers.containsKey(player.getUuid())) {
                long startTime = masterRangerPlayers.get(player.getUuid());
                long currentTime = player.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;

                // Se o Master Ranger acabou
                if (elapsed >= SkillConfig.ARCHER_MASTER_RANGER_DURATION) {
                    endMasterRanger(player);
                }
            }

        } catch (Exception e) {
            // Log with limited frequency to avoid spamming console on tick errors
            if (player.age % 200 == 0) {
                LOGGER.error("Erro no tick do Arqueiro para " + player.getName().getString(), e);
            }
        }
    }

    @Override
    public void updateAttributes(ServerPlayerEntity player, int level) {
        try {
            // --- DANO À DISTÂNCIA (por level) ---
            // +2% por nível como base
            double rangedDamageBonus = level * SkillConfig.ARCHER_DAMAGE_PER_LEVEL;

            // Nível 25: +5% adicional
            if (level >= SkillConfig.ARCHER_BONUS_DAMAGE_LEVEL) {
                rangedDamageBonus += SkillConfig.ARCHER_BONUS_DAMAGE_AMOUNT;
            }

            // Nota: Minecraft não tem atributo nativo de "dano à distância"
            // Este valor será usado via mixin/evento para modificar o dano de projéteis
            // Por enquanto, salvamos como metadata no jogador ou usamos um sistema custom

            LOGGER.debug("Updated archer attributes for {} - Ranged Damage Bonus: {}%",
                    player.getName().getString(), rangedDamageBonus * 100);

        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos do Arqueiro para " + player.getName().getString(), e);
        }
    }

    /**
     * Calcula o multiplicador de dano à distância baseado no nível
     */
    public static double getRangedDamageMultiplier(int level) {
        // +2% por nível
        double multiplier = 1.0 + (level * SkillConfig.ARCHER_DAMAGE_PER_LEVEL);

        // Nível 25: +5% adicional
        if (level >= SkillConfig.ARCHER_BONUS_DAMAGE_LEVEL) {
            multiplier += SkillConfig.ARCHER_BONUS_DAMAGE_AMOUNT;
        }

        return multiplier;
    }

    /**
     * Calcula o multiplicador de velocidade da flecha baseado no nível
     */
    public static double getArrowSpeedMultiplier(int level) {
        // Nível 10: Flechas voam mais rápido
        if (level >= SkillConfig.ARCHER_FAST_ARROWS_LEVEL) {
            return SkillConfig.ARCHER_ARROW_SPEED_MULTIPLIER;
        }
        return 1.0;
    }

    /**
     * Verifica se o jogador tem penetração de flecha (Nível 50)
     */
    public static boolean hasArrowPenetration(int level) {
        return level >= SkillConfig.ARCHER_PENETRATION_LEVEL;
    }

    /**
     * Calcula a redução de spread baseada no nível
     */
    public static double getSpreadReduction(int level) {
        // Nível 75: Disparo mais estável (menos spread)
        if (level >= SkillConfig.ARCHER_STABLE_SHOT_LEVEL) {
            return SkillConfig.ARCHER_SPREAD_REDUCTION;
        }
        return 0.0;
    }

    /**
     * Verifica se o jogador está atualmente com Master Ranger ativo
     */
    public static boolean isMasterRangerActive(ServerPlayerEntity player) {
        if (!masterRangerPlayers.containsKey(player.getUuid())) {
            return false;
        }

        long startTime = masterRangerPlayers.get(player.getUuid());
        long currentTime = player.getEntityWorld().getTime();
        long elapsed = currentTime - startTime;

        return elapsed < SkillConfig.ARCHER_MASTER_RANGER_DURATION;
    }

    /**
     * Registra o último inimigo que o jogador deu dano (para homing arrow)
     */
    public static void setLastDamagedEnemy(ServerPlayerEntity player, UUID enemyUuid) {
        lastDamagedEnemy.put(player.getUuid(), enemyUuid);
    }

    /**
     * Obtém o UUID do último inimigo que o jogador deu dano
     */
    public static UUID getLastDamagedEnemy(ServerPlayerEntity player) {
        return lastDamagedEnemy.get(player.getUuid());
    }

    /**
     * Inicia o modo Master Ranger
     */
    private void startMasterRanger(ServerPlayerEntity player) {
        // Registra o início do Master Ranger
        masterRangerPlayers.put(player.getUuid(), player.getEntityWorld().getTime());

        // Efeitos visuais/sonoros
        player.playSound(SoundEvents.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.5f);

        // Mensagem
        player.sendMessage(Text.literal("🏹 MASTER RANGER ATIVADO! 🏹").formatted(Formatting.GREEN, Formatting.BOLD),
                false);
        player.sendMessage(Text.literal("Flechas perfuram múltiplos alvos e perseguem inimigos por 30 segundos!")
                .formatted(Formatting.YELLOW), true);
    }

    /**
     * Finaliza o modo Master Ranger
     */
    private void endMasterRanger(ServerPlayerEntity player) {
        // Remove do mapa
        masterRangerPlayers.remove(player.getUuid());

        // Som de fim
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 0.5f);

        // Mensagem
        sendMessage(player, "Master Ranger desativado.", Formatting.GRAY, true);

        LOGGER.debug("Player {} saiu do modo Master Ranger", player.getName().getString());
    }
}
