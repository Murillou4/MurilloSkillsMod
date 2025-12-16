package com.murilloskills;

import com.murilloskills.api.SkillRegistry;
import com.murilloskills.commands.SkillAdminCommands;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.impl.MinerSkill;
import com.murilloskills.impl.WarriorSkill;
import com.murilloskills.item.ModItems;
import com.murilloskills.network.ModNetwork;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

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

            // 3. Register Network Payloads (Server <-> Client)
            ModNetwork.register();

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