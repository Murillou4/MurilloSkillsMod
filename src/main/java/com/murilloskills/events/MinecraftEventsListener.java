package com.murilloskills.events;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;

import com.murilloskills.skills.BlockBreakHandler;
import com.murilloskills.skills.CropHarvestHandler;
import com.murilloskills.skills.MobKillHandler;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftEventsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Events");

    public static void initAllListeners() {
        blockBreakedListen();
        killEntityListen();
        playerJoinListen();
        playerRespawnListen();
        playerTickListen();
    }

    public static void blockBreakedListen() {
        // Usando o evento oficial da Fabric API
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // Handler do Minerador
            BlockBreakHandler.handle(player, world, pos, state);
            // Handler do Agricultor (Farmer)
            CropHarvestHandler.handle(player, world, pos, state);
        });
    }

    public static void killEntityListen() {
        // Usando o evento oficial da Fabric API
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((serverWorld, player, entity, damageSource) -> {

            if (player.isPlayer()) {
                final PlayerEntity playerEntity = (PlayerEntity) player;
                MobKillHandler.handle(playerEntity, entity);
            }

        });
    }

    public static void playerJoinListen() {
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD
                .register((player, origin, destination) -> {
                    handlePlayerJoin(player);
                });

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // IMPORTANT: Migrate legacy data BEFORE processing player join
            // This ensures old murilloskills.dat data is moved to the new attachment system
            com.murilloskills.data.LegacyDataMigration.migrateIfNeeded(handler.getPlayer(), server);

            handlePlayerJoin(handler.getPlayer());
            // Sincroniza as skills ao entrar
            SkillsNetworkUtils.syncSkills(handler.getPlayer());
            // Sincroniza os desafios diários ao entrar
            com.murilloskills.utils.DailyChallengeManager.syncChallenges(handler.getPlayer());
        });

        // Cleanup on disconnect to prevent memory leaks
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            com.murilloskills.impl.FarmerSkill.cleanupPlayerState(handler.getPlayer().getUuid());
        });
    }

    /**
     * Registra listener para quando o jogador respawna após morrer.
     * CRÍTICO: Isso garante que os atributos das skills sejam reaplicados após a
     * morte.
     */
    public static void playerRespawnListen() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            try {
                LOGGER.debug("Player {} respawned (alive={}), reapplying skill attributes...",
                        newPlayer.getName().getString(), alive);

                // Mesmo comportamento do handlePlayerJoin - reaplicar todos os atributos
                var playerData = newPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                // Reaplicar atributos para todas as skills selecionadas
                if (playerData.hasSelectedSkills()) {
                    for (MurilloSkillsList skillEnum : playerData.getSelectedSkills()) {
                        AbstractSkill skillObj = SkillRegistry.get(skillEnum);
                        if (skillObj != null) {
                            int level = playerData.getSkill(skillEnum).level;
                            skillObj.onPlayerJoin(newPlayer, level);
                        }
                    }
                }

                // Sincroniza as skills após respawn para garantir UI atualizada
                SkillsNetworkUtils.syncSkills(newPlayer);

                LOGGER.info("Skill attributes reapplied for {} after respawn", newPlayer.getName().getString());

            } catch (Exception e) {
                LOGGER.error("Erro ao reaplicar atributos após respawn para " + newPlayer.getName().getString(), e);
            }
        });
    }

    public static void playerTickListen() {
        // Registra o tick geral do servidor e itera sobre players
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Try-Catch no loop principal para evitar crashar o server inteiro se um player
            // bugar
            try {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    handlePlayerTick(player);
                }
            } catch (Exception e) {
                LOGGER.error("Erro crítico no loop de Player Tick", e);
            }
        });
    }

    /**
     * Handles player join events using the new skill system
     */
    private static void handlePlayerJoin(ServerPlayerEntity player) {
        try {
            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Update attributes for all selected skills
            if (playerData.hasSelectedSkills()) {
                for (MurilloSkillsList skillEnum : playerData.getSelectedSkills()) {
                    AbstractSkill skillObj = SkillRegistry.get(skillEnum);
                    if (skillObj != null) {
                        int level = playerData.getSkill(skillEnum).level;
                        skillObj.onPlayerJoin(player, level);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao processar entrada do jogador " + player.getName().getString(), e);
        }
    }

    /**
     * Handles player tick events using the new skill system
     */
    private static void handlePlayerTick(ServerPlayerEntity player) {
        try {
            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Em vez de chamar MinerAbilityHandler.tick(), WarriorAbilityHandler.tick()...
            // Nós iteramos sobre as skills selecionadas do jogador

            if (playerData.hasSelectedSkills()) {
                for (MurilloSkillsList skillEnum : playerData.getSelectedSkills()) {
                    AbstractSkill skillObj = SkillRegistry.get(skillEnum);
                    if (skillObj != null) {
                        int level = playerData.getSkill(skillEnum).level;

                        // Delega a lógica para a classe da skill
                        skillObj.onTick(player, level);
                    }
                }
            }
        } catch (Exception e) {
            // Logs de erro dentro do tick devem ser cuidadosos com spam
            if (player.age % 100 == 0) {
                LOGGER.error("Erro ao processar tick para o jogador " + player.getName().getString(), e);
            }
        }
    }
}
