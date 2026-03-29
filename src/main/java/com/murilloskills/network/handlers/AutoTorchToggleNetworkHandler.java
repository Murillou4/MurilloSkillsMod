package com.murilloskills.network.handlers;

import com.murilloskills.impl.MinerSkill;
import com.murilloskills.network.AutoTorchSyncS2CPayload;
import com.murilloskills.network.AutoTorchToggleC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for Auto-Torch feature (Miner skill, Level 25+).
 * Validates level requirements and toggles the auto-torch state.
 */
public final class AutoTorchToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-AutoTorchHandler");

    private AutoTorchToggleNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<AutoTorchToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                    // Check if player has MINER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.MINER)) {
                        player.sendMessage(
                                Text.translatable("murilloskills.miner.need_miner_skill")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    var minerStats = playerData.getSkill(MurilloSkillsList.MINER);

                    // Check level requirement (Level 25 for Auto-Torch)
                    if (minerStats.level < SkillConfig.MINER_AUTO_TORCH_LEVEL) {
                        player.sendMessage(
                                Text.translatable("murilloskills.miner.need_level_25_auto_torch")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle
                    boolean newState = MinerSkill.toggleAutoTorch(player);

                    if (newState) {
                        player.sendMessage(
                                Text.translatable("murilloskills.miner.auto_torch_enabled")
                                        .formatted(Formatting.GREEN),
                                true);
                    } else {
                        player.sendMessage(
                                Text.translatable("murilloskills.miner.auto_torch_disabled")
                                        .formatted(Formatting.GRAY),
                                true);
                    }

                    // Sync to client for HUD
                    ServerPlayNetworking.send(player, new AutoTorchSyncS2CPayload(newState));

                } catch (Exception e) {
                    LOGGER.error("Error processing auto-torch toggle", e);
                }
            });
        };
    }
}
