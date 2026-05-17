package com.murilloskills.utils;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.core.compat.CrossModCompatRules;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class BlacksmithMachineSpeedHelper {
    private static final double REBORN_CORE_SPEED_CAP = 0.99D;

    private BlacksmithMachineSpeedHelper() {
    }

    public static int getBestNearbyBlacksmithLevel(ServerWorld world, BlockPos pos) {
        int radius = SkillConfig.getBlacksmithFurnaceSpeedRadius();
        Box searchBox = new Box(pos).expand(radius);
        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                ServerPlayerEntity.class, searchBox, player -> true);

        int bestLevel = 0;
        for (ServerPlayerEntity player : nearbyPlayers) {
            PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
            if (data.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
                int level = data.getSkill(MurilloSkillsList.BLACKSMITH).level;
                bestLevel = Math.max(bestLevel, level);
            }
        }

        return bestLevel;
    }

    public static float getDirectSpeedMultiplier(int level) {
        if (level <= 0) {
            return 1.0f;
        }

        float normalizedLevel = Math.min(1.0f, level / (float) SkillConfig.getMaxLevel());
        float effectiveMaxMultiplier = SkillConfig.getBlacksmithFurnaceSpeedEffectiveMaxMultiplier();
        float maxExtraProgress = Math.max(0.0f, effectiveMaxMultiplier - 1.0f);
        return 1.0f + normalizedLevel * maxExtraProgress;
    }

    public static int getExtraProgressTicks(ServerWorld world, int level) {
        float extraFloat = getDirectSpeedMultiplier(level) - 1.0f;
        if (extraFloat <= 0.0f) {
            return 0;
        }

        int extraTicks = (int) extraFloat;
        float fractional = extraFloat - extraTicks;
        if (fractional > 0.0f && world.random.nextFloat() < fractional) {
            extraTicks++;
        }

        return extraTicks;
    }

    public static double getRebornCoreSpeedBonus(int level) {
        float directMultiplier = getDirectSpeedMultiplier(level);
        if (directMultiplier <= 1.0f) {
            return 0.0D;
        }

        double speedReduction = 1.0D - (1.0D / directMultiplier);
        return Math.min(REBORN_CORE_SPEED_CAP, Math.max(0.0D, speedReduction));
    }

    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Field MISSING_FIELD;
    private static final Method MISSING_METHOD;

    static {
        Field missingField = null;
        try {
            missingField = BlacksmithMachineSpeedHelper.class.getDeclaredField("MISSING_FIELD");
        } catch (NoSuchFieldException ignored) {
        }
        MISSING_FIELD = missingField;

        Method missingMethod = null;
        try {
            missingMethod = BlacksmithMachineSpeedHelper.class.getDeclaredMethod("getRebornCoreSpeedBonus", int.class);
        } catch (NoSuchMethodException ignored) {
        }
        MISSING_METHOD = missingMethod;
    }

    private static Field findField(Class<?> root, String name) {
        String key = root.getName() + "#" + name;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached == MISSING_FIELD ? null : cached;
        }
        for (Class<?> c = root; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                FIELD_CACHE.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        if (MISSING_FIELD != null) {
            FIELD_CACHE.put(key, MISSING_FIELD);
        }
        return null;
    }

    private static Method findMethod(Class<?> root, String name) {
        String key = root.getName() + "#" + name;
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) {
            return cached == MISSING_METHOD ? null : cached;
        }
        for (Class<?> c = root; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    METHOD_CACHE.put(key, m);
                    return m;
                }
            }
        }
        if (MISSING_METHOD != null) {
            METHOD_CACHE.put(key, MISSING_METHOD);
        }
        return null;
    }

    public static void tryBoostEnergizedPowerWorker(Object blockEntity, ServerWorld world, BlockPos pos) {
        try {
            Class<?> cls = blockEntity.getClass();
            Field progressField = findField(cls, "progress");
            Field maxProgressField = findField(cls, "maxProgress");
            if (progressField == null || maxProgressField == null) {
                return;
            }
            int progress = progressField.getInt(blockEntity);
            int maxProgress = maxProgressField.getInt(blockEntity);
            if (progress <= 0 || maxProgress <= 0 || progress >= maxProgress) {
                return;
            }

            int bestLevel = getBestNearbyBlacksmithLevel(world, pos);
            int extraTicks = getExtraProgressTicks(world, bestLevel);
            if (extraTicks <= 0) {
                return;
            }

            progressField.setInt(blockEntity, Math.min(progress + extraTicks, maxProgress - 1));
            spawnSpeedParticles(world, pos);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void tryApplyRebornCoreSpeed(Object blockEntity, ServerWorld world, BlockPos pos) {
        try {
            int bestLevel = getBestNearbyBlacksmithLevel(world, pos);
            double speedBonus = getRebornCoreSpeedBonus(bestLevel);
            if (speedBonus <= 0.0D) {
                return;
            }

            Class<?> cls = blockEntity.getClass();
            Method addSpeedMultiplier = null;
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals("addSpeedMultiplier") && m.getParameterCount() == 1
                            && m.getParameterTypes()[0] == double.class) {
                        m.setAccessible(true);
                        addSpeedMultiplier = m;
                        break;
                    }
                }
                if (addSpeedMultiplier != null) break;
            }
            if (addSpeedMultiplier == null) {
                return;
            }

            addSpeedMultiplier.invoke(blockEntity, speedBonus);
            spawnSpeedParticles(world, pos);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void tryBoostTechRebornElectricFurnace(Object blockEntity, ServerWorld world, BlockPos pos) {
        try {
            Class<?> cls = blockEntity.getClass();

            Field currentRecipeField = findField(cls, "currentRecipe");
            if (currentRecipeField == null || currentRecipeField.get(blockEntity) == null) {
                return;
            }

            Field cookTimeField = findField(cls, "cookTime");
            Field cookTimeTotalField = findField(cls, "cookTimeTotal");
            if (cookTimeField == null || cookTimeTotalField == null) {
                return;
            }

            int cookTime = cookTimeField.getInt(blockEntity);
            int cookTimeTotal = cookTimeTotalField.getInt(blockEntity);
            if (cookTime <= 0 || cookTimeTotal <= 0 || cookTime >= cookTimeTotal) {
                return;
            }

            Method getStored = findMethod(cls, "getStored");
            Method getEuPerTick = null;
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals("getEuPerTick") && m.getParameterCount() == 1
                            && m.getParameterTypes()[0] == long.class) {
                        m.setAccessible(true);
                        getEuPerTick = m;
                        break;
                    }
                }
                if (getEuPerTick != null) break;
            }
            if (getStored == null || getEuPerTick == null) {
                return;
            }
            long stored = ((Number) getStored.invoke(blockEntity)).longValue();
            long euPerTick = ((Number) getEuPerTick.invoke(blockEntity, 1L)).longValue();
            if (stored <= euPerTick) {
                return;
            }

            int bestLevel = getBestNearbyBlacksmithLevel(world, pos);
            int extraTicks = getExtraProgressTicks(world, bestLevel);
            if (extraTicks <= 0) {
                return;
            }

            cookTimeField.setInt(blockEntity, Math.min(cookTime + extraTicks, cookTimeTotal - 1));
            spawnSpeedParticles(world, pos);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void tryBoostTechRebornIronMachine(Object blockEntity, ServerWorld world, BlockPos pos) {
        try {
            Class<?> cls = blockEntity.getClass();

            Field progressField = findField(cls, "progress");
            if (progressField == null) {
                return;
            }

            Method cookingTime = findMethod(cls, "cookingTime");
            Method isBurning = findMethod(cls, "isBurning");
            if (cookingTime == null || isBurning == null) {
                return;
            }

            int progress = progressField.getInt(blockEntity);
            int totalTime = ((Number) cookingTime.invoke(blockEntity)).intValue();
            boolean burning = (Boolean) isBurning.invoke(blockEntity);
            if (!burning || progress <= 0 || totalTime <= 0 || progress >= totalTime) {
                return;
            }

            int bestLevel = getBestNearbyBlacksmithLevel(world, pos);
            int extraTicks = getExtraProgressTicks(world, bestLevel);
            if (extraTicks <= 0) {
                return;
            }

            progressField.setInt(blockEntity, Math.min(progress + extraTicks, totalTime - 1));
            spawnSpeedParticles(world, pos);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void tryBoostGenericMachine(Object blockEntity, ServerWorld world, BlockPos pos) {
        if (blockEntity == null) {
            return;
        }

        String className = blockEntity.getClass().getName();
        String blockId = net.minecraft.registry.Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString();
        if (!CrossModCompatRules.isLikelyMachineIdOrClass(className)
                && !CrossModCompatRules.isLikelyMachineIdOrClass(blockId)) {
            return;
        }

        String[][] progressPairs = {
                { "progress", "maxProgress" },
                { "progress", "progressMax" },
                { "progress", "totalProgress" },
                { "progress", "requiredProgress" },
                { "craftingProgress", "craftingTime" },
                { "craftingProgress", "maxCraftingProgress" },
                { "processingTime", "processingTotalTime" },
                { "processTime", "processTimeTotal" },
                { "cookTime", "cookTimeTotal" },
                { "cookingTimeSpent", "cookingTotalTime" },
                { "workTime", "workTimeTotal" },
                { "burnTime", "maxBurnTime" }
        };

        for (String[] pair : progressPairs) {
            if (tryBoostProgressPair(blockEntity, world, pos, pair[0], pair[1])) {
                return;
            }
        }
    }

    private static boolean tryBoostProgressPair(Object blockEntity, ServerWorld world, BlockPos pos,
            String progressName, String maxName) {
        try {
            Class<?> cls = blockEntity.getClass();
            Field progressField = findField(cls, progressName);
            Field maxField = findField(cls, maxName);
            if (progressField == null || maxField == null
                    || progressField.getType() != int.class || maxField.getType() != int.class) {
                return false;
            }

            int progress = progressField.getInt(blockEntity);
            int maxProgress = maxField.getInt(blockEntity);
            if (progress <= 0 || maxProgress <= 0 || progress >= maxProgress) {
                return false;
            }

            int bestLevel = getBestNearbyBlacksmithLevel(world, pos);
            int extraTicks = getExtraProgressTicks(world, bestLevel);
            if (extraTicks <= 0) {
                return false;
            }

            progressField.setInt(blockEntity, Math.min(progress + extraTicks, maxProgress - 1));
            spawnSpeedParticles(world, pos);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void spawnSpeedParticles(ServerWorld world, BlockPos pos) {
        if (world.getTime() % 10 != 0) {
            return;
        }

        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                2,
                0.2, 0.1, 0.2,
                0.01);
    }
}
