# Testing Patterns

**Analysis Date:** 2026-03-24

## Test Framework

**Runner:**
- JUnit Jupiter 5.11.4
- Config: `build.gradle` — `test { useJUnitPlatform() }`

**Dependencies (from `build.gradle`):**
```groovy
testImplementation "org.junit.jupiter:junit-jupiter-api:5.11.4"
testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:5.11.4"
testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.11.4"
```

**Run Commands:**
```bash
./gradlew test        # Run unit tests
./gradlew build       # Build + run tests
./gradlew runClient   # Launch Minecraft client for manual validation
./gradlew runServer   # Launch dedicated server for manual validation
./gradlew genSources  # Decompile Minecraft sources for IDE navigation
```

## Test File Organization

**Location:**
- Separate test source set: `src/test/java/com/murilloskills/`
- Follows the same package structure as `src/main/java/com/murilloskills/`

**Current test files:**
- `src/test/java/com/murilloskills/skills/UltmineShapeCalculatorTest.java`

**Naming:**
- Pattern: `{ClassUnderTest}Test.java`
- Test method names: full sentences in camelCase describing the scenario and expected outcome — no `test` prefix
  - `shape3x3OnHorizontalSurfaceReturnsNineBlocks()`
  - `lineUsesExactLengthOnDiagonalLook()`
  - `stairsIncreaseHeightByOnePerStep()`
  - `square20x20Depth1ReturnsFourHundredBlocks()`

**Structure:**
```
src/test/java/com/murilloskills/
└── skills/
    └── UltmineShapeCalculatorTest.java
```

## What Is Tested

**Scope:** Only pure logic classes that have no Minecraft server/client dependencies at runtime can be unit tested. The existing test covers `UltmineShapeCalculator`, which is a pure spatial calculation utility (`src/main/java/com/murilloskills/skills/UltmineShapeCalculator.java`).

Classes suitable for unit testing follow this pattern:
- No `ServerPlayerEntity` parameters
- No `World` or `ServerWorld` access
- No Fabric API event calls
- Pure input → output computation

Classes that **cannot** be unit tested without a running Minecraft instance:
- All `AbstractSkill` implementations (require `ServerPlayerEntity`)
- `PlayerSkillData` (requires Codec + Minecraft registry)
- `MinecraftEventsListener` (requires Fabric event system)
- All GUI classes (require `MinecraftClient`)
- All network handlers (require live server context)

## Test Structure Pattern

The single test class demonstrates the expected pattern:

```java
// Package mirrors src/main source tree
package com.murilloskills.skills;

// Only minecraft-independent imports allowed in tests
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Package-private class (no public modifier)
class UltmineShapeCalculatorTest {

    @Test
    void shape3x3OnHorizontalSurfaceReturnsNineBlocks() {
        // Arrange
        BlockPos origin = new BlockPos(0, 64, 0);

        // Act
        List<BlockPos> blocks = UltmineShapeCalculator.getShapeBlocks(
                origin, UltmineShape.S_3x3, 1, 1, Direction.UP);

        // Assert
        assertEquals(9, blocks.size());
        assertTrue(blocks.contains(new BlockPos(-1, 64, -1)));
        assertTrue(blocks.contains(new BlockPos(1, 64, 1)));
        assertTrue(blocks.contains(origin));
    }
}
```

Key patterns:
- No `@BeforeEach` / `@AfterEach` setup (each test is fully self-contained)
- `BlockPos` and `Direction` are Minecraft value types — they work without a running server
- Assertions use `assertEquals` for counts and `assertTrue` for membership checks
- Tests verify both size constraints and specific position contents
- Overloaded method tested with different signatures across tests (e.g., `getShapeBlocks` with/without `Vec3d`)

## Assertions Used

- `assertEquals(expected, actual)` — for exact counts and specific positions
- `assertTrue(condition)` — for membership (`blocks.contains(...)`) and stream predicates (`blocks.stream().allMatch(...)`)
- Static imports from `org.junit.jupiter.api.Assertions`

## Primary Validation Strategy

Because the vast majority of game logic requires a running Minecraft instance, **the primary validation approach is manual in-game testing**:

```bash
# Start client in dev mode for functional testing
./gradlew runClient

# Or start a dedicated server for multiplayer/server-side testing
./gradlew runServer
```

**Manual validation targets:**
- XP gain from breaking blocks, killing mobs, fishing, crafting
- Skill level-up messages and perk activation at milestone levels (10/25/50/75/100)
- Active ability triggering and cooldown enforcement
- Network sync correctness (client GUI reflects server state)
- Prestige system (reset + multiplier application)
- Toggle features (vein miner, area planting, night vision)
- Admin commands (`/murilloskills set`, `/murilloskills addxp`, etc.)

## Runtime Validation at Startup

`SkillRegistry.validateRegistration()` is called in `MurilloSkills.onInitialize()` and acts as a startup assertion that all 8 skills are properly registered:

```java
SkillRegistry.validateRegistration(
    MurilloSkillsList.MINER, MurilloSkillsList.WARRIOR,
    MurilloSkillsList.ARCHER, MurilloSkillsList.FARMER,
    MurilloSkillsList.FISHER, MurilloSkillsList.BLACKSMITH,
    MurilloSkillsList.BUILDER, MurilloSkillsList.EXPLORER);
SkillRegistry.logRegisteredSkills();
```

This logs a failure and returns `false` (but does not crash) if any expected skill is missing. Check the log output after `runClient` or `runServer` for `=== SKILLS REGISTRADAS ===` confirmation block.

## Adding New Tests

Tests for pure calculation logic should be placed at:
- `src/test/java/com/murilloskills/{package}/{ClassName}Test.java`

Mirror the package from `src/main/java/`. The test class does NOT need to be `public`. Use `@Test` on each test method.

**Candidates for future unit tests** (pure logic, no Minecraft server dependency):
- `src/main/java/com/murilloskills/skills/UltmineShape.java` — enum shape definitions
- `src/main/java/com/murilloskills/data/PlayerSkillData.java` → `SkillStats.addXp()` (if Codec dependency can be isolated)
- `src/main/java/com/murilloskills/config/ModConfig.java` — config parsing/defaults
- `src/main/java/com/murilloskills/utils/SkillConfig.java` — formula calculations

## Coverage

**Requirements:** None enforced. No coverage thresholds configured.

**Current state:** One test file with 5 tests covering `UltmineShapeCalculator`. All other logic is validated manually via `runClient`.

---

*Testing analysis: 2026-03-24*
