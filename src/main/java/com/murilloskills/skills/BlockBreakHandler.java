package com.murilloskills.skills;

import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.murilloskills.MurilloSkills;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.utils.MinerXpGetter;
import com.murilloskills.utils.XpStreakManager;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockBreakHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(MurilloSkills.MOD_ID);

    public static ActionResult handle(PlayerEntity player, World world, BlockPos pos, BlockState state) {

        if (world.isClient())
            return ActionResult.PASS;

        final ItemStack tool = player.getMainHandStack();
        final ItemEnchantmentsComponent enchantments = tool.getEnchantments();
        final boolean silkTouch = hasSilkTouch(enchantments);
        final SkillReceptorResult result = MinerXpGetter.isMinerXpBlock(state.getBlock(), silkTouch, false);

        if (result.didGainXp()) {
            final MinecraftServer server = world.getServer();
            if (server == null)
                return ActionResult.FAIL;

            final ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
            SkillGlobalState skillState = SkillGlobalState.getServerState(server);
            SkillGlobalState.PlayerSkillData data = skillState.getPlayerData(serverPlayerEntity);

            // Apply streak bonus
            int baseXp = result.getXpAmount();
            int streakXp = XpStreakManager.applyStreakBonus(serverPlayerEntity.getUuid(), MurilloSkillsList.MINER,
                    baseXp);

            // --- UPDATED CALL: Handles Paragon Logic Internally ---
            final SkillGlobalState.XpAddResult xpResult = data.addXpToSkill(MurilloSkillsList.MINER, streakXp);
            SkillGlobalState.SkillStats stats = data.getSkill(MurilloSkillsList.MINER);

            // Check for milestone rewards
            com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayerEntity, "Minerador",
                    xpResult);

            if (xpResult.leveledUp()) {
                MutableText message = Text.empty()
                        .append(Text.literal("✦ ").formatted(Formatting.GOLD))
                        .append(Text.translatable("murilloskills.notify.level_up").formatted(Formatting.GOLD,
                                Formatting.BOLD))
                        .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                        .append(Text.translatable("murilloskills.skill.name.miner").formatted(Formatting.YELLOW))
                        .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(String.valueOf(stats.level)).formatted(Formatting.WHITE, Formatting.BOLD));

                if (stats.level == 100) {
                    message.append(Text.translatable("murilloskills.notify.paragon").formatted(Formatting.LIGHT_PURPLE,
                            Formatting.BOLD));
                }

                serverPlayerEntity.sendMessage(message, true);
                world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                com.murilloskills.utils.SkillAttributes.updateAllStats(serverPlayerEntity);
            }

            skillState.markDirty();
            SkillsNetworkUtils.syncSkills(serverPlayerEntity);

            // Send XP toast notification (show streak bonus if any)
            String blockName = state.getBlock().getName().getString();
            int streak = XpStreakManager.getCurrentStreak(serverPlayerEntity.getUuid(), MurilloSkillsList.MINER);
            String source = streak > 1 ? blockName + " (x" + streak + ")" : blockName;
            com.murilloskills.utils.XpToastSender.send(serverPlayerEntity, MurilloSkillsList.MINER, streakXp, source);

            // Track daily challenge progress - Miner challenges
            com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayerEntity,
                    com.murilloskills.utils.DailyChallengeManager.ChallengeType.MINE_BLOCKS, 1);

            // Check for ore types
            String blockId = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
            if (blockId.contains("_ore") || blockId.contains("ancient_debris")) {
                com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayerEntity,
                        com.murilloskills.utils.DailyChallengeManager.ChallengeType.MINE_ORES, 1);
            }
            if (blockId.contains("deepslate")) {
                com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayerEntity,
                        com.murilloskills.utils.DailyChallengeManager.ChallengeType.MINE_DEEPSLATE, 1);
            }
            if (blockId.contains("diamond_ore")) {
                com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayerEntity,
                        com.murilloskills.utils.DailyChallengeManager.ChallengeType.FIND_DIAMONDS, 1);
            }

            com.murilloskills.utils.DailyChallengeManager.syncChallenges(serverPlayerEntity);
        }
        return ActionResult.PASS;
    }

    public static boolean hasSilkTouch(ItemEnchantmentsComponent enchantments) {
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(Enchantments.SILK_TOUCH))
                return true;
        }
        return false;
    }
}