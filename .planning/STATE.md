# Project State

**Last updated:** 2026-03-24
**Current milestone:** v1.2.x — Polimento e Estabilidade
**Current phase:** 4 (UX e Dívida Técnica)
**Overall status:** Phase 4 Plan 01 complete — R4.3 config wiring and R4.4 dead code removal done

---

## Active Work

**Phase 4 Plan 02** is next: remaining UX and technical debt items.

---

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Publicar WIP (v1.2.16 + v1.2.17) | Complete |
| 2 | Correções de Bugs Críticos | Complete |
| 3 | Refatoração SkillsScreen | Complete |
| 4 | UX e Dívida Técnica | In progress |

---

## Key Decisions

- Scope locked to v1.2.x (no new skills, no new systems)
- VeinMinerHandler refactor deferred (too risky for this cycle)
- SkillConfig.java migration deferred (regression risk)
- All UI improvements in scope (SkillsScreen, HUD overlays)
- No deadline
- Pre-existing Phase 3 renderer build errors (SkillsScreen, SkillCardRenderer, SkillTooltipRenderer) out of scope for Phase 4 plan 01 — separate working tree issue

---

## Codebase Snapshot (2026-03-24)

- Version: 1.2.17 (committed — 88fd199 + 7c4b22f)
- 8 skills, 22 payloads, 37 mixins
- High-priority bugs: 5 (memory leaks, daily challenge persistence, C2S validation, null check, damage type strings)
- God classes: SkillsScreen (1471 lines), SkillConfig (1341 lines), VeinMinerHandler (864 lines)
- Test coverage: near-zero (only UltmineShapeCalculatorTest exists)
