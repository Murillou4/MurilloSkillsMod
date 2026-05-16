package com.murilloskills.core.platform;

public interface CommandPort<C> {
    void registerCommands(C commandContext);
}
