MurilloSkills multi-loader runtime
==================================

This source set is intentionally Java 8 compatible and avoids direct Minecraft
imports. Target jars compile it once per loader entrypoint and use reflection to
adapt to Fabric API, Forge, NeoForge, and older Forge event surfaces.

The canonical progression and save format live in `common-core`.
