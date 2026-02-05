package com.murilloskills.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Handles registration of all network payloads and channels.
 * Extracts these concerns from the main mod class to adhere to SRP.
 */
public class ModNetwork {

    public static void register() {
        registerS2CPayloads();
        registerC2SPayloads();
    }

    private static void registerS2CPayloads() {
        PayloadTypeRegistry.playS2C().register(SkillsSyncPayload.ID, SkillsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MinerScanResultPayload.ID, MinerScanResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RainDanceS2CPayload.ID, RainDanceS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TreasureHunterS2CPayload.ID, TreasureHunterS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AreaPlantingSyncS2CPayload.ID, AreaPlantingSyncS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(XpGainS2CPayload.ID, XpGainS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DailyChallengesSyncS2CPayload.ID, DailyChallengesSyncS2CPayload.CODEC);
    }

    private static void registerC2SPayloads() {
        PayloadTypeRegistry.playC2S().register(SkillAbilityC2SPayload.ID, SkillAbilityC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ParagonActivationC2SPayload.ID, ParagonActivationC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SkillSelectionC2SPayload.ID, SkillSelectionC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AreaPlantingToggleC2SPayload.ID, AreaPlantingToggleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HollowFillToggleC2SPayload.ID, HollowFillToggleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VeinMinerToggleC2SPayload.ID, VeinMinerToggleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SkillResetC2SPayload.ID, SkillResetC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NightVisionToggleC2SPayload.ID, NightVisionToggleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StepAssistToggleC2SPayload.ID, StepAssistToggleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PrestigeC2SPayload.ID, PrestigeC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FillModeCycleC2SPayload.ID, FillModeCycleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VeinMinerDropsToggleC2SPayload.ID, VeinMinerDropsToggleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuilderUndoC2SPayload.ID, BuilderUndoC2SPayload.CODEC);
    }
}
