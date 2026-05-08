package com.murilloskills.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "com.tom.storagemod.menu.StorageTerminalMenu", remap = false)
public abstract class TomsStorageTerminalDropMixin {
    @Unique
    private static final Logger MURILLOSKILLS_LOGGER = LoggerFactory.getLogger("MurilloSkills-TomsStorageDrop");

    @Inject(
            method = "onInteract(Lcom/tom/storagemod/inventory/StoredItemStack;Lcom/tom/storagemod/util/TerminalSyncManager$SlotAction;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private void murilloskills$dropHoveredStack(@Coerce Object storedStack, @Coerce Object slotAction, boolean marker,
            CallbackInfo ci) {
        if (!marker || storedStack == null || !murilloskills$isCraftMarker(slotAction)) {
            return;
        }

        try {
            Object terminal = murilloskills$readField(this, "te");
            ServerPlayerEntity player = murilloskills$getServerPlayer();
            if (terminal == null || player == null || !murilloskills$canInteractWithTerminal(terminal, player)) {
                ci.cancel();
                return;
            }

            int amount = Math.max(1, murilloskills$getMaxStackSize(storedStack));
            Object pulledStack = murilloskills$invoke(terminal, "pullStack", storedStack, (long) amount);
            if (pulledStack == null) {
                ci.cancel();
                return;
            }

            Object actualStack = murilloskills$invoke(pulledStack, "getActualStack");
            if (actualStack instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                player.dropItem(itemStack, false, true);
                player.currentScreenHandler.sendContentUpdates();
            }
            ci.cancel();
        } catch (Exception e) {
            MURILLOSKILLS_LOGGER.debug("Could not drop a Tom's Simple Storage stack from terminal", e);
            ci.cancel();
        }
    }

    @Unique
    private boolean murilloskills$isCraftMarker(Object slotAction) {
        return slotAction instanceof Enum<?> action && "CRAFT".equals(action.name());
    }

    @Unique
    private ServerPlayerEntity murilloskills$getServerPlayer() throws ReflectiveOperationException {
        Object inventory = murilloskills$readField(this, "pinv");
        Object player;
        try {
            player = murilloskills$readField(inventory, "field_7546");
        } catch (NoSuchFieldException ignored) {
            player = murilloskills$readField(inventory, "player");
        }
        return player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
    }

    @Unique
    private boolean murilloskills$canInteractWithTerminal(Object terminal, ServerPlayerEntity player)
            throws ReflectiveOperationException {
        try {
            Object result = terminal.getClass()
                    .getMethod("canInteractWith", PlayerEntity.class, boolean.class)
                    .invoke(terminal, player, true);
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException ignored) {
            return true;
        }
    }

    @Unique
    private int murilloskills$getMaxStackSize(Object storedStack) throws ReflectiveOperationException {
        Object result = murilloskills$invoke(storedStack, "getMaxStackSize");
        return result instanceof Number number ? number.intValue() : 64;
    }

    @Unique
    private Object murilloskills$readField(Object owner, String fieldName) throws ReflectiveOperationException {
        Field field = murilloskills$findField(owner.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(owner);
    }

    @Unique
    private Field murilloskills$findField(Class<?> type, String fieldName) throws NoSuchFieldException {
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

    @Unique
    private Object murilloskills$invoke(Object owner, String methodName, Object... args)
            throws ReflectiveOperationException {
        Method method = murilloskills$findMethod(owner.getClass(), methodName, args);
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

    @Unique
    private Method murilloskills$findMethod(Class<?> type, String methodName, Object... args)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && murilloskills$accepts(method.getParameterTypes(), args)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }

    @Unique
    private boolean murilloskills$accepts(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] == null) {
                continue;
            }
            Class<?> parameterType = murilloskills$wrap(parameterTypes[i]);
            if (!parameterType.isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private Class<?> murilloskills$wrap(Class<?> type) {
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
