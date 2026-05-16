package com.murilloskills.platform.forge;

import com.murilloskills.runtime.MurilloSkillsRuntime;

final class ForgeRuntimeRegistration {
    private ForgeRuntimeRegistration() {
    }

    static void registerModern(MurilloSkillsRuntime runtime) {
        register(runtime, "com.murilloskills.platform.forge.ForgeModernEventSink");
    }

    static void registerLegacy(MurilloSkillsRuntime runtime) {
        register(runtime, "com.murilloskills.platform.forge.ForgeLegacyEventSink");
    }

    private static void register(MurilloSkillsRuntime runtime, String sinkClassName) {
        try {
            Class<?> minecraftForge = Class.forName("net.minecraftforge.common.MinecraftForge");
            Object eventBus = minecraftForge.getField("EVENT_BUS").get(null);
            java.lang.reflect.Constructor<?> constructor = Class.forName(sinkClassName).getDeclaredConstructor(MurilloSkillsRuntime.class);
            constructor.setAccessible(true);
            Object sink = constructor.newInstance(runtime);
            eventBus.getClass().getMethod("register", Object.class).invoke(eventBus, sink);
            runtime.log("Forge event bridge registered");
        } catch (Throwable ex) {
            runtime.log("Forge event bridge unavailable: " + ex.getClass().getSimpleName());
        }
    }
}
