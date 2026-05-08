package com.murilloskills.integration;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection-only bridge to Tom's Simple Storage. Lets MurilloSkills push items into the
 * storage network bound to an Advanced Wireless Terminal the player is holding, without
 * adding a hard dependency on Tom's Storage.
 *
 * Every entry point is null-safe and silently no-ops if Tom's Storage isn't loaded or its
 * internals shifted between versions.
 */
public final class TomsStorageBridge {
    private static final String WIRELESS_TERMINAL_ITEM = "com.tom.storagemod.item.WirelessTerminal";
    private static final String CONTENT_CLASS = "com.tom.storagemod.Content";
    private static final String GAME_OBJECT_CLASS = "com.tom.storagemod.platform.GameObject";
    private static final String WORLD_POS_CLASS = "com.tom.storagemod.components.WorldPos";
    private static final String STORED_ITEM_STACK_CLASS = "com.tom.storagemod.inventory.StoredItemStack";
    private static final String STORAGE_TERMINAL_BE_CLASS = "com.tom.storagemod.block.entity.StorageTerminalBlockEntity";
    private static final String INVENTORY_CONNECTOR_CLASS = "com.tom.storagemod.block.entity.IInventoryConnector";
    private static final String INVENTORY_ACCESS_CLASS = "com.tom.storagemod.inventory.IInventoryAccess";

    private static volatile boolean disabled = false;
    private static volatile Class<?> wirelessTerminalItemClass;
    private static volatile ComponentType<?> boundPosComponentType;
    private static volatile Class<?> worldPosClass;
    private static volatile Class<?> storedItemStackClass;
    private static volatile Class<?> storageTerminalBeClass;
    private static volatile Class<?> inventoryConnectorClass;
    private static volatile Class<?> inventoryAccessClass;
    private static volatile Constructor<?> storedItemStackConstructor;
    private static volatile Method worldPosPosMethod;
    private static volatile Method worldPosDimMethod;
    private static volatile Method storageTerminalPushStackMethod;
    private static volatile Method storageTerminalPullStackMethod;
    private static volatile Method storedItemStackGetActualStackMethod;
    private static volatile Method connectorGetMergedHandlerMethod;
    private static volatile Method inventoryAccessPushStackMethod;
    private static volatile Method inventoryAccessPullMatchingStackMethod;

    private TomsStorageBridge() {
    }

    public static boolean isHoldingBoundWirelessTerminal(PlayerEntity player) {
        if (disabled || player == null) {
            return false;
        }
        try {
            Class<?> itemClass = resolveWirelessTerminalItemClass();
            if (itemClass == null) {
                return false;
            }
            return findBoundTerminalStack(player, itemClass) != null;
        } catch (Throwable t) {
            disabled = true;
            return false;
        }
    }

    /**
     * Attempts to push the given stack into the storage network bound to the player's
     * Advanced Wireless Terminal. Returns the leftover stack (possibly empty). If routing
     * isn't possible for any reason the original stack is returned unchanged.
     */
    public static ItemStack pushToBoundStorage(ServerPlayerEntity player, ItemStack stack) {
        if (disabled || player == null || stack == null || stack.isEmpty()) {
            return stack;
        }
        try {
            BlockEntity blockEntity = resolveBoundStorageBlockEntity(player);
            if (blockEntity == null) {
                return stack;
            }
            ItemStack terminalLeftover = pushToStorageTerminal(blockEntity, stack);
            if (terminalLeftover != null) {
                return terminalLeftover;
            }
            ItemStack connectorLeftover = pushToInventoryConnector(blockEntity, stack);
            if (connectorLeftover != null) {
                return connectorLeftover;
            }
            return stack;
        } catch (Throwable t) {
            // Don't permanently disable on a single push failure — it might just be a chunk unload.
            return stack;
        }
    }

    /**
     * Pulls up to {@code amount} matching items from the storage network bound to the
     * player's Advanced Wireless Terminal. Returns the stack actually extracted.
     */
    public static ItemStack pullFromBoundStorage(ServerPlayerEntity player, ItemStack itemKey, int amount) {
        if (disabled || player == null || itemKey == null || itemKey.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        try {
            BlockEntity blockEntity = resolveBoundStorageBlockEntity(player);
            if (blockEntity == null) {
                return ItemStack.EMPTY;
            }
            ItemStack terminalPulled = pullFromStorageTerminal(blockEntity, itemKey, amount);
            if (terminalPulled != null) {
                return terminalPulled;
            }
            ItemStack connectorPulled = pullFromInventoryConnector(blockEntity, itemKey, amount);
            if (connectorPulled != null) {
                return connectorPulled;
            }
        } catch (Throwable ignored) {
            // Chunk loads and optional mod internals can legitimately change underneath us.
        }
        return ItemStack.EMPTY;
    }

    private static BlockEntity resolveBoundStorageBlockEntity(ServerPlayerEntity player)
            throws ReflectiveOperationException {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return null;
        }
        Class<?> itemClass = resolveWirelessTerminalItemClass();
        if (itemClass == null) {
            return null;
        }
        ItemStack terminalStack = findBoundTerminalStack(player, itemClass);
        if (terminalStack == null) {
            return null;
        }
        ComponentType<?> componentType = resolveBoundPosComponentType();
        if (componentType == null) {
            return null;
        }
        Object worldPos = terminalStack.get(componentType);
        if (worldPos == null) {
            return null;
        }
        BlockPos pos = invokeWorldPosPos(worldPos);
        RegistryKey<World> dim = invokeWorldPosDim(worldPos);
        if (pos == null || dim == null) {
            return null;
        }
        ServerWorld targetWorld = server.getWorld(dim);
        if (targetWorld == null || !targetWorld.isChunkLoaded(pos)) {
            return null;
        }
        return targetWorld.getBlockEntity(pos);
    }

    private static ItemStack findBoundTerminalStack(PlayerEntity player, Class<?> itemClass) {
        ItemStack main = player.getMainHandStack();
        if (main != null && itemClass.isInstance(main.getItem()) && hasBoundPos(main)) {
            return main;
        }
        ItemStack off = player.getOffHandStack();
        if (off != null && itemClass.isInstance(off.getItem()) && hasBoundPos(off)) {
            return off;
        }
        return null;
    }

    private static boolean hasBoundPos(ItemStack stack) {
        ComponentType<?> componentType = resolveBoundPosComponentType();
        return componentType != null && stack.contains(componentType);
    }

    private static Class<?> resolveWirelessTerminalItemClass() {
        Class<?> cached = wirelessTerminalItemClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(WIRELESS_TERMINAL_ITEM);
            wirelessTerminalItemClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            disabled = true;
            return null;
        }
    }

    private static Class<?> resolveStorageTerminalBeClass() {
        Class<?> cached = storageTerminalBeClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(STORAGE_TERMINAL_BE_CLASS);
            storageTerminalBeClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> resolveStoredItemStackClass() {
        Class<?> cached = storedItemStackClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(STORED_ITEM_STACK_CLASS);
            storedItemStackClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> resolveInventoryConnectorClass() {
        Class<?> cached = inventoryConnectorClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(INVENTORY_CONNECTOR_CLASS);
            inventoryConnectorClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> resolveInventoryAccessClass() {
        Class<?> cached = inventoryAccessClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(INVENTORY_ACCESS_CLASS);
            inventoryAccessClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static ComponentType<?> resolveBoundPosComponentType() {
        ComponentType<?> cached = boundPosComponentType;
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> contentClass = Class.forName(CONTENT_CLASS);
            Field field = contentClass.getField("boundPosComponent");
            Object gameObject = field.get(null);
            if (gameObject == null) {
                return null;
            }
            Class<?> gameObjectClass = Class.forName(GAME_OBJECT_CLASS);
            Method getMethod = gameObjectClass.getMethod("get");
            Object component = getMethod.invoke(gameObject);
            if (component instanceof ComponentType<?> type) {
                boundPosComponentType = type;
                return type;
            }
        } catch (Throwable t) {
            disabled = true;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static BlockPos invokeWorldPosPos(Object worldPos) throws ReflectiveOperationException {
        Method method = worldPosPosMethod;
        if (method == null) {
            Class<?> clazz = resolveWorldPosClass();
            if (clazz == null) {
                return null;
            }
            method = clazz.getMethod("pos");
            worldPosPosMethod = method;
        }
        Object result = method.invoke(worldPos);
        return result instanceof BlockPos bp ? bp : null;
    }

    @SuppressWarnings("unchecked")
    private static RegistryKey<World> invokeWorldPosDim(Object worldPos) throws ReflectiveOperationException {
        Method method = worldPosDimMethod;
        if (method == null) {
            Class<?> clazz = resolveWorldPosClass();
            if (clazz == null) {
                return null;
            }
            method = clazz.getMethod("dim");
            worldPosDimMethod = method;
        }
        Object result = method.invoke(worldPos);
        return result instanceof RegistryKey<?> rk ? (RegistryKey<World>) rk : null;
    }

    private static Class<?> resolveWorldPosClass() {
        Class<?> cached = worldPosClass;
        if (cached != null) {
            return cached;
        }
        try {
            cached = Class.forName(WORLD_POS_CLASS);
            worldPosClass = cached;
            return cached;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static ItemStack pushToStorageTerminal(BlockEntity blockEntity, ItemStack stack)
            throws ReflectiveOperationException {
        Class<?> beClass = resolveStorageTerminalBeClass();
        if (beClass == null || !beClass.isInstance(blockEntity)) {
            return null;
        }
        Method push = resolveStorageTerminalPushStackMethod(beClass);
        if (push == null) {
            return stack;
        }
        Object result = push.invoke(blockEntity, stack);
        return result instanceof ItemStack leftover ? leftover : stack;
    }

    private static ItemStack pullFromStorageTerminal(BlockEntity blockEntity, ItemStack itemKey, int amount)
            throws ReflectiveOperationException {
        Class<?> beClass = resolveStorageTerminalBeClass();
        if (beClass == null || !beClass.isInstance(blockEntity)) {
            return null;
        }
        Method pull = resolveStorageTerminalPullStackMethod(beClass);
        if (pull == null) {
            return ItemStack.EMPTY;
        }
        Object storedItem = newStoredItemStack(itemKey);
        if (storedItem == null) {
            return ItemStack.EMPTY;
        }
        Object result = pull.invoke(blockEntity, storedItem, (long) amount);
        return storedItemStackToItemStack(result);
    }

    private static ItemStack pushToInventoryConnector(BlockEntity blockEntity, ItemStack stack)
            throws ReflectiveOperationException {
        Class<?> connectorClass = resolveInventoryConnectorClass();
        if (connectorClass == null || !connectorClass.isInstance(blockEntity)) {
            return null;
        }
        Method getMergedHandler = resolveConnectorGetMergedHandlerMethod(connectorClass);
        if (getMergedHandler == null) {
            return stack;
        }
        Object handler = getMergedHandler.invoke(blockEntity);
        if (handler == null) {
            return stack;
        }
        Class<?> inventoryAccessClass = resolveInventoryAccessClass();
        if (inventoryAccessClass == null || !inventoryAccessClass.isInstance(handler)) {
            return stack;
        }
        Method push = resolveInventoryAccessPushStackMethod(inventoryAccessClass);
        if (push == null) {
            return stack;
        }
        Object result = push.invoke(handler, stack);
        return result instanceof ItemStack leftover ? leftover : stack;
    }

    private static ItemStack pullFromInventoryConnector(BlockEntity blockEntity, ItemStack itemKey, int amount)
            throws ReflectiveOperationException {
        Class<?> connectorClass = resolveInventoryConnectorClass();
        if (connectorClass == null || !connectorClass.isInstance(blockEntity)) {
            return null;
        }
        Method getMergedHandler = resolveConnectorGetMergedHandlerMethod(connectorClass);
        if (getMergedHandler == null) {
            return ItemStack.EMPTY;
        }
        Object handler = getMergedHandler.invoke(blockEntity);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        Class<?> inventoryAccessClass = resolveInventoryAccessClass();
        if (inventoryAccessClass == null || !inventoryAccessClass.isInstance(handler)) {
            return ItemStack.EMPTY;
        }
        Method pull = resolveInventoryAccessPullMatchingStackMethod(inventoryAccessClass);
        if (pull == null) {
            return ItemStack.EMPTY;
        }
        ItemStack filter = itemKey.copy();
        filter.setCount(1);
        Object result = pull.invoke(handler, filter, (long) amount);
        return result instanceof ItemStack pulled ? pulled : ItemStack.EMPTY;
    }

    private static Method resolveStorageTerminalPushStackMethod(Class<?> beClass) {
        Method cached = storageTerminalPushStackMethod;
        if (cached != null) {
            return cached;
        }
        try {
            // The terminal exposes both `pushStack(StoredItemStack)` and `pushStack(ItemStack)`.
            // We want the ItemStack overload — it returns the leftover ItemStack.
            cached = beClass.getMethod("pushStack", ItemStack.class);
            cached.setAccessible(true);
            storageTerminalPushStackMethod = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method resolveStorageTerminalPullStackMethod(Class<?> beClass) {
        Method cached = storageTerminalPullStackMethod;
        if (cached != null) {
            return cached;
        }
        Class<?> storedClass = resolveStoredItemStackClass();
        if (storedClass == null) {
            return null;
        }
        try {
            cached = beClass.getMethod("pullStack", storedClass, long.class);
            cached.setAccessible(true);
            storageTerminalPullStackMethod = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Object newStoredItemStack(ItemStack itemKey) throws ReflectiveOperationException {
        Constructor<?> constructor = storedItemStackConstructor;
        if (constructor == null) {
            Class<?> storedClass = resolveStoredItemStackClass();
            if (storedClass == null) {
                return null;
            }
            try {
                constructor = storedClass.getConstructor(ItemStack.class);
            } catch (NoSuchMethodException e) {
                constructor = storedClass.getDeclaredConstructor(ItemStack.class);
            }
            constructor.setAccessible(true);
            storedItemStackConstructor = constructor;
        }
        ItemStack filter = itemKey.copy();
        filter.setCount(1);
        return constructor.newInstance(filter);
    }

    private static ItemStack storedItemStackToItemStack(Object storedItemStack) throws ReflectiveOperationException {
        if (storedItemStack == null) {
            return ItemStack.EMPTY;
        }
        Method method = storedItemStackGetActualStackMethod;
        if (method == null) {
            Class<?> storedClass = resolveStoredItemStackClass();
            if (storedClass == null) {
                return ItemStack.EMPTY;
            }
            method = storedClass.getMethod("getActualStack");
            method.setAccessible(true);
            storedItemStackGetActualStackMethod = method;
        }
        Object result = method.invoke(storedItemStack);
        return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static Method resolveConnectorGetMergedHandlerMethod(Class<?> connectorClass) {
        Method cached = connectorGetMergedHandlerMethod;
        if (cached != null) {
            return cached;
        }
        try {
            cached = connectorClass.getMethod("getMergedHandler");
            cached.setAccessible(true);
            connectorGetMergedHandlerMethod = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method resolveInventoryAccessPushStackMethod(Class<?> inventoryAccessClass) {
        Method cached = inventoryAccessPushStackMethod;
        if (cached != null) {
            return cached;
        }
        try {
            cached = inventoryAccessClass.getMethod("pushStack", ItemStack.class);
            cached.setAccessible(true);
            inventoryAccessPushStackMethod = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method resolveInventoryAccessPullMatchingStackMethod(Class<?> inventoryAccessClass) {
        Method cached = inventoryAccessPullMatchingStackMethod;
        if (cached != null) {
            return cached;
        }
        try {
            cached = inventoryAccessClass.getMethod("pullMatchingStack", ItemStack.class, long.class);
            cached.setAccessible(true);
            inventoryAccessPullMatchingStackMethod = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
