# Phase 1 Plan — Publicar WIP (v1.2.16 + v1.2.17)

**Goal:** Commitar todo o trabalho finalizado e não-commitado, produzindo dois commits de release limpos na branch master.

**Phase from ROADMAP:** Fase 1 — Publicar WIP
**Requirements covered:** R1.1, R1.2
**Expected output:** 2 commits de release no histórico git + build verde

---

## Context

O último commit taggeado é `f12ca7f release: 1.2.15`. Existem 38 arquivos modificados e 4 novos arquivos não-trackeados representando duas versões completas e testadas (v1.2.16 e v1.2.17) que ainda não foram commitadas.

**Arquivos novos (untracked):**
- `src/client/java/com/murilloskills/client/config/UltmineClientConfig.java`
- `src/client/java/com/murilloskills/gui/UltmineConfigScreen.java`
- `src/main/java/com/murilloskills/network/XpDirectToggleC2SPayload.java`
- `src/main/java/com/murilloskills/network/handlers/XpDirectToggleNetworkHandler.java`

**Arquivos não-commitados relevantes para v1.2.16:**
- UltmineConfigScreen, UltmineClientConfig (nova tela de config)
- XpDirectToggleC2SPayload, XpDirectToggleNetworkHandler (toggle XP direto)
- UltmineRadialMenuScreen (botão de acesso à config)
- UltmineShape, UltmineShapeCalculator (variantes de shape)
- VeinMinerHandler (XP direct, tool durability)
- ModNetwork, NetworkHandlerRegistry (registro dos novos payloads)
- SkillsSyncPayload, SkillSelectionC2SPayload, SkillSelectionNetworkHandler (maxSelectedSkills)
- ModConfig (maxSelectedSkills config)
- OreFilterScreen, ScrollController (UI fixes)

**Arquivos não-commitados relevantes para v1.2.17:**
- BlacksmithSkill, BuilderSkill, ExplorerSkill, FarmerSkill, FisherSkill (rebalanceamento)
- EnchantmentHelperMixin (warrior looting passivo)
- SkillsScreen, ModInfoScreen (tooltips e guia atualizados)
- SkillConfig (novos valores de config para classes rebalanceadas)
- MinecraftEventsListener (eventos novos para passivas)
- lang/en_us.json, lang/pt_br.json, lang/es_es.json (31 novas chaves)
- UPDATE_1.2.17.md (patch notes)

**Arquivos compartilhados entre ambas as versões:**
- CHANGELOG.md (tem as duas entradas)
- gradle.properties (mod_version=1.2.17)
- ClientSkillData, SkillUiData, UltmineClientState, SkillAdminCommands, SkillsNetworkUtils, BuilderBlockPlacementMixin, UltmineShapeSelectC2SPayload, UltmineShapeSelectNetworkHandler, ColorPalette, PlayerSkillData

---

## Tasks

### Task 1 — Verificar build antes de commitar

**Why:** Garantir que o código está compilável antes de criar o histórico

```bash
./gradlew build
```

**Success:** BUILD SUCCESSFUL, produz `build/libs/murilloskills-1.2.17.jar`
**Failure:** Corrigir erros de compilação antes de continuar

---

### Task 2 — Commit v1.2.16: Ultmine Config + XP Direct + maxSelectedSkills

**Why:** Separar as duas versões no histórico git para rastreabilidade

Staged files para este commit (arquivos que pertencem exclusivamente ao v1.2.16):

```bash
git add \
  src/client/java/com/murilloskills/client/config/UltmineClientConfig.java \
  src/client/java/com/murilloskills/gui/UltmineConfigScreen.java \
  src/main/java/com/murilloskills/network/XpDirectToggleC2SPayload.java \
  src/main/java/com/murilloskills/network/handlers/XpDirectToggleNetworkHandler.java \
  src/client/java/com/murilloskills/gui/UltmineRadialMenuScreen.java \
  src/main/java/com/murilloskills/skills/UltmineShape.java \
  src/main/java/com/murilloskills/skills/UltmineShapeCalculator.java \
  src/main/java/com/murilloskills/skills/VeinMinerHandler.java \
  src/main/java/com/murilloskills/network/ModNetwork.java \
  src/main/java/com/murilloskills/network/handlers/NetworkHandlerRegistry.java \
  src/main/java/com/murilloskills/network/SkillsSyncPayload.java \
  src/main/java/com/murilloskills/network/SkillSelectionC2SPayload.java \
  src/main/java/com/murilloskills/network/handlers/SkillSelectionNetworkHandler.java \
  src/main/java/com/murilloskills/network/UltmineShapeSelectC2SPayload.java \
  src/main/java/com/murilloskills/network/handlers/UltmineShapeSelectNetworkHandler.java \
  src/client/java/com/murilloskills/gui/OreFilterScreen.java \
  src/client/java/com/murilloskills/gui/ScrollController.java \
  src/client/java/com/murilloskills/data/UltmineClientState.java \
  src/client/java/com/murilloskills/MurilloSkillsClient.java
```

**NOTA IMPORTANTE:** Antes de staged os arquivos compartilhados (CHANGELOG.md, gradle.properties, lang/*.json), é necessário decidir: esses arquivos têm mudanças de AMBAS as versões. Para o commit v1.2.16, não adicionar esses arquivos ainda — eles irão no commit v1.2.17.

**Mensagem de commit:**
```
release: 1.2.16 ultmine-config xp-direct max-skills

- Nova tela de configuração do Ultmine (UltmineConfigScreen)
- Config persistente do cliente (UltmineClientConfig)
- Toggle XP direto ao jogador (XpDirectToggleC2SPayload)
- maxSelectedSkills configurável no servidor
- Variantes de shapes do Ultmine (stairs up/down, 20x20 H/V)
- Durabilidade de ferramenta no vein miner corrigida
```

---

### Task 3 — Commit v1.2.17: Classes Rebalanceadas + Warrior Looting

Staged files para este commit (tudo que sobrou):

```bash
git add \
  CHANGELOG.md \
  UPDATE_1.2.17.md \
  gradle.properties \
  src/main/java/com/murilloskills/impl/BlacksmithSkill.java \
  src/main/java/com/murilloskills/impl/BuilderSkill.java \
  src/main/java/com/murilloskills/impl/ExplorerSkill.java \
  src/main/java/com/murilloskills/impl/FarmerSkill.java \
  src/main/java/com/murilloskills/impl/FisherSkill.java \
  src/main/java/com/murilloskills/mixin/EnchantmentHelperMixin.java \
  src/main/java/com/murilloskills/utils/SkillConfig.java \
  src/main/java/com/murilloskills/utils/SkillsNetworkUtils.java \
  src/main/java/com/murilloskills/events/MinecraftEventsListener.java \
  src/main/java/com/murilloskills/config/ModConfig.java \
  src/main/java/com/murilloskills/data/PlayerSkillData.java \
  src/main/java/com/murilloskills/commands/SkillAdminCommands.java \
  src/main/java/com/murilloskills/mixin/BuilderBlockPlacementMixin.java \
  src/client/java/com/murilloskills/data/ClientSkillData.java \
  src/client/java/com/murilloskills/gui/data/SkillUiData.java \
  src/client/java/com/murilloskills/gui/SkillsScreen.java \
  src/client/java/com/murilloskills/gui/ModInfoScreen.java \
  src/client/java/com/murilloskills/gui/ColorPalette.java \
  src/main/resources/assets/murilloskills/lang/en_us.json \
  src/main/resources/assets/murilloskills/lang/pt_br.json \
  src/main/resources/assets/murilloskills/lang/es_es.json
```

**Mensagem de commit:**
```
release: 1.2.17 class-rebalance warrior-looting

- Warrior: Looting passivo baseado em nível (+2% por nível, max +2)
- Farmer: Vitalidade Natural (nível 35), Mestre das Sementes (nível 60)
- Fisher: Benção do Oceano (nível 35), Fortuna do Mar (nível 60)
- Blacksmith: Domínio do Fogo (nível 35), Aura de Reparo (nível 60)
- Builder: Vigor do Construtor (nível 35), Construção Leve (nível 60)
- Explorer: Desbravador (nível 45), Recuperação Rápida (nível 55)
- 31 novas chaves de localização (en_us, pt_br, es_es)
- Tooltips e guia de progressão atualizados
```

---

### Task 4 — Verificar git status limpo

```bash
git status
git log --oneline -5
```

**Success:** Working tree clean. Os 5 commits mais recentes mostram os dois novos releases.

---

### Task 5 — Atualizar STATE.md

Marcar Fase 1 como completa em `.planning/STATE.md`.

---

## Rollback

Se o build falhar na Task 1: não commitar nada. Investigar erro antes de avançar.
Se o staging ficar confuso: `git reset HEAD` para desfazer staging sem perder changes.

## Verification Criteria

- [ ] `./gradlew build` retorna BUILD SUCCESSFUL
- [ ] `git log --oneline -3` mostra dois novos commits de release
- [ ] `git status` mostra working tree clean (exceto arquivos ignorados como `.claude/`, `logs/`, `net/`)
- [ ] `build/libs/murilloskills-1.2.17.jar` existe e tem tamanho razoável (>100KB)
