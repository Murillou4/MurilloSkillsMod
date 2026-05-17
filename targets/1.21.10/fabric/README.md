# Fabric 1.21.10

Status: native-cross-mod-compat.

This target is still backed by the existing root Fabric/Loom source set during the migration. It now depends on `common-core` and writes canonical player saves to:

```text
<world>/murilloskills/players/<uuid>.json
```

Build with:

```powershell
.\gradlew.bat publishCurrentFabricJar
```

Validation evidence:

- `.\gradlew.bat build` passed.
- `runServer` reached `Done`.
- `runClient` opened `CodexSmoke12110`, entered single player, and the `O` key opened the real `SkillsScreen`.
