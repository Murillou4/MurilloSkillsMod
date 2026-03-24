# Codebase Concerns

**Analysis Date:** 2026-03-24

---

## High Priority

### Memory Leaks — Transient Skill State Not Cleaned Up on Disconnect

Most skill implementations hold active-ability state in non-thread-safe `static final Map<UUID, ...>` collections backed by plain `HashMap`. On player disconnect, only `FarmerSkill` and `VeinMinerHandler` have their cleanup methods called from the disconnect event listener in `MinecraftEventsListener.java` (lines 79-81).

The following maps are never cleaned up:

- `BlacksmithSkill.titaniumAuraPlayers` — `src/main/java/com/murilloskills/impl/BlacksmithSkill.java:39`
- `BuilderSkill.creativeBrushPlayers` + `firstCornerPos` + `pausedRemainingTime` + `hollowModeEnabled` + `fillModePreference` + `cylinderHorizontal` — `src/main/java/com/murilloskills/impl/BuilderSkill.java:44-60`
- `FisherSkill.rainDancePlayers` — `src/main/java/com/murilloskills/impl/FisherSkill.java:35`
- `ExplorerSkill.treasureHunterActive` + `lastPositions` + `accumulatedDistance` — `src/main/java/com/murilloskills/impl/ExplorerSkill.java:57,128,130`
- `WarriorSkill.berserkPlayers` — `src/main/java/com/murilloskills/impl/WarriorSkill.java:38`
- `DailyChallengeManager.playerChallenges` — `src/main/java/com/murilloskills/utils/DailyChallengeManager.java:34`

Impact: On long-running servers with many players joining and leaving, these maps grow unbounded. The ExplorerSkill position maps are particularly expensive since they are updated every tick per player.

Fix approach: Add cleanup calls for all affected classes to `MinecraftEventsListener.playerJoinListen()` DISCONNECT handler (line 78), following the existing pattern of `FarmerSkill.cleanupPlayerState()`.

---

### Daily Challenge State Lost on Server Restart

`DailyChallengeManager` stores all challenge progress in a plain `HashMap<UUID, PlayerChallengeData>` at `src/main/java/com/murilloskills/utils/DailyChallengeManager.java:34`. This map is in-memory only. When the server restarts or crashes, all daily challenge progress for every player is silently lost. Players see fresh challenges with zero progress on reconnect.

Impact: Poor player experience on servers that restart daily; challenges are effectively un-completable if the server restarts mid-day.

Fix approach: Serialize challenge state into the `PlayerSkillData` codec (similar to how `achievementStats` and `skillToggles` were added) so challenge progress persists through the Fabric Data Attachments system.

---

### C2S Payload Validation Gap — UltmineShapeSelect Accepts Unclamped Values

`UltmineShapeSelectNetworkHandler.create()` at `src/main/java/com/murilloskills/network/handlers/UltmineShapeSelectNetworkHandler.java:24` passes `payload.shape()`, `payload.depth()`, and `payload.length()` directly to `VeinMinerHandler.setUltmineSelection()`. The normalization (`Math.max`/`Math.min`) occurs inside `setUltmineSelection`, so the server does enforce bounds after the fact. However, the `variant` field from the payload is validated only against `UltmineShape.getVariantCount()` — a malicious client can still send any `int` for `depth`, `length`, and `variant` before clamping. More critically, the shape enum itself is deserialized via `buf.readEnumConstant(UltmineShape.class)` without any check that the player is actually authorized to use Ultmine (Miner master level or permission level requirement).

Files: `src/main/java/com/murilloskills/network/UltmineShapeSelectC2SPayload.java:26`, `src/main/java/com/murilloskills/network/handlers/UltmineShapeSelectNetworkHandler.java`

Impact: A non-Miner-master player can set their Ultmine shape via forged C2S packets. The `UltmineRequestNetworkHandler` does check `VeinMinerHandler.isVeinMinerActive()` but not skill level directly — only whether the toggle is active.

Fix approach: Add the same miner-master/permission check in `UltmineShapeSelectNetworkHandler` that exists in `VeinMinerHandler.shouldUseUltmine()`.

---

### Null Check After Dereference — `SkillSelectionNetworkHandler`

At `src/main/java/com/murilloskills/network/handlers/SkillSelectionNetworkHandler.java:46-52`, `incoming` is assigned from `payload.selectedSkills()` and used to call `incoming.size()` on line 47, then the null check `if (incoming == null || ...)` appears on line 52. If `payload.selectedSkills()` ever returns null (e.g., malformed codec), this causes a `NullPointerException` on line 47 before the guard is reached.

Fix approach: Move the null check to immediately after assignment on line 46, before `newCount` is computed.

---

### Damage Type Detection via String Matching is Fragile

`LivingEntityMixin.applyResistance()` at `src/main/java/com/murilloskills/mixin/LivingEntityMixin.java:89-128` identifies fire/explosion and fall damage using `source.getName().contains("fire")`, `contains("explosion")`, `contains("lava")`, `contains("inFire")`, `contains("onFire")`, and `contains("fall")`. `DamageSource.getName()` returns the translation key which can change between Minecraft versions or with modded damage sources.

Impact: Fire resistance, fall damage reduction (Builder level 25, Explorer level 65), and Blacksmith Forged Resilience all silently stop working if a damage source key is renamed or a modded damage type uses different naming conventions.

Fix approach: Use `DamageSource.isOf(DamageTypes.*)` or `DamageSource.isIn(DamageTypeTags.*)` from Minecraft's type-safe API instead of string matching. These are available in 1.21+.

---

## Medium Priority

### `SkillConfig.java` is a God Class (1341 lines)

`src/main/java/com/murilloskills/utils/SkillConfig.java` has grown to 1341 lines combining two distinct concerns:
1. Dynamic getters delegating to `ModConfig.get()` (the intended design after the config refactor)
2. Legacy `static final` constant fields that duplicate values now in the config (`MINER_SPEED_PER_LEVEL`, `WARRIOR_DAMAGE_PER_LEVEL`, `LIFESTEAL_PERCENTAGE`, etc.)

The legacy constants are still used by mixins and skill implementations, creating two sources of truth. For example, `RESISTANCE_REDUCTION = 0.85f` at line 241 is used directly in `LivingEntityMixin.java:80` while the dynamic version `getResistanceReduction()` at line 197 exists for the same value.

Fix approach: Audit all remaining uses of the legacy constants and migrate them to the dynamic getters. Mark the constants `@Deprecated` in the meantime to surface unreviewed references.

---

### `SkillsScreen.java` is a God Class (1471 lines)

`src/client/java/com/murilloskills/gui/SkillsScreen.java` at 1471 lines handles layout, rendering, input, business logic (prestige eligibility checks, paragon validation), and network sends. This makes adding new skill tabs or modifying any UI element risky because changes can break unrelated sections.

Fix approach: Extract sections into dedicated renderer classes following the partial pattern already established in `src/client/java/com/murilloskills/gui/renderer/` (TabRenderer, RenderingHelper).

---

### `VeinMinerHandler.java` Has Too Many Responsibilities (864 lines)

`src/main/java/com/murilloskills/skills/VeinMinerHandler.java` at 864 lines manages: per-player toggle state, per-player Ultmine shape/depth/length/variant preferences, per-player XP direct-to-player preferences, pending drop collection scheduling, vein mining execution (legacy flood-fill), Ultmine execution (shape-based), preview generation, and block equivalence lookup. The `VeinMinerHandler` is effectively a static service locator for all vein-mining concerns.

Fix approach: Extract per-player preference state (shape, drop mode, XP mode) into a dedicated `VeinMinerPlayerPrefs` class, and move the block equivalence map to `UltmineShapeCalculator` or a dedicated `VeinMinerBlocks` class.

---

### Duplicated `stats.lastAbilityUse = worldTime` Assignment in 4 Skill Implementations

The following skill files contain duplicated assignment of `stats.lastAbilityUse = worldTime` on consecutive lines (copy-paste artifacts from refactoring):

- `src/main/java/com/murilloskills/impl/ArcherSkill.java:73-75` (assigned twice, labelled "4. Ativa" both times)
- `src/main/java/com/murilloskills/impl/BlacksmithSkill.java:82-84` (assigned twice)
- `src/main/java/com/murilloskills/impl/FisherSkill.java:76-78` (assigned twice)
- `src/main/java/com/murilloskills/impl/WarriorSkill.java:77-79` (assigned twice)

Impact: Functionally harmless (idempotent assignment), but indicates the ability activation code was duplicated during development. The duplicate commented headers ("4. Ativa...") also appear in the same blocks.

Fix approach: Remove one of each duplicate assignment and comment.

---

### `isCreativeBrushActive(UUID)` and `isCreativeBrushActive(ServerPlayerEntity)` Are Inconsistent

`BuilderSkill` exposes two overloads:

- `isCreativeBrushActive(ServerPlayerEntity)` at `src/main/java/com/murilloskills/impl/BuilderSkill.java:295` — checks both map presence AND elapsed time against duration
- `isCreativeBrushActive(UUID)` at line 307 — only checks map presence, not elapsed time

Code that calls the UUID overload (e.g., in the `onTick` timer check at line 224) gets a stale "active" state even after the ability logically expired. The `onTick` handler immediately calls `endCreativeBrush()` in that case, so it is not catastrophic, but the inconsistency makes the API confusing.

Fix approach: Consolidate to one implementation or at least document the divergent behavior explicitly.

---

### Synergy Bitmask for "Synergy Master" Only Tracks 7 of 14 Synergies

`SkillSelectionNetworkHandler.trackSynergyForMaster()` at `src/main/java/com/murilloskills/network/handlers/SkillSelectionNetworkHandler.java:131-158` maps only 7 synergy IDs to bit positions. `SkillSynergyManager.getAll()` at `src/main/java/com/murilloskills/utils/SkillSynergyManager.java:29` now returns 14 synergies (the 7 original + 7 new ones: survivor, industrial, sea_warrior, green_archer, prospector, adventurer, and others). The "Synergy Master" check (`newMask == 127`) requires all 7 original synergies to be activated, ignoring the 7 newer ones. New synergies with unrecognized IDs fall through to `default -> 0` and are silently discarded.

Impact: The Synergy Master advancement is still achievable with the original 7 synergies, but any intent to require the new synergies is silently broken.

Fix approach: Either add bit positions for the new synergies and update the `== 127` check accordingly, or document that Synergy Master intentionally only requires the original 7.

---

### `EnchantmentHelperMixin` Applies Warrior Looting Even When Warrior Is Not Selected

`src/main/java/com/murilloskills/mixin/EnchantmentHelperMixin.java:31-36` reads the Warrior level and computes a looting bonus without checking `playerData.isSkillSelected(MurilloSkillsList.WARRIOR)`. A player who had Warrior as a skill, reached level 60, then reset that skill still has `level > 0` in the map (reset only sets to 0 via the reset handler, so freshly joined players without Warrior selected also start at 0 — but if any admin command sets levels directly, the check is bypassed).

Fix approach: Add `playerData.isSkillSelected(MurilloSkillsList.WARRIOR)` guard matching the pattern used in `LivingEntityMixin.applyResistance()`.

---

### `DailyChallengeManager` Uses an Unsafe Cast

`src/main/java/com/murilloskills/utils/DailyChallengeManager.java:43`: `(ServerWorld) player.getEntityWorld()` casts without a check. If this is ever called while the player is in the client world (which the caller already guards against via separate paths), it would throw a `ClassCastException`.

Fix approach: Use `if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) return;` and pass `serverWorld` to `getCurrentGameDay()`.

---

### `BuilderSkill.handleCreativeBrushPlacement` Has a Hardcoded Block Limit

The 1000-block limit for Creative Brush fill operations is hardcoded at `src/main/java/com/murilloskills/impl/BuilderSkill.java:390`:
```java
int maxBlocks = 1000;
```
This is the only configurable parameter not exposed through `ModConfig`/`SkillConfig`, making it impossible to tune on live servers without recompiling.

Fix approach: Add `builderMaxFillBlocks` to `BuilderConfig` in `ModConfig.java` and a getter in `SkillConfig.java`.

---

## Low Priority

### `ExplorerSkill.TREASURE_HUNTER_DURATION_SECONDS` Is a Hardcoded Private Constant

`src/main/java/com/murilloskills/impl/ExplorerSkill.java:60`: `private static final int TREASURE_HUNTER_DURATION_SECONDS = 60;` is not exposed in `ModConfig` or `SkillConfig`. Other similar ability durations (e.g., Fisher Rain Dance, Farmer Harvest Moon) are configurable.

Fix approach: Add `treasureHunterDurationSeconds` to `ExplorerConfig` in `ModConfig.java`.

---

### `PrestigeManager.java` Has Triple-Duplicated Comment Block

`src/main/java/com/murilloskills/utils/PrestigeManager.java:25-28` contains three sequential `// Configurações` comment lines that are identical, a sign of repeated incomplete edits.

Fix approach: Remove the duplicates.

---

### `ExplorerSkill.awardXp` Has a Commented-Out Line of Dead Code

`src/main/java/com/murilloskills/impl/ExplorerSkill.java:238`: `// state.markDirty(); // Auto-persisted` is a leftover from the legacy `PersistentState` system. The migration to Fabric Data Attachments was completed and `markDirty()` is no longer needed.

Fix approach: Delete the comment.

---

### Test Coverage is Almost Nonexistent

Only one test file exists: `src/test/java/com/murilloskills/skills/UltmineShapeCalculatorTest.java` — and it covers only the pure `UltmineShapeCalculator` shape math. No test coverage exists for:

- `PlayerSkillData` XP addition logic and level cap enforcement
- `PrestigeManager.canPrestige()` / `doPrestige()` edge cases
- `VeinMinerHandler` block collection limits
- All 16 C2S network handlers (validation logic)
- `SkillSynergyManager.getActiveSynergies()`
- `DailyChallengeManager` challenge generation and progress recording
- `LegacyDataMigration` format parsing

Risk: Regressions in core progression logic (XP formulas, prestige resets, paragon gating) go undetected until a player reports them. The project's stated validation approach is `./gradlew runClient`, which requires manual testing of every path.

Priority: Medium-High. Start with `PlayerSkillData.addXpToSkill()` and `PrestigeManager` since they touch persistent player data.

---

### `SQUARE_20x20_D1` Ultmine Shape Can Break Up to 400 Blocks Per Use

`UltmineShape.SQUARE_20x20_D1` at `src/main/java/com/murilloskills/skills/UltmineShape.java:11` defines a 20x20 = 400 block area. With `depth = 4` (the configured default maximum at `ModConfig.java:272`), this is 1600 block breaks in a single player action. The `maxBlocksPerUse` config at `ModConfig.UltmineConfig` provides a cap, but the default value must be set conservatively to avoid server lag. Currently the config allows this shape to be selected from the radial menu without verifying `maxBlocksPerUse` is properly set.

Files: `src/main/java/com/murilloskills/skills/UltmineShapeCalculator.java:48`, `src/main/java/com/murilloskills/config/ModConfig.java:272`

Fix approach: Document clearly that `ultmine.maxBlocksPerUse` must be tuned if `SQUARE_20x20_D1` is available to players. Consider removing this shape from the default radial menu or gating it behind a separate permission.

---

### `SkillsScreen.java` Uses `instanceof` Pattern With Concrete Logger Class Name in `AbstractSkill`

The `LOGGER` field in `AbstractSkill` at `src/main/java/com/murilloskills/api/AbstractSkill.java:22` uses `getClass().getSimpleName()` for the logger name, which works correctly. However, it is `protected` and non-final, allowing subclasses to shadow it. The actual concern is that no skill currently overrides it (the behavior is correct), but a new skill author could mistakenly redeclare it.

Fix approach: Change `protected final Logger LOGGER` to document that it must not be redeclared, or make it package-private with an accessor.

---

### Legacy `LegacyDataMigration` Has No Cleanup Path

`src/main/java/com/murilloskills/data/LegacyDataMigration.java` runs a file read on every player join (with early return if new data is present). Once all players have been migrated, the migration code reads the `murilloskills.dat` file existence check on every join indefinitely.

Fix approach: After migration is complete for all players on a server, the legacy file can be renamed or deleted. Add a note or config option to disable migration checks entirely for servers that have already migrated all players (e.g., by checking a `migrationVersion` marker in world data).

---

*Concerns audit: 2026-03-24*
