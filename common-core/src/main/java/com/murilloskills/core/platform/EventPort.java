package com.murilloskills.core.platform;

public interface EventPort {
    void registerLifecycleEvents();

    void registerGameplayEvents();

    void registerClientEvents();
}
