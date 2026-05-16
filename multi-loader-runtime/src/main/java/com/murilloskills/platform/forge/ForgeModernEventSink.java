package com.murilloskills.platform.forge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeModernEventSink {
    private final MurilloSkillsRuntime runtime;

    ForgeModernEventSink(MurilloSkillsRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void onEvent(Event event) {
        runtime.events().handleEvent(event);
    }
}
