# External Integrations

**Analysis Date:** 2026-03-24

## Overview

This mod has **no external service integrations**. All functionality is self-contained within the Minecraft / Fabric ecosystem. There are no HTTP calls, no cloud services, no external databases, and no third-party mod dependencies at runtime.

---

## Fabric API Subsystems Used

These are Fabric API modules consumed as integrations rather than raw Minecraft internals:

**Networking (v1):**
- `net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry` — Registers 9 S2C and 15 C2S custom payloads
- `net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking` — Client-side packet reception
- `net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents` — Connection lifecycle hooks
- Registration: `src/main/java/com/murilloskills/network/ModNetwork.java`
- Handlers: `src/main/java/com/murilloskills/network/handlers/NetworkHandlerRegistry.java`

**Persistent Attachments (v1):**
- `net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry` — Stores `PlayerSkillData` per-player
- `net.fabricmc.fabric.api.attachment.v1.AttachmentType` — Typed attachment with codec persistence and `copyOnDeath()`
- Configured in: `src/main/java/com/murilloskills/data/ModAttachments.java`
- Identifier: `murilloskills:player_skills`

**Commands (v2):**
- `net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback` — Registers `/murilloskills` admin commands
- Implementation: `src/main/java/com/murilloskills/commands/SkillAdminCommands.java`

**Events:**
- `net.fabricmc.fabric.api.event.lifecycle.v1.*` — Server/client tick events
- `net.fabricmc.fabric.api.event.player.*` — Block break, item use events
- Used in: `src/main/java/com/murilloskills/events/MinecraftEventsListener.java`, `BlockPlacementHandler.java`, `ChallengeEventsHandler.java`, `DimensionChangeHandler.java`
- Skill-level handlers: `src/main/java/com/murilloskills/skills/` (`BlockBreakHandler.java`, `MobKillHandler.java`, `CropHarvestHandler.java`, `FishingCatchHandler.java`, `ArcherHitHandler.java`)

**Client Rendering:**
- `net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback` — HUD overlays (deprecated, migration pending)
- `net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents` — 3D world overlays (ore highlight, vein miner preview)
- Renderers: `src/client/java/com/murilloskills/render/` (`OreHighlighter.java`, `TreasureHighlighter.java`, `UltminePreview.java`, `VeinMinerPreview.java`, `XpToastRenderer.java`, `RainDanceEffect.java`, `AreaPlantingHud.java`)

**Keybindings:**
- `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper` — Registers 10 custom keybinds
- GLFW key constants via `org.lwjgl.glfw.GLFW`
- Registered in: `src/client/java/com/murilloskills/MurilloSkillsClient.java`

---

## Data Storage

**Player Data (Persistent):**
- Storage: Fabric Persistent Attachment (`murilloskills:player_skills`)
- Data class: `src/main/java/com/murilloskills/data/PlayerSkillData.java`
- Serialization: Mojang Codec (`com.mojang.serialization.Codec`) with `RecordCodecBuilder`
- Survival: Persists across server restarts and player death (`copyOnDeath()`)
- Storage location (at runtime): Managed entirely by Fabric/Minecraft world save system; no external files

**Runtime Configuration:**
- File: `<minecraft_dir>/config/murilloskills.json`
- Serializer: `com.google.gson.Gson` (Gson, bundled with Minecraft)
- Manager: `src/main/java/com/murilloskills/config/ModConfig.java`
- Read on: `MurilloSkills.onInitialize()` → `ModConfig.load()`
- Reload command: `/murilloskills reload`

**Client Configuration (local, non-synced):**
- `src/client/java/com/murilloskills/client/config/UltmineClientConfig.java` — Ultmine shape preferences
- `src/client/java/com/murilloskills/client/config/OreFilterConfig.java` — Ore highlight filter per player

**No databases** — No SQL, NoSQL, or file-based database is used.

---

## Minecraft Advancement System

**Integration:**
- Mod grants advancements via `src/main/java/com/murilloskills/utils/AdvancementGranter.java`
- Tracked by `src/main/java/com/murilloskills/utils/AchievementTracker.java`
- Advancement JSON files: `src/main/resources/data/murilloskills/advancement/` (organized per skill: `archer/`, `blacksmith/`, `builder/`, `explorer/`, `farmer/`, `fisher/`, `miner/`, `warrior/`, `special/`)
- Uses vanilla Minecraft advancement infrastructure — no external library

---

## Mixin Bytecode Integration

**Framework:** SpongePowered Mixin (`org.spongepowered.asm.mixin.*`)

**Server-side mixins (31 classes) — `src/main/java/com/murilloskills/mixin/`:**
- Block interaction: `CropBlockMixin`, `SeedItemMixin`, `SeedPlantMixin`, `BuilderBlockPlacementMixin`, `LootChestMixin`, `MagmaBlockMixin`, `SoulSandMixin`, `ScaffoldingClimbMixin`
- Entity / combat: `LivingEntityMixin`, `ArrowEntityMixin`, `PersistentProjectileEntityAccessor`, `DamageDealtMixin`, `AnimalBreedMixin`
- Crafting / smithing: `AnvilScreenHandlerMixin`, `EnchantmentScreenHandlerMixin`, `EnchantmentHelperMixin`, `GrindstoneScreenHandlerMixin`, `CraftingResultMixin`, `ForgingScreenHandlerAccessor`, `FurnaceOutputMixin`, `AbstractFurnaceBlockEntityMixin`
- Fishing: `FishingBobberEntityMixin`, `ItemEntityFishingMixin`
- Misc: `PlayerEntityReachMixin`, `PlayerEventsMixin`, `PlayerSleepMixin`, `FoodEatMixin`, `ExplorerBreathMixin`, `WanderingTraderMixin`, `SheepShearMixin`, `ItemStackMixin`, `ApplyBonusLootFunctionMixin`

**Client-side mixins (1 class) — `src/client/java/com/murilloskills/mixin/client/`:**
- `ExampleClientMixin` — Placeholder/stub (from template, minimal usage)

---

## Logging

**Framework:** SLF4J (`org.slf4j.Logger` / `LoggerFactory`) — bundled with Fabric Loader

**Pattern:** Each major class creates its own named logger:
```java
// In MurilloSkills.java
public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

// In AbstractSkill.java (inherited by all 8 skill implementations)
protected final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-" + getClass().getSimpleName());
```

**Output:** Standard Minecraft log (`logs/latest.log`) — no external log aggregation.

---

## CI/CD & Distribution

**CI Pipeline:** None detected — no `.github/workflows/`, no CI config files.

**Build Script:** `repo.bat` — local Windows batch script for builds/releases (not a CI system).

**Distribution:** Manual JAR distribution (Modrinth/CurseForge assumed). Built artifact: `build/libs/murilloskills-<version>.jar`.

**No remote publishing configured** — `build.gradle` has empty `publishing.repositories` block.

---

## Authentication & Identity

Not applicable — this is a client/server Minecraft mod. Authentication is handled entirely by Minecraft's own systems. The mod identifies players by `ServerPlayerEntity` objects (UUID-based) from the Minecraft server, accessed via the Fabric attachment on `ServerPlayerEntity`.

---

## Webhooks & Callbacks

**Incoming:** None.

**Outgoing:** None.

---

## Secrets & Environment Variables

None required. No API keys, tokens, or environment variables are used. The mod operates fully offline within the Minecraft JVM.

---

*Integration audit: 2026-03-24*
