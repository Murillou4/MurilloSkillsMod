package com.murilloskills.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class RuntimeClientBridge {
    private static Object skillsKeyBinding;
    private static KeyRegistration[] registeredKeys = new KeyRegistration[0];

    private RuntimeClientBridge() {
    }

    public static void registerFabricClient(final MurilloSkillsRuntime runtime) {
        try {
            registeredKeys = registerKeyBindings();
            registerClientTick(runtime);
            runtime.log("client keybindings registered: " + registeredKeys.length);
        } catch (Throwable error) {
            runtime.log("client keybinding unavailable: " + shortError(error));
        }
    }

    private static KeyRegistration[] registerKeyBindings() throws ReflectiveOperationException {
        KeySpec[] specs = new KeySpec[] {
                new KeySpec("open_gui", "key.murilloskills.open_gui", 79),
                new KeySpec("use_ability", "key.murilloskills.use_ability", 90),
                new KeySpec("ultplace_undo", "key.murilloskills.ultplace_undo", 90),
                new KeySpec("area_planting_toggle", "key.murilloskills.area_planting_toggle", 71),
                new KeySpec("hollow_fill_toggle", "key.murilloskills.hollow_fill_toggle", 72),
                new KeySpec("night_vision_toggle", "key.murilloskills.night_vision_toggle", 78),
                new KeySpec("step_assist_toggle", "key.murilloskills.step_assist_toggle", 86),
                new KeySpec("ultplace_toggle", "key.murilloskills.ultplace_toggle", 86),
                new KeySpec("speed_boost_toggle", "key.murilloskills.speed_boost_toggle", 66),
                new KeySpec("ultplace_config", "key.murilloskills.ultplace_config", 66),
                new KeySpec("fill_mode_cycle", "key.murilloskills.fill_mode_cycle", 74),
                new KeySpec("vein_miner_toggle", "key.murilloskills.vein_miner_toggle", 46),
                new KeySpec("vein_miner_drops_toggle", "key.murilloskills.vein_miner_drops_toggle", 44),
                new KeySpec("auto_torch_toggle", "key.murilloskills.auto_torch_toggle", 84),
                new KeySpec("melting_touch_toggle", "key.murilloskills.melting_touch_toggle", 77),
                new KeySpec("ultmine_menu", "key.murilloskills.ultmine_menu", 39)
        };
        KeyRegistration[] registrations = new KeyRegistration[specs.length];
        for (int i = 0; i < specs.length; i++) {
            Object keyBinding = registerKeyBinding(createKeyBinding(specs[i].translationKey, specs[i].defaultKey));
            registrations[i] = new KeyRegistration(specs[i], keyBinding);
            if ("open_gui".equals(specs[i].id)) {
                skillsKeyBinding = keyBinding;
            }
        }
        return registrations;
    }

    private static Object createKeyBinding(String translationKey, int defaultKey) throws ReflectiveOperationException {
        Class<?> keyBindingClass = findClass(
                "net.minecraft.class_304",
                "net.minecraft.client.option.KeyBinding",
                "net.minecraft.client.settings.KeyBinding");

        try {
            return keyBindingClass.getConstructor(String.class, int.class, String.class).newInstance(
                    translationKey,
                    Integer.valueOf(defaultKey),
                    "key.category.murilloskills.keybinds");
        } catch (NoSuchMethodException ignored) {
            Class<?> inputTypeClass = findClass(
                    "net.minecraft.class_3675$class_307",
                    "net.minecraft.client.util.InputUtil$Type");
            Object keySym = enumOrField(inputTypeClass, "KEYSYM", "field_1668");
            return keyBindingClass
                    .getConstructor(String.class, inputTypeClass, int.class, String.class)
                    .newInstance(translationKey, keySym, Integer.valueOf(defaultKey),
                            "key.category.murilloskills.keybinds");
        }
    }

    private static Object registerKeyBinding(Object keyBinding) throws ReflectiveOperationException {
        Class<?> helper = Class.forName("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper");
        for (Method method : helper.getMethods()) {
            if ("registerKeyBinding".equals(method.getName()) && method.getParameterTypes().length == 1) {
                return method.invoke(null, keyBinding);
            }
        }
        throw new NoSuchMethodException("KeyBindingHelper.registerKeyBinding");
    }

    private static void registerClientTick(final MurilloSkillsRuntime runtime) throws ReflectiveOperationException {
        Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
        Field endClientTick = eventsClass.getField("END_CLIENT_TICK");
        Object event = endClientTick.get(null);
        Method register = null;
        for (Method method : event.getClass().getMethods()) {
            if ("register".equals(method.getName()) && method.getParameterTypes().length == 1) {
                register = method;
                register.setAccessible(true);
                break;
            }
        }
        if (register == null) {
            throw new NoSuchMethodException("ClientTickEvents.END_CLIENT_TICK.register");
        }

        Object invoker = invokeAny(event, "invoker");
        Class<?> callbackType = findCallbackInterface(invoker);
        Object proxy = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType },
                (proxyObject, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxyObject, method, args);
                    }
                    Object client = args != null && args.length > 0 ? args[0] : minecraftClient();
                    while (consumeWasPressed(skillsKeyBinding)) {
                        openSkillsScreen(runtime, client);
                    }
                    for (KeyRegistration key : registeredKeys) {
                        if (key == null || key.binding == skillsKeyBinding) {
                            continue;
                        }
                        while (consumeWasPressed(key.binding)) {
                            notifyRuntimeKey(runtime, client, key.spec.id);
                        }
                    }
                    return null;
                });
        register.invoke(event, proxy);
    }

    private static Class<?> findCallbackInterface(Object invoker) {
        if (invoker != null) {
            Class<?>[] interfaces = invoker.getClass().getInterfaces();
            for (Class<?> candidate : interfaces) {
                if (candidate != null && candidate != Object.class) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Could not resolve Fabric client tick callback interface.");
    }

    private static boolean consumeWasPressed(Object keyBinding) {
        if (keyBinding == null) {
            return false;
        }
        Object value = invokeAny(keyBinding, "method_1436", "wasPressed");
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private static void openSkillsScreen(MurilloSkillsRuntime runtime, Object client) {
        try {
            if (client == null) {
                client = minecraftClient();
            }
            if (client == null) {
                return;
            }
            Object screen = Class.forName("com.murilloskills.runtime.RuntimeSkillsScreenModern")
                    .getConstructor()
                    .newInstance();
            Method setScreen = findSetScreen(client.getClass(), screen);
            if (setScreen == null) {
                throw new NoSuchMethodException("MinecraftClient.setScreen");
            }
            setScreen.invoke(client, screen);
        } catch (Throwable error) {
            runtime.log("skills screen unavailable: " + shortError(error));
            Object player = ReflectionSupport.getFieldAny(client, "field_1724", "player");
            ReflectionSupport.sendMessage(player,
                    "MurilloSkills runtime ativo. Use /murilloskills stats ou /murilloskills select <skill>.");
        }
    }

    private static void notifyRuntimeKey(MurilloSkillsRuntime runtime, Object client, String id) {
        Object player = ReflectionSupport.getFieldAny(client, "field_1724", "player");
        if (player != null) {
            ReflectionSupport.sendMessage(player,
                    "MurilloSkills runtime: tecla '" + id + "' registrada. Recursos completos ficam no target nativo.");
        }
    }

    private static Method findSetScreen(Class<?> clientClass, Object screen) {
        for (Method method : clientClass.getMethods()) {
            String name = method.getName();
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && ("method_1507".equals(name) || "setScreen".equals(name))
                    && params[0].isAssignableFrom(screen.getClass())) {
                return method;
            }
        }
        return null;
    }

    private static Object minecraftClient() {
        try {
            Class<?> clientClass = findClass("net.minecraft.class_310", "net.minecraft.client.MinecraftClient");
            Object client = invokeStaticAny(clientClass, "method_1551", "getInstance");
            return client;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeAny(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeStaticAny(Class<?> type, String... names) {
        for (String name : names) {
            try {
                Method method = type.getMethod(name);
                return method.invoke(null);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object objectMethod(Object proxyObject, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "MurilloSkillsRuntimeClientTick";
        }
        if ("hashCode".equals(name)) {
            return Integer.valueOf(System.identityHashCode(proxyObject));
        }
        if ("equals".equals(name)) {
            return Boolean.valueOf(proxyObject == (args == null ? null : args[0]));
        }
        return null;
    }

    private static Object enumOrField(Class<?> type, String enumName, String fieldName) throws ReflectiveOperationException {
        if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            for (Object value : values) {
                if (((Enum<?>) value).name().equals(enumName)) {
                    return value;
                }
            }
        }
        return type.getField(fieldName).get(null);
    }

    private static Class<?> findClass(String... names) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException error) {
                last = error;
            }
        }
        throw last;
    }

    private static String shortError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    private static final class KeySpec {
        private final String id;
        private final String translationKey;
        private final int defaultKey;

        private KeySpec(String id, String translationKey, int defaultKey) {
            this.id = id;
            this.translationKey = translationKey;
            this.defaultKey = defaultKey;
        }
    }

    private static final class KeyRegistration {
        private final KeySpec spec;
        private final Object binding;

        private KeyRegistration(KeySpec spec, Object binding) {
            this.spec = spec;
            this.binding = binding;
        }
    }
}
