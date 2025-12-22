package com.murilloskills.network.handlers;

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
                    var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

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

                            // Grant "Trio Chosen" advancement
                            com.murilloskills.utils.AdvancementGranter.grantTrioChosen(player);

                            // Check for active synergies and grant First Synergy achievement
                            var activeSynergies = com.murilloskills.utils.SkillSynergyManager
                                    .getActiveSynergies(player);
                            if (!activeSynergies.isEmpty()) {
                                com.murilloskills.utils.AdvancementGranter.grantFirstSynergy(player);

                                // Track unique synergies activated for Synergy Master achievement
                                for (var synergy : activeSynergies) {
                                    trackSynergyForMaster(player, synergy.id(), data);
                                }
                            }
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

    /**
     * Tracks unique synergies activated for Synergy Master achievement.
     * Uses a bitmask to track which synergies have been activated.
     */
    private static void trackSynergyForMaster(net.minecraft.server.network.ServerPlayerEntity player,
            String synergyId, com.murilloskills.data.PlayerSkillData data) {
        // Map synergy ID to bit position
        int bit = switch (synergyId) {
            case "iron_will" -> 1;
            case "forge_master" -> 2;
            case "ranger" -> 4;
            case "natures_bounty" -> 8;
            case "treasure_hunter" -> 16;
            case "combat_master" -> 32;
            case "master_crafter" -> 64;
            default -> 0;
        };

        if (bit == 0)
            return;

        // Get current synergy bitmask from achievement stats
        int currentMask = 0;
        if (data.achievementStats != null) {
            currentMask = data.achievementStats.getOrDefault("synergies_activated", 0);
        } else {
            data.achievementStats = new java.util.HashMap<>();
        }

        // Add new synergy to mask
        int newMask = currentMask | bit;
        data.achievementStats.put("synergies_activated", newMask);

        // Check if all 7 synergies activated (bitmask = 127 = 1+2+4+8+16+32+64)
        if (newMask == 127) {
            com.murilloskills.utils.AdvancementGranter.grantSynergyMaster(player);
        }
    }
}
