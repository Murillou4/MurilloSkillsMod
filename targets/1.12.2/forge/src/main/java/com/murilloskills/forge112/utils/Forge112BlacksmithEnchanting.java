package com.murilloskills.forge112.utils;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.data.PlayerRuntime;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.murilloskills.forge112.MurilloSkillsForge112.CONFIG;
import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.isSelected;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.itemId;

public final class Forge112BlacksmithEnchanting {
    public static final int EFFICIENT_ANVIL_LEVEL = 25;
    public static final int OVER_ENCHANT_LEVEL = 99;
    public static final int OVER_ENCHANT_MAX_LEVEL = 8;
    private static final int OVER_ENCHANT_BASE_COST = 6;
    private static final int OVER_ENCHANT_STEP_COST = 4;
    private static final int MASTER_MAX_ANVIL_COST = 25;
    private static final float TABLE_BASE_CHANCE = 0.25F;

    private Forge112BlacksmithEnchanting() {
    }

    public static void applyAnvilUpdate(AnvilUpdateEvent event) {
        EntityPlayer player = findAnvilPlayer(event.getLeft(), event.getRight());
        if (player == null) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        SkillStatsCore stats = data.getSkill(SkillType.BLACKSMITH);
        if (!isMasterBlacksmith(data, stats)) {
            return;
        }
        OverEnchantResult result = tryApplyAnvil(event.getLeft(), event.getRight(), event.getCost(), stats);
        if (result == null) {
            return;
        }
        event.setOutput(result.stack);
        event.setCost(result.cost);
        event.setMaterialCost(result.materialCost);
        LOG.info("[MurilloSkills][1.12.2][Blacksmith] Master Enchanter anvil output for {} cost={} {}",
                player.getName(), result.cost, describeEnchantments(result.stack));
    }

    public static void applyEnchantmentLevelBoost(EnchantmentLevelSetEvent event) {
        EntityPlayer player = findEnchantingPlayer(event);
        if (player == null) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        SkillStatsCore stats = data.getSkill(SkillType.BLACKSMITH);
        if (!isMasterBlacksmith(data, stats) || event.getLevel() <= 0) {
            return;
        }
        event.setLevel(Math.max(event.getLevel(), 30));
    }

    public static void tickEnchantingTable(EntityPlayer player, PlayerSkillDataCore data, PlayerRuntime runtime) {
        if (!(player.openContainer instanceof ContainerEnchantment)) {
            runtime.blacksmithEnchantingSlotFingerprint = null;
            runtime.blacksmithEnchantingSlotHadEnchantments = false;
            return;
        }
        ItemStack stack = stackInSlot(player.openContainer, 0);
        if (stack.isEmpty()) {
            runtime.blacksmithEnchantingSlotFingerprint = null;
            runtime.blacksmithEnchantingSlotHadEnchantments = false;
            return;
        }

        String fingerprint = fingerprint(stack);
        Map<Enchantment, Integer> enchantments = getEnchantments(stack);
        boolean hasEnchantments = !enchantments.isEmpty();
        boolean justEnchanted = runtime.blacksmithEnchantingSlotFingerprint != null
                && !runtime.blacksmithEnchantingSlotHadEnchantments
                && hasEnchantments
                && !fingerprint.equals(runtime.blacksmithEnchantingSlotFingerprint);

        SkillStatsCore stats = data.getSkill(SkillType.BLACKSMITH);
        if (justEnchanted && isMasterBlacksmith(data, stats)
                && !fingerprint.equals(runtime.blacksmithEnchantingProcessedFingerprint)) {
            Random random = new Random(player.world.getTotalWorldTime() ^ player.getUniqueID().getLeastSignificantBits());
            if (tryApplyTableBonus(stack, stats, random)) {
                runtime.blacksmithEnchantingProcessedFingerprint = fingerprint(stack);
                LOG.info("[MurilloSkills][1.12.2][Blacksmith] Master Enchanter table upgraded {} for {}",
                        describeEnchantments(stack), player.getName());
                fingerprint = fingerprint(stack);
                hasEnchantments = true;
            } else {
                runtime.blacksmithEnchantingProcessedFingerprint = fingerprint;
            }
        }

        runtime.blacksmithEnchantingSlotFingerprint = fingerprint;
        runtime.blacksmithEnchantingSlotHadEnchantments = hasEnchantments;
    }

    public static OverEnchantResult tryApplyAnvil(ItemStack leftInput, ItemStack rightInput, int currentCost,
            SkillStatsCore stats) {
        if (leftInput == null || rightInput == null || leftInput.isEmpty() || rightInput.isEmpty()) {
            return null;
        }
        Map<Enchantment, Integer> left = getEnchantments(leftInput);
        Map<Enchantment, Integer> right = getEnchantments(rightInput);
        if (left.isEmpty() || right.isEmpty()) {
            return null;
        }

        ItemStack resultStack = leftInput.copy();
        resultStack.setCount(1);
        Map<Enchantment, Integer> resultEnchantments = new LinkedHashMap<Enchantment, Integer>(left);
        boolean changed = false;
        int extraCost = 0;

        for (Map.Entry<Enchantment, Integer> entry : right.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int leftLevel = levelOf(resultEnchantments, enchantment);
            int rightLevel = entry.getValue();
            if (rightLevel <= 0) {
                continue;
            }
            if (leftLevel <= 0 && !canApplyToResult(leftInput, resultStack, enchantment, resultEnchantments.keySet())) {
                continue;
            }

            int vanillaMax = Math.max(1, enchantment.getMaxLevel());
            int targetLevel = targetAnvilLevel(leftLevel, rightLevel, vanillaMax);
            if (targetLevel <= 0) {
                continue;
            }
            targetLevel = Math.min(OVER_ENCHANT_MAX_LEVEL, targetLevel);
            int currentLevel = levelOf(resultEnchantments, enchantment);
            if (targetLevel <= currentLevel) {
                continue;
            }

            resultEnchantments.put(enchantment, targetLevel);
            if (targetLevel > vanillaMax) {
                changed = true;
                extraCost += extraAnvilCost(vanillaMax, targetLevel);
            }
        }

        if (!changed) {
            return null;
        }

        applyEnchantments(resultStack, resultEnchantments);
        int preDiscount = Math.max(1, currentCost + Math.max(1, extraCost));
        preDiscount = Math.min(preDiscount, MASTER_MAX_ANVIL_COST);
        int cost = Math.max(1, Math.round(preDiscount * (1.0F - anvilDiscount(stats))));
        int materialCost = rightInput.getItem() == Items.ENCHANTED_BOOK ? 1 : 0;
        return new OverEnchantResult(resultStack, cost, materialCost);
    }

    public static boolean tryApplyTableBonus(ItemStack stack, SkillStatsCore stats, Random random) {
        Map<Enchantment, Integer> enchantments = getEnchantments(stack);
        if (enchantments.isEmpty()) {
            return false;
        }
        if (stats.getPrestige() < CONFIG.getMaxPrestigeLevel() && random.nextFloat() >= tableChance(stats)) {
            return false;
        }

        boolean changed = false;
        Map<Enchantment, Integer> upgraded = new LinkedHashMap<Enchantment, Integer>(enchantments);
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int current = entry.getValue();
            int target = tableTargetLevel(enchantment, stats, random);
            if (target > current) {
                upgraded.put(enchantment, target);
                changed = true;
            }
        }
        if (!changed) {
            return false;
        }
        applyEnchantments(stack, upgraded);
        return true;
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Enchantment, Integer> result = new LinkedHashMap<Enchantment, Integer>();
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            NBTTagList list = ItemEnchantedBook.getEnchantments(stack);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                Enchantment enchantment = Enchantment.getEnchantmentByID(tag.getShort("id"));
                if (enchantment != null) {
                    result.put(enchantment, Math.max(result.containsKey(enchantment) ? result.get(enchantment) : 0,
                            tag.getShort("lvl")));
                }
            }
            return result;
        }
        result.putAll(EnchantmentHelper.getEnchantments(stack));
        return result;
    }

    public static String describeEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = getEnchantments(stack);
        if (enchantments.isEmpty()) {
            return "no-enchants";
        }
        List<Map.Entry<Enchantment, Integer>> entries = new ArrayList<Map.Entry<Enchantment, Integer>>(enchantments.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<Enchantment, Integer>>() {
            @Override
            public int compare(Map.Entry<Enchantment, Integer> a, Map.Entry<Enchantment, Integer> b) {
                return enchantmentKey(a.getKey()).compareTo(enchantmentKey(b.getKey()));
            }
        });
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> entry : entries) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(enchantmentKey(entry.getKey())).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static boolean isMasterBlacksmith(PlayerSkillDataCore data, SkillStatsCore stats) {
        return isSelected(data, SkillType.BLACKSMITH) && stats != null && stats.getLevel() >= OVER_ENCHANT_LEVEL;
    }

    private static EntityPlayer findAnvilPlayer(ItemStack left, ItemStack right) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getPlayerList() == null) {
            return null;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player.openContainer instanceof ContainerRepair
                    && (matchesInputSlots(player.openContainer, left, right, 1, 2)
                    || matchesInputSlots(player.openContainer, left, right, 0, 1))) {
                return player;
            }
        }
        return null;
    }

    private static EntityPlayer findEnchantingPlayer(EnchantmentLevelSetEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getPlayerList() == null) {
            return null;
        }
        EntityPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (!(player.openContainer instanceof ContainerEnchantment) || player.world != event.getWorld()) {
                continue;
            }
            ItemStack slot = stackInSlot(player.openContainer, 0);
            if (!slot.isEmpty() && !stacksMatch(slot, event.getItem())) {
                continue;
            }
            double distance = player.getDistanceSq(event.getPos());
            if (distance < nearestDistance && distance <= 144.0D) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean matchesInputSlots(Container container, ItemStack left, ItemStack right, int leftSlot, int rightSlot) {
        return stacksMatch(stackInSlot(container, leftSlot), left) && stacksMatch(stackInSlot(container, rightSlot), right);
    }

    private static ItemStack stackInSlot(Container container, int index) {
        try {
            if (container == null || index < 0 || index >= container.inventorySlots.size()) {
                return ItemStack.EMPTY;
            }
            Slot slot = container.getSlot(index);
            return slot == null ? ItemStack.EMPTY : slot.getStack();
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean stacksMatch(ItemStack a, ItemStack b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        return a.getCount() == b.getCount()
                && ItemStack.areItemsEqual(a, b)
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static int targetAnvilLevel(int leftLevel, int rightLevel, int vanillaMax) {
        if (leftLevel <= 0) {
            return rightLevel;
        }
        if (rightLevel <= 0) {
            return leftLevel;
        }
        if (leftLevel == rightLevel) {
            if (leftLevel >= OVER_ENCHANT_MAX_LEVEL) {
                return OVER_ENCHANT_MAX_LEVEL;
            }
            return leftLevel + 1;
        }
        return Math.max(leftLevel, rightLevel);
    }

    private static int tableTargetLevel(Enchantment enchantment, SkillStatsCore stats, Random random) {
        if (stats.getPrestige() >= CONFIG.getMaxPrestigeLevel()) {
            return OVER_ENCHANT_MAX_LEVEL;
        }
        int vanillaMax = Math.max(1, enchantment.getMaxLevel());
        int min = Math.min(vanillaMax, OVER_ENCHANT_MAX_LEVEL);
        if (min >= OVER_ENCHANT_MAX_LEVEL) {
            return min;
        }
        return min + random.nextInt(OVER_ENCHANT_MAX_LEVEL - min + 1);
    }

    private static float tableChance(SkillStatsCore stats) {
        if (stats.getPrestige() >= CONFIG.getMaxPrestigeLevel()) {
            return 1.0F;
        }
        return Math.min(1.0F, TABLE_BASE_CHANCE + stats.getPrestige() * 0.075F);
    }

    private static float anvilDiscount(SkillStatsCore stats) {
        if (stats == null || stats.getLevel() < EFFICIENT_ANVIL_LEVEL) {
            return 0.0F;
        }
        float levelProgress = Math.min(1.0F, (stats.getLevel() - EFFICIENT_ANVIL_LEVEL) / 75.0F);
        float discount = 0.40F + levelProgress * 0.50F;
        discount += Math.min(CONFIG.getMaxPrestigeLevel(), stats.getPrestige()) * 0.01F;
        return Math.min(0.95F, discount);
    }

    private static int extraAnvilCost(int vanillaMax, int targetLevel) {
        int overLevels = Math.max(0, targetLevel - vanillaMax);
        return overLevels * OVER_ENCHANT_BASE_COST + overLevels * (overLevels - 1) / 2 * OVER_ENCHANT_STEP_COST;
    }

    private static boolean canApplyToResult(ItemStack firstInput, ItemStack resultStack, Enchantment enchantment,
            Set<Enchantment> existing) {
        if (!isCompatible(existing, enchantment)) {
            return false;
        }
        if (resultStack.getItem() == Items.ENCHANTED_BOOK || firstInput.getItem() == Items.ENCHANTED_BOOK) {
            return enchantment.isAllowedOnBooks();
        }
        return enchantment.canApply(resultStack) || enchantment.canApplyAtEnchantingTable(resultStack);
    }

    private static boolean isCompatible(Set<Enchantment> existing, Enchantment candidate) {
        for (Enchantment enchantment : existing) {
            if (enchantment == candidate) {
                continue;
            }
            if (!enchantment.isCompatibleWith(candidate) || !candidate.isCompatibleWith(enchantment)) {
                return false;
            }
        }
        return true;
    }

    private static int levelOf(Map<Enchantment, Integer> enchantments, Enchantment enchantment) {
        Integer level = enchantments.get(enchantment);
        return level == null ? 0 : level.intValue();
    }

    private static void applyEnchantments(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                tag.removeTag("StoredEnchantments");
            }
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                ItemEnchantedBook.addEnchantment(stack, new EnchantmentData(entry.getKey(), entry.getValue()));
            }
            return;
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }

    private static String fingerprint(ItemStack stack) {
        StringBuilder builder = new StringBuilder(itemId(stack)).append('@').append(stack.getMetadata()).append('#').append(stack.getCount());
        Map<Enchantment, Integer> enchantments = getEnchantments(stack);
        List<Enchantment> keys = new ArrayList<Enchantment>(new LinkedHashSet<Enchantment>(enchantments.keySet()));
        Collections.sort(keys, new Comparator<Enchantment>() {
            @Override
            public int compare(Enchantment a, Enchantment b) {
                return enchantmentKey(a).compareTo(enchantmentKey(b));
            }
        });
        for (Enchantment enchantment : keys) {
            builder.append('|').append(enchantmentKey(enchantment)).append('=').append(enchantments.get(enchantment));
        }
        return builder.toString();
    }

    private static String enchantmentKey(Enchantment enchantment) {
        ResourceLocation id = Enchantment.REGISTRY.getNameForObject(enchantment);
        return id == null ? "unknown:" + Enchantment.getEnchantmentID(enchantment) : id.toString();
    }

    public static final class OverEnchantResult {
        public final ItemStack stack;
        public final int cost;
        public final int materialCost;

        private OverEnchantResult(ItemStack stack, int cost, int materialCost) {
            this.stack = stack;
            this.cost = cost;
            this.materialCost = materialCost;
        }
    }
}
