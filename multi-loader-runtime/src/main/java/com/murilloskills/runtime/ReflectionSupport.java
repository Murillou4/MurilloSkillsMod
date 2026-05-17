package com.murilloskills.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

final class ReflectionSupport {
    private ReflectionSupport() {
    }

    static PlayerIdentity findPlayerIdentity(Object root) {
        Object player = findPlayerObject(root, 0, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
        if (player == null) {
            return PlayerIdentity.UNKNOWN;
        }
        UUID uuid = readUuid(player);
        return uuid == null ? PlayerIdentity.UNKNOWN : new PlayerIdentity(uuid, readName(player));
    }

    static Path findServerSaveRoot(Object server) {
        if (server == null) {
            return null;
        }
        Object file = invokeAny(server, "getSavePath", "getWorldPath", "getRunDirectory", "getFile");
        if (file instanceof Path) {
            return (Path) file;
        }
        if (file instanceof java.io.File) {
            return ((java.io.File) file).toPath();
        }
        if (file instanceof String) {
            return Paths.get((String) file);
        }
        return null;
    }

    static void sendMessage(Object source, String message) {
        if (source == null) {
            return;
        }
        Object text = createText(message);
        if (trySend(source, message, text)) {
            return;
        }
        Object player = findPlayerObject(source, 0, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
        if (player != null) {
            trySend(player, message, text);
        }
    }

    static Object invokeAny(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String name : methodNames) {
            Method method = findNoArgMethod(target.getClass(), name);
            if (method != null) {
                try {
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static Object getFieldAny(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            Field field = findField(target.getClass(), name);
            if (field != null) {
                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static String describe(Object root) {
        StringBuilder out = new StringBuilder();
        describe(root, 0, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()), out);
        return out.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private static Object findPlayerObject(Object root, int depth, Set<Object> visited) {
        if (root == null || depth > 3 || visited.contains(root)) {
            return null;
        }
        visited.add(root);
        if (readUuid(root) != null) {
            return root;
        }

        Object direct = invokeAny(root, "getPlayer", "getEntity", "getEntityLiving", "getSource", "getServerPlayer", "player");
        Object found = findPlayerObject(direct, depth + 1, visited);
        if (found != null) {
            return found;
        }

        for (Method method : root.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || method.getReturnType().isPrimitive()
                    || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!(name.contains("player") || name.contains("entity") || name.contains("source") || name.contains("handler"))) {
                continue;
            }
            try {
                method.setAccessible(true);
                found = findPlayerObject(method.invoke(root), depth + 1, visited);
                if (found != null) {
                    return found;
                }
            } catch (Throwable ignored) {
                // Some Minecraft getters are side-sensitive or throw before worlds are ready.
            }
        }
        return null;
    }

    private static void describe(Object root, int depth, Set<Object> visited, StringBuilder out) {
        if (root == null || depth > 2 || visited.contains(root)) {
            return;
        }
        if (root instanceof Object[]) {
            Object[] values = (Object[]) root;
            for (Object value : values) {
                describe(value, depth + 1, visited, out);
            }
            return;
        }
        visited.add(root);
        append(out, root.getClass().getName());
        append(out, String.valueOf(root));

        for (Method method : root.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (!(lower.contains("block") || lower.contains("state") || lower.contains("item")
                    || lower.contains("stack") || lower.contains("entity") || lower.contains("type")
                    || lower.contains("registry") || lower.equals("getid") || lower.equals("getname")
                    || lower.contains("translation") || lower.contains("description"))) {
                continue;
            }
            try {
                method.setAccessible(true);
                Object value = method.invoke(root);
                if (value == root) {
                    continue;
                }
                append(out, name);
                if (value == null || value instanceof Number || value instanceof Boolean || value instanceof CharSequence
                        || value.getClass().isEnum()) {
                    append(out, String.valueOf(value));
                } else {
                    describe(value, depth + 1, visited, out);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void append(StringBuilder out, String value) {
        if (value != null && value.length() > 0) {
            out.append(' ').append(value);
        }
    }

    private static UUID readUuid(Object player) {
        Object uuid = invokeAny(player, "getUUID", "getUuid", "getUniqueID", "getGameProfile");
        if (uuid instanceof UUID) {
            return (UUID) uuid;
        }
        if (uuid != null && uuid != player) {
            Object nested = invokeAny(uuid, "getId");
            if (nested instanceof UUID) {
                return (UUID) nested;
            }
        }
        return null;
    }

    private static String readName(Object player) {
        Object name = invokeAny(player, "getName", "getDisplayName", "getGameProfile");
        if (name == null) {
            return "unknown";
        }
        Object nested = invokeAny(name, "getString", "getFormattedText", "getName");
        return nested == null ? String.valueOf(name) : String.valueOf(nested);
    }

    private static boolean trySend(Object target, String raw, Object text) {
        for (Method method : target.getClass().getMethods()) {
            String name = method.getName().toLowerCase();
            if (!(name.contains("send") || name.contains("display"))) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1) {
                Object arg = argumentFor(params[0], raw, text);
                if (arg != null && invokeVoid(method, target, arg)) {
                    return true;
                }
            } else if (params.length == 2) {
                Object first = argumentFor(params[0], raw, text);
                Object second = argumentFor(params[1], raw, text);
                if (first != null && second != null && invokeVoid(method, target, first, second)) {
                    return true;
                }
            } else if (params.length == 3) {
                Object first = argumentFor(params[0], raw, text);
                Object second = argumentFor(params[1], raw, text);
                Object third = argumentFor(params[2], raw, text);
                if (first != null && second != null && third != null && invokeVoid(method, target, first, second, third)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object argumentFor(Class<?> type, String raw, Object text) {
        if (type == String.class) {
            return raw;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.FALSE;
        }
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (text != null && type.isAssignableFrom(text.getClass())) {
            return text;
        }
        return null;
    }

    private static boolean invokeVoid(Method method, Object target, Object... args) {
        try {
            method.setAccessible(true);
            method.invoke(target, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createText(String message) {
        Object component = staticLiteral("net.minecraft.network.chat.Component", message);
        if (component != null) {
            return component;
        }
        component = staticLiteral("net.minecraft.text.Text", message);
        if (component != null) {
            return component;
        }
        component = construct("net.minecraft.network.chat.TextComponent", message);
        if (component != null) {
            return component;
        }
        component = construct("net.minecraft.util.text.StringTextComponent", message);
        if (component != null) {
            return component;
        }
        return construct("net.minecraft.util.text.TextComponentString", message);
    }

    private static Object staticLiteral(String className, String message) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod("literal", String.class);
            return method.invoke(null, message);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object construct(String className, String message) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getConstructor(String.class);
            return constructor.newInstance(message);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
