package com.murilloskills.runtime;

import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.io.PlayerSkillJsonCodec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class RuntimePlayerStore {
    private final Map<UUID, PlayerSkillDataCore> players = Collections.synchronizedMap(new HashMap<UUID, PlayerSkillDataCore>());
    private Path playerDirectory;

    RuntimePlayerStore(Path playerDirectory) {
        this.playerDirectory = playerDirectory;
    }

    Path getPlayerDirectory() {
        return playerDirectory;
    }

    void setPlayerDirectory(Path playerDirectory) {
        if (playerDirectory != null) {
            this.playerDirectory = playerDirectory;
        }
    }

    PlayerSkillDataCore load(UUID uuid) {
        PlayerSkillDataCore cached = players.get(uuid);
        if (cached != null) {
            return cached;
        }

        Path path = path(uuid);
        PlayerSkillDataCore loaded;
        try {
            loaded = java.nio.file.Files.exists(path) ? PlayerSkillJsonCodec.read(path) : new PlayerSkillDataCore();
        } catch (IOException ex) {
            System.err.println("[MurilloSkills] Failed to read " + path + ": " + ex.getMessage());
            loaded = new PlayerSkillDataCore();
        }
        players.put(uuid, loaded);
        return loaded;
    }

    void put(UUID uuid, PlayerSkillDataCore data) {
        players.put(uuid, data);
    }

    void save(UUID uuid) {
        PlayerSkillDataCore data = players.get(uuid);
        if (data == null) {
            return;
        }
        try {
            PlayerSkillJsonCodec.write(path(uuid), data);
        } catch (IOException ex) {
            System.err.println("[MurilloSkills] Failed to save " + uuid + ": " + ex.getMessage());
        }
    }

    void saveAll() {
        UUID[] ids;
        synchronized (players) {
            ids = players.keySet().toArray(new UUID[players.size()]);
        }
        for (UUID id : ids) {
            save(id);
        }
    }

    private Path path(UUID uuid) {
        return playerDirectory.resolve(uuid.toString() + ".json");
    }
}
