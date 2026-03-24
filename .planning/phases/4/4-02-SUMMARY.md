---
phase: 4-ux-divida-tecnica
plan: "02"
subsystem: client-rendering
tags: [color-palette, refactor, hud, consistency]
dependency_graph:
  requires: []
  provides: [ColorPalette-consistent HUD overlay, ColorPalette-consistent bonus indicator]
  affects: [XpToastRenderer, DailyChallengeRenderer]
tech_stack:
  added: []
  patterns: [ColorPalette single source of truth for theme colors]
key_files:
  created: []
  modified:
    - src/client/java/com/murilloskills/render/XpToastRenderer.java
    - src/client/java/com/murilloskills/gui/renderer/DailyChallengeRenderer.java
decisions:
  - "Used PALETTE.bgGradientTop() with 0xCC alpha mask to preserve alpha semantics while sourcing RGB from palette"
  - "getSkillColor() method retained but now delegates to PALETTE.getSkillColor() for single-call clarity"
metrics:
  duration: "~8 minutes"
  completed_date: "2026-03-24"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 2
---

# Phase 4 Plan 02: Migrate HUD Renderers to ColorPalette Summary

Migrated XpToastRenderer and DailyChallengeRenderer to use shared ColorPalette constants for visual consistency, replacing hardcoded hex color literals with `ColorPalette.premium()` sourced values.

## Tasks Completed

| # | Task | Commit | Status |
|---|------|--------|--------|
| 1 | Migrate XpToastRenderer to ColorPalette | f346044 | Done |
| 2 | Replace hardcoded gold glow in DailyChallengeRenderer | f8009cd | Done |

## What Was Built

**Task 1 — XpToastRenderer:**
- Added `import com.murilloskills.gui.ColorPalette`
- Added `private static final ColorPalette PALETTE = ColorPalette.premium()` as the first static field (before BG_COLOR/BORDER_COLOR which depend on it)
- Replaced `BG_COLOR = 0xCC101018` with `0xCC000000 | (PALETTE.bgGradientTop() & 0x00FFFFFF)` — preserves the 0xCC alpha while sourcing RGB from `bgGradientTop` (0x101018)
- Replaced `BORDER_COLOR = 0xFF2A2A3A` with `PALETTE.sectionBorder()` — 1:1 color match (0xFF2A2A3A)
- Replaced 8-case `getSkillColor()` switch with single `return PALETTE.getSkillColor(skill)` delegation

**Task 2 — DailyChallengeRenderer:**
- Replaced `0x40FFD700` literal in bonus indicator fill with `palette.cardGlowParagon()` — 1:1 color match; palette is already a parameter

## Acceptance Criteria Verified

| Criterion | Result |
|-----------|--------|
| `ColorPalette` in XpToastRenderer — 3+ matches | Pass (import + PALETTE field + getSkillColor) |
| `0xFF2A2A3A` count in XpToastRenderer = 0 | Pass |
| `0xCC101018` count in XpToastRenderer = 0 | Pass |
| `PALETTE.getSkillColor` count in XpToastRenderer = 1 | Pass |
| `0xFF888888` count in XpToastRenderer = 0 | Pass |
| `PALETTE.sectionBorder()` in XpToastRenderer = 1 match | Pass |
| `PALETTE.bgGradientTop()` in XpToastRenderer = 1 match | Pass |
| `0x40FFD700` count in DailyChallengeRenderer = 0 | Pass |
| `palette.cardGlowParagon()` count in DailyChallengeRenderer = 1 | Pass |

## Build Status

Build has pre-existing errors in unrelated files (`SkillsScreen.java`, `SkillCardRenderer.java`, `SkillStatsRenderer.java`, `SkillTooltipRenderer.java`, and main-source packages). Neither `XpToastRenderer.java` nor `DailyChallengeRenderer.java` produced any compiler errors.

## Deviations from Plan

None — plan executed exactly as written. All color values are exact 1:1 matches to their palette equivalents (except skill colors which intentionally align to the canonical ColorPalette values, which is the goal of this migration).

## Known Stubs

None. Both changes are complete migrations with no placeholders.
