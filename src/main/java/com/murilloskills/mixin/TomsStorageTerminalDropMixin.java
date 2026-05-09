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
    @Unique
    private static final int MAX_SAFE_DROP_STACK = 64;

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

            Object liveStack = murilloskills$findLiveStorageStack(storedStack);
            if (liveStack == null) {
                ci.cancel();
                return;
            }

            ItemStack itemKey = murilloskills$readItemStack(liveStack);
            if (itemKey == null || itemKey.isEmpty()) {
                ci.cancel();
                return;
            }

            int amount = murilloskills$getSafeDropAmount(liveStack, itemKey);
            Object pulledStack = murilloskills$invoke(terminal, "pullStack", liveStack, (long) amount);
            if (pulledStack == null) {
                ci.cancel();
                return;
            }

            Object actualStack = murilloskills$invoke(pulledStack, "getActualStack");
            if (actualStack instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                if (itemStack.getCount() > amount) {
                    ItemStack overflow = itemStack.copy();
                    overflow.setCount(itemStack.getCount() - amount);
                    itemStack.setCount(amount);
                    murilloskills$returnOverflowToTerminal(terminal, overflow);
                }
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
    private Object murilloskills$findLiveStorageStack(Object requestedStack) throws ReflectiveOperationException {
        ItemStack requestedItem = murilloskills$readItemStack(requestedStack);
        if (requestedItem == null || requestedItem.isEmpty()) {
            return null;
        }
        Object itemList = murilloskills$readField(this, "itemList");
        if (!(itemList instanceof Iterable<?> entries)) {
            return null;
        }
        for (Object entry : entries) {
            if (entry == null || murilloskills$getQuantity(entry) <= 0) {
                continue;
            }
            ItemStack candidate = murilloskills$readItemStack(entry);
            if (candidate != null && !candidate.isEmpty()
                    && ItemStack.areItemsAndComponentsEqual(candidate, requestedItem)) {
                return entry;
            }
        }
        return null;
    }

    @Unique
    private ItemStack murilloskills$readItemStack(Object storedStack) throws ReflectiveOperationException {
        try {
            Object stack = murilloskills$readField(storedStack, "stack");
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        } catch (NoSuchFieldException ignored) {
            Object stack = murilloskills$invoke(storedStack, "getStack");
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        }
    }

    @Unique
    private int murilloskills$getSafeDropAmount(Object liveStack, ItemStack itemKey) throws ReflectiveOperationException {
        long quantity = Math.max(0L, murilloskills$getQuantity(liveStack));
        int itemMaxCount = Math.max(1, itemKey.getMaxCount());
        long safe = Math.min(quantity, Math.min(itemMaxCount, MAX_SAFE_DROP_STACK));
        return (int) Math.max(1L, safe);
    }

    @Unique
    private long murilloskills$getQuantity(Object storedStack) throws ReflectiveOperationException {
        Object result = murilloskills$invoke(storedStack, "getQuantity");
        return result instanceof Number number ? number.longValue() : 0L;
    }

    @Unique
    private void murilloskills$returnOverflowToTerminal(Object terminal, ItemStack overflow) {
        if (terminal == null || overflow == null || overflow.isEmpty()) {
            return;
        }
        try {
            murilloskills$invoke(terminal, "pushStack", overflow);
        } catch (ReflectiveOperationException e) {
            MURILLOSKILLS_LOGGER.warn("Could not return excess Tom's Storage drop overflow", e);
        }
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
