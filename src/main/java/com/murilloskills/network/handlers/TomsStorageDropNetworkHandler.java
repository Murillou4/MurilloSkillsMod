package com.murilloskills.network.handlers;

import com.murilloskills.network.TomsStorageDropC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional Tom's Simple Storage compat. Reflection keeps MurilloSkills usable
 * without a hard dependency on Tom's classes.
 */
public final class TomsStorageDropNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-TomsStorageDrop");
    private static final String STORAGE_MENU_CLASS = "com.tom.storagemod.menu.StorageTerminalMenu";
    private static final Map<UUID, Long> LAST_DROP_TICK = new ConcurrentHashMap<>();

    private TomsStorageDropNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<TomsStorageDropC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                ServerPlayerEntity player = context.player();
                ItemStack sampleStack = payload.sampleStack();
                if (!canProcessDrop(player, sampleStack)) {
                    return;
                }

                Object menu = player.currentScreenHandler;
                Object terminal = readField(menu, "te");
                if (terminal == null || !canInteractWithTerminal(terminal, player)) {
                    return;
                }

                Object storedStack = findStoredStack(terminal, sampleStack);
                if (storedStack == null || getQuantity(storedStack) <= 0) {
                    return;
                }

                int amount = Math.max(1, getMaxStackSize(storedStack));
                Object pulledStack = invoke(terminal, "pullStack", storedStack, (long) amount);
                if (pulledStack == null) {
                    return;
                }

                Object actualStack = invoke(pulledStack, "getActualStack");
                if (!(actualStack instanceof ItemStack itemStack) || itemStack.isEmpty()) {
                    return;
                }

                player.dropItem(itemStack, false, true);
                player.currentScreenHandler.sendContentUpdates();
            } catch (Exception e) {
                LOGGER.debug("Could not drop a Tom's Simple Storage stack from terminal", e);
            }
        });
    }

    private static boolean canProcessDrop(ServerPlayerEntity player, ItemStack sampleStack) {
        if (sampleStack == null || sampleStack.isEmpty() || !isStorageTerminalMenu(player.currentScreenHandler)) {
            return false;
        }
        long worldTime = player.getEntityWorld().getTime();
        Long previous = LAST_DROP_TICK.put(player.getUuid(), worldTime);
        return previous == null || previous.longValue() != worldTime;
    }

    private static boolean isStorageTerminalMenu(ScreenHandler handler) {
        if (handler == null) {
            return false;
        }
        Class<?> type = handler.getClass();
        while (type != null) {
            if (STORAGE_MENU_CLASS.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean canInteractWithTerminal(Object terminal, ServerPlayerEntity player)
            throws ReflectiveOperationException {
        try {
            Object result = terminal.getClass()
                    .getMethod("canInteractWith", net.minecraft.entity.player.PlayerEntity.class, boolean.class)
                    .invoke(terminal, player, true);
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException ignored) {
            return true;
        }
    }

    private static long getQuantity(Object storedStack) throws ReflectiveOperationException {
        Object result = invoke(storedStack, "getQuantity");
        return result instanceof Number number ? number.longValue() : 0L;
    }

    private static int getMaxStackSize(Object storedStack) throws ReflectiveOperationException {
        Object result = invoke(storedStack, "getMaxStackSize");
        return result instanceof Number number ? number.intValue() : 64;
    }

    private static Object findStoredStack(Object terminal, ItemStack sampleStack) throws ReflectiveOperationException {
        Object stacks = invoke(terminal, "getStacks");
        if (!(stacks instanceof Map<?, ?> map) || map.isEmpty()) {
            return null;
        }

        Object sampleStoredStack = createStoredItemStack(sampleStack);
        Object fallback = null;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object candidate = entry.getValue() != null ? entry.getValue() : entry.getKey();
            if (candidate == null || getQuantity(candidate) <= 0) {
                continue;
            }
            if (matchesStoredStack(candidate, sampleStoredStack, sampleStack)) {
                return candidate;
            }
            if (fallback == null && matchesItemOnly(candidate, sampleStack)) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private static Object createStoredItemStack(ItemStack sampleStack) throws ReflectiveOperationException {
        Class<?> storedStackClass = Class.forName("com.tom.storagemod.inventory.StoredItemStack");
        Constructor<?> constructor = storedStackClass.getConstructor(ItemStack.class);
        ItemStack sample = sampleStack.copy();
        sample.setCount(1);
        return constructor.newInstance(sample);
    }

    private static boolean matchesStoredStack(Object candidate, Object sampleStoredStack, ItemStack sampleStack)
            throws ReflectiveOperationException {
        Object equalDetails = invoke(candidate, "equalDetails", sampleStoredStack);
        if (Boolean.TRUE.equals(equalDetails)) {
            return true;
        }
        Object candidateStack = invoke(candidate, "getStack");
        return candidateStack instanceof ItemStack itemStack && ItemStack.areItemsAndComponentsEqual(itemStack, sampleStack);
    }

    private static boolean matchesItemOnly(Object candidate, ItemStack sampleStack) throws ReflectiveOperationException {
        Object candidateStack = invoke(candidate, "getStack");
        return candidateStack instanceof ItemStack itemStack && itemStack.isOf(sampleStack.getItem());
    }

    private static Object readField(Object owner, String fieldName) throws ReflectiveOperationException {
        Field field = findField(owner.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static Object invoke(Object owner, String methodName, Object... args) throws ReflectiveOperationException {
        Method method = findMethod(owner.getClass(), methodName, args);
        method.setAccessible(true);
        try {
            return method.invoke(owner, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw e;
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Object[] args) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && accepts(method.getParameterTypes(), args)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }

    private static boolean accepts(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] == null) {
                continue;
            }
            Class<?> parameterType = wrap(parameterTypes[i]);
            if (!parameterType.isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }
}
