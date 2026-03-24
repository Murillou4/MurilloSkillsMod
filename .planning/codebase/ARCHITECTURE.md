# Architecture

**Analysis Date:** 2026-03-24

## Pattern Overview

**Overall:** Strategy + Registry pattern for skill dispatch; event-driven XP collection via Fabric API and Mixins; client-server split with explicit S2C/C2S network payloads.

**Key Characteristics:**
- All 8 skills extend `AbstractSkill` and register into a static `EnumMap`-backed `SkillRegistry`
- Player data persists as a Fabric Data Attachment (`ModAttachments.PLAYER_SKILLS`) — survives death, auto-serialized via Mojang Codec
- Client never touches server state directly; all changes go through named `CustomPayload` packets
- XP is collected by dedicated event handlers and XpGetter utility classes, never inside skill implementations
- Configuration is externalised to `config/murilloskills.json` (Gson); all tunable values accessed through `SkillConfig` static getters

## Layers

**API Layer:**
- Purpose: Defines the skill contract and the central registry
- Location: `src/main/java/com/murilloskills/api/`
- Contains: `AbstractSkill.java`, `SkillRegistry.java`
- Depends on: `skills/MurilloSkillsList`, `data/PlayerSkillData`
- Used by: `impl/`, `events/`, `network/handlers/`

**Skill Implementations Layer:**
- Purpose: Concrete skill behaviour — passive bonuses, active ability, tick logic
- Location: `src/main/java/com/murilloskills/impl/`
- Contains: `MinerSkill`, `WarriorSkill`, `ArcherSkill`, `FarmerSkill`, `FisherSkill`, `BlacksmithSkill`, `BuilderSkill`, `ExplorerSkill`, `BuilderFillMode`
- Depends on: `api/AbstractSkill`, `data/ModAttachments`, `utils/SkillConfig`, `network/` payloads for S2C feedback
- Used by: `SkillRegistry` (looked up by enum key); dispatched from `events/MinecraftEventsListener`

**Data Layer (Server):**
- Purpose: Persistent per-player RPG state
- Location: `src/main/java/com/murilloskills/data/`
- Contains: `PlayerSkillData` (root object), inner `SkillStats` record, `XpAddResult` record, `ModAttachments` (Fabric attachment registration), `LegacyDataMigration`
- Key design: `PlayerSkillData.addXpToSkill()` is the single place where XP caps, prestige multipliers, and paragon constraints are enforced
- Serialization: Mojang `Codec` + `RecordCodecBuilder`; stored per-player via Fabric Attachments API with `copyOnDeath()`

**Data Layer (Client):**
- Purpose: Client-side mirror of server state; consumed by all GUI screens
- Location: `src/client/java/com/murilloskills/data/`
- Contains: `ClientSkillData` (static fields, updated by network handlers), `UltmineClientState`
- Design: Static utility class; GUI screens read from it without network round-trips

**Event / XP Collection Layer:**
- Purpose: Intercept Minecraft actions and route XP to the correct skill
- Location: `src/main/java/com/murilloskills/events/` and `src/main/java/com/murilloskills/skills/`
- Contains:
  - `MinecraftEventsListener` — registers Fabric API events (block break, mob kill, player join/respawn/tick)
  - `BlockBreakHandler` — awards Miner/Farmer XP on block break
  - `CropHarvestHandler` — awards Farmer XP specifically for crops
  - `MobKillHandler` — awards Warrior/Archer XP on kills
  - `FishingCatchHandler` — awards Fisher XP on fish catch
  - `ArcherHitHandler` — awards Archer XP on arrow hits
  - `VeinMinerHandler` — manages Vein Miner / Ultmine multi-break
  - `BlockPlacementHandler`, `ChallengeEventsHandler`, `DimensionChangeHandler`
- XP amount decisions are delegated to `utils/*XpGetter` classes, not made in handlers

**XpGetter Utilities:**
- Purpose: Return XP amounts for specific blocks/actions, keeping handlers logic-free
- Location: `src/main/java/com/murilloskills/utils/`
- Files: `MinerXpGetter`, `WarriorXpGetter`, `ArcherXpGetter`, `FarmerXpGetter`, `FisherXpGetter`, `BlacksmithXpGetter`, `BuilderXpGetter`, `ExplorerXpGetter`
- Return type: `SkillReceptorResult` (`models/SkillReceptorResult.java`) — boolean `didGainXp()` + int `getXpAmount()`

**Mixin Layer:**
- Purpose: Hook into Minecraft internals not reachable via Fabric events
- Location: `src/main/java/com/murilloskills/mixin/` (32 server-side mixins) and `src/client/java/com/murilloskills/mixin/client/` (1 stub)
- Config: `src/main/resources/murilloskills.mixins.json` and `src/client/resources/murilloskills.client.mixins.json`
- Notable mixins: `EnchantmentHelperMixin` (Blacksmith fortune bonus), `LivingEntityMixin` (Warrior damage reduction), `BuilderBlockPlacementMixin` (Builder area-fill), `FishingBobberEntityMixin` (Fisher bonuses), `AnvilScreenHandlerMixin` / `EnchantmentScreenHandlerMixin` (Blacksmith cost reduction), `PlayerEntityReachMixin` (Explorer reach extension)

**Networking Layer:**
- Purpose: Typed, bidirectional packet exchange; 9 S2C + 15 C2S payloads
- Location: `src/main/java/com/murilloskills/network/`
- Registration: `ModNetwork.register()` called in `MurilloSkills.onInitialize()`
- Handler registration: `NetworkHandlerRegistry.registerAll()` wires 16 C2S handlers via `ServerPlayNetworking.registerGlobalReceiver()`
- Handler classes: `src/main/java/com/murilloskills/network/handlers/` (16 handler classes)
- Client receivers: registered inline in `MurilloSkillsClient.onInitializeClient()`

**Progression Utilities:**
- Purpose: Cross-cutting progression systems layered on top of raw XP
- Location: `src/main/java/com/murilloskills/utils/`
- Contains:
  - `PrestigeManager` — prestige gating, XP multiplier, passive multiplier
  - `DailyChallengeManager` — per-player daily challenge generation + progress tracking (in-memory `Map<UUID, PlayerChallengeData>`)
  - `SkillSynergyManager` — 7+ two-skill synergy definitions, checked at XP/attribute application time
  - `XpStreakManager` — consecutive-action XP combo multiplier (in-memory, resets on disconnect)
  - `AchievementTracker` + `AdvancementGranter` — integration with vanilla Minecraft advancement system

**Configuration Layer:**
- Purpose: External tuning of all gameplay values
- Location: `src/main/java/com/murilloskills/config/ModConfig.java`, `src/main/java/com/murilloskills/utils/SkillConfig.java`
- Pattern: `ModConfig` holds nested POJO classes serialized by Gson to `config/murilloskills.json`. `SkillConfig` exposes typed static getters over `ModConfig.get()`. All gameplay code reads through `SkillConfig`, never through `ModConfig` directly.
- XP curve: `SkillConfig.getXpForLevel(level)` → `base + level*multiplier + exponent*level²` (defaults: 60, 15, 2)

**GUI / Client Rendering Layer:**
- Purpose: Player-facing screens and HUD overlays; client-only
- Location: `src/client/java/com/murilloskills/gui/` and `src/client/java/com/murilloskills/render/`
- Screens: `SkillsScreen` (main RPG screen), `ModInfoScreen` (help/guide), `OreFilterScreen` (Miner ore whitelist), `UltmineRadialMenuScreen` (shape picker radial), `UltmineConfigScreen`, `ConfirmationScreen`
- Renderers: `OreHighlighter` (world-space ore outlines), `TreasureHighlighter`, `VeinMinerPreview`, `UltminePreview`, `XpToastRenderer` (HUD XP popups), `RainDanceEffect`, `AreaPlantingHud`
- GUI sub-system: `gui/renderer/RenderingHelper`, `gui/renderer/TabRenderer`, `gui/renderer/RenderContext`; `gui/data/SkillUiData` (perk/synergy definitions for display)

**Admin Commands Layer:**
- Purpose: Operator tooling
- Location: `src/main/java/com/murilloskills/commands/SkillAdminCommands.java`
- Commands: `/skill setlevel <target> <skill> <level>`, `/skill setprestige`, `/skill addxp`, `/skill reset`, `/skill paragon`
- Requires OP level 2

## Data Flow

**XP Gain (most common path):**

1. Player action triggers a Fabric event (e.g. `PlayerBlockBreakEvents.AFTER`) or a Mixin `@Inject`
2. Handler class (`BlockBreakHandler`, `MobKillHandler`, etc.) checks `world.isClient()` → skips on client
3. Handler calls the corresponding `XpGetter` (e.g. `MinerXpGetter.isMinerXpBlock()`) which returns `SkillReceptorResult`
4. `XpStreakManager.applyStreakBonus()` is applied to the base XP
5. `playerData.addXpToSkill(skill, amount)` is called — this applies prestige multiplier, enforces paragon cap, and returns `XpAddResult`
6. If `xpResult.leveledUp()`, the handler calls `SkillRegistry.get(skill).onLevelUp(player, newLevel)` (which calls `updateAttributes()` internally)
7. `AchievementTracker` and `DailyChallengeManager` are notified of the action
8. `SkillsNetworkUtils.syncSkills(player)` sends a `SkillsSyncPayload` S2C
9. `XpToastSender` sends an `XpGainS2CPayload` S2C for the HUD toast

**Skill Selection (first-time setup):**

1. Client sends `SkillSelectionC2SPayload` (up to 3 skills)
2. `SkillSelectionNetworkHandler` validates and calls `playerData.setSelectedSkills()`
3. `SkillRegistry.get(skill).onPlayerJoin(player, level)` called for each new skill to apply passive attributes
4. `SkillsNetworkUtils.syncSkills()` pushes updated state to client
5. `ClientSkillData.setSelectedSkills()` stores it; `SkillsScreen` refreshes if open

**Active Ability Trigger:**

1. Player presses ability keybind → `MurilloSkillsClient` sends `SkillAbilityC2SPayload`
2. `AbilityNetworkHandler` resolves the player's paragon skill → calls `SkillRegistry.get(paragon).onActiveAbility(player, stats)`
3. Skill implementation executes the ability (e.g. `MinerSkill.onActiveAbility()` scans for ores)
4. S2C payload sent back for client visual feedback (e.g. `MinerScanResultPayload` → `OreHighlighter`)

**Server Tick → Passive Effects:**

1. `ServerTickEvents.END_SERVER_TICK` fires every tick
2. `MinecraftEventsListener.playerTickListen()` iterates all online players
3. For each player's selected skills, calls `SkillRegistry.get(skill).onTick(player, level)`
4. Each skill's `onTick` applies recurring effects (e.g. night vision grant, step-assist check)

**Player Join / Respawn:**

1. `ServerPlayConnectionEvents.JOIN` fires
2. `LegacyDataMigration.migrateIfNeeded()` checks for old `murilloskills.dat` and migrates if present
3. `handlePlayerJoin()` calls `onPlayerJoin()` on each selected skill → reapplies all attributes
4. `SkillsNetworkUtils.syncSkills()` syncs full state to the newly connected client
5. `DailyChallengeManager.syncChallenges()` syncs daily challenge state

**State Management:**
- Server: `PlayerSkillData` persisted automatically as a Fabric attachment (NBT, per-world-save)
- Client: `ClientSkillData` static fields updated on every `SkillsSyncPayload` receipt; no persistence
- In-memory only: `DailyChallengeManager.playerChallenges`, `XpStreakManager.playerStreaks`, `VeinMinerHandler` per-player state maps — all cleared on disconnect

## Key Abstractions

**AbstractSkill:**
- Purpose: Contract for all skill implementations; provides default no-op implementations with error handling
- Location: `src/main/java/com/murilloskills/api/AbstractSkill.java`
- Override hooks: `getSkillType()` (required), `onLevelUp()`, `onActiveAbility()`, `updateAttributes()`, `onTick()`, `onPlayerJoin()`
- Pattern: Template Method — base class handles error logging; subclasses provide logic

**SkillRegistry:**
- Purpose: Type-safe EnumMap from `MurilloSkillsList` → `AbstractSkill`; sole dispatch point
- Location: `src/main/java/com/murilloskills/api/SkillRegistry.java`
- Usage: `SkillRegistry.get(MurilloSkillsList.MINER)` returns `MinerSkill` instance
- All skills registered in `MurilloSkills.registerSkills()` during `onInitialize()`

**MurilloSkillsList:**
- Purpose: Enum serving as the canonical key for all skill lookups, network serialization, data storage
- Location: `src/main/java/com/murilloskills/skills/MurilloSkillsList.java`
- Values: `FARMER, FISHER, ARCHER, MINER, BUILDER, BLACKSMITH, EXPLORER, WARRIOR`

**PlayerSkillData:**
- Purpose: Complete server-side player state for the mod
- Location: `src/main/java/com/murilloskills/data/PlayerSkillData.java`
- Fields: `EnumMap<MurilloSkillsList, SkillStats> skills`, `MurilloSkillsList paragonSkill`, `List<MurilloSkillsList> selectedSkills`, `Map<String, Boolean> skillToggles`, `Map<String, Integer> achievementStats`
- Accessed via: `player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS)`
- Inner types: `SkillStats` (level, xp, lastAbilityUse, prestige), `XpAddResult` record

**SkillReceptorResult:**
- Purpose: Return value from XpGetter calls — signals whether XP was earned and how much
- Location: `src/main/java/com/murilloskills/models/SkillReceptorResult.java`
- Pattern: Value object; handlers check `.didGainXp()` before proceeding

**ModNetwork:**
- Purpose: Single registration point for all 24 packet types
- Location: `src/main/java/com/murilloskills/network/ModNetwork.java`
- S2C payloads (9): `SkillsSyncPayload`, `XpGainS2CPayload`, `MinerScanResultPayload`, `RainDanceS2CPayload`, `TreasureHunterS2CPayload`, `AreaPlantingSyncS2CPayload`, `DailyChallengesSyncS2CPayload`, `UltminePreviewS2CPayload`, `UltmineResultS2CPayload`
- C2S payloads (15): `SkillAbilityC2SPayload`, `ParagonActivationC2SPayload`, `SkillSelectionC2SPayload`, `SkillResetC2SPayload`, `PrestigeC2SPayload`, `AreaPlantingToggleC2SPayload`, `HollowFillToggleC2SPayload`, `VeinMinerToggleC2SPayload`, `NightVisionToggleC2SPayload`, `StepAssistToggleC2SPayload`, `FillModeCycleC2SPayload`, `VeinMinerDropsToggleC2SPayload`, `UltmineShapeSelectC2SPayload`, `UltmineRequestC2SPayload`, `XpDirectToggleC2SPayload`
- Each payload is a Java record implementing `CustomPayload` with static `ID` and `CODEC` fields

## Entry Points

**Server Initializer:**
- Location: `src/main/java/com/murilloskills/MurilloSkills.java`
- Triggers: Fabric `ModInitializer.onInitialize()`
- Responsibilities: Load config → register skills → register attachments → register items → register event handlers → register commands → register network payloads → register network handlers → validate registry

**Client Initializer:**
- Location: `src/client/java/com/murilloskills/MurilloSkillsClient.java`
- Triggers: Fabric `ClientModInitializer.onInitializeClient()`
- Responsibilities: Register tooltip appender → register S2C packet receivers (inline lambdas) → register join event (config sync) → register 10 keybindings → register HUD render callbacks → register world render callbacks → register client tick listener

**Mod ID:** `murilloskills` (used as namespace for all identifiers and assets)

## Error Handling

**Strategy:** All `AbstractSkill` lifecycle methods are wrapped in `try-catch(Exception)` — a single bad skill cannot crash the server.

**Patterns:**
- Tick errors: logged at most once every 100–200 ticks per player to prevent log spam
- Network handler registration: any failure throws `RuntimeException` to abort startup with a clear message
- Data deserialization: Codec parses with `optionalFieldOf` everywhere; unknown enum names are silently skipped (forward compatibility)
- Startup validation: `SkillRegistry.validateRegistration()` logs errors if any expected skill is missing

## Cross-Cutting Concerns

**Logging:** SLF4J via `LoggerFactory.getLogger("MurilloSkills-{Component}")` — each class creates its own named logger. Logger names: `MurilloSkills`, `MurilloSkills-Registry`, `MurilloSkills-Events`, `MurilloSkills-NetworkRegistry`, `MurilloSkills-Admin`, `MurilloSkills-Migration`.

**Validation:** Done at the call site in handlers (level requirements, cooldowns, paragon checks). `PlayerSkillData.addXpToSkill()` is the central enforcement point for XP rules.

**Authentication / Permission:** Admin commands require OP level 2 (`source.hasPermissionLevel(2)`). No player-facing permission checks beyond the paragon/selection constraints in `PlayerSkillData`.

**Internationalisation:** All player-facing strings use `Text.translatable("murilloskills.*")`. Translation files at `src/main/resources/assets/murilloskills/lang/en_us.json`, `pt_br.json`, `es_es.json`.

---

*Architecture analysis: 2026-03-24*
