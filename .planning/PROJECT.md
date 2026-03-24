# MurilloSkills Mod — Project Context

## What This Is

MurilloSkills is a Fabric 1.21.10 Minecraft mod that adds a full RPG skill progression system to the base game. Players choose up to 3 skills from 8 available classes (Miner, Warrior, Archer, Farmer, Fisher, Blacksmith, Builder, Explorer), level them from 0 to 100, unlock milestone perks at levels 10/25/50/75/100, and designate one as their Paragon (the only skill that can reach level 100). The mod also includes a prestige system (0–10), daily challenges, skill synergies for combined bonuses, and achievement tracking.

## Current State

- **Version:** 1.2.17 (uncommitted — last tagged release is 1.2.15)
- **Platform:** Minecraft 1.21.10 + Fabric Loader 0.18.1 + Fabric API 0.138.3
- **Language:** Java 21
- **Uncommitted work:** v1.2.16 (UltmineConfigScreen, XpDirect toggle, maxSelectedSkills config) + v1.2.17 (class rebalancing, Warrior looting passive) — all done, needs committing

## Who Uses It

Solo developer project. Deployed on personal/private servers. No external dependencies beyond Fabric API.

## Active Milestone

**v1.2.x — Polimento e Estabilidade**

Goal: commit the finished WIP, fix the high-priority bugs identified in the codebase audit, and improve the UI. No new systems or skills in scope. No deadline.

## Key Constraints

- Validation is done by running `./gradlew runClient` — no automated test suite beyond `UltmineShapeCalculatorTest`
- Split source sets (`src/main/` server, `src/client/` client) enforced by Fabric Loom — changes must respect this boundary
- All player-facing text must be added to all 3 lang files (en_us, pt_br, es_es)
- Config changes require both `ModConfig.java` addition and `SkillConfig.java` getter

## Codebase Map

Located in `.planning/codebase/`:
- `STACK.md` — Java 21, Fabric, Gradle, no external mod deps
- `INTEGRATIONS.md` — Fabric API modules used
- `ARCHITECTURE.md` — Strategy+Registry, split source sets, data flow
- `STRUCTURE.md` — Directory layout, 37 mixins, 22 payloads
- `CONVENTIONS.md` — Naming, comment language (PT/EN mix), patterns
- `TESTING.md` — No test suite; runClient validation
- `CONCERNS.md` — High/medium/low priority technical debt (audited 2026-03-24)
