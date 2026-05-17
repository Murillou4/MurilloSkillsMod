# Fabric 1.18.2 Port

Status: native-smoke-tested.

Built as a native Fabric port from the full MurilloSkills codebase, not the `multi-loader-runtime` compatibility screen. The packaged jar is `dist/1.18.2/fabric/murilloskills-1.2.74+mc1.18.2-fabric.jar`.

Validation on 2026-05-16:
- `gradlew.bat --no-daemon --console=plain -Pminecraft_version=1.18.2 -Pyarn_mappings=1.18.2+build.4 -Pfabric_version=0.77.0+1.18.2 -Ploader_version=0.16.14 build` passed.
- Jar inspection found `MurilloSkillsClient.class` and `SkillsScreen.class`, with zero runtime-compatibility classes.
- `runServer` reached `Done (40.691s)!` with all 8 skills registered.
- `runClient` opened Minecraft 1.18.2/Fabric, loaded the `CodexSmoke` single-player world, and logged `Player143 joined the game`.
- Pressing `O` opened the real MurilloSkills skill selection screen.

Evidence:
- `.codex-temp/native-fabric/1.18.2-fabric/logs/screenshots/1.18.2-fabric-world-loaded.png`
- `.codex-temp/native-fabric/1.18.2-fabric/logs/screenshots/1.18.2-fabric-skills-menu-after-o.png`
