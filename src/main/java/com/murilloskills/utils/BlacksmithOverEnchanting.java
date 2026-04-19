package com.murilloskills.utils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic over-enchanting rules for high-level Blacksmiths.
 * Over-enchanting only happens through the anvil by combining two equal levels
 * that are already at or above the vanilla cap. Each step increases the level
 * by exactly one, up to the configured hard cap.
 */
public final class BlacksmithOverEnchanting {

    private BlacksmithOverEnchanting() {
    }

    public static boolean isUnlocked(int blacksmithLevel) {
        return blacksmithLevel >= getConfiguredUnlockLevel();
    }

    public static int getMaxOverEnchantLevel() {
        return getConfiguredMaxLevel();
    }

    public static int getOverEnchantResultLevel(int leftLevel, int rightLevel, int vanillaMaxLevel) {
        if (leftLevel <= 0 || rightLevel <= 0) {
            return 0;
        }
        if (leftLevel != rightLevel) {
            return Math.max(leftLevel, rightLevel);
        }
        if (leftLevel < vanillaMaxLevel) {
            return leftLevel + 1;
        }
        if (leftLevel >= getMaxOverEnchantLevel()) {
            return getMaxOverEnchantLevel();
        }
        return leftLevel + 1;
    }

    public static boolean shouldOverEnchant(int leftLevel, int rightLevel, int vanillaMaxLevel) {
        return leftLevel > 0
                && leftLevel == rightLevel
                && leftLevel >= vanillaMaxLevel
                && leftLevel < getMaxOverEnchantLevel();
    }

    public static int getExtraAnvilCost(int vanillaMaxLevel, int resultLevel) {
        int overLevels = Math.max(0, resultLevel - vanillaMaxLevel);
        if (overLevels == 0) {
            return 0;
        }

        int baseCost = getConfiguredBaseCost();
        int stepCost = getConfiguredStepCost();
        return (overLevels * baseCost) + (overLevels * (overLevels - 1) / 2 * stepCost);
    }

    public static OverEnchantResult tryApply(ItemStack firstInput, ItemStack secondInput, ItemStack vanillaResult,
            int currentCost) {
        if (firstInput.isEmpty() || secondInput.isEmpty()) {
            return null;
        }

        Map<RegistryEntry<Enchantment>, Integer> left = getEnchantments(firstInput);
        Map<RegistryEntry<Enchantment>, Integer> right = getEnchantments(secondInput);
        if (left.isEmpty() || right.isEmpty()) {
            return null;
        }

        ItemStack resultStack = vanillaResult.isEmpty() ? firstInput.copy() : vanillaResult.copy();
        resultStack.setCount(1);

        Map<RegistryEntry<Enchantment>, Integer> resultEnchantments = vanillaResult.isEmpty()
                ? mergeCompatibleEnchantments(left, right)
                : getEnchantments(resultStack);
        boolean changed = false;
        int extraCost = 0;
        int overMaxLevel = getMaxOverEnchantLevel();

        Set<RegistryEntry<Enchantment>> union = new LinkedHashSet<>();
        union.addAll(left.keySet());
        union.addAll(right.keySet());

        for (RegistryEntry<Enchantment> enchantment : union) {
            int leftLevel = left.getOrDefault(enchantment, 0);
            int rightLevel = right.getOrDefault(enchantment, 0);
            int vanillaMaxLevel = enchantment.value().getMaxLevel();

            int targetLevel;
            if (leftLevel > 0 && leftLevel == rightLevel && leftLevel >= vanillaMaxLevel) {
                targetLevel = leftLevel + 1;
            } else {
                targetLevel = Math.max(leftLevel, rightLevel);
            }
            targetLevel = Math.min(targetLevel, overMaxLevel);

            if (targetLevel <= vanillaMaxLevel) {
                continue;
            }

            int currentLevel = resultEnchantments.getOrDefault(enchantment, 0);
            if (targetLevel <= currentLevel) {
                continue;
            }

            resultEnchantments.put(enchantment, targetLevel);
            extraCost += getExtraAnvilCost(vanillaMaxLevel, targetLevel);
            changed = true;
        }

        if (!changed) {
            return null;
        }

        applyEnchantments(resultStack, resultEnchantments);
        int levelCost = Math.max(1, currentCost + extraCost);
        return new OverEnchantResult(resultStack, levelCost);
    }

    public static int getEnchantingTableMinimumLevel(int vanillaMaxLevel) {
        return Math.min(vanillaMaxLevel, getMaxOverEnchantLevel());
    }

    public static int rollEnchantingTableLevel(int vanillaMaxLevel, Random random) {
        int minLevel = getEnchantingTableMinimumLevel(vanillaMaxLevel);
        int maxLevel = getMaxOverEnchantLevel();
        if (minLevel >= maxLevel) {
            return minLevel;
        }
        return minLevel + random.nextInt(maxLevel - minLevel + 1);
    }

    public static boolean tryApplyEnchantingTableBonus(ItemStack stack, Random random) {
        Map<RegistryEntry<Enchantment>, Integer> enchantments = getEnchantments(stack);
        if (enchantments.isEmpty()) {
            return false;
        }

        boolean changed = false;
        Map<RegistryEntry<Enchantment>, Integer> upgradedEnchantments = new LinkedHashMap<>(enchantments);
        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : enchantments.entrySet()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int currentLevel = entry.getValue();
            int upgradedLevel = rollEnchantingTableLevel(enchantment.value().getMaxLevel(), random);
            if (upgradedLevel > currentLevel) {
                upgradedEnchantments.put(enchantment, upgradedLevel);
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        applyEnchantments(stack, upgradedEnchantments);
        return true;
    }

    private static Map<RegistryEntry<Enchantment>, Integer> getEnchantments(ItemStack stack) {
        ItemEnchantmentsComponent component = getEnchantmentsComponent(stack);
        Map<RegistryEntry<Enchantment>, Integer> enchantments = new LinkedHashMap<>();
        for (RegistryEntry<Enchantment> enchantment : component.getEnchantments()) {
            enchantments.put(enchantment, component.getLevel(enchantment));
        }
        return enchantments;
    }

    private static ItemEnchantmentsComponent getEnchantmentsComponent(ItemStack stack) {
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            return stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
        return stack.getEnchantments();
    }

    private static void applyEnchantments(ItemStack stack, Map<RegistryEntry<Enchantment>, Integer> enchantments) {
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : enchantments.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }

        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            stack.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
        } else {
            stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        }
    }

    public record OverEnchantResult(ItemStack stack, int levelCost) {
    }

    private static Map<RegistryEntry<Enchantment>, Integer> mergeCompatibleEnchantments(
            Map<RegistryEntry<Enchantment>, Integer> left,
            Map<RegistryEntry<Enchantment>, Integer> right) {
        Map<RegistryEntry<Enchantment>, Integer> merged = new LinkedHashMap<>(left);
        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : right.entrySet()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int rightLevel = entry.getValue();
            int leftLevel = merged.getOrDefault(enchantment, 0);

            if (leftLevel > 0) {
                int resultLevel = getOverEnchantResultLevel(leftLevel, rightLevel, enchantment.value().getMaxLevel());
                merged.put(enchantment, Math.max(leftLevel, resultLevel));
                continue;
            }

            boolean compatible = true;
            for (RegistryEntry<Enchantment> existing : merged.keySet()) {
                if (existing.equals(enchantment)) {
                    continue;
                }
                if (!Enchantment.canBeCombined(existing, enchantment)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                merged.put(enchantment, rightLevel);
            }
        }
        return merged;
    }

    private static int getConfiguredUnlockLevel() {
        try {
            return SkillConfig.getBlacksmithOverEnchantUnlockLevel();
        } catch (Exception ignored) {
            return SkillConfig.BLACKSMITH_OVERENCHANT_LEVEL;
        }
    }

    private static int getConfiguredMaxLevel() {
        try {
            return SkillConfig.getBlacksmithOverEnchantMaxLevel();
        } catch (Exception ignored) {
            return SkillConfig.BLACKSMITH_OVERENCHANT_MAX_LEVEL;
        }
    }

    private static int getConfiguredBaseCost() {
        try {
            return SkillConfig.getBlacksmithOverEnchantBaseCost();
        } catch (Exception ignored) {
            return SkillConfig.BLACKSMITH_OVERENCHANT_BASE_COST;
        }
    }

    private static int getConfiguredStepCost() {
        try {
            return SkillConfig.getBlacksmithOverEnchantStepCost();
        } catch (Exception ignored) {
            return SkillConfig.BLACKSMITH_OVERENCHANT_STEP_COST;
        }
    }
}
