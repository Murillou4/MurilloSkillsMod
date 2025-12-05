package com.murilloskills;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.item.ModItems;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.ParagonActivationC2SPayload;
import com.murilloskills.network.RainDanceS2CPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillSelectionC2SPayload;
import com.murilloskills.network.AreaPlantingToggleC2SPayload;
import com.murilloskills.network.AreaPlantingSyncS2CPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.skills.MurilloSkillsList;
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
        LOGGER.info("Inicializando MurilloSkills...");

        try {
            // 1. Registrar Skills no Registry
            registerSkills();

            // 2. Registrar Items
            ModItems.registerModItems();

            // 3. Payloads
            PayloadTypeRegistry.playS2C().register(SkillsSyncPayload.ID, SkillsSyncPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(MinerScanResultPayload.ID, MinerScanResultPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(RainDanceS2CPayload.ID, RainDanceS2CPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillAbilityC2SPayload.ID, SkillAbilityC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(ParagonActivationC2SPayload.ID, ParagonActivationC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillSelectionC2SPayload.ID, SkillSelectionC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(AreaPlantingToggleC2SPayload.ID, AreaPlantingToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(AreaPlantingSyncS2CPayload.ID, AreaPlantingSyncS2CPayload.CODEC);

            // 4. Receiver: Habilidade Ativa (Tecla Z) - Usando Registry
            registerAbilityReceiver();

            // 5. Receiver: Toggle Area Planting (Tecla G)
            registerAreaPlantingReceiver();

            // 5. Outros receivers existentes...

            // Receiver: Ativação de Paragon (Botão na GUI)
            ServerPlayNetworking.registerGlobalReceiver(ParagonActivationC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var data = state.getPlayerData(player);

                    if (data.paragonSkill != null) {
                        player.sendMessage(Text.literal("Você já escolheu um Paragon!").formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // VALIDATION: Paragon can only be activated on selected skills
                    if (!data.isSkillSelected(payload.skill())) {
                        player.sendMessage(
                                Text.literal("Você só pode ativar Paragon em uma das suas habilidades selecionadas!")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    var stats = data.getSkill(payload.skill());
                    // Paragon pode ser selecionado no nível 99 (trava no 99 até escolher)
                    if (stats.level >= 99) {
                        data.paragonSkill = payload.skill();
                        state.markDirty();
                        SkillsNetworkUtils.syncSkills(player);
                        player.sendMessage(Text.literal("Paragon Definido: " + payload.skill().name())
                                .formatted(Formatting.GOLD, Formatting.BOLD), false);
                    } else {
                        player.sendMessage(Text.literal("Nível insuficiente para Paragon.").formatted(Formatting.RED),
                                true);
                    }
                });
            });

            // Receiver: Seleção de Skills (Escolha das 2 habilidades principais)
            ServerPlayNetworking.registerGlobalReceiver(SkillSelectionC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var data = state.getPlayerData(player);

                    // Validation: Check if player already selected skills (permanent choice)
                    if (data.hasSelectedSkills()) {
                        player.sendMessage(Text.literal("Você já escolheu suas habilidades! Esta escolha é permanente.")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Validation: Must select exactly 2 skills
                    if (payload.selectedSkills() == null
                            || payload.selectedSkills().size() != SkillSelectionC2SPayload.MAX_SELECTED_SKILLS) {
                        player.sendMessage(Text.literal("Você deve selecionar exatamente 2 habilidades!")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Validation: No duplicate skills
                    if (payload.selectedSkills().get(0) == payload.selectedSkills().get(1)) {
                        player.sendMessage(Text.literal("Você não pode selecionar a mesma habilidade duas vezes!")
                                .formatted(Formatting.RED), true);
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
                        player.sendMessage(Text.literal("Erro ao selecionar habilidades.").formatted(Formatting.RED),
                                true);
                    }
                });
            });

            // Validar que as skills esperadas estão registradas
            SkillRegistry.validateRegistration(MurilloSkillsList.MINER, MurilloSkillsList.WARRIOR,
                    MurilloSkillsList.ARCHER, MurilloSkillsList.FARMER, MurilloSkillsList.FISHER);
            SkillRegistry.logRegisteredSkills();

            LOGGER.info("MurilloSkills Initialized with Skill Specialization System!");

        } catch (Exception e) {
            LOGGER.error("ERRO CRÍTICO NA INICIALIZAÇÃO DO MOD!", e);
        }
    }

    /**
     * Registra todas as skills no SkillRegistry
     */
    private void registerSkills() {
        try {
            // Registrar skills implementadas
            SkillRegistry.register(new MinerSkill());
            SkillRegistry.register(new WarriorSkill());
            SkillRegistry.register(new ArcherSkill());
            SkillRegistry.register(new FarmerSkill());
            SkillRegistry.register(new FisherSkill());

            LOGGER.info("Skills registradas com sucesso no SkillRegistry");
        } catch (Exception e) {
            LOGGER.error("Erro ao registrar skills", e);
        }
    }

    /**
     * Registra o receiver para habilidades ativas usando o SkillRegistry
     */
    private void registerAbilityReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(SkillAbilityC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var playerData = state.getPlayerData(player);

                    if (playerData.paragonSkill == null) {
                        player.sendMessage(Text.of(
                                "§cVocê precisa confirmar uma habilidade Paragon Nível 100 (Tecla 'O') para usar o poder!"),
                                true);
                        return;
                    }

                    // O CÓDIGO LIMPO ESTÁ AQUI:
                    AbstractSkill skill = SkillRegistry.get(playerData.paragonSkill);
                    if (skill != null) {
                        var stats = playerData.getSkill(playerData.paragonSkill);
                        skill.onActiveAbility(player, stats); // Polimorfismo!
                    } else {
                        LOGGER.warn("Skill Paragon não encontrada no Registry: {}", playerData.paragonSkill);
                        player.sendMessage(Text.of("§eHabilidade Paragon para " + playerData.paragonSkill.name()
                                + " ainda em desenvolvimento."), true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Erro ao processar pacote de Habilidade", e);
                }
            });
        });
    }

    /**
     * Registra o receiver para toggle de plantio em área (3x3)
     */
    private void registerAreaPlantingReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(AreaPlantingToggleC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var playerData = state.getPlayerData(player);

                    // Check if player has FARMER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.FARMER)) {
                        player.sendMessage(Text.literal("Você precisa ter Agricultor como uma das suas habilidades!")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    var farmerStats = playerData.getSkill(MurilloSkillsList.FARMER);

                    // Check level requirement
                    if (farmerStats.level < com.murilloskills.utils.SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
                        player.sendMessage(
                                Text.literal("Você precisa ser Nível 25 de Agricultor para usar Plantio em Área!")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle and get new state
                    boolean nowEnabled = FarmerSkill.toggleAreaPlanting(player, farmerStats.level);

                    // Send sync to client for HUD indicator
                    ServerPlayNetworking.send(player, new AreaPlantingSyncS2CPayload(nowEnabled));

                    // Feedback message
                    if (nowEnabled) {
                        player.sendMessage(Text.literal("🌱 Plantio em Área: ATIVADO (3x3)")
                                .formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.literal("🌱 Plantio em Área: DESATIVADO")
                                .formatted(Formatting.GRAY), true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Erro ao processar toggle de Plantio em Área", e);
                }
            });
        });
    }
}