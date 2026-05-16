# Fabric 1.21.10

Status: implemented.

This target is still backed by the existing root Fabric/Loom source set during the migration. It now depends on `common-core` and writes canonical player saves to:

```text
<world>/murilloskills/players/<uuid>.json
```

Build with:

```powershell
.\gradlew.bat publishCurrentFabricJar
```
