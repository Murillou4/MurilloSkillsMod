package com.murilloskills.mixin.client;

import com.murilloskills.network.TomsStorageDropC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "com.tom.storagemod.screen.AbstractStorageTerminalScreen", remap = false)
public abstract class TomsStorageTerminalBulkActionsMixin {
    @Shadow(remap = false)
    protected int slotIDUnderMouse;

    @Unique
    private static final int BULK_PULL_INTERVAL_TICKS = 2;
    @Unique
    private static final int DROP_INTERVAL_TICKS = 2;
    @Unique
    private static final String SHIFT_PULL_ACTION_CLASS = "com.tom.storagemod.util.TerminalSyncManager$SlotAction";

    @Unique
    private boolean murilloskills$bulkPullHeld;
    @Unique
    private boolean murilloskills$bulkDropHeld;
    @Unique
    private int murilloskills$bulkPullCooldown;
    @Unique
    private int murilloskills$bulkDropCooldown;
    @Unique
    private boolean murilloskills$reflectionDisabled;

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void murilloskills$startBulkPull(@Coerce Object mouseInput, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (murilloskills$getButton(mouseInput) != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || !murilloskills$isShiftDown()
                || murilloskills$isControlDown()
                || !murilloskills$hasHoveredStoredStack()) {
            return;
        }

        murilloskills$sendTomShiftPull();
        murilloskills$bulkPullHeld = true;
        murilloskills$bulkPullCooldown = BULK_PULL_INTERVAL_TICKS;
        cir.setReturnValue(true);
    }

    @Inject(method = "method_25404", at = @At("HEAD"), cancellable = true, remap = false)
    private void murilloskills$startBulkDrop(@Coerce Object keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (!murilloskills$isControlDown()
                || !murilloskills$isDropKey(murilloskills$getKeyCode(keyInput))
                || !murilloskills$hasHoveredStoredStack()) {
            return;
        }

        murilloskills$sendStorageDrop();
        murilloskills$bulkDropHeld = true;
        murilloskills$bulkDropCooldown = DROP_INTERVAL_TICKS;
        cir.setReturnValue(true);
    }

    @Inject(method = "method_37432", at = @At("TAIL"), remap = false, require = 0)
    private void murilloskills$tickBulkActions(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        if (murilloskills$bulkPullHeld) {
            if (!murilloskills$isShiftDown()
                    || murilloskills$isControlDown()
                    || !murilloskills$isLeftMouseHeld(client)
                    || !murilloskills$hasHoveredStoredStack()) {
                murilloskills$bulkPullHeld = false;
            } else if (--murilloskills$bulkPullCooldown <= 0) {
                murilloskills$sendTomShiftPull();
                murilloskills$bulkPullCooldown = BULK_PULL_INTERVAL_TICKS;
            }
        }

        if (murilloskills$bulkDropHeld) {
            if (!murilloskills$isControlDropHeld(client) || !murilloskills$hasHoveredStoredStack()) {
                murilloskills$bulkDropHeld = false;
            } else if (--murilloskills$bulkDropCooldown <= 0) {
                murilloskills$sendStorageDrop();
                murilloskills$bulkDropCooldown = DROP_INTERVAL_TICKS;
            }
        }
    }

    @Unique
    private boolean murilloskills$isLeftMouseHeld(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Unique
    private boolean murilloskills$isControlDropHeld(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        return murilloskills$isControlDown(handle)
                && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_Q) == GLFW.GLFW_PRESS;
    }

    @Unique
    private boolean murilloskills$isDropKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_Q;
    }

    @Unique
    private boolean murilloskills$isShiftDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Unique
    private boolean murilloskills$isControlDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        return murilloskills$isControlDown(client.getWindow().getHandle());
    }

    @Unique
    private boolean murilloskills$isControlDown(long handle) {
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Unique
    private void murilloskills$sendStorageDrop() {
        if (this.slotIDUnderMouse < 0 || !murilloskills$hasHoveredStoredStack()) {
            return;
        }
        ClientPlayNetworking.send(new TomsStorageDropC2SPayload(this.slotIDUnderMouse));
    }

    @Unique
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void murilloskills$sendTomShiftPull() {
        if (murilloskills$reflectionDisabled) {
            return;
        }
        try {
            Object storedStack = murilloskills$getHoveredStoredStack();
            if (storedStack == null || murilloskills$getQuantity(storedStack) <= 0) {
                return;
            }
            Object shiftPullAction = Enum.valueOf(
                    Class.forName(SHIFT_PULL_ACTION_CLASS).asSubclass(Enum.class),
                    "SHIFT_PULL");
            Method method = murilloskills$findMethod(getClass(), "storageSlotClick", 3);
            method.setAccessible(true);
            method.invoke(this, storedStack, shiftPullAction, true);
        } catch (Exception e) {
            murilloskills$reflectionDisabled = true;
        }
    }

    @Unique
    private boolean murilloskills$hasHoveredStoredStack() {
        try {
            Object storedStack = murilloskills$getHoveredStoredStack();
            return storedStack != null && murilloskills$getQuantity(storedStack) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private Object murilloskills$getHoveredStoredStack() throws ReflectiveOperationException {
        if (this.slotIDUnderMouse < 0) {
            return null;
        }
        Object handler = murilloskills$getHandler();
        if (handler == null) {
            return null;
        }
        Object slot = murilloskills$invoke(handler, "getSlotByID", this.slotIDUnderMouse);
        if (slot == null) {
            return null;
        }
        return murilloskills$readField(slot, "stack");
    }

    @Unique
    private long murilloskills$getQuantity(Object storedStack) throws ReflectiveOperationException {
        Object result = murilloskills$invoke(storedStack, "getQuantity");
        return result instanceof Number number ? number.longValue() : 0L;
    }

    @Unique
    private Object murilloskills$getHandler() throws ReflectiveOperationException {
        try {
            return murilloskills$readField(this, "field_2797");
        } catch (NoSuchFieldException ignored) {
            return murilloskills$readField(this, "handler");
        }
    }

    @Unique
    private int murilloskills$getButton(Object mouseInput) {
        return murilloskills$readIntMethod(mouseInput, "method_74245", "button");
    }

    @Unique
    private int murilloskills$getKeyCode(Object keyInput) {
        return murilloskills$readIntMethod(keyInput, "comp_4795", "key");
    }

    @Unique
    private int murilloskills$readIntMethod(Object owner, String... methodNames) {
        if (owner == null) {
            return Integer.MIN_VALUE;
        }
        for (String methodName : methodNames) {
            try {
                Object result = owner.getClass().getMethod(methodName).invoke(owner);
                if (result instanceof Number number) {
                    return number.intValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return Integer.MIN_VALUE;
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
    private Method murilloskills$findMethod(Class<?> type, String methodName, int parameterCount)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
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
