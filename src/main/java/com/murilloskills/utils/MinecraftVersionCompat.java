package com.murilloskills.utils;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class MinecraftVersionCompat {
    private MinecraftVersionCompat() {
    }

    public static EntityAttributeInstance getAttributeInstance(LivingEntity entity, String vanillaId) {
        RegistryEntry<EntityAttribute> attribute = getAttribute(vanillaId);
        return attribute == null ? null : entity.getAttributeInstance(attribute);
    }

    public static RegistryEntry<EntityAttribute> getAttribute(String vanillaId) {
        Identifier id = Identifier.ofVanilla(vanillaId);
        return Registries.ATTRIBUTE.getEntry(id).orElse(null);
    }

    public static Vec3d pos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    public static ServerWorld serverWorld(ServerPlayerEntity player) {
        return (ServerWorld) player.getEntityWorld();
    }

    public static int experienceOrbValue(ExperienceOrbEntity orb) {
        Object value = invokeNoArgs(orb, "getValue", "getExperienceAmount", "method_5919");
        if (value instanceof Number number) {
            return number.intValue();
        }
        value = readField(orb, "amount", "value", "field_6159", "field_55950");
        return value instanceof Number number ? number.intValue() : 0;
    }

    public static boolean damage(LivingEntity target, ServerWorld world, DamageSource source, float amount) {
        Object result = invokeArgs(target, new Object[] { world, source, Float.valueOf(amount) },
                "damage", "method_64397");
        if (result instanceof Boolean bool) {
            return bool.booleanValue();
        }
        result = invokeArgs(target, new Object[] { source, Float.valueOf(amount) },
                "damage", "method_5643");
        return result instanceof Boolean bool && bool.booleanValue();
    }

    @SuppressWarnings("unchecked")
    public static RegistryEntry<Enchantment> enchantmentEntry(EnchantmentLevelEntry entry) {
        Object value = invokeNoArgs(entry, "enchantment", "b", "comp_3486");
        if (value == null) {
            value = readField(entry, "enchantment", "a", "field_9093", "comp_3486");
        }
        return value instanceof RegistryEntry<?> registryEntry ? (RegistryEntry<Enchantment>) registryEntry : null;
    }

    public static int enchantmentLevel(EnchantmentLevelEntry entry) {
        Object value = invokeNoArgs(entry, "level", "c", "comp_3487");
        if (value == null) {
            value = readField(entry, "level", "b", "field_9094", "comp_3487");
        }
        return value instanceof Number number ? number.intValue() : 0;
    }

    public static Registry<Enchantment> enchantmentRegistry(ServerWorld world) {
        return registry(world, RegistryKeys.ENCHANTMENT);
    }

    @SuppressWarnings("unchecked")
    public static <T> Registry<T> registry(ServerWorld world, RegistryKey<Registry<T>> key) {
        Object manager = world.getRegistryManager();
        Object registry = invokeOneArg(manager, key, "getOrThrow", "get", "method_30530");
        if (registry == null) {
            throw new IllegalStateException("Registry not found: " + key.getValue());
        }
        return (Registry<T>) registry;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryEntry.Reference<T>> registryEntry(Registry<T> registry, RegistryKey<T> key) {
        Object optional = invokeOneArg(registry, key, "getOptional", "getEntry", "getOrThrow", "method_30530");
        if (optional instanceof Optional<?> castOptional) {
            return (Optional<RegistryEntry.Reference<T>>) castOptional;
        }
        if (optional instanceof RegistryEntry.Reference<?> reference) {
            return Optional.of((RegistryEntry.Reference<T>) reference);
        }
        return Optional.empty();
    }

    private static Object invokeNoArgs(Object target, String... names) {
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

    private static Object invokeOneArg(Object target, Object arg, String... names) {
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(target, arg);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object invokeArgs(Object target, Object[] args, String... names) {
        for (String name : names) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object readField(Object target, String... names) {
        for (String name : names) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
