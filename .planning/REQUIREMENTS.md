# Requirements — v1.2.x Polimento e Estabilidade

## Objetivo do Milestone

Publicar o trabalho WIP já finalizado (v1.2.16 + v1.2.17), corrigir os bugs de alta prioridade identificados na auditoria do codebase, e melhorar toda a UI/UX do mod. Sem novas skills ou sistemas novos neste ciclo.

---

## R1 — Publicar WIP Finalizado

### R1.1 — Commit v1.2.16
- Todos os arquivos modificados e novos relacionados ao Ultmine config, XpDirect toggle, e maxSelectedSkills devem estar commitados com mensagem de release
- Arquivos-chave: `UltmineConfigScreen.java`, `UltmineClientConfig.java`, `XpDirectToggleC2SPayload.java`, `XpDirectToggleNetworkHandler.java`

### R1.2 — Commit v1.2.17
- Todas as modificações de rebalanceamento de classes (Farmer, Fisher, Blacksmith, Builder, Explorer) e Warrior looting passivo devem estar commitadas
- `gradle.properties` com `mod_version=1.2.17`

---

## R2 — Correções de Bugs Críticos

### R2.1 — Memory Leaks: Cleanup de State em Disconnect
- Ao desconectar, todos os static maps de skill devem ser limpos:
  - `BlacksmithSkill.titaniumAuraPlayers`
  - `BuilderSkill.creativeBrushPlayers` + 4 maps associados
  - `FisherSkill.rainDancePlayers`
  - `ExplorerSkill.treasureHunterActive` + `lastPositions` + `accumulatedDistance`
  - `WarriorSkill.berserkPlayers`
  - `DailyChallengeManager.playerChallenges`
- Padrão: adicionar cleanup ao handler de DISCONNECT em `MinecraftEventsListener`

### R2.2 — Daily Challenge: Persistência Entre Restarts
- Progresso de Daily Challenge deve persistir via `PlayerSkillData` codec (Fabric Data Attachments)
- Estado não pode ser perdido em restart de servidor

### R2.3 — C2S Validation: UltmineShapeSelect Permission Check
- `UltmineShapeSelectNetworkHandler` deve verificar se o jogador tem Miner no nível master antes de aceitar o payload
- Evita que jogadores sem Miner usem Ultmine via pacotes forjados

### R2.4 — Null Check: SkillSelectionNetworkHandler
- Mover o null check de `incoming` para antes do `incoming.size()` em `SkillSelectionNetworkHandler.java:46-52`
- Evita `NullPointerException` em payload malformado

### R2.5 — Damage Type API: Substituir String Matching
- `LivingEntityMixin.applyResistance()` deve usar `DamageSource.isOf(DamageTypes.*)` / `DamageSource.isIn(DamageTypeTags.*)`
- Elimina dependência de translation keys que podem mudar entre versões

---

## R3 — Refatoração SkillsScreen

### R3.1 — Extrair Renderers
- `SkillsScreen.java` (1471 linhas) deve ser decomposto em classes de renderer dedicadas
- Seguir o padrão já iniciado em `src/client/java/com/murilloskills/gui/renderer/`
- Classes alvo: TabRenderer, SkillCardRenderer, SkillStatsRenderer, PrestigeRenderer
- Comportamento visual deve permanecer idêntico após refatoração

### R3.2 — SkillsScreen Orquestra, Não Renderiza
- A classe `SkillsScreen` deve ficar responsável por layout, input e envio de network
- Lógica de renderização deve estar nos renderers extraídos

---

## R4 — UX e Dívida Técnica

### R4.1 — Synergy Bitmask: Expandir para 14 Synergies
- `SkillSelectionNetworkHandler.trackSynergyForMaster()` deve incluir todas as 14 synergies
- Bit positions devem cobrir os 7 novos IDs (survivor, industrial, sea_warrior, green_archer, prospector, adventurer, etc.)

### R4.2 — EnchantmentHelperMixin: Guard isSkillSelected para Warrior
- Adicionar verificação `playerData.isSkillSelected(MurilloSkillsList.WARRIOR)` antes de aplicar Looting bonus

### R4.3 — Builder Fill Blocks: Configurável via ModConfig
- `BuilderSkill.handleCreativeBrushPlacement()` linha 390: `int maxBlocks = 1000` → ler de `SkillConfig.getBuilderMaxFillBlocks()`
- Adicionar `builderMaxFillBlocks` em `BuilderConfig` em `ModConfig.java`

### R4.4 — Limpeza de Duplicatas e Dead Code
- Remover `stats.lastAbilityUse` duplicado de ArcherSkill, BlacksmithSkill, FisherSkill, WarriorSkill
- Remover `// state.markDirty();` de `ExplorerSkill.awardXp` (linha 238)
- Remover comentários duplicados em `PrestigeManager.java:25-28`

### R4.5 — HUD Overlays: Revisão Visual
- Revisar XP toasts, contextual XP numbers, e pinned info para consistência visual
- Garantir que todos os overlays respeitam a paleta de cores atual (`ColorPalette.java`)

---

## Fora do Escopo (v1.2.x)

- Novas skills
- Novos sistemas (PvP arenas, dungeons, quests)
- Migração de versão do Minecraft
- VeinMinerHandler refactor (grande demais para este ciclo)
- SkillConfig.java god class migration (riscos altos de regressão)
