package com.murilloskills.network.handlers;

import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.network.AreaPlantingToggleC2SPayload;
import com.murilloskills.network.AreaPlantingSyncS2CPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles cycling Farmer area mode (off, 3x3, 5x5, 7x7, 9x9).
 * Validates level requirements and syncs the selected size to the client.
 */
public final class AreaPlantingNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-AreaPlantingHandler");

    private AreaPlantingNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for area planting toggle requests.
     * 
     * @return ServerPlayNetworking handler for AreaPlantingToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<AreaPlantingToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                    // Check if player has FARMER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.FARMER)) {
                        player.sendMessage(Text.translatable("murilloskills.farmer.need_farmer_skill")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    var farmerStats = playerData.getSkill(MurilloSkillsList.FARMER);

                    // Check level requirement
                    if (farmerStats.level < SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
                        player.sendMessage(
                                Text.translatable("murilloskills.farmer.need_level_25")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    int newRadius = FarmerSkill.cycleAreaPlantingMode(player, farmerStats.level);
                    boolean enabled = newRadius > 0;
                    String areaLabel = FarmerSkill.getAreaPlantingLabel(newRadius);

                    // Send sync to client for HUD indicator
                    ServerPlayNetworking.send(player, new AreaPlantingSyncS2CPayload(enabled,
                            FarmerSkill.getAreaPlantingDiameter(newRadius)));

                    // Feedback message
                    if (enabled) {
                        player.sendMessage(Text.translatable("murilloskills.farmer.area_enabled", areaLabel)
                                .formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.farmer.area_disabled")
                                .formatted(Formatting.GRAY), true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing area planting toggle", e);
                }
            });
        };
    }
}
