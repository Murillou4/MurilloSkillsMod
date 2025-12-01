package com.murilloskills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.item.ModItems;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.ParagonActivationC2SPayload; // Import
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.skills.miner.MinerAbilityHandler;
import com.murilloskills.utils.SkillsNetworkUtils; // Import
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting; // Import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MurilloSkills implements ModInitializer {
    public static final String MOD_ID = "murilloskills";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();

        // 1. Payloads
        PayloadTypeRegistry.playS2C().register(SkillsSyncPayload.ID, SkillsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MinerScanResultPayload.ID, MinerScanResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SkillAbilityC2SPayload.ID, SkillAbilityC2SPayload.CODEC);

        // NOVO REGISTRO
        PayloadTypeRegistry.playC2S().register(ParagonActivationC2SPayload.ID, ParagonActivationC2SPayload.CODEC);

        // 2. Receiver: Habilidade Ativa (Tecla Z)
        ServerPlayNetworking.registerGlobalReceiver(SkillAbilityC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                var playerData = state.getPlayerData(player);
                MurilloSkillsList paragon = playerData.paragonSkill;

                if (paragon == null) {
                    player.sendMessage(Text.of("§cVocê precisa confirmar uma habilidade Paragon Nível 100 (Tecla 'O') para usar o poder!"), true);
                    return;
                }

                // Dispatcher
                switch (paragon) {
                    case MINER -> MinerAbilityHandler.triggerActiveAbility(player);
                    // Adicione outros cases aqui (Warrior, etc)
                    default -> player.sendMessage(Text.of("§eHabilidade Paragon para " + paragon.name() + " ainda em desenvolvimento."), true);
                }
            });
        });

        // 3. Receiver: Ativação de Paragon (Botão na GUI)
        ServerPlayNetworking.registerGlobalReceiver(ParagonActivationC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                var data = state.getPlayerData(player);

                if (data.paragonSkill != null) {
                    player.sendMessage(Text.literal("Você já escolheu um Paragon!").formatted(Formatting.RED), true);
                    return;
                }

                var stats = data.getSkill(payload.skill());
                if (stats.level >= 100) {
                    data.paragonSkill = payload.skill();
                    state.markDirty();
                    SkillsNetworkUtils.syncSkills(player); // Sincroniza de volta para atualizar a tela
                    player.sendMessage(Text.literal("Paragon Definido: " + payload.skill().name()).formatted(Formatting.GOLD, Formatting.BOLD), false);
                } else {
                    player.sendMessage(Text.literal("Nível insuficiente para Paragon.").formatted(Formatting.RED), true);
                }
            });
        });

        LOGGER.info("MurilloSkills Initialized with Paragon System!");
    }
}