package com.murilloskills.platform.forge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ForgeLegacyEventSink {
    private final MurilloSkillsRuntime runtime;

    ForgeLegacyEventSink(MurilloSkillsRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void onEvent(Event event) {
        runtime.events().handleEvent(event);
    }
}
