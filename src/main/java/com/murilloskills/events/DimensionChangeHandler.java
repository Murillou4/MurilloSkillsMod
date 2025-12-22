package com.murilloskills.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for dimension change events to track for Dimension Walker
 * achievement.
 * Tracks visits to Overworld, Nether, and End.
 */
public class DimensionChangeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-DimensionChange");

    /**
     * Registers the dimension change event handler.
     * Call this from the main mod initializer.
     */
    public static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            // Get current dimension
            var dimension = destination.getRegistryKey();
            String dimensionName = dimension.getValue().toString();

            LOGGER.debug("Player {} changed dimension to {}", player.getName().getString(), dimensionName);

            // Determine bit for this dimension
            int bit = 0;
            if (dimensionName.equals("minecraft:overworld")) {
                bit = 1;
            } else if (dimensionName.equals("minecraft:the_nether")) {
                bit = 2;
            } else if (dimensionName.equals("minecraft:the_end")) {
                bit = 4;
            }

            if (bit == 0)
                return;

            // Get player skill data
            var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            // Check if player has Explorer skill selected
            if (!data.isSkillSelected(com.murilloskills.skills.MurilloSkillsList.EXPLORER)) {
                return;
            }

            // Get current dimension bitmask from achievement stats
            int currentMask = 0;
            if (data.achievementStats != null) {
                currentMask = data.achievementStats.getOrDefault(
                        com.murilloskills.utils.AchievementTracker.KEY_DIMENSIONS_VISITED, 0);
            } else {
                data.achievementStats = new java.util.HashMap<>();
            }

            // Add new dimension to mask
            int newMask = currentMask | bit;
            data.achievementStats.put(com.murilloskills.utils.AchievementTracker.KEY_DIMENSIONS_VISITED, newMask);

            LOGGER.debug("Player {} dimension mask updated: {} -> {}", player.getName().getString(), currentMask,
                    newMask);

            // Check if all 3 dimensions visited (bitmask = 7 = 1+2+4)
            if (newMask == 7) {
                LOGGER.info("Player {} has visited all 3 dimensions!", player.getName().getString());
                com.murilloskills.utils.AdvancementGranter.grantDimensionWalker(player);
            }
        });

        LOGGER.info("DimensionChangeHandler registered for Explorer achievements");
    }
}
