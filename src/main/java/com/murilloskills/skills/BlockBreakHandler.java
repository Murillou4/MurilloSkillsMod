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

            // --- UPDATED CALL: Handles Paragon Logic Internally ---
            final SkillGlobalState.XpAddResult xpResult = data.addXpToSkill(MurilloSkillsList.MINER,
                    result.getXpAmount());
            SkillGlobalState.SkillStats stats = data.getSkill(MurilloSkillsList.MINER);

            // Check for milestone rewards
            com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayerEntity, "Minerador",
                    xpResult);

            if (xpResult.leveledUp()) {
                MutableText message = Text.empty()
                        .append(Text.literal("✦ ").formatted(Formatting.GOLD))
                        .append(Text.literal("LEVEL UP!").formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal("Minerador").formatted(Formatting.YELLOW))
                        .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(String.valueOf(stats.level)).formatted(Formatting.WHITE, Formatting.BOLD));

                if (stats.level == 100) {
                    message.append(Text.literal(" [PARAGON]").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
                }

                serverPlayerEntity.sendMessage(message, true);
                world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                com.murilloskills.utils.SkillAttributes.updateMinerStats(serverPlayerEntity);
            }

            skillState.markDirty();
            SkillsNetworkUtils.syncSkills(serverPlayerEntity);
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