# Roadmap — MurilloSkills v1.2.x

**Milestone:** Polimento e Estabilidade
**Objetivo:** Publicar WIP finalizado, corrigir bugs críticos, melhorar toda a UI/UX
**Sem deadline**

---

## Fase 1 — Publicar WIP (v1.2.16 + v1.2.17)

**Objetivo:** Commitar e taggear o trabalho já finalizado antes de qualquer refatoração

**Escopo:**
- Commit com todos os arquivos não-commitados do v1.2.16:
  - `UltmineConfigScreen.java`, `UltmineClientConfig.java`
  - `XpDirectToggleC2SPayload.java`, `XpDirectToggleNetworkHandler.java`
  - Todas as modificações de `UltmineRadialMenuScreen`, `OreFilterScreen`, `ScrollController`, etc.
  - `CHANGELOG.md` atualizado com entrada v1.2.16
- Commit com todas as mudanças do v1.2.17:
  - Rebalanceamento de 5 classes (Farmer, Fisher, Blacksmith, Builder, Explorer)
  - Warrior looting passivo
  - `gradle.properties`: `mod_version=1.2.17`
  - `CHANGELOG.md` atualizado com entrada v1.2.17

**Requisitos atendidos:** R1.1, R1.2
**Verificação:** `./gradlew build` produz `murilloskills-1.2.17.jar` sem erros

---

## Fase 2 — Correções de Bugs Críticos

**Objetivo:** Eliminar os bugs de alta prioridade que afetam estabilidade em servidores

**Escopo:**

### 2a — Memory Leaks (disconnect cleanup)
- Adicionar chamadas de cleanup em `MinecraftEventsListener` para: `BlacksmithSkill`, `BuilderSkill`, `FisherSkill`, `ExplorerSkill`, `WarriorSkill`, `DailyChallengeManager`
- Seguir padrão de `FarmerSkill.cleanupPlayerState()`

### 2b — Daily Challenge Persistence
- Adicionar serialização de `PlayerChallengeData` ao codec de `PlayerSkillData`
- Progresso de challenge persiste via Fabric Data Attachments (sobrevive restart)

### 2c — C2S Validation
- `UltmineShapeSelectNetworkHandler`: adicionar check de nível Miner antes de processar payload
- `SkillSelectionNetworkHandler`: mover null check de `incoming` para antes de `incoming.size()`

### 2d — Damage Type API
- `LivingEntityMixin.applyResistance()`: substituir `.contains("fire")`, `.contains("fall")` etc. por `isOf(DamageTypes.*)` / `isIn(DamageTypeTags.*)`

**Requisitos atendidos:** R2.1, R2.2, R2.3, R2.4, R2.5
**Versão alvo:** v1.2.18
**Verificação:** Conectar e desconectar 3x no `runServer`; confirmar que maps ficam vazios; confirmar daily challenge sobrevive a restart

---

## Fase 3 — Refatoração SkillsScreen

**Objetivo:** Reduzir `SkillsScreen.java` de 1471 linhas para classes menores e coesas

**Escopo:**
- Analisar as seções lógicas atuais de `SkillsScreen`
- Extrair para `src/client/java/com/murilloskills/gui/renderer/`:
  - `SkillCardRenderer` — renderização de cada card de skill
  - `SkillStatsRenderer` — stats panel (nível, XP, prestige badge)
  - `PrestigeOverlayRenderer` — modal de prestige
- `SkillsScreen` fica responsável por: layout geral, captura de input, envio de payloads de rede
- Comportamento visual 100% idêntico após refatoração

**Requisitos atendidos:** R3.1, R3.2
**Versão alvo:** v1.2.19
**Verificação:** Abrir SkillsScreen no `runClient`; navegar entre tabs; fazer prestige; confirmar visual idêntico

---

## Fase 4 — UX e Dívida Técnica

**Objetivo:** Polimento final de UX e limpeza de dívida técnica identificada

**Escopo:**

### 4a — Synergy Bitmask Completo
- `SkillSelectionNetworkHandler.trackSynergyForMaster()`: mapear todos os 14 synergy IDs
- Atualizar mask check de `== 127` para cobrir todos os bits necessários

### 4b — Guards e Fixes Simples
- `EnchantmentHelperMixin`: adicionar guard `isSkillSelected(WARRIOR)`
- `BuilderSkill`: `int maxBlocks = 1000` → ler de config via `SkillConfig.getBuilderMaxFillBlocks()`
- `ModConfig.BuilderConfig`: adicionar campo `builderMaxFillBlocks`

### 4c — Limpeza de Dead Code
- Remover `stats.lastAbilityUse` duplicado de 4 skills
- Remover `// state.markDirty()` de ExplorerSkill
- Remover comentários triplicados em PrestigeManager

### 4d — HUD Overlays
- Revisar consistência visual dos overlays de XP (toasts, contextual numbers, pinned info)
- Verificar uso correto de `ColorPalette.java` em todos os overlays

**Requisitos atendidos:** R4.1, R4.2, R4.3, R4.4, R4.5
**Versão alvo:** v1.2.20
**Verificação:** Testar synergy master com todos os 14 synergies; testar warrior looting com skill não selecionada; confirmar HUD visual no runClient

---

## Sumário

| Fase | Descrição | Versão | Status |
|------|-----------|--------|--------|
| 1 | Publicar WIP (v1.2.16 + v1.2.17) | 1.2.17 | Pendente |
| 2 | Correções de Bugs Críticos | 1.2.18 | Pendente |
| 3 | Refatoração SkillsScreen | 1.2.19 | Pendente |
| 4 | UX e Dívida Técnica | 1.2.20 | Pendente |
