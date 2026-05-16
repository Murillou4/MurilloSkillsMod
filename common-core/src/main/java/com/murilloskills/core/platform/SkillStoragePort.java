package com.murilloskills.core.platform;

import com.murilloskills.core.data.PlayerSkillDataCore;

import java.io.IOException;
import java.util.UUID;

public interface SkillStoragePort {
    PlayerSkillDataCore load(UUID playerId) throws IOException;

    void save(UUID playerId, PlayerSkillDataCore data) throws IOException;
}
