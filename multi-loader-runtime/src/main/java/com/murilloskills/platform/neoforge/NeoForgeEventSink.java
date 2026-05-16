package com.murilloskills.platform.neoforge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;

public final class NeoForgeEventSink {
    private final MurilloSkillsRuntime runtime;

    NeoForgeEventSink(MurilloSkillsRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void onEvent(Event event) {
        runtime.events().handleEvent(event);
    }
}
