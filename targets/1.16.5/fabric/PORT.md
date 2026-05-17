# Fabric 1.16.5 Port

Status: native-smoke-tested.

Native Fabric port built from `.codex-temp/native-fabric/1.16.5-fabric` into `dist/1.16.5/fabric/murilloskills-1.2.74+mc1.16.5-fabric.jar`.

Validation evidence:
- `gradlew build` passed with Minecraft 1.16.5, Yarn `1.16.5+build.10`, Fabric API `0.42.0+1.16`, Loader `0.16.14`.
- Jar inspection: includes `com/murilloskills/MurilloSkillsClient.class`, `com/murilloskills/gui/SkillsScreen.class`, and `assets/murilloskills/lang/en_us.json`; no `RuntimeClientBridge`, `RuntimeSkillsScreen`, or `multi-loader-runtime` entries.
- `runServer` reached `Done (24.657s)!` using Java 17 for the Minecraft process.
- `runClient` opened Minecraft 1.16.5/Fabric, entered `CodexSmoke`, logged `joined the game`, and `O` opened the real `SkillsScreen` with translated labels and the selection layout corrected.

Screenshots:
- `.codex-temp/native-fabric/1.16.5-fabric/logs/screenshots/1.16.5-fabric-world-loaded-final.png`
- `.codex-temp/native-fabric/1.16.5-fabric/logs/screenshots/1.16.5-fabric-skills-menu-after-o-final.png`
