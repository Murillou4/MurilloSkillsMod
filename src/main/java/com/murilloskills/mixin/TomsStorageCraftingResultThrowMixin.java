package com.murilloskills.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(ScreenHandler.class)
public abstract class TomsStorageCraftingResultThrowMixin {
    @Unique
    private static final String MURILLOSKILLS_TOM_CRAFTING_MENU =
            "com.tom.storagemod.menu.CraftingTerminalMenu";

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void murilloskills$blockTomsCraftingResultThrow(int slotIndex, int button,
            SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (actionType != SlotActionType.THROW || slotIndex < 0) {
            return;
        }
        Object handler = this;
        if (!MURILLOSKILLS_TOM_CRAFTING_MENU.equals(handler.getClass().getName())) {
            return;
        }
        try {
            Slot resultSlot = murilloskills$getCraftingResultSlot(handler);
            if (resultSlot != null && murilloskills$getSlotId(resultSlot) == slotIndex) {
                if (player != null) {
                    player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.throw_blocked"), true);
                }
                ci.cancel();
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Unique
    private Slot murilloskills$getCraftingResultSlot(Object handler) throws ReflectiveOperationException {
        Object slot = murilloskills$invoke(handler, "getCraftingResultSlot");
        return slot instanceof Slot resultSlot ? resultSlot : null;
    }

    @Unique
    private int murilloskills$getSlotId(Slot slot) throws ReflectiveOperationException {
        try {
            Object id = murilloskills$readField(slot, "id");
            return id instanceof Number number ? number.intValue() : -1;
        } catch (NoSuchFieldException ignored) {
            Object id = murilloskills$readField(slot, "field_7874");
            return id instanceof Number number ? number.intValue() : -1;
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
                if (method.getName().equals(methodName)
                        && method.getParameterCount() == args.length) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }
}
