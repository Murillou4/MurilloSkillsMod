package com.murilloskills.utils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
        if (left.isEmpty() && right.isEmpty()) {
            return null;
        }

        ItemStack resultStack = vanillaResult.isEmpty() ? firstInput.copy() : vanillaResult.copy();
        resultStack.setCount(1);

        Map<RegistryEntry<Enchantment>, Integer> baselineEnchantments = vanillaResult.isEmpty()
                ? new LinkedHashMap<>(left)
                : getEnchantments(resultStack);
        Map<RegistryEntry<Enchantment>, Integer> resultEnchantments = vanillaResult.isEmpty()
                ? mergeCompatibleEnchantments(baselineEnchantments, right, firstInput, resultStack)
                : new LinkedHashMap<>(baselineEnchantments);
        boolean changed = false;
        int extraCost = 0;
        int overMaxLevel = getMaxOverEnchantLevel();

        Set<RegistryEntry<Enchantment>> union = new LinkedHashSet<>();
        union.addAll(left.keySet());
        union.addAll(right.keySet());
        Set<String> processedEnchantments = new LinkedHashSet<>();

        for (RegistryEntry<Enchantment> enchantment : union) {
            if (!processedEnchantments.add(getEnchantmentIdentity(enchantment))) {
                continue;
            }

            int leftLevel = getLevel(left, enchantment);
            int rightLevel = getLevel(right, enchantment);
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

            boolean alreadyPresentInResult = containsEnchantment(resultEnchantments, enchantment);
            if (!alreadyPresentInResult) {
                if (!vanillaResult.isEmpty()) {
                    // Keep vanilla compatibility rules for normal anvil merges.
                    continue;
                }
                if (!canApplyToResultItem(firstInput, resultStack, enchantment)) {
                    continue;
                }
                if (!isCompatibleWithExisting(resultEnchantments.keySet(), enchantment)) {
                    continue;
                }
            }

            int currentLevel = getLevel(resultEnchantments, enchantment);
            int baselineLevel = getLevel(baselineEnchantments, enchantment);
            if (targetLevel <= currentLevel) {
                if (targetLevel > baselineLevel) {
                    extraCost += getExtraAnvilCost(vanillaMaxLevel, targetLevel);
                    changed = true;
                }
                continue;
            }

            setLevel(resultEnchantments, enchantment, targetLevel);
            if (targetLevel > baselineLevel) {
                extraCost += getExtraAnvilCost(vanillaMaxLevel, targetLevel);
                changed = true;
            }
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

    public static List<EnchantmentLevelEntry> applyDeterministicEnchantingTableBonus(
            List<EnchantmentLevelEntry> enchantments,
            int enchantingSeed,
            int slot) {
        if (enchantments == null || enchantments.isEmpty()) {
            return enchantments;
        }

        Random random = createEnchantingTableBonusRandom(enchantingSeed, slot);
        if (random.nextFloat() >= SkillConfig.getBlacksmithSuperEnchantChance()) {
            return enchantments;
        }

        List<EnchantmentLevelEntry> upgradedEntries = null;
        for (int i = 0; i < enchantments.size(); i++) {
            EnchantmentLevelEntry entry = enchantments.get(i);
            int upgradedLevel = rollEnchantingTableLevel(entry.enchantment().value().getMaxLevel(), random);
            if (upgradedLevel <= entry.level()) {
                continue;
            }

            if (upgradedEntries == null) {
                upgradedEntries = new ArrayList<>(enchantments);
            }
            upgradedEntries.set(i, new EnchantmentLevelEntry(entry.enchantment(), upgradedLevel));
        }

        return upgradedEntries != null ? upgradedEntries : enchantments;
    }

    static Random createEnchantingTableBonusRandom(int enchantingSeed, int slot) {
        long seed = Integer.toUnsignedLong(enchantingSeed);
        seed = seed * 341873128712L + (long) (slot + 1) * 132897987541L + 0x9E3779B97F4A7C15L;
        return Random.create(seed);
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
            ItemEnchantmentsComponent stored = stack.getOrDefault(
                    DataComponentTypes.STORED_ENCHANTMENTS,
                    ItemEnchantmentsComponent.DEFAULT);
            if (!stored.isEmpty()) {
                return stored;
            }
            // Compatibility fallback: some external commands/mods write enchanted-book data
            // into ENCHANTMENTS instead of STORED_ENCHANTMENTS.
            return stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
        return stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
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
            Map<RegistryEntry<Enchantment>, Integer> right,
            ItemStack firstInput,
            ItemStack resultStack) {
        Map<RegistryEntry<Enchantment>, Integer> merged = new LinkedHashMap<>(left);
        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : right.entrySet()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int rightLevel = entry.getValue();
            int leftLevel = getLevel(merged, enchantment);

            if (leftLevel > 0) {
                int resultLevel = getOverEnchantResultLevel(leftLevel, rightLevel, enchantment.value().getMaxLevel());
                setLevel(merged, enchantment, Math.max(leftLevel, resultLevel));
                continue;
            }

            boolean compatible = true;
            for (RegistryEntry<Enchantment> existing : merged.keySet()) {
                if (sameEnchantment(existing, enchantment)) {
                    continue;
                }
                if (!Enchantment.canBeCombined(existing, enchantment)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible && canApplyToResultItem(firstInput, resultStack, enchantment)) {
                setLevel(merged, enchantment, rightLevel);
            }
        }
        return merged;
    }

    private static boolean canApplyToResultItem(
            ItemStack firstInput,
            ItemStack resultStack,
            RegistryEntry<Enchantment> enchantment) {
        if (resultStack.isOf(Items.ENCHANTED_BOOK) || firstInput.isOf(Items.ENCHANTED_BOOK)) {
            return true;
        }
        return enchantment.value().isAcceptableItem(resultStack) || enchantment.value().isAcceptableItem(firstInput);
    }

    private static boolean isCompatibleWithExisting(
            Set<RegistryEntry<Enchantment>> existingEnchantments,
            RegistryEntry<Enchantment> candidate) {
        for (RegistryEntry<Enchantment> existing : existingEnchantments) {
            if (sameEnchantment(existing, candidate)) {
                continue;
            }
            if (!Enchantment.canBeCombined(existing, candidate)) {
                return false;
            }
        }
        return true;
    }

    private static int getLevel(Map<RegistryEntry<Enchantment>, Integer> enchantments, RegistryEntry<Enchantment> target) {
        RegistryEntry<Enchantment> match = findMatchingEnchantment(enchantments, target);
        return match != null ? enchantments.getOrDefault(match, 0) : 0;
    }

    private static boolean containsEnchantment(
            Map<RegistryEntry<Enchantment>, Integer> enchantments,
            RegistryEntry<Enchantment> target) {
        return findMatchingEnchantment(enchantments, target) != null;
    }

    private static void setLevel(
            Map<RegistryEntry<Enchantment>, Integer> enchantments,
            RegistryEntry<Enchantment> target,
            int level) {
        RegistryEntry<Enchantment> match = findMatchingEnchantment(enchantments, target);
        if (match != null) {
            enchantments.put(match, level);
        } else {
            enchantments.put(target, level);
        }
    }

    private static RegistryEntry<Enchantment> findMatchingEnchantment(
            Map<RegistryEntry<Enchantment>, Integer> enchantments,
            RegistryEntry<Enchantment> target) {
        for (RegistryEntry<Enchantment> candidate : enchantments.keySet()) {
            if (sameEnchantment(candidate, target)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean sameEnchantment(
            RegistryEntry<Enchantment> first,
            RegistryEntry<Enchantment> second) {
        if (first == second) {
            return true;
        }
        var firstKey = first.getKey();
        var secondKey = second.getKey();
        if (firstKey.isPresent() && secondKey.isPresent()) {
            return firstKey.get().equals(secondKey.get());
        }
        return first.value() == second.value();
    }

    private static String getEnchantmentIdentity(RegistryEntry<Enchantment> enchantment) {
        return enchantment.getKey()
                .map(key -> key.getValue().toString())
                .orElseGet(() -> "direct:" + System.identityHashCode(enchantment.value()));
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
