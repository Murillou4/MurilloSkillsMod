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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Drops extra items after a vanilla block break to bypass loot table caps
 * that prevent Miner skill fortune from actually increasing drops.
 *
 * - Glowstone vanilla limit_count caps dust at 4 regardless of fortune.
 * - Leaves use a fixed-length chances[] array for sapling/stick rolls,
 *   so fortune past index 3 is clamped.
 *
 * This handler adds independent extra rolls scaled by miner fortune
 * (level + prestige) so the bonus is actually visible in-world.
 */
public final class MinerBonusDropHandler {
    private MinerBonusDropHandler() {
    }

    public static void onBlockBreak(PlayerEntity player, World world, BlockPos pos, BlockState state) {
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
            applyGlowstoneBonus(serverWorld, pos, totalBonus);
            return;
        }

        if (state.isIn(BlockTags.LEAVES) && tool.getItem() instanceof AxeItem) {
            applyLeavesBonus(serverWorld, pos, block, skillBonus);
        }
    }

    private static void applyGlowstoneBonus(ServerWorld world, BlockPos pos, int totalFortune) {
        // Mirrors vanilla uniform_bonus_count(bonusMultiplier=1) but bypasses limit_count(4).
        int extra = world.getRandom().nextInt(totalFortune + 1);
        if (extra > 0) {
            Block.dropStack(world, pos, new ItemStack(Items.GLOWSTONE_DUST, extra));
        }
    }

    private static void applyLeavesBonus(ServerWorld world, BlockPos pos, Block block, int skillBonus) {
        Item sapling = saplingFor(block);
        for (int i = 0; i < skillBonus; i++) {
            if (sapling != null && world.getRandom().nextFloat() < 0.05f) {
                Block.dropStack(world, pos, new ItemStack(sapling));
            }
            if (world.getRandom().nextFloat() < 0.02f) {
                Block.dropStack(world, pos, new ItemStack(Items.STICK));
            }
            if (dropsApples(block) && world.getRandom().nextFloat() < 0.005f) {
                Block.dropStack(world, pos, new ItemStack(Items.APPLE));
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

    private static Item saplingFor(Block block) {
        if (block == Blocks.OAK_LEAVES) return Items.OAK_SAPLING;
        if (block == Blocks.BIRCH_LEAVES) return Items.BIRCH_SAPLING;
        if (block == Blocks.SPRUCE_LEAVES) return Items.SPRUCE_SAPLING;
        if (block == Blocks.JUNGLE_LEAVES) return Items.JUNGLE_SAPLING;
        if (block == Blocks.ACACIA_LEAVES) return Items.ACACIA_SAPLING;
        if (block == Blocks.DARK_OAK_LEAVES) return Items.DARK_OAK_SAPLING;
        if (block == Blocks.CHERRY_LEAVES) return Items.CHERRY_SAPLING;
        if (block == Blocks.MANGROVE_LEAVES) return Items.MANGROVE_PROPAGULE;
        return null;
    }

    private static boolean dropsApples(Block block) {
        return block == Blocks.OAK_LEAVES || block == Blocks.DARK_OAK_LEAVES;
    }
}
