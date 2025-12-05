package com.murilloskills.events;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.BlockBreakHandler;
import com.murilloskills.skills.CropHarvestHandler;
import com.murilloskills.skills.MobKillHandler;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftEventsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Events");

    public static void initAllListeners() {
        blockBreakedListen();
        killEntityListen();
        playerJoinListen();
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
            handlePlayerJoin(handler.getPlayer());
            // Sincroniza as skills ao entrar
            SkillsNetworkUtils.syncSkills(handler.getPlayer());
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
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = state.getPlayerData(player);

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
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = state.getPlayerData(player);

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
