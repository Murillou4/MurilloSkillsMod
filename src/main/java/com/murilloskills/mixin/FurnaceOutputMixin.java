package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for AbstractFurnaceScreenHandler to grant Blacksmith XP when taking
 * smelted ore items from the output slot.
 * 
 * This targets all furnace types: Furnace, Blast Furnace, Smoker.
 * XP is only granted for smelting ores (iron, gold, copper, ancient debris).
 * 
 * The output slot is slot index 2 in furnaces.
 */
@Mixin(AbstractFurnaceScreenHandler.class)
public abstract class FurnaceOutputMixin {

    @Unique
    private static final int FURNACE_OUTPUT_SLOT = 2;

    /**
     * Hook into quickMove to detect shift-click on the output slot.
     */
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void onFurnaceQuickMove(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot != FURNACE_OUTPUT_SLOT) {
            return;
        }

        if (player.getEntityWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        AbstractFurnaceScreenHandler handler = (AbstractFurnaceScreenHandler) (Object) this;

        // Check what's in the output slot
        Slot outputSlot = handler.getSlot(FURNACE_OUTPUT_SLOT);
        ItemStack outputStack = outputSlot.getStack();

        if (outputStack.isEmpty()) {
            return;
        }

        grantSmeltingXpByOutput(serverPlayer, outputStack);
    }

    /**
     * Grants Blacksmith XP based on what was smelted (checking the output item).
     * Only grants XP for ore smelting (ingots/netherite scrap).
     */
    @Unique
    private void grantSmeltingXpByOutput(ServerPlayerEntity serverPlayer, ItemStack outputStack) {
        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        // Only grant XP if player has BLACKSMITH selected
        if (!playerData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        // Check if output is a valid smelted ore product
        SkillReceptorResult xpResult = getSmeltingXpByOutput(outputStack);
        if (!xpResult.didGainXp()) {
            return;
        }

        // Multiply XP by stack count (but cap to avoid exploits)
        int stackCount = Math.min(outputStack.getCount(), 64);
        int totalXp = xpResult.getXpAmount() * stackCount;

        // Add XP using the central method that handles paragon constraints
        SkillGlobalState.XpAddResult xpAddResult = playerData.addXpToSkill(MurilloSkillsList.BLACKSMITH, totalXp);
        state.markDirty();

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Ferreiro", xpAddResult);

        // Notify player on level up
        if (xpAddResult.leveledUp()) {
            int newLevel = playerData.getSkill(MurilloSkillsList.BLACKSMITH).level;
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BLACKSMITH, newLevel);
        }

        // Sync skill data with client
        SkillsNetworkUtils.syncSkills(serverPlayer);

        // Track daily challenge progress - Blacksmith challenges
        com.murilloskills.events.ChallengeEventsHandler.onItemSmelted(serverPlayer, stackCount);
    }

    /**
     * Check if the output item is from ore smelting and return XP.
     */
    @Unique
    private SkillReceptorResult getSmeltingXpByOutput(ItemStack stack) {
        var item = stack.getItem();

        // Iron Ingot
        if (item == net.minecraft.item.Items.IRON_INGOT) {
            return new SkillReceptorResult(true, 15);
        }
        // Gold Ingot
        if (item == net.minecraft.item.Items.GOLD_INGOT) {
            return new SkillReceptorResult(true, 25);
        }
        // Copper Ingot
        if (item == net.minecraft.item.Items.COPPER_INGOT) {
            return new SkillReceptorResult(true, 12);
        }
        // Netherite Scrap (from Ancient Debris)
        if (item == net.minecraft.item.Items.NETHERITE_SCRAP) {
            return new SkillReceptorResult(true, 80);
        }

        return new SkillReceptorResult(false, 0);
    }
}
