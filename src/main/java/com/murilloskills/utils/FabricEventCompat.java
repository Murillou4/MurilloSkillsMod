package com.murilloskills.utils;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BiConsumer;

public final class FabricEventCompat {
    private FabricEventCompat() {
    }

    public static void registerAfterKilledOtherEntity(final BiConsumer<Entity, LivingEntity> handler) {
        try {
            Object event = ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY;
            Object invoker = event.getClass().getMethod("invoker").invoke(event);
            Class<?> callbackType = firstInterface(invoker);
            Method register = registerMethod(event);
            Object proxy = Proxy.newProxyInstance(
                    callbackType.getClassLoader(),
                    new Class<?>[] { callbackType },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if (method.getDeclaringClass() == Object.class) {
                                return objectMethod(proxy, method, args);
                            }
                            if (args != null && args.length >= 3
                                    && args[1] instanceof Entity entity
                                    && args[2] instanceof LivingEntity killedEntity) {
                                handler.accept(entity, killedEntity);
                            }
                            return null;
                        }
                    });
            register.invoke(event, proxy);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not register Fabric combat event", error);
        }
    }

    private static Class<?> firstInterface(Object invoker) {
        for (Class<?> candidate : invoker.getClass().getInterfaces()) {
            if (candidate != Object.class) {
                return candidate;
            }
        }
        throw new IllegalStateException("Fabric event invoker has no callback interface");
    }

    private static Method registerMethod(Object event) throws NoSuchMethodException {
        for (Method method : event.getClass().getMethods()) {
            if ("register".equals(method.getName()) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("register");
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "MurilloSkillsAfterKilledOtherEntity";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(name)) {
            return proxy == (args == null ? null : args[0]);
        }
        return null;
    }
}
