# Codebase Structure

**Analysis Date:** 2026-03-24

## Directory Layout

```
MurilloSkillsMod/
├── src/
│   ├── main/                          # Server-side and common code
│   │   ├── java/com/murilloskills/
│   │   │   ├── MurilloSkills.java     # Server mod initializer (entry point)
│   │   │   ├── api/                   # AbstractSkill + SkillRegistry
│   │   │   ├── commands/              # /skill admin commands
│   │   │   ├── config/                # ModConfig (Gson-based external config)
│   │   │   ├── data/                  # PlayerSkillData, ModAttachments, migration
│   │   │   ├── events/                # Fabric API event registrations
│   │   │   ├── impl/                  # Concrete skill implementations (8 skills)
│   │   │   ├── item/                  # ModItems (placeholder)
│   │   │   ├── mixin/                 # 32 server-side Mixin classes
│   │   │   ├── models/                # Shared value objects (SkillReceptorResult)
│   │   │   ├── network/               # Payload definitions (24 packets)
│   │   │   │   └── handlers/          # 16 C2S network handler classes
│   │   │   ├── skills/                # MurilloSkillsList enum + XP event handlers
│   │   │   └── utils/                 # XpGetters, PrestigeManager, DailyChallenges, etc.
│   │   └── resources/
│   │       ├── fabric.mod.json        # Mod metadata + entrypoints + mixin configs
│   │       ├── murilloskills.mixins.json
│   │       ├── assets/murilloskills/
│   │       │   ├── lang/              # en_us.json, pt_br.json, es_es.json
│   │       │   └── textures/gui/      # Advancement background textures
│   │       └── data/murilloskills/
│   │           └── advancement/       # JSON advancement definitions (per skill)
│   ├── client/                        # Client-only code (cannot be imported by main)
│   │   ├── java/com/murilloskills/
│   │   │   ├── MurilloSkillsClient.java  # Client mod initializer
│   │   │   ├── client/config/         # OreFilterConfig, UltmineClientConfig
│   │   │   ├── data/                  # ClientSkillData, UltmineClientState
│   │   │   ├── gui/                   # All Screen classes
│   │   │   │   ├── data/              # SkillUiData (perk/synergy display data)
│   │   │   │   └── renderer/          # RenderingHelper, TabRenderer, RenderContext
│   │   │   ├── mixin/client/          # Client mixin stub (ExampleClientMixin)
│   │   │   ├── render/                # HUD overlays and world renderers
│   │   │   └── tooltip/               # SkillTooltipAppender
│   │   └── resources/
│   │       └── murilloskills.client.mixins.json
│   └── test/
│       └── java/com/murilloskills/skills/   # Unit test stubs (JUnit 5)
├── build.gradle                       # Fabric Loom build config
├── gradle.properties                  # Versions: MC, Fabric, mod
├── settings.gradle
├── CLAUDE.md                          # Project guidance for Claude
├── DEFAULT_XP_VALUES.txt              # Reference sheet for XP tuning
└── CHANGELOG.md
```

## Directory Purposes

**`src/main/java/com/murilloskills/api/`:**
- Purpose: Core skill contract and registry
- Key files: `AbstractSkill.java`, `SkillRegistry.java`
- Rule: Only the API and `impl/` classes depend on this package; never `mixin/` or `network/`

**`src/main/java/com/murilloskills/impl/`:**
- Purpose: One class per skill; all gameplay behaviour for that skill
- Key files: `MinerSkill.java`, `WarriorSkill.java`, `ArcherSkill.java`, `FarmerSkill.java`, `FisherSkill.java`, `BlacksmithSkill.java`, `BuilderSkill.java`, `ExplorerSkill.java`, `BuilderFillMode.java`
- Pattern: Each file extends `AbstractSkill`, overrides relevant lifecycle hooks, and reads config via `SkillConfig`

**`src/main/java/com/murilloskills/data/`:**
- Purpose: Server-side persistent state and data migration
- Key files: `PlayerSkillData.java` (full player state + Codec), `ModAttachments.java` (Fabric attachment registration), `LegacyDataMigration.java`
- Rule: Access player data only via `player.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS)` — never pass `PlayerSkillData` across the network boundary

**`src/main/java/com/murilloskills/skills/`:**
- Purpose: Enum definition and XP event handler classes
- Key files: `MurilloSkillsList.java` (8-value enum), `BlockBreakHandler.java`, `MobKillHandler.java`, `CropHarvestHandler.java`, `FishingCatchHandler.java`, `ArcherHitHandler.java`, `VeinMinerHandler.java`, `UltmineShape.java`, `UltmineShapeCalculator.java`

**`src/main/java/com/murilloskills/events/`:**
- Purpose: Fabric event registrations that cannot be done in mixins
- Key files: `MinecraftEventsListener.java` (main hub, call `initAllListeners()` from here), `BlockPlacementHandler.java`, `ChallengeEventsHandler.java`, `DimensionChangeHandler.java`

**`src/main/java/com/murilloskills/mixin/`:**
- Purpose: SpongePowered Mixin hooks into Minecraft internals
- 32 server-side mixin classes; notable examples:
  - `EnchantmentHelperMixin.java` — Blacksmith fortune/cost hooks
  - `LivingEntityMixin.java` — Warrior damage reduction
  - `BuilderBlockPlacementMixin.java` — Builder area-fill intercept
  - `FishingBobberEntityMixin.java` — Fisher wait-time and catch bonuses
  - `DamageDealtMixin.java` — Warrior damage output bonus
  - `PlayerEntityReachMixin.java` — Explorer extended reach
  - `ScaffoldingClimbMixin.java`, `MagmaBlockMixin.java`, `SoulSandMixin.java` — Explorer environmental bonuses
  - `ItemStackMixin.java`, `ItemEntityFishingMixin.java` — Fisher item bonuses
  - `CraftingResultMixin.java`, `FurnaceOutputMixin.java` — Blacksmith crafting/smelting hooks

**`src/main/java/com/murilloskills/network/`:**
- Purpose: One Java record per packet type; all implement `CustomPayload`
- Naming convention: `{Purpose}{Direction}Payload` — e.g. `SkillAbilityC2SPayload`, `SkillsSyncPayload` (S2C)
- Each record has: static `ID` field (`CustomPayload.Id`), static `CODEC` field (`PacketCodec`)

**`src/main/java/com/murilloskills/network/handlers/`:**
- Purpose: C2S handler implementations; one class per logical operation
- Key files: `NetworkHandlerRegistry.java` (registers all 16 handlers), then individual handlers like `AbilityNetworkHandler.java`, `SkillSelectionNetworkHandler.java`, `PrestigeNetworkHandler.java`, `UltmineRequestNetworkHandler.java`
- Pattern: Each handler class has a static `create()` factory that returns a `PlayPayloadHandler<T>` lambda

**`src/main/java/com/murilloskills/utils/`:**
- Purpose: Stateless helper functions and stateful progression managers
- XpGetter files (8): `MinerXpGetter.java`, `WarriorXpGetter.java`, `ArcherXpGetter.java`, `FarmerXpGetter.java`, `FisherXpGetter.java`, `BlacksmithXpGetter.java`, `BuilderXpGetter.java`, `ExplorerXpGetter.java`
- Managers: `PrestigeManager.java`, `DailyChallengeManager.java`, `SkillSynergyManager.java`, `XpStreakManager.java`, `AchievementTracker.java`, `AdvancementGranter.java`
- Config access: `SkillConfig.java` (all static getters over `ModConfig.get()`)
- Notifiers: `SkillNotifier.java`, `XpToastSender.java`, `SkillsNetworkUtils.java`, `VanillaXpRewarder.java`

**`src/main/java/com/murilloskills/config/`:**
- Purpose: External config file loading with Gson
- Key file: `ModConfig.java` — nested static inner classes per skill (`MinerConfig`, `WarriorConfig`, etc.); root `ConfigData` class; file at `config/murilloskills.json`

**`src/main/java/com/murilloskills/commands/`:**
- Purpose: Operator admin commands
- Key file: `SkillAdminCommands.java` — registers `/skill` subcommands via Brigadier

**`src/main/java/com/murilloskills/models/`:**
- Purpose: Shared value objects used across layers
- Key file: `SkillReceptorResult.java` — returned by XpGetter methods

**`src/client/java/com/murilloskills/data/`:**
- Purpose: Client-side state mirror
- Key files: `ClientSkillData.java` (static fields, updated from S2C packets), `UltmineClientState.java` (ultmine preview block positions)

**`src/client/java/com/murilloskills/gui/`:**
- Purpose: All Minecraft Screen subclasses
- Key files:
  - `SkillsScreen.java` — main RPG skill screen (select, prestige, paragon)
  - `ModInfoScreen.java` — guide/help tab screen
  - `OreFilterScreen.java` — Miner ore highlight filter
  - `UltmineRadialMenuScreen.java` — radial shape selector
  - `UltmineConfigScreen.java` — Ultmine settings
  - `ConfirmationScreen.java` — reusable yes/no confirmation dialog
  - `ColorPalette.java` — shared colour constants for all screens
  - `ScrollController.java` — reusable scroll state

**`src/client/java/com/murilloskills/gui/data/`:**
- Purpose: UI-only data (perk definitions, synergy display info, guide entries)
- Key file: `SkillUiData.java` — static maps of `PerkInfo`, `SynergyInfo`, `GuideEntry`, `XpSourceInfo` records consumed by both `SkillsScreen` and `ModInfoScreen`

**`src/client/java/com/murilloskills/gui/renderer/`:**
- Purpose: Reusable GUI rendering helpers
- Key files: `RenderingHelper.java`, `TabRenderer.java`, `RenderContext.java`

**`src/client/java/com/murilloskills/render/`:**
- Purpose: HUD overlays and world-space renderers registered via Fabric rendering callbacks
- Key files:
  - `OreHighlighter.java` — renders highlight boxes around ores found by Miner ability
  - `TreasureHighlighter.java` — Explorer treasure chest highlights
  - `VeinMinerPreview.java` — outlines blocks that would be vein-mined
  - `UltminePreview.java` — outlines the current Ultmine selection shape
  - `XpToastRenderer.java` — floating XP gain numbers on HUD
  - `RainDanceEffect.java` — Fisher Rain Dance visual effect
  - `AreaPlantingHud.java` — Farmer area-planting indicator

**`src/client/java/com/murilloskills/client/config/`:**
- Purpose: Client-local config (not synced to server)
- Key files: `OreFilterConfig.java`, `UltmineClientConfig.java`

**`src/client/java/com/murilloskills/tooltip/`:**
- Purpose: Item tooltip extension for skill-related items
- Key file: `SkillTooltipAppender.java`

**`src/main/resources/data/murilloskills/advancement/`:**
- Purpose: Vanilla advancement JSON definitions, one sub-folder per skill
- Directories: `archer/`, `blacksmith/`, `builder/`, `explorer/`, `farmer/`, `fisher/`, `miner/`, `warrior/`, `special/`
- Granted programmatically via `AdvancementGranter.java`

**`src/test/java/com/murilloskills/skills/`:**
- Purpose: JUnit 5 unit test stubs
- Generated: No
- Status: Stubs present; no meaningful test coverage; see TESTING.md

## Key File Locations

**Entry Points:**
- `src/main/java/com/murilloskills/MurilloSkills.java` — server initializer
- `src/client/java/com/murilloskills/MurilloSkillsClient.java` — client initializer

**Configuration:**
- `src/main/java/com/murilloskills/config/ModConfig.java` — config POJO + file I/O
- `src/main/java/com/murilloskills/utils/SkillConfig.java` — typed getters used by all gameplay code
- `src/main/resources/fabric.mod.json` — mod metadata
- `gradle.properties` — dependency versions

**Core Logic:**
- `src/main/java/com/murilloskills/api/AbstractSkill.java` — skill interface
- `src/main/java/com/murilloskills/api/SkillRegistry.java` — EnumMap registry
- `src/main/java/com/murilloskills/data/PlayerSkillData.java` — complete player state + Codec
- `src/main/java/com/murilloskills/data/ModAttachments.java` — Fabric attachment type declaration
- `src/main/java/com/murilloskills/skills/MurilloSkillsList.java` — the 8-skill enum
- `src/main/java/com/murilloskills/events/MinecraftEventsListener.java` — Fabric event hub

**Networking:**
- `src/main/java/com/murilloskills/network/ModNetwork.java` — payload registration
- `src/main/java/com/murilloskills/network/handlers/NetworkHandlerRegistry.java` — handler registration

**Mixin Configs:**
- `src/main/resources/murilloskills.mixins.json`
- `src/client/resources/murilloskills.client.mixins.json`

**Translations:**
- `src/main/resources/assets/murilloskills/lang/en_us.json`
- `src/main/resources/assets/murilloskills/lang/pt_br.json`
- `src/main/resources/assets/murilloskills/lang/es_es.json`

## Naming Conventions

**Files:**
- Skill implementations: `{SkillName}Skill.java` (e.g. `MinerSkill.java`)
- XP calculators: `{SkillName}XpGetter.java` (e.g. `MinerXpGetter.java`)
- Network payloads: `{Purpose}{Direction}Payload.java` where direction is `C2S` or `S2C` (e.g. `SkillAbilityC2SPayload.java`, `XpGainS2CPayload.java`; S2C-only payloads may omit direction suffix)
- Network handlers: `{Feature}NetworkHandler.java` (e.g. `AbilityNetworkHandler.java`)
- Mixin classes: `{TargetClass}Mixin.java` or `{TargetClass}Accessor.java` (e.g. `LivingEntityMixin.java`, `ForgingScreenHandlerAccessor.java`)
- Screen classes: `{Feature}Screen.java` (e.g. `SkillsScreen.java`)
- Renderers: `{Feature}Renderer.java` or `{Feature}Highlighter.java` or `{Feature}Preview.java`

**Directories:**
- Lowercase, no hyphens (standard Java package convention)
- `impl/` holds skill classes (not `skills/`; that holds the enum and XP handlers)
- `utils/` holds both stateless helpers and stateful in-memory managers

**Classes:**
- PascalCase for all class names
- Static factory methods on handlers named `create()`
- Payload record fields: camelCase, accessed via generated record accessors

**Translation keys:**
- Pattern: `murilloskills.{category}.{subcategory}` — e.g. `murilloskills.skill.name.miner`, `murilloskills.notify.level_up_message`, `murilloskills.error.cooldown_minutes`

## Where to Add New Code

**New Skill:**
1. Add enum value to `src/main/java/com/murilloskills/skills/MurilloSkillsList.java`
2. Create `src/main/java/com/murilloskills/impl/{Name}Skill.java` extending `AbstractSkill`
3. Register in `MurilloSkills.registerSkills()` inside `src/main/java/com/murilloskills/MurilloSkills.java`
4. Create `src/main/java/com/murilloskills/utils/{Name}XpGetter.java`
5. Wire XpGetter into the appropriate handler in `src/main/java/com/murilloskills/skills/` or `events/`
6. Add config section to `src/main/java/com/murilloskills/config/ModConfig.java` and getters to `src/main/java/com/murilloskills/utils/SkillConfig.java`
7. Add translation keys to all three lang JSON files
8. Add advancement JSON files to `src/main/resources/data/murilloskills/advancement/{name}/`
9. Add perk definitions to `src/client/java/com/murilloskills/gui/data/SkillUiData.java`

**New Network Packet:**
1. Create payload record in `src/main/java/com/murilloskills/network/` with static `ID` and `CODEC`
2. Register in `ModNetwork.registerS2CPayloads()` or `registerC2SPayloads()` in `src/main/java/com/murilloskills/network/ModNetwork.java`
3. For C2S: create handler class in `src/main/java/com/murilloskills/network/handlers/` and add to `NetworkHandlerRegistry.registerAll()`
4. For S2C: register client receiver inline in `MurilloSkillsClient.onInitializeClient()`

**New Mixin:**
1. Create class in `src/main/java/com/murilloskills/mixin/` (or `mixin/client/` for client-only)
2. Add the simple class name to `src/main/resources/murilloskills.mixins.json` (or client mixins json)

**New GUI Screen:**
1. Create in `src/client/java/com/murilloskills/gui/`
2. Open it from `MurilloSkillsClient` keybind handler via `MinecraftClient.getInstance().setScreen(...)`

**New HUD Renderer:**
1. Create in `src/client/java/com/murilloskills/render/`
2. Register callback in `MurilloSkillsClient.onInitializeClient()` via `HudRenderCallback.EVENT.register()` or `WorldRenderEvents`

**New Config Value:**
1. Add field to appropriate inner class in `ModConfig.ConfigData` (in `src/main/java/com/murilloskills/config/ModConfig.java`)
2. Add typed getter in `src/main/java/com/murilloskills/utils/SkillConfig.java`
3. All gameplay code must use `SkillConfig.getXxx()` — never reference `ModConfig` directly

**New Skill Toggle (feature on/off per player):**
- Use `playerData.getToggle(MurilloSkillsList.SKILL, "toggleName", defaultValue)` and `setToggle()` — keys stored in `PlayerSkillData.skillToggles` as `"SKILL.toggleName"` strings
- Add corresponding C2S payload + handler (see `NightVisionToggleC2SPayload` + `NightVisionToggleNetworkHandler` as reference)

## Special Directories

**`.planning/codebase/`:**
- Purpose: GSD planning documents
- Generated: By Claude Code mapping sessions
- Committed: Yes

**`run/`:**
- Purpose: Minecraft dev client/server run directory (options, user cache)
- Generated: Yes, by `./gradlew runClient`
- Committed: No (in `.gitignore`)

**`build/`:**
- Purpose: Gradle build output including compiled jar
- Generated: Yes
- Committed: No

**`net/`:**
- Purpose: Unknown / appears to be an artefact in the project root
- Generated: Possibly by the dev environment
- Committed: Not tracked (listed in git status as untracked)

**`src/main/resources/data/murilloskills/advancement/`:**
- Purpose: Vanilla advancement JSON definitions; 9 subdirectories (one per skill + `special/`)
- Generated: No — hand-authored JSON
- Committed: Yes

---

*Structure analysis: 2026-03-24*
