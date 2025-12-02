package com.murilloskills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.item.ModItems;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.ParagonActivationC2SPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillSelectionC2SPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.skills.miner.MinerAbilityHandler;
import com.murilloskills.skills.warrior.WarriorAbilityHandler;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
        PayloadTypeRegistry.playC2S().register(ParagonActivationC2SPayload.ID, ParagonActivationC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SkillSelectionC2SPayload.ID, SkillSelectionC2SPayload.CODEC);

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
                    case WARRIOR -> WarriorAbilityHandler.triggerActiveAbility(player);
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

                // VALIDATION: Paragon can only be activated on selected skills
                if (!data.isSkillSelected(payload.skill())) {
                    player.sendMessage(Text.literal("Você só pode ativar Paragon em uma das suas habilidades selecionadas!").formatted(Formatting.RED), true);
                    return;
                }

                var stats = data.getSkill(payload.skill());
                // Paragon pode ser selecionado no nível 99 (trava no 99 até escolher)
                if (stats.level >= 99) {
                    data.paragonSkill = payload.skill();
                    state.markDirty();
                    SkillsNetworkUtils.syncSkills(player);
                    player.sendMessage(Text.literal("Paragon Definido: " + payload.skill().name()).formatted(Formatting.GOLD, Formatting.BOLD), false);
                } else {
                    player.sendMessage(Text.literal("Nível insuficiente para Paragon.").formatted(Formatting.RED), true);
                }
            });
        });

        // 4. Receiver: Seleção de Skills (Escolha das 2 habilidades principais)
        ServerPlayNetworking.registerGlobalReceiver(SkillSelectionC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                var data = state.getPlayerData(player);

                // Validation: Check if player already selected skills (permanent choice)
                if (data.hasSelectedSkills()) {
                    player.sendMessage(Text.literal("Você já escolheu suas habilidades! Esta escolha é permanente.").formatted(Formatting.RED), true);
                    return;
                }

                // Validation: Must select exactly 2 skills
                if (payload.selectedSkills() == null || payload.selectedSkills().size() != SkillSelectionC2SPayload.MAX_SELECTED_SKILLS) {
                    player.sendMessage(Text.literal("Você deve selecionar exatamente 2 habilidades!").formatted(Formatting.RED), true);
                    return;
                }

                // Validation: No duplicate skills
                if (payload.selectedSkills().get(0) == payload.selectedSkills().get(1)) {
                    player.sendMessage(Text.literal("Você não pode selecionar a mesma habilidade duas vezes!").formatted(Formatting.RED), true);
                    return;
                }

                // Apply the selection
                if (data.setSelectedSkills(payload.selectedSkills())) {
                    state.markDirty();
                    SkillsNetworkUtils.syncSkills(player);

                    String skill1 = payload.selectedSkills().get(0).name();
                    String skill2 = payload.selectedSkills().get(1).name();
                    player.sendMessage(Text.literal("Habilidades Selecionadas: " + skill1 + " e " + skill2 + "!")
                            .formatted(Formatting.GREEN, Formatting.BOLD), false);
                    player.sendMessage(Text.literal("Agora você pode ganhar XP apenas nessas habilidades.")
                            .formatted(Formatting.YELLOW), false);
                } else {
                    player.sendMessage(Text.literal("Erro ao selecionar habilidades.").formatted(Formatting.RED), true);
                }
            });
        });

        LOGGER.info("MurilloSkills Initialized with Skill Specialization System!");
    }
}