# Coding Conventions

**Analysis Date:** 2026-03-24

## Language Policy

The codebase deliberately mixes two languages:
- **Code identifiers** (class names, method names, field names, variable names): English exclusively
- **Comments**: Mix of Portuguese and English — both are accepted; Portuguese predominates in older sections, English in newer ones
- **Log messages**: Mix; error strings often in Portuguese (`"Erro ao processar..."`, `"Skill registrada com sucesso"`), info/debug in English
- **Translation keys** (`en_us.json`, `pt_br.json`, `es_es.json`): All user-facing strings go through `Text.translatable()` — never hardcode user-visible text

This is intentional. Do not "fix" Portuguese comments to English. New comments can be in either language.

## Naming Patterns

**Classes:**
- PascalCase throughout: `MinerSkill`, `SkillRegistry`, `PlayerSkillData`, `ModNetwork`
- Skill implementations: `{SkillName}Skill` — e.g., `MinerSkill`, `FarmerSkill`, `WarriorSkill`
- Network payloads: `{Action}{Direction}Payload` — e.g., `SkillsSyncPayload` (S2C), `SkillSelectionC2SPayload`, `XpGainS2CPayload`
- Mixin classes: `{TargetClass}Mixin` — e.g., `EnchantmentHelperMixin`, `BuilderBlockPlacementMixin`, `CropBlockMixin`
- Network handlers: `{Feature}NetworkHandler` — e.g., `SkillSelectionNetworkHandler`, `AbilityNetworkHandler`
- Config inner classes: `{Skill}Config` — e.g., `MinerConfig`, `FarmerConfig`, `XpConfig`
- Records (data carriers): PascalCase — e.g., `XpAddResult`, `PerkInfo`, `SynergyInfo`, `GuideEntry`

**Methods:**
- camelCase throughout
- Event registration methods: `{thing}Listen()` or `{thing}Handler()` — e.g., `blockBreakedListen()`, `playerTickListen()`
- Lifecycle overrides in skills: `onLevelUp()`, `onActiveAbility()`, `updateAttributes()`, `onTick()`, `onPlayerJoin()`
- Factory/creator methods on handlers: `create()` (returns a lambda handler)
- Static initialization: `register()`, `registerAll()`, `initAllListeners()`
- Toggle check pattern: `is{State}()` — e.g., `isHarvestMoonActive()`, `isSkillSelected()`, `isVeinMinerKeyHeld()`

**Fields:**
- camelCase for instance fields: `selectedSkills`, `paragonSkill`, `skillToggles`, `achievementStats`
- SCREAMING_SNAKE_CASE for static final constants: `MINER_SPEED_ID`, `SKILLS`, `MOD_ID`, `MAX_LEVEL`, `AREA_PLANTING_COOLDOWN_MS`
- Static maps tracking per-player state: use `SCREAMING_SNAKE_CASE` — e.g., `HOLDING_KEY`, `ACTIVE_PLAYERS`, `DROPS_TO_INVENTORY`
- Identifier constants: always `{SKILL}_{THING}_ID` — e.g., `MINER_SPEED_ID`, `BLACKSMITH_KNOCKBACK_RESISTANCE_ID`

**Enums:**
- SCREAMING_SNAKE_CASE values: `MurilloSkillsList.MINER`, `MurilloSkillsList.WARRIOR`, etc.
- Enum names are the canonical skill identifier used everywhere (toggle keys, codec strings, log output)

**Mixin injected methods:**
- Prefix with `murilloskills$` to avoid conflicts: `murilloskills$onSuccessfulPlacement`

**Toggle key strings:**
- camelCase, stored as `"SKILLNAME.toggleName"` — e.g., `"EXPLORER.nightVision"`, `"EXPLORER.stepAssist"`, `"FARMER.areaPlanting"`
- Constants for toggle names defined in the skill class: `private static final String TOGGLE_NIGHT_VISION = "nightVision";`

## Import Organization

1. Mod-internal imports (`com.murilloskills.*`)
2. Fabric API imports (`net.fabricmc.*`)
3. Minecraft imports (`net.minecraft.*`)
4. Standard library imports (`java.*`)

No enforced tool (no Checkstyle or Google Java Format detected). Convention is followed manually. Star imports (`java.util.*`) appear in files with many collection types (`ExplorerSkill.java`).

## Class Structure Patterns

**Utility/Registry classes (non-instantiable):**
```java
public final class NetworkHandlerRegistry {
    private NetworkHandlerRegistry() {
        // Utility class - prevent instantiation
    }
    // All methods static
}
```
Examples: `NetworkHandlerRegistry`, `UltmineShapeCalculator`, `VeinMinerHandler` (declared `final`)

**Skill implementation classes:**
```java
public class MinerSkill extends AbstractSkill {
    private static final Identifier MINER_SPEED_ID = Identifier.of("murilloskills", "miner_speed_bonus");
    // Per-player state maps (if needed):
    private static final Map<UUID, Long> statePlayers = new HashMap<>();

    @Override
    public MurilloSkillsList getSkillType() { return MurilloSkillsList.MINER; }

    @Override
    public void onActiveAbility(...) { try { ... } catch (Exception e) { LOGGER.error(...); } }

    @Override
    public void onTick(...) { try { ... } catch (Exception e) { if (player.age % 200 == 0) { ... } } }

    @Override
    public void updateAttributes(...) { ... }

    // Private helpers below public overrides
    private void handleNightVision(...) { ... }
}
```

**Network payload classes (records):**
```java
public record SkillSelectionC2SPayload(List<MurilloSkillsList> selectedSkills) implements CustomPayload {
    public static final CustomPayload.Id<SkillSelectionC2SPayload> ID = new CustomPayload.Id<>(...);
    public static final PacketCodec<RegistryByteBuf, SkillSelectionC2SPayload> CODEC = PacketCodec.ofStatic(...);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
```

**Network handler classes:**
```java
public final class SkillSelectionNetworkHandler {
    private SkillSelectionNetworkHandler() { /* Utility class */ }

    public static ServerPlayNetworking.PlayPayloadHandler<SkillSelectionC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try { ... }
                catch (Exception e) { LOGGER.error(...); }
            });
        };
    }
}
```

**Data classes with Codecs:**
```java
public class PlayerSkillData {
    public EnumMap<MurilloSkillsList, SkillStats> skills = new EnumMap<>(...);
    // public fields — no private + getters pattern for data bags

    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(...);

    public static class SkillStats { // Inner class
        public int level;
        public double xp;
        public static final Codec<SkillStats> CODEC = ...;
    }

    public record XpAddResult(boolean leveledUp, int oldLevel, int newLevel) {
        public static final XpAddResult NO_CHANGE = new XpAddResult(false, 0, 0);
    }
}
```

## Error Handling

**Universal pattern:** wrap all lifecycle methods in `try { ... } catch (Exception e) { LOGGER.error(...); }`.

This is mandatory in:
- All `AbstractSkill` overrides (`onTick`, `onActiveAbility`, `onLevelUp`, `updateAttributes`, `onPlayerJoin`)
- All Fabric event callbacks (especially tick handlers)
- All network handler `context.server().execute()` bodies
- Mod initialization (`onInitialize`)

**Tick error rate limiting** — do not spam logs on every tick:
```java
if (player.age % 200 == 0) {
    LOGGER.error("Erro no tick da skill " + getSkillType() + " para " + player.getName().getString(), e);
}
```

**Null guards before registry lookups:**
```java
AbstractSkill skill = SkillRegistry.get(skillEnum);
if (skill != null) { skill.onPlayerJoin(...); }
```

**Validation before action** — active ability pattern:
1. Check level requirement
2. Check if already active (if applicable)
3. Check cooldown
4. Execute ability

## Logging

**Logger instantiation:**
- Per-class in skill implementations: `protected final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-" + getClass().getSimpleName());`
- In static utility classes: `private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-{Role}");`
  - Examples: `"MurilloSkills-Registry"`, `"MurilloSkills-Events"`, `"MurilloSkills-NetworkRegistry"`, `"MurilloSkills-SelectionHandler"`
- Root logger: `LoggerFactory.getLogger(MOD_ID)` in `MurilloSkills.java` (MOD_ID = `"murilloskills"`)

**Log levels:**
- `LOGGER.info(...)` — registration confirmations, player actions (level-ups, ability use, respawn)
- `LOGGER.debug(...)` — attribute updates, minor state transitions
- `LOGGER.warn(...)` — null lookups, duplicate registrations, missing expected data
- `LOGGER.error(...)` — exceptions in catch blocks, critical failures

**Message language:** Portuguese error strings in older code (`"Erro ao..."`, `"Skill registrada..."`), English in newer handler code. Both accepted.

## Per-Player State Management

Skills that need per-player temporary state use `static final Map<UUID, T>`:
```java
// In FarmerSkill / BlacksmithSkill / ExplorerSkill
private static final Map<UUID, Long> harvestMoonPlayers = new HashMap<>();
// In VeinMinerHandler
private static final Map<UUID, Boolean> DROPS_TO_INVENTORY = new ConcurrentHashMap<>();
```

- Use `ConcurrentHashMap` for maps accessed from multiple threads (VeinMinerHandler)
- Use plain `HashMap` for maps only accessed server-tick (skill implementations)
- Always clean up on disconnect via `DISCONNECT` event: `FarmerSkill.cleanupPlayerState(uuid)`, `VeinMinerHandler.cleanupPlayerState(uuid)`

## Config Access Pattern

Never access `ModConfig.get()` directly in skill logic. Use the typed getter facade in `utils/SkillConfig.java`:
```java
// Correct:
int level = SkillConfig.getMinerNightVisionLevel();
float speed = SkillConfig.getMinerSpeedPerLevel();

// Also used: public static final int constants for backward compat
public static final int MAX_LEVEL = 100;
// But prefer: SkillConfig.getMaxLevel()
```

## Network Payload Pattern

Every C2S or S2C payload must have:
- `public static final CustomPayload.Id<T> ID` with `Identifier.of(MurilloSkills.MOD_ID, "snake_case_name")`
- `public static final PacketCodec<RegistryByteBuf, T> CODEC` via `PacketCodec.ofStatic()`
- `@Override public Id<? extends CustomPayload> getId() { return ID; }`

Identifier strings use `snake_case`: `"skills_sync"`, `"skill_selection"`, `"miner_scan_result"`.

## Mixin Conventions

- One Minecraft class per mixin file
- Class javadoc describes what the mixin intercepts and why
- `@At("RETURN")` is the most common injection point
- Client-side guard always first: `if (world.isClient()) return;`
- Pattern check always second: `if (!(player instanceof ServerPlayerEntity serverPlayer)) return;`
- Injected method names prefixed with `murilloskills$` to prevent conflicts

## Data Field Visibility

`PlayerSkillData` and `SkillStats` use `public` fields directly — no getter/setter boilerplate for plain data bags. This is a deliberate choice for Codec compatibility and brevity. Example:
```java
// Direct field access is correct:
int level = data.getSkill(skill).level;
stats.lastAbilityUse = worldTime;
```

## Comments

**Numbered step comments** inside active ability methods:
```java
// 1. Verifica Nível
// 2. Verifica Cooldown
// 3. Executa Habilidade
```

**Section dividers** in longer files:
```java
// --- LEVEL 10: NIGHT VISION ---
// === PREMIUM COLOR PALETTE ===
// ============ MÉTODOS PRIVADOS ============
```

**Inline rationale** for non-obvious choices:
```java
// Map instead of long if-else chain for O(1) lookup and cleaner code
// Limit blocks scanned per invocation to prevent lag (5000 blocks max)
// Log with limited frequency to avoid spamming console on tick errors
```

**`@SuppressWarnings` with rationale** comment — see `MurilloSkillsClient.java`:
```java
@SuppressWarnings("deprecation") // HudRenderCallback is deprecated but still functional, migration pending
```

## Java 21 Features Used

- **Records** for immutable data: `XpAddResult`, `PerkInfo`, `SynergyInfo`, `GuideEntry`, `XpSourceInfo`, `ColorPalette`
- **Pattern matching instanceof**: `player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null`
- **Switch expressions**: used in `trackSynergyForMaster()` bitmask mapping
- **`var` for local variables**: `var data = player.getAttachedOrCreate(...)`, `var config = ModConfig.get().xp`
- **`List.getFirst()`** in test assertions (Java 21 sequenced collections)
- **Text blocks**: not observed; string concatenation used in log messages

---

*Convention analysis: 2026-03-24*
