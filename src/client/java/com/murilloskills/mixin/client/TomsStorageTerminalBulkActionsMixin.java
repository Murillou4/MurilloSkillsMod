package com.murilloskills.mixin.client;

import com.murilloskills.data.TerminalMachineTargetClientState;
import com.murilloskills.gui.TerminalMachineTransferAmountScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
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
    private static final String TOM_SLOT_ACTION_CLASS = "com.tom.storagemod.util.TerminalSyncManager$SlotAction";

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
    // ItemStack identity captured when CTRL+Q starts. We don't pin the StoredItemStack
    // reference itself because Tom's network layer encodes the action by an integer ID
    // looked up in idMap via .equals(), and StoredItemStack.equals compares the item AND
    // the count. The count changes every time we drop, so a frozen reference would resolve
    // to a stale (or wrong) ID after the first drop. Instead, we keep the ItemStack and
    // re-resolve the live StoredItemStack from itemList on every send.
    @Unique
    private net.minecraft.item.ItemStack murilloskills$bulkDropItem;

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
        int keyCode = murilloskills$getKeyCode(keyInput);
        if (murilloskills$isControlDown() && murilloskills$isAltDown() && keyCode == GLFW.GLFW_KEY_T) {
            if (murilloskills$openMachineTransferScreen()) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (!murilloskills$isControlDown() || !murilloskills$isDropKey(keyCode)) {
            return;
        }

        if (murilloskills$bulkDropItem != null) {
            Object liveTarget = murilloskills$findLiveTargetFor(murilloskills$bulkDropItem);
            if (liveTarget == null) {
                return;
            }
            murilloskills$sendStorageDrop(liveTarget);
            murilloskills$bulkDropHeld = true;
            murilloskills$bulkDropCooldown = DROP_INTERVAL_TICKS;
            cir.setReturnValue(true);
            return;
        }

        if (murilloskills$captureBulkDropTarget()) {
            cir.setReturnValue(true);
        }
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

        boolean ctrlDropPressed = murilloskills$isControlDropHeld(client);
        if (!ctrlDropPressed) {
            murilloskills$bulkDropHeld = false;
            murilloskills$bulkDropItem = null;
            murilloskills$bulkDropCooldown = 0;
        } else if (murilloskills$bulkDropItem == null) {
            murilloskills$captureBulkDropTarget();
        } else {
            murilloskills$bulkDropHeld = true;
            Object liveTarget = murilloskills$findLiveTargetFor(murilloskills$bulkDropItem);
            if (liveTarget != null && --murilloskills$bulkDropCooldown <= 0) {
                murilloskills$sendStorageDrop(liveTarget);
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
    private boolean murilloskills$captureBulkDropTarget() {
        Object target = murilloskills$resolveHoveredStoredStack();
        if (target == null) {
            return false;
        }

        net.minecraft.item.ItemStack itemKey = murilloskills$readItemStackField(target);
        if (itemKey == null || itemKey.isEmpty()) {
            return false;
        }

        murilloskills$bulkDropItem = itemKey.copy();
        murilloskills$sendStorageDrop(target);
        murilloskills$bulkDropHeld = true;
        murilloskills$bulkDropCooldown = DROP_INTERVAL_TICKS;
        return true;
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
    private boolean murilloskills$isAltDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    @Unique
    private boolean murilloskills$isControlDown(long handle) {
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Unique
    private void murilloskills$sendStorageDrop(Object storedStack) {
        murilloskills$sendTomStorageAction("CRAFT", storedStack);
    }

    @Unique
    private void murilloskills$sendTomShiftPull() {
        Object storedStack = murilloskills$resolveHoveredStoredStack();
        if (storedStack == null) {
            return;
        }
        murilloskills$sendTomStorageAction("SHIFT_PULL", storedStack);
    }

    @Unique
    private boolean murilloskills$openMachineTransferScreen() {
        Object storedStack = murilloskills$resolveHoveredStoredStack();
        if (storedStack == null) {
            return false;
        }
        net.minecraft.item.ItemStack itemKey = murilloskills$readItemStackField(storedStack);
        if (itemKey == null || itemKey.isEmpty()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        BlockPos targetPos = TerminalMachineTargetClientState.getTargetPos();
        Direction targetFace = TerminalMachineTargetClientState.getTargetFace();
        if (targetPos == null || targetFace == null) {
            HitResult target = client.crosshairTarget;
            if (!(target instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
                target = client.player.raycast(12.0D, 0.0F, false);
            }
            if (target instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                targetPos = hit.getBlockPos();
                targetFace = hit.getSide();
            }
        }
        if (targetPos == null || targetFace == null) {
            client.player.sendMessage(Text.translatable("murilloskills.terminal_transfer.no_target"), true);
            return true;
        }
        long available;
        try {
            available = murilloskills$getQuantity(storedStack);
        } catch (ReflectiveOperationException e) {
            available = 1L;
        }
        client.setScreen(new TerminalMachineTransferAmountScreen((Screen) (Object) this, itemKey.copy(), available,
                targetPos, targetFace));
        return true;
    }

    @Unique
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void murilloskills$sendTomStorageAction(String actionName, Object storedStack) {
        if (murilloskills$reflectionDisabled || storedStack == null) {
            return;
        }
        try {
            Object action = Enum.valueOf(
                    Class.forName(TOM_SLOT_ACTION_CLASS).asSubclass(Enum.class),
                    actionName);
            Method method = murilloskills$findMethod(getClass(), "storageSlotClick", 3);
            method.setAccessible(true);
            method.invoke(this, storedStack, action, true);
        } catch (Exception e) {
            murilloskills$reflectionDisabled = true;
        }
    }

    @Unique
    private boolean murilloskills$hasHoveredStoredStack() {
        Object stored = murilloskills$resolveHoveredStoredStack();
        return stored != null;
    }

    @Unique
    private Object murilloskills$resolveHoveredStoredStack() {
        try {
            Object stored = murilloskills$getHoveredStoredStack();
            if (stored == null || murilloskills$getQuantity(stored) <= 0) {
                return null;
            }
            return stored;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Re-find the live StoredItemStack from the menu's itemList that matches the captured
     * ItemStack identity. This must be re-resolved every tick because Tom's network layer
     * encodes the action through an idMap keyed by StoredItemStack, and StoredItemStack
     * equality includes the count field — meaning a frozen reference would resolve to a
     * stale id (or none at all, which encodes as 0 and corresponds to a wrong slot).
     */
    @Unique
    private Object murilloskills$findLiveTargetFor(net.minecraft.item.ItemStack itemKey) {
        if (itemKey == null || itemKey.isEmpty()) {
            return null;
        }
        try {
            Object handler = murilloskills$getHandler();
            if (handler == null) {
                return null;
            }
            Object itemList = murilloskills$readField(handler, "itemList");
            if (itemList instanceof Iterable<?> entries) {
                for (Object entry : entries) {
                    if (entry == null) {
                        continue;
                    }
                    if (murilloskills$getQuantity(entry) <= 0) {
                        continue;
                    }
                    net.minecraft.item.ItemStack candidate = murilloskills$readItemStackField(entry);
                    if (candidate == null || candidate.isEmpty()) {
                        continue;
                    }
                    if (net.minecraft.item.ItemStack.areItemsAndComponentsEqual(candidate, itemKey)) {
                        return entry;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Unique
    private net.minecraft.item.ItemStack murilloskills$readItemStackField(Object storedStack) {
        if (storedStack == null) {
            return null;
        }
        try {
            Object value = murilloskills$readField(storedStack, "stack");
            return value instanceof net.minecraft.item.ItemStack stack ? stack : null;
        } catch (Exception ignored) {
            return null;
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
