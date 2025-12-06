package com.murilloskills.mixin;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.ExplorerXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to grant Explorer XP when opening a loot chest for the first time.
 * Targets the LootableInventory interface which is implemented by chests,
 * barrels, etc.
 */
@Mixin(LootableInventory.class)
public interface LootChestMixin {

    /**
     * Inject before loot table is generated (when chest is first opened).
     * This happens only once per chest as the loot table is cleared after
     * generation.
     */
    @Inject(method = "generateLoot", at = @At("HEAD"))
    private void onGenerateLoot(PlayerEntity player, CallbackInfo ci) {
        // Player can be null for interface methods
        if (player == null)
            return;

        // Only process if there's a loot table (world-generated chest)
        LootableInventory self = (LootableInventory) (Object) this;
        if (self.getLootTable() == null)
            return;

        if (player.getEntityWorld().isClient())
            return;

        if (!(player instanceof ServerPlayerEntity serverPlayer))
            return;

        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var playerData = state.getPlayerData(serverPlayer);

        // Check if Explorer is selected
        if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER))
            return;

        // Award XP for opening a loot chest
        int xp = ExplorerXpGetter.getLootChestXp();
        boolean leveledUp = playerData.addXpToSkill(MurilloSkillsList.EXPLORER, xp);
        state.markDirty();

        // Sync and notify
        SkillsNetworkUtils.syncSkills(serverPlayer);

        if (leveledUp) {
            int newLevel = playerData.getSkill(MurilloSkillsList.EXPLORER).level;
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.EXPLORER, newLevel);

            // Update attributes on level up
            com.murilloskills.utils.SkillAttributes.updateAllStats(serverPlayer);
        }
    }
}
