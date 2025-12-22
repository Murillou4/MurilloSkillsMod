package com.murilloskills.data;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles migration of player data from the legacy PersistentState system
 * (murilloskills.dat) to the new Fabric Data Attachments system.
 * 
 * Legacy format (murilloskills.dat):
 * - Root compound contains player UUIDs as keys
 * - Each player has a compound with skill data
 * - Skills stored as "SKILL_NAME" with level, xp, lastAbilityUse, prestige
 * - paragonSkill: String name of paragon skill
 * - selectedSkills: List of skill names
 */
public class LegacyDataMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Migration");
    private static final String LEGACY_FILE_NAME = "murilloskills.dat";

    /**
     * Attempts to migrate legacy data for a player when they join.
     * Should be called when a player connects to the server.
     * 
     * @param player The player to migrate data for
     * @param server The Minecraft server instance
     */
    public static void migrateIfNeeded(ServerPlayerEntity player, MinecraftServer server) {
        try {
            // Check if player already has data in new system (any skill with level > 0)
            PlayerSkillData currentData = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            boolean hasNewData = false;
            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                if (currentData.getSkill(skill).level > 0) {
                    hasNewData = true;
                    break;
                }
            }

            // Also check if player has selected skills (partial selection counts as having
            // data)
            if (!currentData.selectedSkills.isEmpty()) {
                hasNewData = true;
            }

            // If player already has data in new system, skip migration
            if (hasNewData) {
                LOGGER.debug("Player {} already has data in new system, skipping migration",
                        player.getName().getString());
                return;
            }

            // Try to load legacy data
            NbtCompound legacyData = loadLegacyData(server);
            if (legacyData == null) {
                LOGGER.debug("No legacy data file found");
                return;
            }

            // Check if this player has legacy data
            String playerUuid = player.getUuidAsString();
            if (!legacyData.contains(playerUuid)) {
                LOGGER.debug("No legacy data for player {}", player.getName().getString());
                return;
            }

            // Migrate player data
            NbtCompound playerNbt = legacyData.getCompoundOrEmpty(playerUuid);
            migratePlayerData(player, currentData, playerNbt);

            LOGGER.info("Successfully migrated legacy data for player {}", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy data for player {}", player.getName().getString(), e);
        }
    }

    /**
     * Loads the legacy murilloskills.dat file from the world data folder.
     * Returns the "players" compound containing all player UUIDs as keys.
     */
    private static NbtCompound loadLegacyData(MinecraftServer server) {
        try {
            ServerWorld overworld = server.getOverworld();
            if (overworld == null) {
                return null;
            }

            File worldDir = overworld.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
            File dataDir = new File(worldDir, "data");
            File legacyFile = new File(dataDir, LEGACY_FILE_NAME);

            if (!legacyFile.exists()) {
                LOGGER.debug("Legacy file not found: {}", legacyFile.getPath());
                return null;
            }

            LOGGER.info("Found legacy data file: {}", legacyFile.getPath());

            // Read the NBT file
            NbtCompound root = NbtIo.readCompressed(legacyFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());

            // The PersistentState format wraps data in a "data" compound
            NbtCompound dataCompound = root;
            if (root.contains("data")) {
                dataCompound = root.getCompoundOrEmpty("data");
            }

            // Legacy SkillGlobalState stores player data under "players" key
            if (dataCompound.contains("players")) {
                return dataCompound.getCompoundOrEmpty("players");
            }

            // Fallback: maybe the data is directly at root level
            return dataCompound;

        } catch (Exception e) {
            LOGGER.error("Failed to load legacy data file", e);
            return null;
        }
    }

    /**
     * Migrates a player's data from legacy NBT format to new PlayerSkillData.
     */
    private static void migratePlayerData(ServerPlayerEntity player, PlayerSkillData data, NbtCompound playerNbt) {
        LOGGER.info("Migrating data for player {} from legacy format", player.getName().getString());

        // Migrate each skill
        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            String skillName = skill.name();
            if (playerNbt.contains(skillName)) {
                NbtCompound skillNbt = playerNbt.getCompoundOrEmpty(skillName);

                int level = skillNbt.getInt("level", 0);
                double xp = skillNbt.getDouble("xp", 0.0);
                long lastAbilityUse = skillNbt.getLong("lastAbilityUse", -1L);
                int prestige = skillNbt.getInt("prestige", 0);

                data.setSkill(skill, level, xp, lastAbilityUse, prestige);

                LOGGER.debug("  Migrated {}: level={}, xp={}, prestige={}", skillName, level, xp, prestige);
            }
        }

        // Migrate paragon skill
        if (playerNbt.contains("paragonSkill")) {
            String paragonName = playerNbt.getString("paragonSkill", "");
            if (!paragonName.isEmpty()) {
                try {
                    data.paragonSkill = MurilloSkillsList.valueOf(paragonName);
                    LOGGER.debug("  Migrated paragon skill: {}", paragonName);
                } catch (Exception ignored) {
                }
            }
        }

        // Migrate selected skills - Legacy format uses compound with "count" +
        // "skill0", "skill1", etc.
        if (playerNbt.contains("selectedSkills")) {
            NbtCompound selectedNbt = playerNbt.getCompoundOrEmpty("selectedSkills");
            int count = selectedNbt.getInt("count", 0);
            List<MurilloSkillsList> selected = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                try {
                    String skillName = selectedNbt.getString("skill" + i, "");
                    if (!skillName.isEmpty()) {
                        selected.add(MurilloSkillsList.valueOf(skillName));
                    }
                } catch (Exception ignored) {
                }
            }

            if (!selected.isEmpty()) {
                data.selectedSkills = selected;
                LOGGER.debug("  Migrated selected skills: {}", selected);
            }
        }

        // Migrate skill toggles if present
        if (playerNbt.contains("skillToggles")) {
            NbtCompound togglesNbt = playerNbt.getCompoundOrEmpty("skillToggles");
            for (String key : togglesNbt.getKeys()) {
                data.skillToggles.put(key, togglesNbt.getBoolean(key, false));
            }
            LOGGER.debug("  Migrated {} skill toggles", data.skillToggles.size());
        }

        // Migrate achievement stats if present
        if (playerNbt.contains("achievementStats")) {
            NbtCompound statsNbt = playerNbt.getCompoundOrEmpty("achievementStats");
            for (String key : statsNbt.getKeys()) {
                data.achievementStats.put(key, statsNbt.getInt(key, 0));
            }
            LOGGER.debug("  Migrated {} achievement stats", data.achievementStats.size());
        }
    }
}
