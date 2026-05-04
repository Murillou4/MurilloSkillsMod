package com.murilloskills.skills;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Drops extra items after a vanilla block break to bypass loot table caps
 * that prevent Miner skill fortune from actually increasing drops.
 *
 * - Glowstone vanilla limit_count caps dust at 4 regardless of fortune.
 * - Leaves use a fixed-length chances[] array for sapling/stick rolls,
 *   so fortune past index 3 is clamped.
 *
 * This handler adds independent extra rolls scaled by miner fortune
 * (level + prestige) so the bonus is actually visible in-world. Leaves use
 * their real loot table for those extra rolls so modded leaves keep their own
 * saplings, fruits, sticks, and custom drops.
 */
public final class MinerBonusDropHandler {
    private MinerBonusDropHandler() {
    }

    public static void onBlockBreak(PlayerEntity player, World world, BlockPos pos, BlockState state) {
        onBlockBreak(player, world, pos, state, false);
    }

    public static void onBlockBreak(PlayerEntity player, World world, BlockPos pos, BlockState state,
            boolean dropsToInventory) {
        if (world.isClient()) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        ItemStack tool = player.getMainHandStack();
        Block block = state.getBlock();
        String blockId = Registries.BLOCK.getId(block).toString();

        if (BlockBreakHandler.hasSilkTouch(tool.getEnchantments())) {
            return;
        }

        PlayerSkillData data = serverPlayer.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        var minerStats = data.getSkill(MurilloSkillsList.MINER);
        int skillBonus = MinerFortuneHandler.getSkillFortuneBonus(
                minerStats.level, minerStats.prestige, blockId, tool);
        if (skillBonus <= 0) {
            return;
        }

        int enchantFortune = getFortuneEnchantLevel(tool);
        int totalBonus = skillBonus + enchantFortune;

        if (block == Blocks.GLOWSTONE) {
            applyGlowstoneBonus(serverPlayer, serverWorld, pos, totalBonus, dropsToInventory);
            return;
        }

        if (isLeavesBlock(state, blockId) && tool.getItem() instanceof AxeItem) {
            applyLeavesBonus(serverPlayer, serverWorld, pos, state, tool, totalBonus, dropsToInventory);
        }
    }

    private static void applyGlowstoneBonus(ServerPlayerEntity player, ServerWorld world, BlockPos pos,
            int totalFortune, boolean dropsToInventory) {
        // Mirrors vanilla uniform_bonus_count(bonusMultiplier=1) but bypasses limit_count(4).
        int extra = world.getRandom().nextInt(totalFortune + 1);
        if (extra > 0) {
            dropOrInsert(player, world, pos, new ItemStack(Items.GLOWSTONE_DUST, extra), dropsToInventory);
        }
    }

    private static void applyLeavesBonus(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state,
            ItemStack tool, int totalFortune, boolean dropsToInventory) {
        for (int i = 0; i < totalFortune; i++) {
            List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, tool);
            for (ItemStack drop : drops) {
                dropOrInsert(player, world, pos, drop, dropsToInventory);
            }
        }
    }

    private static int getFortuneEnchantLevel(ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            return 0;
        }
        ItemEnchantmentsComponent enchantments = tool.getEnchantments();
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(Enchantments.FORTUNE)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    private static boolean isLeavesBlock(BlockState state, String blockId) {
        return state.isIn(BlockTags.LEAVES) || MinerFortuneHandler.isLeavesBlockId(blockId);
    }

    private static void dropOrInsert(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ItemStack stack,
            boolean dropsToInventory) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        if (dropsToInventory) {
            player.getInventory().insertStack(remaining);
            if (remaining.isEmpty()) {
                return;
            }
        }
        Block.dropStack(world, pos, remaining);
    }
}
