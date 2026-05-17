# Fabric 1.20.1 Port

Status: native-smoke-tested.

Built from the full Fabric source adapted to Minecraft `1.20.1` APIs, not from `multi-loader-runtime`.

Validation evidence:

- `gradlew.bat build` succeeded for `1.20.1` with Fabric API `0.92.6+1.20.1`.
- `runServer` reached `Done`.
- `runClient` opened `CodexSmoke`, entered single player, and the `O` key opened the real `SkillsScreen`.
