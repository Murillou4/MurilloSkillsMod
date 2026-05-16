package com.murilloskills.platform.forge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.minecraftforge.fml.common.Mod;

@Mod("murilloskills")
public final class MurilloSkillsForgeEntrypoint {
    public MurilloSkillsForgeEntrypoint() {
        MurilloSkillsRuntime runtime = MurilloSkillsRuntime.bootstrap("forge", "unspecified");
        ForgeRuntimeRegistration.registerModern(runtime);
    }
}
