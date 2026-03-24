# Technology Stack

**Analysis Date:** 2026-03-24

## Languages

**Primary:**
- Java 21 — All source code (server-side, client-side, tests)

**Data / Config:**
- JSON — Config file (`config/murilloskills.json`), lang files, advancement definitions, mixin configs

## Runtime

**Environment:**
- JVM 21 (required: `java >= 21` per `fabric.mod.json`)
- Target: Minecraft 1.21.10 client and dedicated server

**Package Manager:**
- Gradle (Groovy DSL) — `build.gradle` / `gradle.properties`
- Gradle wrapper present: `gradlew` / `gradlew.bat`
- Lockfile: Not present (versions pinned in `gradle.properties`)

## Frameworks

**Core:**
- Fabric Loader 0.18.1 — Mod loading, entrypoints, mixin injection
- Fabric API 0.138.3+1.21.10 — Events, networking, attachments, keybindings, rendering callbacks
- Minecraft 1.21.10 (Mojang) — Game engine (accessed via deobfuscated Yarn mappings)
- Yarn Mappings 1.21.10+build.3 — Deobfuscation layer for Minecraft internals

**Build / Dev:**
- Fabric Loom 1.13-SNAPSHOT — Gradle plugin that handles Minecraft remapping, source sets, mixin processing, `runClient`/`runServer` tasks
- `splitEnvironmentSourceSets()` — Loom feature that enforces server (`src/main/`) vs client (`src/client/`) separation at compile time

**Mixin Framework:**
- Mixin (SpongePowered ASM) — Bytecode injection into Minecraft classes; accessed via `org.spongepowered.asm.mixin.*`
- Server mixin config: `src/main/resources/murilloskills.mixins.json` (31 mixins)
- Client mixin config: `src/client/resources/murilloskills.client.mixins.json` (1 mixin currently)

**Testing:**
- JUnit Jupiter 5.11.4 — `src/test/java/`, run with `./gradlew test`
- JUnit Platform Launcher 1.11.4 — Test runner
- Note: Tests require only pure-Java logic (no Minecraft runtime); the project otherwise validates via `./gradlew runClient`

## Key Dependencies

**Critical:**
- `net.fabricmc:fabric-loader:0.18.1` — Mod entrypoint bootstrap
- `net.fabricmc.fabric-api:fabric-api:0.138.3+1.21.10` — All Fabric API modules (events, networking v1, attachments, client rendering, keybinding, commands)
- `com.mojang:minecraft:1.21.10` — Minecraft game code

**Bundled at Runtime (via Minecraft / Fabric):**
- `com.google.gson:gson` — JSON serialization for `ModConfig`; available transitively from Minecraft
- `org.slf4j:slf4j-api` — Logging (`LoggerFactory.getLogger`); available transitively from Minecraft/Fabric Loader
- `com.mojang:datafixers` / `com.mojang:serialization` — Codecs used for `PlayerSkillData` persistence; part of Minecraft distribution
- `org.lwjgl:lwjgl-glfw` — GLFW key constants for keybindings (client only); bundled with Minecraft

**No external mod dependencies** — `fabric.mod.json` declares only `fabricloader >= 0.16.0`, `minecraft ~1.21.10`, `java >= 21`, `fabric-api *`.

## Configuration

**Runtime Config:**
- File: `config/murilloskills.json` (generated in the Minecraft `config/` directory at first launch)
- Managed by: `src/main/java/com/murilloskills/config/ModConfig.java`
- Serialization: Gson `GsonBuilder().setPrettyPrinting()`
- Typed access: `src/main/java/com/murilloskills/utils/SkillConfig.java` (delegates to `ModConfig.get()`)
- No external config library — fully custom implementation using `FabricLoader.getInstance().getConfigDir()`

**Client Config:**
- `src/client/java/com/murilloskills/client/config/UltmineClientConfig.java` — Client-side ultmine settings
- `src/client/java/com/murilloskills/client/config/OreFilterConfig.java` — Per-player ore highlight filter

**Build Config:**
- `gradle.properties` — All version pins (`minecraft_version`, `yarn_mappings`, `loader_version`, `fabric_version`, `mod_version`)
- `build.gradle` — Loom setup, dependencies, Java 21 compilation target
- `settings.gradle` — Plugin repositories: Fabric Maven, Maven Central, Gradle Plugin Portal

## Platform Requirements

**Development:**
- JDK 21+
- `./gradlew genSources` to generate decompiled Minecraft sources for IDE navigation
- Gradle max heap: 1 GB (`org.gradle.jvmargs=-Xmx1G`)
- `org.gradle.parallel=true`; configuration cache disabled (Loom compatibility issue)

**Production:**
- Fabric Loader 0.16.0+ on Minecraft 1.21.10
- Fabric API required at runtime
- Works on both client (`environment: *`) and dedicated server

## Build Commands

```bash
./gradlew build        # Produces build/libs/murilloskills-<version>.jar
./gradlew genSources   # Decompile Minecraft for IDE
./gradlew runClient    # Launch dev client (primary validation method)
./gradlew runServer    # Launch test dedicated server
./gradlew test         # Run JUnit tests (pure-Java only)
```

**Output:** `build/libs/murilloskills-1.2.17.jar`

---

*Stack analysis: 2026-03-24*
