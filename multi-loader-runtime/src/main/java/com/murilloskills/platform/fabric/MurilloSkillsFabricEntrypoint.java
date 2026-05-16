package com.murilloskills.platform.fabric;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.fabricmc.api.ModInitializer;

public final class MurilloSkillsFabricEntrypoint implements ModInitializer {
    private final String minecraftVersion;

    public MurilloSkillsFabricEntrypoint() {
        this("unspecified");
    }

    public MurilloSkillsFabricEntrypoint(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public void onInitialize() {
        MurilloSkillsRuntime.bootstrap("fabric", minecraftVersion).registerFabricHooks();
    }
}
