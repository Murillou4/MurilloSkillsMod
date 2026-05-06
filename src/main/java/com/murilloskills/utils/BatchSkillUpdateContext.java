package com.murilloskills.utils;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coalesces noisy skill UI updates during server-side bulk actions.
 */
public final class BatchSkillUpdateContext {
    private static final Map<UUID, BatchState> ACTIVE = new ConcurrentHashMap<>();

    private BatchSkillUpdateContext() {
    }

    public static void begin(ServerPlayerEntity player, String sourceLabel) {
        if (player == null) {
            return;
        }
        ACTIVE.compute(player.getUuid(), (uuid, current) -> {
            if (current == null) {
                return new BatchState(sourceLabel == null || sourceLabel.isBlank() ? "Batch" : sourceLabel);
            }
            current.depth++;
            return current;
        });
    }

    public static void end(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        BatchState state = ACTIVE.get(player.getUuid());
        if (state == null) {
            return;
        }
        state.depth--;
        if (state.depth > 0) {
            return;
        }
        ACTIVE.remove(player.getUuid());
        flush(player, state);
    }

    public static boolean queueSkillSync(ServerPlayerEntity player) {
        BatchState state = getState(player);
        if (state == null) {
            return false;
        }
        state.skillSyncRequested = true;
        return true;
    }

    public static boolean queueChallengeSync(ServerPlayerEntity player) {
        BatchState state = getState(player);
        if (state == null) {
            return false;
        }
        state.challengeSyncRequested = true;
        return true;
    }

    public static boolean queueToast(ServerPlayerEntity player, MurilloSkillsList skill, int xpAmount) {
        BatchState state = getState(player);
        if (state == null || skill == null || xpAmount <= 0) {
            return false;
        }
        state.toastXp.merge(skill, xpAmount, Integer::sum);
        return true;
    }

    private static BatchState getState(ServerPlayerEntity player) {
        return player == null ? null : ACTIVE.get(player.getUuid());
    }

    private static void flush(ServerPlayerEntity player, BatchState state) {
        for (Map.Entry<MurilloSkillsList, Integer> entry : state.toastXp.entrySet()) {
            XpToastSender.sendNow(player, entry.getKey(), entry.getValue(), state.sourceLabel);
        }
        if (state.skillSyncRequested) {
            SkillsNetworkUtils.syncSkillsNow(player);
        }
        if (state.challengeSyncRequested) {
            DailyChallengeManager.syncChallengesNow(player);
        }
    }

    private static final class BatchState {
        private final String sourceLabel;
        private final EnumMap<MurilloSkillsList, Integer> toastXp = new EnumMap<>(MurilloSkillsList.class);
        private int depth = 1;
        private boolean skillSyncRequested;
        private boolean challengeSyncRequested;

        private BatchState(String sourceLabel) {
            this.sourceLabel = sourceLabel;
        }
    }
}
