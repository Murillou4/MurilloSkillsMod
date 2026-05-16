package com.murilloskills.platform.neoforge;

import com.murilloskills.runtime.MurilloSkillsRuntime;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

final class NeoForgeRuntimeRegistration {
    private static final String[] EVENT_CLASSES = {
            "net.neoforged.neoforge.event.RegisterCommandsEvent",
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent",
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent",
            "net.neoforged.neoforge.event.server.ServerStoppingEvent",
            "net.neoforged.neoforge.event.tick.ServerTickEvent$Post",
            "net.neoforged.neoforge.event.level.BlockEvent$BreakEvent",
            "net.neoforged.neoforge.event.entity.living.LivingDeathEvent",
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$ItemCraftedEvent",
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$ItemSmeltedEvent"
    };

    private NeoForgeRuntimeRegistration() {
    }

    static void register(MurilloSkillsRuntime runtime) {
        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object eventBus = neoForge.getField("EVENT_BUS").get(null);
            Method addListener = eventBus.getClass().getMethod("addListener", Class.class, Consumer.class);
            Consumer<Object> consumer = event -> runtime.events().handleEvent(event);
            int registered = 0;
            for (String eventClassName : EVENT_CLASSES) {
                Class<?> eventClass = Class.forName(eventClassName);
                addListener.invoke(eventBus, eventClass, consumer);
                registered++;
            }
            runtime.log("NeoForge event bridge registered: " + registered + " events");
        } catch (Throwable ex) {
            Throwable cause = ex instanceof InvocationTargetException && ((InvocationTargetException) ex).getCause() != null
                    ? ((InvocationTargetException) ex).getCause()
                    : ex;
            runtime.log("NeoForge event bridge unavailable: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }
}
