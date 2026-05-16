package com.murilloskills.platform.fabric;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.fabricmc.api.ClientModInitializer;

public final class MurilloSkillsFabricClientEntrypoint implements ClientModInitializer {
    public void onInitializeClient() {
        MurilloSkillsRuntime.get().log("client entrypoint loaded");
    }
}
