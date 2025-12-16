package com.murilloskills.network.handlers;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.SkillSelectionC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.DailyChallengeManager;
import com.murilloskills.utils.SkillAttributes;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles initial skill selection (1-3 skills).
 * Validates selection constraints and applies skill attributes.
 */
public final class SkillSelectionNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-SelectionHandler");

    private SkillSelectionNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for skill selection requests.
     * 
     * @return ServerPlayNetworking handler for SkillSelectionC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<SkillSelectionC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var data = state.getPlayerData(player);

                    // Validation: Check if player already has 3 skills selected (maxed out)
                    if (data.hasSelectedSkills()) {
                        player.sendMessage(Text.translatable("murilloskills.selection.limit_reached")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    List<MurilloSkillsList> incoming = payload.selectedSkills();
                    int newCount = incoming.size();

                    // Validation: Must select between 1 and 3 skills
                    if (incoming == null || newCount < 1 || newCount > SkillSelectionC2SPayload.MAX_SELECTED_SKILLS) {
                        player.sendMessage(Text.translatable("murilloskills.selection.select_1_to_3")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Validation: No duplicate skills in the payload
                    if (newCount != incoming.stream().distinct().count()) {
                        player.sendMessage(Text.translatable("murilloskills.selection.duplicates")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Apply the selection (Overwrite existing selection with new cumulative list)
                    // Note: Client should send the FULL list of selected skills (old + new)
                    if (data.setSelectedSkills(incoming)) {
                        state.markDirty();

                        // Apply attributes for selected skills immediately
                        SkillAttributes.updateAllStats(player);

                        SkillsNetworkUtils.syncSkills(player);

                        // Force regenerate daily challenges to match newly selected skills
                        DailyChallengeManager.forceRegenerate(player);

                        // Feedback Messages
                        int currentCount = data.getSelectedSkills().size();
                        if (currentCount == SkillSelectionC2SPayload.MAX_SELECTED_SKILLS) {
                            // Complete selection
                            player.sendMessage(Text.translatable("murilloskills.selection.success")
                                    .formatted(Formatting.GREEN, Formatting.BOLD), false);
                            player.sendMessage(Text.translatable("murilloskills.selection.chose_3")
                                    .formatted(Formatting.YELLOW), false);
                        } else {
                            // Partial selection
                            int remaining = SkillSelectionC2SPayload.MAX_SELECTED_SKILLS - currentCount;
                            player.sendMessage(Text.translatable("murilloskills.selection.partial_saved", currentCount)
                                    .formatted(Formatting.GREEN), false);
                            player.sendMessage(
                                    Text.translatable("murilloskills.selection.can_choose_more", remaining)
                                            .formatted(Formatting.YELLOW),
                                    false);
                            player.sendMessage(Text.translatable("murilloskills.selection.use_again")
                                    .formatted(Formatting.GRAY), false);
                        }
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.selection.error").formatted(Formatting.RED),
                                true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing skill selection", e);
                }
            });
        };
    }
}
