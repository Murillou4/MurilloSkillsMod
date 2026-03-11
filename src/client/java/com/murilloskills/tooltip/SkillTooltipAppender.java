package com.murilloskills.tooltip;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class SkillTooltipAppender {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.##",
            DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final BlockState PICKAXE_REFERENCE = Blocks.STONE.getDefaultState();
    private static final BlockState AXE_REFERENCE = Blocks.OAK_LOG.getDefaultState();
    private static final BlockState SHOVEL_REFERENCE = Blocks.DIRT.getDefaultState();
    private static final BlockState HOE_REFERENCE = Blocks.HAY_BLOCK.getDefaultState();
    private static final BlockState SHEARS_REFERENCE = Blocks.COBWEB.getDefaultState();

    private SkillTooltipAppender() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register(SkillTooltipAppender::appendTooltip);
    }

    private static void appendTooltip(ItemStack stack, Item.TooltipContext context,
            net.minecraft.item.tooltip.TooltipType type, List<Text> lines) {
        if (stack.isEmpty()) {
            return;
        }

        boolean appendedSection = appendMiningTooltip(stack, lines);
        appendedSection = appendMeleeTooltip(stack, lines) || appendedSection;
        appendedSection = appendFishingTooltip(stack, lines, appendedSection) || appendedSection;
        appendRangedTooltip(stack, lines, appendedSection);
    }

    private static boolean appendMiningTooltip(ItemStack stack, List<Text> lines) {
        var stats = ClientSkillData.get(MurilloSkillsList.MINER);
        if (stats.level <= 0) {
            return false;
        }

        float baseSpeed = getReferenceMiningSpeed(stack);
        if (baseSpeed <= 1.0f) {
            return false;
        }

        float prestigeMultiplier = PrestigeManager.getPassiveMultiplier(stats.prestige);
        double skillMultiplier = 1.0 + (stats.level * SkillConfig.getMinerSpeedPerLevel() * prestigeMultiplier);
        int efficiencyLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.EFFICIENCY);
        double efficiencyBonus = efficiencyLevel > 0 ? (efficiencyLevel * efficiencyLevel) + 1.0 : 0.0;
        double baseWithEnchant = baseSpeed + efficiencyBonus;
        double finalSpeed = baseWithEnchant * skillMultiplier;

        lines.add(Text.empty());
        lines.add(Text.translatable(
                "murilloskills.tooltip.mining_speed_final",
                format(finalSpeed),
                format(baseWithEnchant),
                formatPercent(skillMultiplier - 1.0))
                .formatted(Formatting.AQUA));
        return true;
    }

    private static boolean appendMeleeTooltip(ItemStack stack, List<Text> lines) {
        var stats = ClientSkillData.get(MurilloSkillsList.WARRIOR);
        if (stats.level <= 0 || !isMeleeWeapon(stack.getItem())) {
            return false;
        }

        double itemDamage = getMainHandAttackDamage(stack);
        if (itemDamage <= 0.0) {
            return false;
        }

        float prestigeMultiplier = PrestigeManager.getPassiveMultiplier(stats.prestige);
        double warriorBonus = stats.level * SkillConfig.getWarriorDamagePerLevel() * prestigeMultiplier;
        int sharpnessLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.SHARPNESS);
        int smiteLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.SMITE);
        int baneLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.BANE_OF_ARTHROPODS);

        double baseDamage = 1.0 + itemDamage;
        double genericEnchantBonus = getSharpnessBonus(sharpnessLevel);
        double finalDamage = baseDamage + warriorBonus + genericEnchantBonus;

        lines.add(Text.empty());
        lines.add(Text.translatable(
                "murilloskills.tooltip.attack_damage_final",
                format(finalDamage),
                format(baseDamage),
                format(warriorBonus),
                format(genericEnchantBonus))
                .formatted(Formatting.RED));

        if (smiteLevel > 0) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.attack_damage_undead",
                    format(baseDamage + warriorBonus + getSmiteBonus(smiteLevel)))
                    .formatted(Formatting.DARK_RED));
        }

        if (baneLevel > 0) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.attack_damage_arthropods",
                    format(baseDamage + warriorBonus + getBaneBonus(baneLevel)))
                    .formatted(Formatting.DARK_GREEN));
        }

        return true;
    }

    private static boolean appendFishingTooltip(ItemStack stack, List<Text> lines, boolean appendedSection) {
        var stats = ClientSkillData.get(MurilloSkillsList.FISHER);
        if (stats.level <= 0 || !(stack.getItem() instanceof FishingRodItem)) {
            return false;
        }

        int lureLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.LURE);
        int enchantLuckLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.LUCK_OF_THE_SEA);
        int skillLuckLevel = FisherSkill.getLuckOfTheSeaBonus(stats.level);
        int totalLuckLevel = enchantLuckLevel + skillLuckLevel;
        float fishingSpeedBonus = FisherSkill.getFishingSpeedBonus(stats.level, stats.prestige);
        float waitReduction = 1.0f - FisherSkill.getWaitTimeMultiplier(stats.level);
        float treasureBonus = stats.level >= SkillConfig.getFisherTreasureBonusLevel()
                ? SkillConfig.getFisherTreasureBonus()
                : 0.0f;
        float xpBonus = stats.level >= SkillConfig.getFisherTreasureBonusLevel()
                ? SkillConfig.getFisherXpBonus()
                : 0.0f;

        if (!appendedSection) {
            lines.add(Text.empty());
        }

        lines.add(Text.translatable(
                "murilloskills.tooltip.fisher_speed_bonus",
                formatPercent(fishingSpeedBonus))
                .formatted(Formatting.AQUA));

        if (waitReduction > 0.0f) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.fisher_wait_reduction",
                    formatPercent(waitReduction))
                    .formatted(Formatting.BLUE));
        }

        if (lureLevel > 0) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.fisher_lure_level",
                    lureLevel)
                    .formatted(Formatting.BLUE));
        }

        if (totalLuckLevel > 0) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.fisher_total_luck",
                    totalLuckLevel,
                    enchantLuckLevel,
                    skillLuckLevel)
                    .formatted(Formatting.AQUA));
        }

        if (treasureBonus > 0.0f || xpBonus > 0.0f) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.fisher_treasure_bonus",
                    formatPercent(treasureBonus),
                    formatPercent(xpBonus))
                    .formatted(Formatting.GOLD));
        }

        return true;
    }

    private static void appendRangedTooltip(ItemStack stack, List<Text> lines, boolean appendedSection) {
        var stats = ClientSkillData.get(MurilloSkillsList.ARCHER);
        if (stats.level <= 0) {
            return;
        }

        Item item = stack.getItem();
        if (!(item instanceof BowItem) && !(item instanceof CrossbowItem)) {
            return;
        }

        double damageMultiplier = ArcherSkill.getRangedDamageMultiplier(stats.level, stats.prestige);
        int powerLevel = getEnchantmentLevel(stack.getEnchantments(), Enchantments.POWER);
        double powerBonus = powerLevel > 0 ? 0.5 + (powerLevel * 0.5) : 0.0;

        if (!appendedSection) {
            lines.add(Text.empty());
        }

        double estimatedBaseDamage = (item instanceof BowItem ? 6.0 : 9.0) + powerBonus;
        double estimatedFinalDamage = estimatedBaseDamage * damageMultiplier;
        lines.add(Text.translatable(
                "murilloskills.tooltip.archer_damage_final",
                format(estimatedFinalDamage),
                format(estimatedBaseDamage),
                formatPercent(damageMultiplier - 1.0))
                .formatted(Formatting.GOLD));

        lines.add(Text.translatable(
                "murilloskills.tooltip.archer_damage_bonus",
                formatPercent(damageMultiplier - 1.0))
                .formatted(Formatting.YELLOW));

        if (powerBonus > 0.0) {
            lines.add(Text.translatable(
                    "murilloskills.tooltip.archer_power_bonus",
                    format(powerBonus))
                    .formatted(Formatting.YELLOW));
        }
    }

    private static float getReferenceMiningSpeed(ItemStack stack) {
        return Math.max(
                stack.getMiningSpeedMultiplier(PICKAXE_REFERENCE),
                Math.max(
                        stack.getMiningSpeedMultiplier(AXE_REFERENCE),
                        Math.max(
                                stack.getMiningSpeedMultiplier(SHOVEL_REFERENCE),
                                Math.max(
                                        stack.getMiningSpeedMultiplier(HOE_REFERENCE),
                                        stack.getMiningSpeedMultiplier(SHEARS_REFERENCE)))));
    }

    private static boolean isMeleeWeapon(Item item) {
        return !(item instanceof RangedWeaponItem)
                && !(item instanceof BowItem)
                && !(item instanceof CrossbowItem);
    }

    private static double getMainHandAttackDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.getOrDefault(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.DEFAULT);
        AtomicReference<Double> total = new AtomicReference<>(0.0);

        modifiers.applyModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (!attribute.equals(EntityAttributes.ATTACK_DAMAGE)) {
                return;
            }

            double current = total.get();
            if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    || modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                total.set(current + modifier.value());
            } else {
                total.set(current + modifier.value());
            }
        });

        return total.get();
    }

    private static int getEnchantmentLevel(ItemEnchantmentsComponent enchantments, RegistryKey<Enchantment> key) {
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(key)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    private static double getSharpnessBonus(int level) {
        if (level <= 0) {
            return 0.0;
        }
        return 1.0 + ((level - 1) * 0.5);
    }

    private static double getSmiteBonus(int level) {
        return level <= 0 ? 0.0 : (level * 2.5);
    }

    private static double getBaneBonus(int level) {
        return level <= 0 ? 0.0 : (level * 2.5);
    }

    private static String format(double value) {
        return NUMBER_FORMAT.format(value);
    }

    private static String formatPercent(double value) {
        return NUMBER_FORMAT.format(value * 100.0);
    }
}
