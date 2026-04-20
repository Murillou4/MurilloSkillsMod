package com.murilloskills.accessor;

/**
 * Server-authoritative anvil cost sync bridge.
 *
 * Exposes the original (pre-discount) cost that is synchronized from the
 * AnvilScreenHandler on both sides, so the client UI can render exact values
 * without re-estimating from formulas.
 */
public interface AnvilCostSyncAccessor {
    int murilloskills$getSyncedOriginalLevelCost();
}
