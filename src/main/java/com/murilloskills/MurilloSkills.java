package com.murilloskills;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.item.ModItems;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.ParagonActivationC2SPayload;
import com.murilloskills.network.RainDanceS2CPayload;
import com.murilloskills.network.TreasureHunterS2CPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillSelectionC2SPayload;
import com.murilloskills.network.SkillResetC2SPayload;
import com.murilloskills.network.NightVisionToggleC2SPayload;
import com.murilloskills.network.StepAssistToggleC2SPayload;
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

import java.util.List;

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

            // 2.1. Registrar Event Handlers
            com.murilloskills.events.BlockPlacementHandler.register();

            // 3. Payloads
            PayloadTypeRegistry.playS2C().register(SkillsSyncPayload.ID, SkillsSyncPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(MinerScanResultPayload.ID, MinerScanResultPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(RainDanceS2CPayload.ID, RainDanceS2CPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(TreasureHunterS2CPayload.ID, TreasureHunterS2CPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillAbilityC2SPayload.ID, SkillAbilityC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(ParagonActivationC2SPayload.ID, ParagonActivationC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillSelectionC2SPayload.ID, SkillSelectionC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(AreaPlantingToggleC2SPayload.ID, AreaPlantingToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(AreaPlantingSyncS2CPayload.ID, AreaPlantingSyncS2CPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(com.murilloskills.network.HollowFillToggleC2SPayload.ID,
                    com.murilloskills.network.HollowFillToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillResetC2SPayload.ID, SkillResetC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(NightVisionToggleC2SPayload.ID, NightVisionToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(StepAssistToggleC2SPayload.ID, StepAssistToggleC2SPayload.CODEC);

            // 4. Receiver: Habilidade Ativa (Tecla Z) - Usando Registry
            registerAbilityReceiver();

            // 5. Receiver: Toggle Area Planting (Tecla G)
            registerAreaPlantingReceiver();

            // 6. Receiver: Toggle Hollow/Filled (Tecla H)
            registerHollowFillReceiver();

            // 7. Receiver: Toggle Night Vision (Tecla N)
            registerNightVisionToggleReceiver();

            // 8. Receiver: Toggle Step Assist (Tecla V)
            registerStepAssistToggleReceiver();

            // 5. Outros receivers existentes...

            // Receiver: Skill Reset (Botão na GUI)
            registerSkillResetReceiver();

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

            // Receiver: Seleção de Skills (Escolha das habilidades principais)
            ServerPlayNetworking.registerGlobalReceiver(SkillSelectionC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var data = state.getPlayerData(player);

                    // Validation: Check if player already has 3 skills selected (maxed out)
                    if (data.hasSelectedSkills()) {
                        player.sendMessage(Text.literal("Você já atingiu o limite de 3 habilidades!")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    List<MurilloSkillsList> incoming = payload.selectedSkills();
                    int newCount = incoming.size();

                    // Validation: Must select between 1 and 3 skills
                    if (incoming == null || newCount < 1 || newCount > SkillSelectionC2SPayload.MAX_SELECTED_SKILLS) {
                        player.sendMessage(Text.literal("Selecione entre 1 e 3 habilidades.")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Validation: No duplicate skills in the payload
                    if (newCount != incoming.stream().distinct().count()) {
                        player.sendMessage(Text.literal("Habilidades duplicadas detectadas!")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Apply the selection (Overwrite existing selection with new cumulative list)
                    // Note: Client should send the FULL list of selected skills (old + new)
                    if (data.setSelectedSkills(incoming)) {
                        state.markDirty();

                        // Apply attributes for selected skills immediately
                        com.murilloskills.utils.SkillAttributes.updateAllStats(player);

                        SkillsNetworkUtils.syncSkills(player);

                        // Feedback Messages
                        int currentCount = data.getSelectedSkills().size();
                        if (currentCount == SkillSelectionC2SPayload.MAX_SELECTED_SKILLS) {
                            // Complete selection
                            player.sendMessage(Text.literal("Habilidades Definidas com Sucesso!")
                                    .formatted(Formatting.GREEN, Formatting.BOLD), false);
                            player.sendMessage(Text.literal("Você escolheu suas 3 habilidades principais.")
                                    .formatted(Formatting.YELLOW), false);
                        } else {
                            // Partial selection
                            int remaining = SkillSelectionC2SPayload.MAX_SELECTED_SKILLS - currentCount;
                            player.sendMessage(Text.literal("Seleção Parcial Salva (" + currentCount + "/3)")
                                    .formatted(Formatting.GREEN), false);
                            player.sendMessage(
                                    Text.literal("Você ainda pode escolher mais " + remaining + " habilidade(s).")
                                            .formatted(Formatting.YELLOW),
                                    false);
                            player.sendMessage(Text.literal("Use o item ou comando novamente para completar.")
                                    .formatted(Formatting.GRAY), false);
                        }
                    } else {
                        player.sendMessage(Text.literal("Erro ao selecionar habilidades.").formatted(Formatting.RED),
                                true);
                    }
                });
            });

            // Validar que as skills esperadas estão registradas
            SkillRegistry.validateRegistration(MurilloSkillsList.MINER, MurilloSkillsList.WARRIOR,
                    MurilloSkillsList.ARCHER, MurilloSkillsList.FARMER, MurilloSkillsList.FISHER,
                    MurilloSkillsList.BLACKSMITH, MurilloSkillsList.BUILDER, MurilloSkillsList.EXPLORER);
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
            SkillRegistry.register(new BlacksmithSkill());
            SkillRegistry.register(new BuilderSkill());
            SkillRegistry.register(new ExplorerSkill());

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

    /**
     * Registra o receiver para toggle hollow/filled (Builder)
     */
    private void registerHollowFillReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(com.murilloskills.network.HollowFillToggleC2SPayload.ID,
                (payload, context) -> {
                    context.server().execute(() -> {
                        try {
                            var player = context.player();
                            SkillGlobalState state = SkillGlobalState
                                    .getServerState(player.getEntityWorld().getServer());
                            var playerData = state.getPlayerData(player);

                            // Check if player has BUILDER selected
                            if (!playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
                                player.sendMessage(
                                        Text.literal("Você precisa ter Construtor como uma das suas habilidades!")
                                                .formatted(Formatting.RED),
                                        true);
                                return;
                            }

                            // Toggle hollow mode
                            boolean nowHollow = com.murilloskills.impl.BuilderSkill.toggleHollowMode(player);

                            // Feedback message
                            if (nowHollow) {
                                player.sendMessage(Text.literal("🧱 Modo Creative Brush: OCO (apenas paredes)")
                                        .formatted(Formatting.AQUA), true);
                            } else {
                                player.sendMessage(Text.literal("🧱 Modo Creative Brush: SÓLIDO (preenchido)")
                                        .formatted(Formatting.GREEN), true);
                            }

                        } catch (Exception e) {
                            LOGGER.error("Erro ao processar toggle de Hollow/Filled", e);
                        }
                    });
                });
    }

    /**
     * Registra o receiver para resetar skill (nível e XP voltam para 0)
     */
    private void registerSkillResetReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(SkillResetC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var data = state.getPlayerData(player);

                    // Validation: Can only reset selected skills
                    if (!data.isSkillSelected(payload.skill())) {
                        player.sendMessage(Text.literal("Você só pode resetar uma das suas habilidades selecionadas!")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Reset the skill to level 0 and XP 0
                    var stats = data.getSkill(payload.skill());
                    stats.level = 0;
                    stats.xp = 0;
                    stats.lastAbilityUse = -1; // Reset cooldown too

                    // If this was the paragon skill, remove paragon status
                    if (data.paragonSkill == payload.skill()) {
                        data.paragonSkill = null;
                    }

                    // Remove skill from selection - player can now choose a new one
                    data.selectedSkills.remove(payload.skill());

                    state.markDirty();

                    // Update attributes to reflect reset
                    com.murilloskills.utils.SkillAttributes.updateAllStats(player);

                    SkillsNetworkUtils.syncSkills(player);

                    player.sendMessage(
                            Text.literal("🔄 Skill " + payload.skill().name() + " removida! Escolha uma nova skill.")
                                    .formatted(Formatting.YELLOW),
                            false);

                    LOGGER.info("Player {} resetou a skill {} para nivel 0",
                            player.getName().getString(), payload.skill().name());

                } catch (Exception e) {
                    LOGGER.error("Erro ao processar reset de skill", e);
                }
            });
        });
    }

    /**
     * Registra o receiver para toggle de Night Vision (Explorer)
     */
    private void registerNightVisionToggleReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(NightVisionToggleC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var playerData = state.getPlayerData(player);

                    // Check if player has EXPLORER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
                        player.sendMessage(
                                Text.literal("Você precisa ter Explorador como uma das suas habilidades!")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    var explorerStats = playerData.getSkill(MurilloSkillsList.EXPLORER);

                    // Check level requirement (Level 35 for Night Vision)
                    if (explorerStats.level < com.murilloskills.utils.SkillConfig.EXPLORER_NIGHT_VISION_LEVEL) {
                        player.sendMessage(
                                Text.literal("Você precisa ser Nível 35 de Explorador para Visão Noturna!")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle Night Vision using the ExplorerSkill method
                    ExplorerSkill explorerSkill = (ExplorerSkill) com.murilloskills.api.SkillRegistry
                            .get(MurilloSkillsList.EXPLORER);
                    if (explorerSkill != null) {
                        explorerSkill.toggleNightVision(player);
                    }

                } catch (Exception e) {
                    LOGGER.error("Erro ao processar toggle de Night Vision", e);
                }
            });
        });
    }

    /**
     * Registra o receiver para toggle de Step Assist (Explorer)
     */
    private void registerStepAssistToggleReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(StepAssistToggleC2SPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var playerData = state.getPlayerData(player);

                    // Check if player has EXPLORER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
                        player.sendMessage(
                                Text.literal("Você precisa ter Explorador como uma das suas habilidades!")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    var explorerStats = playerData.getSkill(MurilloSkillsList.EXPLORER);

                    // Check level requirement (Level 10 for Step Assist)
                    if (explorerStats.level < com.murilloskills.utils.SkillConfig.EXPLORER_STEP_ASSIST_LEVEL) {
                        player.sendMessage(
                                Text.literal("Você precisa ser Nível 10 de Explorador para Passo Leve!")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle Step Assist using the ExplorerSkill method
                    ExplorerSkill explorerSkill = (ExplorerSkill) com.murilloskills.api.SkillRegistry
                            .get(MurilloSkillsList.EXPLORER);
                    if (explorerSkill != null) {
                        explorerSkill.toggleStepAssist(player);
                    }

                } catch (Exception e) {
                    LOGGER.error("Erro ao processar toggle de Step Assist", e);
                }
            });
        });
    }
}