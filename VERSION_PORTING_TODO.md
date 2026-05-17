# Version Porting TODO

This file tracks changes intentionally made in only one Minecraft target so they are not forgotten when the rest of the version matrix is updated.

## Policy

- When a user asks for a fix or feature in a single Minecraft version first, add an entry here for the other supported versions before calling the work done.
- Keep the entry until the same behavior is ported and validated in each relevant loader/version target.

## Pending

- [ ] Port the 1.20.1 Fabric Skills UI polish to the rest of the supported targets.
  - Source target changed now: `.codex-temp/native-fabric/1.20.1-fabric/src/client/java/com/murilloskills/gui/SkillsScreen.java`
  - Changes: Miner Filter and Ultmine Config buttons use smaller item icons instead of text labels, sit directly to the left of the reset button away from the perk markers, and expanded skill tooltip lines no longer add manual leading spaces.
  - Remaining targets to check/port: 1.21.10 Fabric, 1.21.1 Fabric, 1.19.2 Fabric, 1.18.2 Fabric, 1.16.5 Fabric, plus Forge/NeoForge/Legacy targets if those UI sources are maintained separately.
