package com.murilloskills.runtime;

import java.util.UUID;

final class PlayerIdentity {
    static final PlayerIdentity UNKNOWN = new PlayerIdentity(null, "unknown");

    private final UUID uuid;
    private final String name;

    PlayerIdentity(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name == null ? "unknown" : name;
    }

    boolean hasUuid() {
        return uuid != null;
    }

    UUID getUuid() {
        return uuid;
    }

    String getName() {
        return name;
    }
}
