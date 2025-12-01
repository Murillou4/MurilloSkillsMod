package com.murilloskills.events;


import com.murilloskills.skills.BlockBreakHandler;
import com.murilloskills.skills.MobKillHandler;
import com.murilloskills.skills.miner.MinerAbilityHandler;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class MinecraftEventsListener {

    public static void initAllListeners() {
        blockBreakedListen();
        killEntityListen();
        playerJoinListen();
    }


    public static void blockBreakedListen() {
        // Usando o evento oficial da Fabric API
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // Chama o seu handler
            BlockBreakHandler.handle(player, world, pos, state);
        });
    }

    public static void killEntityListen() {
        // Usando o evento oficial da Fabric API
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((serverWorld, player, entity, damageSource) -> {

            if(player.isPlayer()){
                final PlayerEntity playerEntity = (PlayerEntity) player;
                MobKillHandler.handle( playerEntity, entity);
            }

        });
    }

    public static void playerJoinListen() {
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            com.murilloskills.utils.SkillAttributes.updateMinerStats(player);
        });


        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            com.murilloskills.utils.SkillAttributes.updateMinerStats(handler.getPlayer());

            // ADICIONE ISSO: Sincroniza as skills ao entrar
            SkillsNetworkUtils.syncSkills(handler.getPlayer());
        });
    }
    public static void playerTickListen() {
        // Registra o tick geral do servidor e itera sobre players
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                MinerAbilityHandler.onPlayerTick(player);
            }
        });
    }
}
