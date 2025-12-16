package com.murilloskills;

import com.murilloskills.api.SkillRegistry;
import com.murilloskills.commands.SkillAdminCommands;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.item.ModItems;
import com.murilloskills.network.*;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
            com.murilloskills.events.ChallengeEventsHandler.register();

            // 2.2. Register Admin Commands
            CommandRegistrationCallback.EVENT.register(SkillAdminCommands::register);
            LOGGER.info("Skill admin commands registered");

            // 3. Payloads - Server to Client
            PayloadTypeRegistry.playS2C().register(SkillsSyncPayload.ID, SkillsSyncPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(MinerScanResultPayload.ID, MinerScanResultPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(RainDanceS2CPayload.ID, RainDanceS2CPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(TreasureHunterS2CPayload.ID, TreasureHunterS2CPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(AreaPlantingSyncS2CPayload.ID, AreaPlantingSyncS2CPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(XpGainS2CPayload.ID, XpGainS2CPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DailyChallengesSyncS2CPayload.ID,
                    DailyChallengesSyncS2CPayload.CODEC);

            // 4. Payloads - Client to Server
            PayloadTypeRegistry.playC2S().register(SkillAbilityC2SPayload.ID, SkillAbilityC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(ParagonActivationC2SPayload.ID, ParagonActivationC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillSelectionC2SPayload.ID, SkillSelectionC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(AreaPlantingToggleC2SPayload.ID, AreaPlantingToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(HollowFillToggleC2SPayload.ID, HollowFillToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(SkillResetC2SPayload.ID, SkillResetC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(NightVisionToggleC2SPayload.ID, NightVisionToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(StepAssistToggleC2SPayload.ID, StepAssistToggleC2SPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(PrestigeC2SPayload.ID, PrestigeC2SPayload.CODEC);

            // 5. Register all network handlers (replaces 9 individual registerXXXReceiver
            // methods)
            com.murilloskills.network.handlers.NetworkHandlerRegistry.registerAll();

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
}