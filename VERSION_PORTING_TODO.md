# Version Porting TODO

This file tracks changes intentionally made in only one Minecraft target so they are not forgotten when the rest of the version matrix is updated.

## Policy

- When a user asks for a fix or feature in a single Minecraft version first, add an entry here for the other supported versions before calling the work done.
- Keep the entry until the same behavior is ported and validated in each relevant loader/version target.

## Pending

- [ ] Decide whether the Forge 1.12.2 native split should be mirrored in other maintained legacy loader targets.
  - Source target changed now: `targets/1.12.2/forge/src/main/java/com/murilloskills/forge112/`
  - Changes: replaced the old single-file Forge 1.12.2 port with focused files for lifecycle, events, passives, skill math, timed effects, mining/Ultmine tools, environment effects, abilities, player/storage helpers, command, self-test, client hooks, client input, HUD, world render, UI screens, and client config. The target now also has an original-style `api/AbstractSkill`, `api/SkillRegistry`, and `impl/*Skill` strategy layer so each skill owns its active ability, passives, tick behavior, and event hooks. Client parity was expanded with XP toasts, notification cards, daily challenge cards, active-mode HUD cards, Paragon ability cards, UltPlace config, storage/trash/classic pickers, terminal amount dialogs, client state mirrors, first-time hints, achievements, and synergy/daily challenge services.
  - Remaining targets to check/port: 1.12.2 Legacy Fabric and any other maintained legacy-native loader target if they later receive the same native/full-client treatment.
  - Validation note: build/runtime/selftests are intentionally paused until explicitly requested.

- [ ] Port the 1.20.1 Fabric Skills UI polish to the rest of the supported targets.
  - Source target changed now: `.codex-temp/native-fabric/1.20.1-fabric/src/client/java/com/murilloskills/gui/SkillsScreen.java`
  - Changes: Miner Filter and Ultmine Config buttons use smaller item icons instead of text labels, sit directly to the left of the reset button away from the perk markers, and expanded skill tooltip lines no longer add manual leading spaces.
  - Remaining targets to check/port: 1.21.10 Fabric, 1.21.1 Fabric, 1.19.2 Fabric, 1.18.2 Fabric, 1.16.5 Fabric, plus Forge/NeoForge/Legacy targets if those UI sources are maintained separately.
