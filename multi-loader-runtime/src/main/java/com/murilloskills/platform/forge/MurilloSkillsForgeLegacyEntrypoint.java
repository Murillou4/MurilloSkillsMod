package com.murilloskills.platform.forge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "murilloskills", name = "Murillo Skills", version = "1.2.74")
public final class MurilloSkillsForgeLegacyEntrypoint {
    public MurilloSkillsForgeLegacyEntrypoint() {
        MurilloSkillsRuntime runtime = MurilloSkillsRuntime.bootstrap("forge", "1.12.2");
        ForgeRuntimeRegistration.registerLegacy(runtime);
    }
}
