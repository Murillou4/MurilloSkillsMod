# Fabric 1.21.1 Port

Status: native-smoke-tested.

Built from the full Fabric source adapted to Minecraft `1.21.1` APIs, not from `multi-loader-runtime`.

Validation evidence:

- `gradlew.bat build` succeeded for `1.21.1` with Fabric API `0.116.7+1.21.1`.
- `runServer` reached `Done`.
- `runClient` opened `CodexSmoke1211`, entered single player, and the `O` key opened the real `SkillsScreen`.
