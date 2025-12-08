package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.ExplorerXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to grant Explorer XP when trading with a Wandering Trader.
 */
@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraderMixin {

    /**
     * Inject after a trade is completed with the Wandering Trader.
     */
    @Inject(method = "afterUsing", at = @At("TAIL"))
    private void onTradeComplete(TradeOffer offer, CallbackInfo ci) {
        WanderingTraderEntity trader = (WanderingTraderEntity) (Object) this;

        if (trader.getEntityWorld().isClient())
            return;

        // Get the customer (player trading with the trader)
        PlayerEntity customer = trader.getCustomer();
        if (!(customer instanceof ServerPlayerEntity serverPlayer))
            return;

        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        // Check if Explorer is selected
        if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER))
            return;

        // Award XP for trading
        int xp = ExplorerXpGetter.getWanderingTradeXp();
        SkillGlobalState.XpAddResult xpResult = playerData.addXpToSkill(MurilloSkillsList.EXPLORER, xp);
        state.markDirty();

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Explorador", xpResult);

        // Sync and notify
        SkillsNetworkUtils.syncSkills(serverPlayer);

        if (xpResult.leveledUp()) {
            int newLevel = playerData.getSkill(MurilloSkillsList.EXPLORER).level;
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.EXPLORER, newLevel);

            // Update attributes on level up
            com.murilloskills.utils.SkillAttributes.updateAllStats(serverPlayer);
        }
    }
}
