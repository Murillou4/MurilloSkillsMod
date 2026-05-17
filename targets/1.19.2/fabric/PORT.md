# Fabric 1.19.2 Port

Status: native-smoke-tested.

This target is no longer a runtime compatibility port. It was rebuilt from the full Fabric source in
`.codex-temp/native-fabric/1.19.2-fabric` and published to
`dist/1.19.2/fabric/murilloskills-1.2.74+mc1.19.2-fabric.jar`.

Validation evidence:
- Build passed with `.\gradlew.bat --no-daemon --console=plain -Pminecraft_version=1.19.2 -Pyarn_mappings=1.19.2+build.28 -Pfabric_version=0.77.0+1.19.2 -Ploader_version=0.16.14 build`.
- Jar inspection found `com/murilloskills/MurilloSkillsClient.class` and `com/murilloskills/gui/SkillsScreen.class`, with no `RuntimeClientBridge` or `RuntimeSkillsScreen`.
- `runServer` reached `Done (35.718s)!`.
- `runClient` opened Minecraft 1.19.2, entered the disposable single-player world `CodexSmoke`, and logged `Player921 joined the game`.
- Pressing `O` opened the real MurilloSkills selection screen.

Screenshots:
- `.codex-temp/native-fabric/1.19.2-fabric/logs/screenshots/1.19.2-fabric-world-loaded.png`
- `.codex-temp/native-fabric/1.19.2-fabric/logs/screenshots/1.19.2-fabric-skills-menu-after-o.png`
