package com.murilloskills.integration;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runs Tom's Storage crafting-terminal bulk jobs in small server-tick batches.
 * It never creates items directly; each step delegates to Tom's existing
 * shift-click craft path after checking that the output can fit in the player's
 * inventory.
 */
public final class TerminalBulkCraftService {
    public static final int MAX_REQUEST_AMOUNT = 100_000;

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-TerminalBulkCraft");
    private static final String TOM_CRAFTING_TERMINAL_MENU = "com.tom.storagemod.menu.CraftingTerminalMenu";
    private static final int CRAFTS_PER_TICK = 16;
    private static final Map<UUID, BulkCraftJob> JOBS = new HashMap<>();

    private TerminalBulkCraftService() {
    }

    public static void start(ServerPlayerEntity player, int requestedAmount) {
        if (player == null) {
            return;
        }
        if (JOBS.containsKey(player.getUuid())) {
            player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.busy"), false);
            return;
        }

        ScreenHandler handler = player.currentScreenHandler;
        if (!isCraftingTerminalMenu(handler)) {
            player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.not_crafting_terminal"), false);
            return;
        }

        try {
            Slot resultSlot = getCraftingResultSlot(handler);
            if (resultSlot == null || !resultSlot.hasStack()) {
                player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.no_result"), false);
                return;
            }

            int slotId = getSlotId(resultSlot);
            if (slotId < 0) {
                player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.failed"), false);
                return;
            }

            int amount = clampAmount(requestedAmount);
            ItemStack resultKey = resultSlot.getStack().copy();
            resultKey.setCount(1);
            JOBS.put(player.getUuid(), new BulkCraftJob(handler, slotId, resultKey, amount));
            player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.started",
                    amount, resultKey.getName()), false);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not inspect Tom's Storage crafting terminal", e);
            player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.failed"), false);
        }
    }

    public static void tick(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        BulkCraftJob job = JOBS.get(player.getUuid());
        if (job == null) {
            return;
        }
        try {
            tickJob(player, job);
        } catch (ReflectiveOperationException | RuntimeException e) {
            JOBS.remove(player.getUuid());
            LOGGER.warn("Terminal bulk craft job failed", e);
            player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.failed"), false);
        }
    }

    public static void cancel(UUID playerId) {
        if (playerId != null) {
            JOBS.remove(playerId);
        }
    }

    private static void tickJob(ServerPlayerEntity player, BulkCraftJob job)
            throws ReflectiveOperationException {
        if (player.currentScreenHandler != job.handler || !isCraftingTerminalMenu(player.currentScreenHandler)) {
            finish(player, job, "cancelled");
            return;
        }

        Slot resultSlot = getCraftingResultSlot(job.handler);
        if (resultSlot == null || getSlotId(resultSlot) != job.resultSlotId) {
            finish(player, job, "cancelled");
            return;
        }

        int actions = 0;
        while (actions < CRAFTS_PER_TICK && job.crafted < job.targetAmount) {
            ItemStack result = resultSlot.getStack();
            if (result.isEmpty()) {
                finish(player, job, job.crafted > 0 ? "partial" : "no_materials");
                return;
            }
            if (!ItemStack.areItemsAndComponentsEqual(result, job.resultKey)) {
                finish(player, job, "result_changed");
                return;
            }

            int resultCount = Math.max(1, result.getCount());
            int remaining = job.targetAmount - job.crafted;
            if (resultCount > remaining && job.crafted > 0) {
                finish(player, job, "partial_exact_guard");
                return;
            }
            if (inventoryCapacity(player, result) < resultCount) {
                finish(player, job, job.crafted > 0 ? "partial_no_space" : "no_space");
                return;
            }

            ItemStack crafted = invokeShiftClick(job.handler, player, job.resultSlotId);
            if (crafted.isEmpty()) {
                finish(player, job, job.crafted > 0 ? "partial" : "no_materials");
                return;
            }
            if (!ItemStack.areItemsAndComponentsEqual(crafted, job.resultKey)) {
                finish(player, job, "result_changed");
                return;
            }

            job.crafted += Math.max(1, resultCount);
            actions++;
        }

        job.handler.sendContentUpdates();
        if (job.crafted >= job.targetAmount) {
            finish(player, job, "success");
        }
    }

    private static void finish(ServerPlayerEntity player, BulkCraftJob job, String reason) {
        JOBS.remove(player.getUuid());
        job.handler.sendContentUpdates();

        Text itemName = job.resultKey.getName();
        switch (reason) {
            case "success" -> player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.success",
                    job.crafted, itemName), false);
            case "no_space" -> player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.no_space",
                    itemName), false);
            case "no_materials" -> player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.no_materials",
                    itemName), false);
            case "result_changed" -> player.sendMessage(Text.translatable(
                    "murilloskills.terminal_bulk_craft.result_changed"), false);
            case "cancelled" -> player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.cancelled",
                    job.crafted, job.targetAmount, itemName), false);
            case "partial_no_space" -> player.sendMessage(Text.translatable(
                    "murilloskills.terminal_bulk_craft.partial_no_space", job.crafted, job.targetAmount, itemName),
                    false);
            case "partial_exact_guard" -> player.sendMessage(Text.translatable(
                    "murilloskills.terminal_bulk_craft.partial_exact_guard", job.crafted, job.targetAmount, itemName),
                    false);
            default -> player.sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.partial",
                    job.crafted, job.targetAmount, itemName), false);
        }
    }

    private static boolean isCraftingTerminalMenu(Object handler) {
        return handler != null && TOM_CRAFTING_TERMINAL_MENU.equals(handler.getClass().getName());
    }

    private static int clampAmount(int requestedAmount) {
        return Math.max(1, Math.min(requestedAmount, MAX_REQUEST_AMOUNT));
    }

    private static int inventoryCapacity(ServerPlayerEntity player, ItemStack result) {
        int capacity = 0;
        int slots = Math.min(36, player.getInventory().size());
        for (int i = 0; i < slots; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                capacity += result.getMaxCount();
                continue;
            }
            if (!ItemStack.areItemsAndComponentsEqual(stack, result)) {
                continue;
            }
            capacity += Math.max(0, Math.min(stack.getMaxCount(), result.getMaxCount()) - stack.getCount());
        }
        return capacity;
    }

    private static Slot getCraftingResultSlot(ScreenHandler handler) throws ReflectiveOperationException {
        Object slot = invoke(handler, "getCraftingResultSlot");
        return slot instanceof Slot resultSlot ? resultSlot : null;
    }

    private static int getSlotId(Slot slot) throws ReflectiveOperationException {
        try {
            Object id = readField(slot, "id");
            return id instanceof Number number ? number.intValue() : -1;
        } catch (NoSuchFieldException ignored) {
            Object id = readField(slot, "field_7874");
            return id instanceof Number number ? number.intValue() : -1;
        }
    }

    private static ItemStack invokeShiftClick(ScreenHandler handler, PlayerEntity player, int slotId)
            throws ReflectiveOperationException {
        Object result = invoke(handler, "shiftClickItems", player, slotId);
        return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
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

    private static Object invoke(Object owner, String methodName, Object... args)
            throws ReflectiveOperationException {
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

    private static Method findMethod(Class<?> type, String methodName, Object... args)
            throws NoSuchMethodException {
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

    private static final class BulkCraftJob {
        private final ScreenHandler handler;
        private final int resultSlotId;
        private final ItemStack resultKey;
        private final int targetAmount;
        private int crafted;

        private BulkCraftJob(ScreenHandler handler, int resultSlotId, ItemStack resultKey, int targetAmount) {
            this.handler = handler;
            this.resultSlotId = resultSlotId;
            this.resultKey = resultKey;
            this.targetAmount = targetAmount;
        }
    }
}
