package com.murilloskills.network.handlers;

import com.murilloskills.network.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for all network handlers in MurilloSkills.
 * Replaces the scattered registerXXXReceiver() methods from MurilloSkills.java
 * with a clean, centralized registration system following the Strategy Pattern.
 * 
 * This class is responsible for:
 * - Registering all Client-to-Server (C2S) payload handlers
 * - Providing a single entry point for network initialization
 * - Maintaining separation of concerns (networking logic separate from mod
 * initialization)
 */
public final class NetworkHandlerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-NetworkRegistry");

    private NetworkHandlerRegistry() {
        // Utility class - prevent instantiation
    }

    /**
     * Registers all network handlers for the mod.
     * Should be called once during mod initialization.
     * 
     * This method replaces the following methods from MurilloSkills.java:
     * - registerAbilityReceiver()
     * - registerAreaPlantingReceiver()
     * - registerHollowFillReceiver()
     * - registerSkillResetReceiver()
     * - registerNightVisionToggleReceiver()
     * - registerStepAssistToggleReceiver()
     * - registerPrestigeReceiver()
     * - Inline ParagonActivation receiver
     * - Inline SkillSelection receiver
     */
    public static void registerAll() {
        try {
            LOGGER.info("Registering network handlers...");

            // Skill Management Handlers
            registerHandler("SkillSelection", SkillSelectionC2SPayload.ID, SkillSelectionNetworkHandler.create());
            registerHandler("ParagonActivation", ParagonActivationC2SPayload.ID,
                    ParagonActivationNetworkHandler.create());
            registerHandler("SkillAbility", SkillAbilityC2SPayload.ID, AbilityNetworkHandler.create());
            registerHandler("SkillReset", SkillResetC2SPayload.ID, SkillResetNetworkHandler.create());
            registerHandler("Prestige", PrestigeC2SPayload.ID, PrestigeNetworkHandler.create());

            // Skill Toggle Handlers (Feature Activation)
            registerHandler("AreaPlanting", AreaPlantingToggleC2SPayload.ID, AreaPlantingNetworkHandler.create());
            registerHandler("HollowFill", HollowFillToggleC2SPayload.ID, HollowFillNetworkHandler.create());
            registerHandler("VeinMiner", VeinMinerToggleC2SPayload.ID, VeinMinerToggleNetworkHandler.create());
            registerHandler("NightVision", NightVisionToggleC2SPayload.ID, NightVisionToggleNetworkHandler.create());
            registerHandler("StepAssist", StepAssistToggleC2SPayload.ID, StepAssistToggleNetworkHandler.create());
            registerHandler("FillModeCycle", FillModeCycleC2SPayload.ID, FillModeCycleNetworkHandler.create());
            registerHandler("VeinMinerDrops", VeinMinerDropsToggleC2SPayload.ID, VeinMinerDropsToggleNetworkHandler.create());
            registerHandler("BuilderUndo", BuilderUndoC2SPayload.ID, BuilderUndoNetworkHandler.create());

            LOGGER.info("Successfully registered 13 network handlers");

        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to register network handlers!", e);
            throw new RuntimeException("Network handler registration failed", e);
        }
    }

    /**
     * Helper method to register a single handler with logging.
     * 
     * @param name    Human-readable name for logging
     * @param id      Payload type identifier
     * @param handler The handler implementation
     */
    private static <T extends net.minecraft.network.packet.CustomPayload> void registerHandler(
            String name,
            net.minecraft.network.packet.CustomPayload.Id<T> id,
            ServerPlayNetworking.PlayPayloadHandler<T> handler) {

        try {
            ServerPlayNetworking.registerGlobalReceiver(id, handler);
            LOGGER.debug("Registered {} handler", name);
        } catch (Exception e) {
            LOGGER.error("Failed to register {} handler", name, e);
            throw e;
        }
    }
}
