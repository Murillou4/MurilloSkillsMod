# Project State

**Last updated:** 2026-03-24
**Current milestone:** v1.2.x — Polimento e Estabilidade
**Current phase:** 2 (Correções de Bugs Críticos)
**Overall status:** Phase 1 complete — ready for Phase 2

---

## Active Work

**Phase 2** is next: fix the 5 high-priority bugs (memory leaks, daily challenge persistence, C2S validation, null check, damage type strings).

---

## Phase Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Publicar WIP (v1.2.16 + v1.2.17) | Complete |
| 2 | Correções de Bugs Críticos | Not started |
| 3 | Refatoração SkillsScreen | Not started |
| 4 | UX e Dívida Técnica | Not started |

---

## Key Decisions

- Scope locked to v1.2.x (no new skills, no new systems)
- VeinMinerHandler refactor deferred (too risky for this cycle)
- SkillConfig.java migration deferred (regression risk)
- All UI improvements in scope (SkillsScreen, HUD overlays)
- No deadline

---

## Codebase Snapshot (2026-03-24)

- Version: 1.2.17 (committed — 88fd199 + 7c4b22f)
- 8 skills, 22 payloads, 37 mixins
- High-priority bugs: 5 (memory leaks, daily challenge persistence, C2S validation, null check, damage type strings)
- God classes: SkillsScreen (1471 lines), SkillConfig (1341 lines), VeinMinerHandler (864 lines)
- Test coverage: near-zero (only UltmineShapeCalculatorTest exists)
