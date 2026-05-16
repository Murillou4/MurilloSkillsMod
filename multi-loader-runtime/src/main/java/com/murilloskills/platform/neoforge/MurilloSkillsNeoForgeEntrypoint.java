package com.murilloskills.platform.neoforge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.neoforged.fml.common.Mod;

@Mod("murilloskills")
public final class MurilloSkillsNeoForgeEntrypoint {
    public MurilloSkillsNeoForgeEntrypoint() {
        MurilloSkillsRuntime runtime = MurilloSkillsRuntime.bootstrap("neoforge", "1.21.1");
        NeoForgeRuntimeRegistration.register(runtime);
    }
}
