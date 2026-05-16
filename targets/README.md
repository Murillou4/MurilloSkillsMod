# Multi-target Port Layout

This directory is the landing zone for loader/version-specific ports.

The current Fabric 1.21.10 target remains the existing source tree at the repo root. The requested backports and loader ports are built from `common-core` plus `multi-loader-runtime`, which keeps the progression/save logic shared and uses loader-specific entrypoints.

```text
targets/<minecraft-version>/<loader>/
```

Each full native target may provide:

- loader metadata (`fabric.mod.json`, `mods.toml`, or legacy equivalents)
- version-specific mixin configs
- platform implementations for the `common-core` port interfaces
- resource transforms for pack format and conditional recipe syntax
- a Gradle build or package task that publishes jars to `dist/<minecraft-version>/<loader>/`

The current target matrix is tracked in `tools/ports-matrix.json`, and `tools/build-all.ps1` publishes the built jars.
