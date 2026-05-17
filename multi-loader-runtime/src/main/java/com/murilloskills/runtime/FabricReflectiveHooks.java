package com.murilloskills.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class FabricReflectiveHooks {
    private FabricReflectiveHooks() {
    }

    static void register(MurilloSkillsRuntime runtime) {
        registerEvent(runtime, "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", "SERVER_STARTED",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted", "server-started");
        registerEvent(runtime, "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", "SERVER_STOPPING",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStopping", "server-stopping");
        registerEvent(runtime, "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents", "END_SERVER_TICK",
                "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick", "server-tick-end");
        if (!registerEvent(runtime, "net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback", "EVENT",
                "net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback", "command-registration")) {
            registerEvent(runtime, "net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback", "EVENT",
                    "net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback", "command-registration");
        }
        registerEvent(runtime, "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents", "JOIN",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Join", "player-join");
        registerEvent(runtime, "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents", "DISCONNECT",
                "net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Disconnect", "player-disconnect");
        registerEvent(runtime, "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents", "AFTER",
                "net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents$After", "block-break");
        registerEvent(runtime, "net.fabricmc.fabric.api.event.player.UseBlockCallback", "EVENT",
                "net.fabricmc.fabric.api.event.player.UseBlockCallback", "use-block");
        registerEvent(runtime, "net.fabricmc.fabric.api.event.player.AttackEntityCallback", "EVENT",
                "net.fabricmc.fabric.api.event.player.AttackEntityCallback", "attack-entity");
        registerEvent(runtime, "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents",
                "AFTER_KILLED_OTHER_ENTITY",
                "net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents$AfterKilledOtherEntity",
                "entity-kill");

        registerEvent(runtime, "net.legacyfabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", "SERVER_STARTED",
                "net.legacyfabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted", "server-started");
        registerEvent(runtime, "net.legacyfabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", "SERVER_STOPPING",
                "net.legacyfabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStopping", "server-stopping");
        registerEvent(runtime, "net.legacyfabric.fabric.api.event.lifecycle.v1.ServerTickEvents", "END_SERVER_TICK",
                "net.legacyfabric.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick", "server-tick-end");
        registerEvent(runtime, "net.legacyfabric.fabric.api.networking.v1.ServerPlayConnectionEvents", "JOIN",
                "net.legacyfabric.fabric.api.networking.v1.ServerPlayConnectionEvents$Join", "player-join");
        registerEvent(runtime, "net.legacyfabric.fabric.api.networking.v1.ServerPlayConnectionEvents", "DISCONNECT",
                "net.legacyfabric.fabric.api.networking.v1.ServerPlayConnectionEvents$Disconnect", "player-disconnect");
    }

    private static boolean registerEvent(final MurilloSkillsRuntime runtime, String holderClassName, String fieldName,
                                         String listenerClassName, final String callbackName) {
        try {
            Class<?> holder = Class.forName(holderClassName);
            Class<?> listener = Class.forName(listenerClassName);
            Field field = holder.getField(fieldName);
            Object event = field.get(null);
            Method register = findRegister(event.getClass());
            register.setAccessible(true);
            Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] {listener}, new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if (!method.getDeclaringClass().equals(Object.class)) {
                        runtime.events().handleFabricCallback(callbackName, args);
                    }
                    return defaultReturn(method);
                }
            });
            register.invoke(event, proxy);
            runtime.log("Fabric hook registered: " + callbackName);
            return true;
        } catch (Throwable ex) {
            runtime.log("Fabric hook unavailable: " + callbackName + " (" + holderClassName + ": "
                    + ex.getClass().getSimpleName() + ")");
            return false;
        }
    }

    private static Object defaultReturn(Method method) {
        Class<?> type = method.getReturnType();
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (type == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (type == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (type == Short.TYPE) {
            return Short.valueOf((short) 0);
        }
        if (type == Byte.TYPE) {
            return Byte.valueOf((byte) 0);
        }
        if (type == Float.TYPE) {
            return Float.valueOf(0.0f);
        }
        if (type == Double.TYPE) {
            return Double.valueOf(0.0d);
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    if ("PASS".equals(String.valueOf(constant))) {
                        return constant;
                    }
                }
                if (constants.length > 0) {
                    return constants[0];
                }
            }
        }
        return null;
    }

    private static Method findRegister(Class<?> eventClass) throws NoSuchMethodException {
        for (Method method : eventClass.getMethods()) {
            if ("register".equals(method.getName()) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException("register");
    }
}
