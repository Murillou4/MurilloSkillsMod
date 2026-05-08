package com.murilloskills.skills;

import com.murilloskills.utils.MinerXpGetter;

import java.util.Set;

final class ClassicUltmineTargetRules {
    private ClassicUltmineTargetRules() {
    }

    static boolean isOriginBlocked(String blockId, Set<String> blockedBlockIds) {
        return blockId != null && blockedBlockIds != null && blockedBlockIds.contains(blockId);
    }

    static boolean shouldExpandIntoConnectedOres(String originBlockId, int variant, Set<String> blockedBlockIds) {
        return variant == 1
                && !isOriginBlocked(originBlockId, blockedBlockIds);
    }

    static boolean isConnectedOreCandidate(String originBlockId, String candidateBlockId, int variant,
            Set<String> blockedBlockIds) {
        return shouldExpandIntoConnectedOres(originBlockId, variant, blockedBlockIds)
                && !isOriginBlocked(candidateBlockId, blockedBlockIds)
                && MinerXpGetter.isOreResourceId(candidateBlockId);
    }
}
