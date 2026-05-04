package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.network.MeltingTouchSyncS2CPayload;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.PrestigeManager;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    public static boolean tryDropMelted(Block block, World world, PlayerEntity player, BlockPos pos, BlockState state,
            BlockEntity blockEntity, ItemStack tool) {
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        if (!isEnabled(serverPlayer)) {
            return false;
        }
        if (!MinerXpGetter.isDetectableOreBlock(block)) {
            return false;
        }
        if (BlockBreakHandler.hasSilkTouch(tool.getEnchantments())) {
            return false;
        }

        List<ItemStack> vanillaDrops = Block.getDroppedStacks(state, serverWorld, pos, blockEntity, player, tool);
        Optional<ItemStack> blockCooked = getCookedOutput(serverWorld, new ItemStack(block.asItem()));
        List<ItemStack> meltedDrops = meltDrops(serverWorld, vanillaDrops, blockCooked, tool, serverPlayer);
        if (meltedDrops.isEmpty()) {
            return false;
        }

        player.incrementStat(Stats.MINED.getOrCreateStat(block));
        player.addExhaustion(0.005F);
        for (ItemStack drop : meltedDrops) {
            Block.dropStack(serverWorld, pos, drop);
        }
        state.onStacksDropped(serverWorld, pos, tool, true);
        return true;
    }

    private static List<ItemStack> meltDrops(ServerWorld world, List<ItemStack> vanillaDrops,
            Optional<ItemStack> blockCooked, ItemStack tool, ServerPlayerEntity player) {
        List<ItemStack> melted = new ArrayList<>();
        if (vanillaDrops.isEmpty()) {
            if (blockCooked.isEmpty()) {
                return melted;
            }
            ItemStack stack = blockCooked.get().copy();
            stack.setCount(blockCooked.get().getCount());
            melted.add(stack);
            return melted;
        }

        for (ItemStack drop : vanillaDrops) {
            if (drop.isEmpty()) {
                continue;
            }

            Optional<ItemStack> cookedDrop = getCookedOutput(world, drop);
            Optional<ItemStack> cookedSource = cookedDrop.isPresent() ? cookedDrop : blockCooked;
            if (cookedSource.isEmpty()) {
                continue;
            }

            ItemStack cooked = cookedSource.get().copy();
            int fallbackCount = Math.max(drop.getCount(), getFortuneAdjustedBlockCount(tool, player, world));
            int count = cooked.getCount() * (cookedDrop.isPresent() ? drop.getCount() : fallbackCount);
            cooked.setCount(Math.max(1, count));
            melted.add(cooked);
        }

        return melted;
    }

    private static int getFortuneAdjustedBlockCount(ItemStack tool, ServerPlayerEntity player, ServerWorld world) {
        int fortune = getTotalFortuneLevel(tool, player, world);
        if (fortune <= 0) {
            return 1;
        }
        return 1 + world.getRandom().nextInt(fortune + 1);
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
