# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MurilloSkills is a Fabric 1.21.10 Minecraft mod (Java 21) that adds an RPG skill progression system with 8 skills (Miner, Warrior, Archer, Farmer, Fisher, Blacksmith, Builder, Explorer). Each skill has 100 levels, milestone perks at 10/25/50/75/100, a prestige system (0-10), and a level-100 active ability. Players choose up to 3 skills to specialize in and designate one as their Paragon (the only skill allowed to reach level 100).

## Build & Run Commands

```bash
# Build the mod (output in build/libs/)
./gradlew build

# Generate Minecraft decompiled sources (for IDE navigation)
./gradlew genSources

# Run the Minecraft client in dev mode
./gradlew runClient

# Run a dedicated test server
./gradlew runServer
```

There is no test suite. Validation is done by running the client (`./gradlew runClient`).

## Architecture

### Split Source Sets (Fabric Loom)

The build uses `splitEnvironmentSourceSets()` which separates code into:
- **`src/main/`** — Server-side and common code (runs on both client and server)
- **`src/client/`** — Client-only code (rendering, GUI, keybindings). Cannot be imported by main.

### Skill System (Strategy + Registry Pattern)

The core abstraction is `api/AbstractSkill.java` — all 8 skills extend it and override lifecycle methods:
- `onLevelUp()`, `onActiveAbility()`, `updateAttributes()`, `onTick()`, `onPlayerJoin()`

`api/SkillRegistry.java` is an EnumMap-based registry that maps `MurilloSkillsList` enum values to their `AbstractSkill` implementations. All skills are registered in `MurilloSkills.onInitialize()`.

Skill implementations live in `impl/` (e.g., `MinerSkill.java`, `WarriorSkill.java`).

### Player Data Model

- **`data/PlayerSkillData`** — Server-side state per player: an `EnumMap<MurilloSkillsList, SkillStats>` holding level/xp/prestige per skill, plus paragon choice, selected skills (1-3), and feature toggles. Serialized via Codecs.
- **`data/SkillStats`** (inner class) — Level (0-100), XP, prestige (0-10), lastAbilityUse cooldown.
- **`data/ModAttachments`** — Registers `PlayerSkillData` as a Fabric persistent attachment (survives death, auto-saved).
- **`data/ClientSkillData`** — Client-side mirror synced via network packets. The GUI reads from this.

### Networking

22 custom payloads registered in `network/ModNetwork.java`. The pattern is:
- **S2C (server→client):** `SkillsSyncPayload`, `XpGainS2CPayload`, `MinerScanResultPayload`, etc.
- **C2S (client→server):** `SkillAbilityC2SPayload`, `SkillSelectionC2SPayload`, `PrestigeC2SPayload`, etc.

Handlers live in `network/handlers/` (14 handler classes). `NetworkHandlerRegistry` routes payloads to them.

### XP Event Tracking

XP gain is driven by Fabric events and Mixins that intercept Minecraft actions:
- **Event handlers** in `skills/`: `BlockBreakHandler`, `MobKillHandler`, `CropHarvestHandler`, `FishingCatchHandler`, `ArcherHitHandler`
- **Mixin classes** (37 server + client) in `mixin/`: intercept block interactions, damage, crafting, enchanting, fishing, loot, etc.
- **XP getters** in `utils/`: per-skill classes (e.g., `MinerXpGetter`, `WarriorXpGetter`) that determine XP amounts for specific actions

Mixin configs: `src/main/resources/murilloskills.mixins.json` and `src/client/resources/murilloskills.client.mixins.json`.

### Configuration

`config/ModConfig.java` uses OwO Lib's config system. Generates `config/murilloskills.json` at runtime with all tunable values (XP formulas, cooldowns, ability parameters, prestige multipliers, synergy bonuses, daily challenge settings). `utils/SkillConfig.java` provides typed getter methods over the config.

XP curve formula: `base + (level * multiplier) + (exponent * level^2)` (defaults: 60 + 15*lvl + 2*lvl^2).

### Client-Side

- **GUI:** `gui/SkillsScreen.java` (main skill interface), `gui/ModInfoScreen.java` (help), `gui/OreFilterScreen.java` (miner ore filter)
- **Rendering:** `render/` contains HUD overlays (XP toasts, contextual XP numbers, pinned info) and world renderers (ore highlighting, treasure highlighting, vein miner preview)
- **Keybindings:** 10 keys registered in `MurilloSkillsClient.java` (skills screen, active ability, vein miner toggle, etc.)

### Progression Systems

Beyond basic leveling:
- **Prestige** (`utils/PrestigeManager.java`) — Reset to level 0 for XP multiplier bonuses (up to 10 prestiges)
- **Daily Challenges** (`utils/DailyChallengeManager.java`) — 3 challenges per MC day with adaptive difficulty
- **Synergies** (`utils/SkillSynergyManager.java`) — 2-skill combos grant bonus effects
- **Achievements** (`utils/AchievementTracker.java`) — Advancement integration

### Admin Commands

`commands/SkillAdminCommands.java` registers `/murilloskills` subcommands for setting levels, adding XP, managing paragon status, and resetting skills.

## Key Dependencies

- **Fabric API** (0.138.3+1.21.10) — Events, networking, attachments
- **OwO Lib** (0.12.11+1.21) — Configuration framework (bundled via `include`)
- **Yarn Mappings** (1.21.10+build.3) — Minecraft deobfuscation

## Adding a New Skill

1. Add entry to `skills/MurilloSkillsList` enum
2. Create implementation in `impl/` extending `AbstractSkill`
3. Register in `MurilloSkills.onInitialize()` via `SkillRegistry.register()`
4. Create XP getter in `utils/` and wire it to event handlers in `skills/`
5. Add mixin classes if new Minecraft hooks are needed
6. Add config section in `config/ModConfig.java`
7. Add translation keys in `assets/murilloskills/lang/` (en_us, pt_br, es_es)
8. Update `ClientSkillData` and `SkillsScreen` for client display

## Language

The codebase mixes Portuguese (comments, some log messages) and English (code, API). Translation files support en_us, pt_br, and es_es.
