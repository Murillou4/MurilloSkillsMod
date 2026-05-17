package com.murilloskills.platform.fabric;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import com.murilloskills.runtime.RuntimeClientBridge;
import net.fabricmc.api.ClientModInitializer;

public final class MurilloSkillsFabricClientEntrypoint implements ClientModInitializer {
    public void onInitializeClient() {
        MurilloSkillsRuntime runtime = MurilloSkillsRuntime.get();
        runtime.log("client entrypoint loaded");
        RuntimeClientBridge.registerFabricClient(runtime);
    }
}
