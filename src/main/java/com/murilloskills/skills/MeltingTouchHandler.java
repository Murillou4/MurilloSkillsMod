package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.network.MeltingTouchSyncS2CPayload;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MeltingTouchHandler {
    private static final String TOGGLE_MELTING_TOUCH = "meltingTouch";

    private MeltingTouchHandler() {
    }

    public static boolean toggle(ServerPlayerEntity player) {
        PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        boolean newState = !data.getToggle(MurilloSkillsList.MINER, TOGGLE_MELTING_TOUCH, false);
        data.setToggle(MurilloSkillsList.MINER, TOGGLE_MELTING_TOUCH, newState);
        sync(player);
        return newState;
    }

    public static boolean isEnabled(ServerPlayerEntity player) {
        PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        return data.getToggle(MurilloSkillsList.MINER, TOGGLE_MELTING_TOUCH, false) && isAvailable(data);
    }

    public static boolean isAvailable(ServerPlayerEntity player) {
        return isAvailable(player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS));
    }

    public static boolean isAvailable(PlayerSkillData data) {
        if (!data.isSkillSelected(MurilloSkillsList.MINER) || !data.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return false;
        }

        PlayerSkillData.SkillStats miner = data.getSkill(MurilloSkillsList.MINER);
        PlayerSkillData.SkillStats blacksmith = data.getSkill(MurilloSkillsList.BLACKSMITH);
        int requiredLevel = SkillConfig.MELTING_TOUCH_LEVEL;
        return miner.level >= requiredLevel
                && blacksmith.level >= requiredLevel;
    }

    public static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new MeltingTouchSyncS2CPayload(isEnabled(player)));
    }

    /**
     * Hooked into {@code Block.getDroppedStacks(...)} so vanilla still handles XP orb drops,
     * stat tracking, and exhaustion via the normal flow. We only swap the items.
     *
     * Returns {@code null} when the original drop list should be used unchanged.
     */
    public static List<ItemStack> tryMeltDrops(Block block, ServerWorld world, Entity entity, ItemStack tool,
            List<ItemStack> drops) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return null;
        }
        if (!isEnabled(player)) {
            return null;
        }
        if (!MinerXpGetter.isDetectableOreBlock(block)) {
            return null;
        }
        if (tool != null && BlockBreakHandler.hasSilkTouch(tool.getEnchantments())) {
            return null;
        }
        if (drops == null || drops.isEmpty()) {
            return null;
        }

        Optional<ItemStack> blockCooked = getCookedOutput(world, new ItemStack(block.asItem()));
        boolean anyMelted = false;
        List<ItemStack> result = new ArrayList<>(drops.size());

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                result.add(drop);
                continue;
            }

            Optional<ItemStack> cookedDrop = getCookedOutput(world, drop);
            Optional<ItemStack> source = cookedDrop.isPresent() ? cookedDrop : blockCooked;
            if (source.isEmpty()) {
                // No recipe for this drop and no fallback for the block: keep the original
                result.add(drop);
                continue;
            }

            ItemStack out = source.get().copy();
            int count = Math.max(1, out.getCount() * drop.getCount());
            out.setCount(count);
            result.add(out);
            anyMelted = true;
        }

        if (!anyMelted) {
            return null;
        }
        return result;
    }

    public static int getTotalFortuneLevel(ItemStack tool, ServerPlayerEntity player, ServerWorld world) {
        int toolFortune = 0;
        try {
            RegistryEntry<Enchantment> fortuneEntry = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.FORTUNE);
            toolFortune = EnchantmentHelper.getLevel(fortuneEntry, tool);
        } catch (Exception ignored) {
        }

        PlayerSkillData data = player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        PlayerSkillData.SkillStats minerStats = data.getSkill(MurilloSkillsList.MINER);
        float prestigeMultiplier = PrestigeManager.getPassiveMultiplier(minerStats.prestige);
        int skillFortune = (int) (minerStats.level * SkillConfig.getMinerFortunePerLevel() * prestigeMultiplier
                + minerStats.prestige * SkillConfig.getMinerFortunePerPrestige());
        return Math.max(0, toolFortune + skillFortune);
    }

    private static Optional<ItemStack> getCookedOutput(ServerWorld world, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }

        Optional<ItemStack> smelting = getCookingOutput(world, RecipeType.SMELTING, input);
        return smelting.isPresent() ? smelting : getCookingOutput(world, RecipeType.BLASTING, input);
    }

    private static <T extends AbstractCookingRecipe> Optional<ItemStack> getCookingOutput(ServerWorld world,
            RecipeType<T> recipeType, ItemStack inputStack) {
        SingleStackRecipeInput input = new SingleStackRecipeInput(inputStack);
        Optional<RecipeEntry<T>> recipe = world.getRecipeManager().getFirstMatch(recipeType, input, world);
        return recipe
                .map(entry -> entry.value().craft(input, world.getRegistryManager()))
                .filter(stack -> !stack.isEmpty());
    }
}
