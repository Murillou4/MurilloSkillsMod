package com.murilloskills.data;

import com.murilloskills.MurilloSkills;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.io.PlayerSkillJsonCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlayerSkillJsonStorage {
    private static final String SAVE_DIR = "murilloskills/players";

    private PlayerSkillJsonStorage() {
    }

    public static boolean loadIfPresent(ServerPlayerEntity player, MinecraftServer server) {
        Path path = playerPath(server, player);
        if (!Files.exists(path)) {
            return false;
        }

        try {
            PlayerSkillDataCore core = PlayerSkillJsonCodec.read(path);
            PlayerSkillData current = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            PlayerSkillCoreAdapter.copyInto(PlayerSkillCoreAdapter.fromCore(core), current);
            MurilloSkills.LOGGER.debug("Loaded MurilloSkills JSON data for {} from {}",
                    player.getName().getString(), path);
            return true;
        } catch (Exception e) {
            MurilloSkills.LOGGER.error("Failed to load MurilloSkills JSON data for {} from {}",
                    player.getName().getString(), path, e);
            return false;
        }
    }

    public static boolean exists(ServerPlayerEntity player, MinecraftServer server) {
        return Files.exists(playerPath(server, player));
    }

    public static void save(ServerPlayerEntity player, MinecraftServer server) {
        if (server == null) {
            return;
        }
        try {
            PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            data.normalizeParagonState();
            PlayerSkillJsonCodec.write(playerPath(server, player), PlayerSkillCoreAdapter.toCore(data));
        } catch (IOException e) {
            MurilloSkills.LOGGER.error("Failed to save MurilloSkills JSON data for {}",
                    player.getName().getString(), e);
        }
    }

    public static Path playerPath(MinecraftServer server, ServerPlayerEntity player) {
        return server.getSavePath(WorldSavePath.ROOT)
                .resolve(SAVE_DIR)
                .resolve(player.getUuidAsString() + ".json");
    }
}
