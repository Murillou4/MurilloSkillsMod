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
    private static final String STORAGE_TERMINAL_BE_CLASS = "com.tom.storagemod.block.entity.StorageTerminalBlockEntity";
    private static final String INVENTORY_CONNECTOR_CLASS = "com.tom.storagemod.block.entity.IInventoryConnector";
    private static final String INVENTORY_ACCESS_CLASS = "com.tom.storagemod.inventory.IInventoryAccess";

    private static volatile boolean disabled = false;
    private static volatile Class<?> wirelessTerminalItemClass;
    private static volatile ComponentType<?> boundPosComponentType;
    private static volatile Class<?> worldPosClass;
    private static volatile Class<?> storageTerminalBeClass;
    private static volatile Class<?> inventoryConnectorClass;
    private static volatile Class<?> inventoryAccessClass;
    private static volatile Method worldPosPosMethod;
    private static volatile Method worldPosDimMethod;
    private static volatile Method storageTerminalPushStackMethod;
    private static volatile Method connectorGetMergedHandlerMethod;
    private static volatile Method inventoryAccessPushStackMethod;

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
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return stack;
        }
        try {
            Class<?> itemClass = resolveWirelessTerminalItemClass();
            if (itemClass == null) {
                return stack;
            }
            ItemStack terminalStack = findBoundTerminalStack(player, itemClass);
            if (terminalStack == null) {
                return stack;
            }
            ComponentType<?> componentType = resolveBoundPosComponentType();
            if (componentType == null) {
                return stack;
            }
            Object worldPos = terminalStack.get(componentType);
            if (worldPos == null) {
                return stack;
            }
            BlockPos pos = invokeWorldPosPos(worldPos);
            RegistryKey<World> dim = invokeWorldPosDim(worldPos);
            if (pos == null || dim == null) {
                return stack;
            }
            ServerWorld targetWorld = server.getWorld(dim);
            if (targetWorld == null || !targetWorld.isChunkLoaded(pos)) {
                return stack;
            }
            BlockEntity blockEntity = targetWorld.getBlockEntity(pos);
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
}
