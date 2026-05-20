# Forge 1.12.2 Port

Status: native-forge-port-refactored-validated.

This target is now a native Forge 1.12.2 source port instead of a `multi-loader-runtime` compatibility jar.

The initial implementation was split out of the old single-file port into focused Forge 1.12.2 packages and then reorganized around the original skill architecture:

- `com.murilloskills.forge112` keeps only the mod entrypoint and shared constants/state.
- `api/AbstractSkill.java` and `api/SkillRegistry.java` provide the same strategy/registry shape used by the main mod.
- `impl/*Skill.java` owns each skill's active ability, passives, periodic tick behavior, and Forge event hooks.
- `commands`, `config`, `data`, `events`, `skills`, `utils`, and `dev` mirror the original server/common source layout.
- `events/Forge112SkillEvents.java` is now a Forge callback dispatcher instead of the owner of every skill behavior.
- `client`, `client.input`, `client.render`, `client.config`, and `client.gui` mirror the original client-side split for keybinds, HUD/world rendering, client config, and screens.
- GUI classes such as `SkillsGuiParity`, `GuideGuiParity`, `OreFilterGui112`, `UltmineConfigGui112`, `UltmineRadialGui112`, `ParagonAbilityGui112`, `UltPlaceConfigGui112`, `StorageWhitelistPickerGui112`, `TrashItemPickerGui112`, `UltmineClassicBlockPickerGui112`, and terminal amount screens live under `client.gui`.
- `client.render` now includes XP toasts, notification cards, active-mode HUD cards, and world/highlight renderers as separate client-only surfaces.
- `utils` now includes Forge 1.12.2 equivalents for daily challenges, skill synergies, first-time hints, achievements, and notification dispatch.
- The Forge 1.12.2 Gradle file no longer patches cached Forge jar binaries; jar output must be produced from source through Gradle.

Validation completed on 2026-05-19 with `clean build`, disposable `runClient`, server self-test, UI self-test, screenshots, and copied dist jar.
