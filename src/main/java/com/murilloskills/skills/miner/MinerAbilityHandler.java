package com.murilloskills.skills.miner;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

public class MinerAbilityHandler {

    // Chamado a cada Tick do Jogador (Event Listener)
    public static void onPlayerTick(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return; // Executa apenas 1 vez por segundo para otimização

        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        int level = state.getPlayerData(player).getSkill(MurilloSkillsList.MINER).level;

        // --- LEVEL 10: NIGHT VISION ---
        if (level >= SkillConfig.MINER_NIGHT_VISION_LEVEL) {
            handleNightVision(player);
        }

        // --- LEVEL 60: RADAR ---
        if (level >= SkillConfig.MINER_RADAR_LEVEL) {
            handleRadar(player);
        }
    }

    private static void handleNightVision(ServerPlayerEntity player) {
        // Aplica apenas se estiver no subsolo (Y < 50) e em local escuro
        // Não aplica se estiver com o céu visível (ex: noite na superfície) para não atrapalhar
        if (player.getY() < 55 && !player.getEntityWorld().isSkyVisible(player.getBlockPos())) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
    }

    private static void handleRadar(ServerPlayerEntity player) {
        // A cada 2 segundos (checagem dentro do tick de 1s, usamos age % 40)
        if (player.age % 40 != 0) return;

        BlockPos playerPos = player.getBlockPos();
        int radius = 5;
        boolean foundRareOre = false;

        // Escaneia área pequena
        for (BlockPos pos : BlockPos.iterate(playerPos.add(-radius, -radius, -radius), playerPos.add(radius, radius, radius))) {
            BlockState state = player.getEntityWorld().getBlockState(pos);
            var result = MinerXpGetter.isMinerXpBlock(state.getBlock(), false, true);

            // Só apita para minérios valiosos (XP > 5)
            if (result.didGainXp() && result.getXpAmount() > 5) {
                foundRareOre = true;
                break;
            }
        }

        if (foundRareOre) {
            // Toca um som sutil "Geiger Counter"
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value());
        }
    }

    // Chamado quando aperta a tecla da Habilidade (Level 100)
    public static void triggerActiveAbility(ServerPlayerEntity player) {
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        var stats = state.getPlayerData(player).getSkill(MurilloSkillsList.MINER);

        // 1. Verifica Nível
        if (stats.level < SkillConfig.MINER_MASTER_LEVEL) {
            player.sendMessage(Text.of("§cVocê precisa ser Nível 100 de Minerador!"), true);
            return;
        }

        // 2. Verifica Cooldown
        long worldTime = player.getEntityWorld().getTime();
        long timeSinceUse = worldTime - stats.lastAbilityUse;

        if (timeSinceUse < SkillConfig.MINER_ABILITY_COOLDOWN) {
            long minutesLeft = (SkillConfig.MINER_ABILITY_COOLDOWN - timeSinceUse) / 20 / 60;
            player.sendMessage(Text.of("§cHabilidade em recarga: " + minutesLeft + " minutos."), true);
            return;
        }

        // 3. Executa Habilidade
        stats.lastAbilityUse = worldTime;
        state.markDirty();

        List<BlockPos> ores = new ArrayList<>();
        BlockPos center = player.getBlockPos();
        int r = SkillConfig.MINER_ABILITY_RADIUS;

        for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            var result = MinerXpGetter.isMinerXpBlock(player.getEntityWorld().getBlockState(pos).getBlock(), false, true);
            if (result.didGainXp()) {
                ores.add(pos.toImmutable());
            }
        }

        if (!ores.isEmpty()) {
            // Envia pacote para o cliente renderizar
            ServerPlayNetworking.send(player, new MinerScanResultPayload(ores));
            player.sendMessage(Text.literal("Instinto ativado! Minérios revelados.").formatted(Formatting.GREEN), true);
            player.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM);
        } else {
            player.sendMessage(Text.of("§eNenhum minério encontrado por perto."), true);
        }
    }
}