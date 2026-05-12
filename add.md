# UltPlace — Colocação em Massa para o Builder

## Context

O Builder hoje só ganha a feature de "construção em massa" no nível 100 (Creative Brush — fill entre dois pontos, 2 minutos, cooldown 7.5min). Falta uma ferramenta *utilitária do dia-a-dia* para construção — espelho do **Ultmine** do Miner, mas para **colocar** blocos: stamps 3×3, paredes, túneis, colunas, escadas etc., disponível desde o nível 1.

Decisões já confirmadas com o usuário:
- **Nível mínimo:** 1 (todas as formas liberadas de cara).
- **Formatos:** Plano NxN, Linha, Parede, Escadas, Coluna, Túnel 3x3 oco — + extras úteis que eu propus (ver abaixo).
- **Ativação:** Toggle on/off (não hold, diferente do ultmine).
- **Custo:** só consome inventário — sem XP cost, sem cooldown.

A arquitetura espelha fielmente o Ultmine (`VeinMinerHandler`, `UltmineShape`, `UltmineShapeCalculator`, `UltmineConfigScreen`, `UltminePreview`) para manter consistência de código e de UX.

## Arquitetura

### Novos arquivos (server/common)
- `src/main/java/com/murilloskills/skills/UltPlaceShape.java` — enum espelhando `UltmineShape`:
  - `PLANE_NxN` (stamp no plano da face clicada)
  - `LINE` (linha na direção olhada)
  - `WALL` (retângulo vertical)
  - `STAIRS` (degraus, variantes sobe/desce)
  - `COLUMN` (pilar para cima/baixo, variante)
  - `TUNNEL_3x3` (túnel oco 3x3 na direção olhada, comprimento configurável)
  - `CIRCLE` *(extra útil — disco horizontal)*
  - `SPHERE_SHELL` *(extra útil — casca esférica oca, para domos)*
  - `SINGLE` (desliga o ult place efetivamente, equivalente ao "off")
- `src/main/java/com/murilloskills/skills/UltPlaceShapeCalculator.java` — pura função `List<BlockPos> getShapeBlocks(origin, shape, size, length, face, lookVec, variant)`. Reaproveita helpers de `UltmineShapeCalculator.java:20-52` onde possível (planar, line, stairs).
- `src/main/java/com/murilloskills/skills/UltPlaceHandler.java` — espelha `VeinMinerHandler` (`src/main/java/com/murilloskills/skills/VeinMinerHandler.java:43-51`):
  - Mapas `ConcurrentHashMap<UUID, ...>` para `shape`, `size`, `length`, `variant`, `enabled` (toggle).
  - `handle(player, world, clickedPos, face, itemStack, hand)` — chamado após o placement bem-sucedido para expandir.
  - `setSelection(...)`, `setEnabled(...)`, `isEnabled(...)`.
  - Reentrância protegida via set `ACTIVE_PLAYERS` (mesmo padrão `VeinMinerHandler.java:261`).
  - Inventário: procura em toda inventário+offhand o mesmo item (reutiliza lógica de `BuilderSkill.findBlockInInventory` em `impl/BuilderSkill.java:589-600`).
  - Substitui apenas blocos que `canReplace` retorna true (ar, água, grama alta) — nunca sobrescreve construção existente.
  - **Undo buffer:** guarda `Map<UUID, Deque<UndoSnapshot>>` com as últimas 5 ativações — `UndoSnapshot` contém lista de `(BlockPos, BlockState anterior)`. Tecla de undo restaura o último snapshot e devolve os itens ao inventário.

### Novas payloads de rede (`src/main/java/com/murilloskills/network/`)
- `UltPlaceConfigC2SPayload(UltPlaceShape shape, int size, int length, int variant, boolean enabled)` — cliente → servidor ao mudar config.
- `UltPlacePreviewRequestC2SPayload(BlockPos pos, Direction face)` — cliente pede preview (rate-limited ~ 4 ticks, igual `UltmineRequestC2SPayload`).
- `UltPlacePreviewS2CPayload(List<BlockPos> positions)` — servidor responde com posições válidas.
- `UltPlaceUndoC2SPayload()` — pede undo da última colocação em massa.

Handlers novos em `src/main/java/com/murilloskills/network/handlers/`:
- `UltPlaceConfigNetworkHandler`
- `UltPlacePreviewNetworkHandler`
- `UltPlaceUndoNetworkHandler`

Registrar em `src/main/java/com/murilloskills/network/ModNetwork.java` e em `NetworkHandlerRegistry`.

### Hook de placement (server)
Modificar `src/main/java/com/murilloskills/events/BlockPlacementHandler.java:29` — `onBlockPlaced`:
- Após lógica de XP já existente, chamar `UltPlaceHandler.handle(...)` passando player, world, placementPos, face, itemStack.
- A face clicada precisa vir do mixin — ajustar `src/main/java/com/murilloskills/mixin/BuilderBlockPlacementMixin.java:23` para capturar `ItemPlacementContext.getSide()` e propagar até o handler (adicionar parâmetro novo em `onBlockPlaced`).

### Cliente

**Novos arquivos:**
- `src/client/java/com/murilloskills/data/UltPlaceClientState.java` — espelho de `UltmineClientState.java`: armazena shape, size, length, variant, enabled, preview list.
- `src/client/java/com/murilloskills/render/UltPlacePreview.java` — espelha `UltminePreview.java:23-69`. Renderiza outlines nos blocos que serão colocados (cor diferente — ex. azul-claro/verde, em vez do vermelho do ultmine) **apenas quando a toggle está ligada** E o player está segurando um `BlockItem`.
- `src/client/java/com/murilloskills/gui/UltPlaceConfigScreen.java` — espelha `UltmineConfigScreen.java:28-813`:
  - Tabs por shape (PLANE, LINE, WALL, STAIRS, COLUMN, TUNNEL, CIRCLE, SPHERE)
  - Campo de tamanho (`size`) com min=1, max por shape via `SkillConfig`.
  - Campo de length (para LINE, TUNNEL, COLUMN, STAIRS).
  - Botões ◀ ▶ de variante (direção, orientação).
  - Toggle master on/off.
  - Botão "Undo last placement" (atalho visual para a tecla).

**Modificar `src/client/java/com/murilloskills/MurilloSkillsClient.java:275`** — adicionar 3 keybinds novos:
- `ultPlaceToggleKey` — default `V` — alterna on/off e envia `UltPlaceConfigC2SPayload`.
- `ultPlaceConfigKey` — default `B` — abre `UltPlaceConfigScreen`.
- `ultPlaceUndoKey` — default `Z` (ou `Ctrl+Z` via check de modifier) — envia `UltPlaceUndoC2SPayload`.

Loop de preview request (semelhante ao do ultmine em `MurilloSkillsClient.java:267`): se toggle ligada e raycast num bloco, enviar `UltPlacePreviewRequestC2SPayload` a cada 4 ticks.

### Config (`src/main/java/com/murilloskills/config/ModConfig.java`)

Adicionar nova seção `BuilderUltPlaceConfig` (paralela à `UltmineConfig.java:259-310`):
```java
public static class BuilderUltPlaceConfig {
    public boolean enabled = true;
    public int maxBlocksPerUse = 200;   // teto de segurança
    public int undoHistorySize = 5;
    // por-shape: (defaultSize, maxSize, defaultLength, maxLength)
    public UltPlaceShapeSettings plane      = new UltPlaceShapeSettings(3, 15, 1, 1);
    public UltPlaceShapeSettings line       = new UltPlaceShapeSettings(1, 1, 5, 64);
    public UltPlaceShapeSettings wall       = new UltPlaceShapeSettings(3, 15, 3, 15);
    public UltPlaceShapeSettings stairs     = new UltPlaceShapeSettings(1, 1, 8, 32);
    public UltPlaceShapeSettings column     = new UltPlaceShapeSettings(1, 1, 5, 32);
    public UltPlaceShapeSettings tunnel3x3  = new UltPlaceShapeSettings(3, 3, 5, 32);
    public UltPlaceShapeSettings circle     = new UltPlaceShapeSettings(3, 20, 1, 1);
    public UltPlaceShapeSettings sphereShell= new UltPlaceShapeSettings(3, 15, 1, 1);
}
```

Criar `UltPlaceShapeSettings` (record simples com 4 ints) — ou reaproveitar `UltmineShapeSettings` se a assinatura bater exatamente.

Adicionar getters em `src/main/java/com/murilloskills/utils/SkillConfig.java:103-186` espelhando os getters de ultmine.

### Locale
Adicionar chaves em `src/main/resources/assets/murilloskills/lang/{en_us,pt_br,es_es}.json`:
- `murilloskills.ultplace.shape.plane`, `.line`, `.wall`, `.stairs`, `.column`, `.tunnel_3x3`, `.circle`, `.sphere_shell`
- `murilloskills.ultplace.toggle`, `.size`, `.length`, `.variant`, `.undo`
- `murilloskills.keybind.ultplace_toggle`, `.ultplace_config`, `.ultplace_undo`
- `murilloskills.ultplace.enabled.on`, `.enabled.off`

### Registro dos keybinds
No `CLAUDE.md` já se menciona 10 keybinds — essa feature adiciona 3, fica em 13. Registrar sob a mesma categoria `"key.categories.murilloskills"`.

## Arquivos críticos a modificar

| Arquivo | O que muda |
|---|---|
| `src/main/java/com/murilloskills/events/BlockPlacementHandler.java:29` | Chamar `UltPlaceHandler.handle(...)` após XP |
| `src/main/java/com/murilloskills/mixin/BuilderBlockPlacementMixin.java:23` | Propagar `Direction face` para o handler |
| `src/main/java/com/murilloskills/config/ModConfig.java:201-222` | Adicionar `BuilderUltPlaceConfig` |
| `src/main/java/com/murilloskills/utils/SkillConfig.java:103-186` | Adicionar getters do UltPlace |
| `src/main/java/com/murilloskills/network/ModNetwork.java` | Registrar 4 payloads novos |
| `src/main/java/com/murilloskills/MurilloSkills.java` (`onInitialize`) | Registrar receivers server-side |
| `src/client/java/com/murilloskills/MurilloSkillsClient.java:275-362` | 3 keybinds + tick loop de preview |
| `src/main/resources/assets/murilloskills/lang/*.json` | Chaves de tradução |

## Arquivos novos

Server/common:
- `skills/UltPlaceShape.java`
- `skills/UltPlaceShapeCalculator.java`
- `skills/UltPlaceHandler.java`
- `network/UltPlaceConfigC2SPayload.java`
- `network/UltPlacePreviewRequestC2SPayload.java`
- `network/UltPlacePreviewS2CPayload.java`
- `network/UltPlaceUndoC2SPayload.java`
- `network/handlers/UltPlaceConfigNetworkHandler.java`
- `network/handlers/UltPlacePreviewNetworkHandler.java`
- `network/handlers/UltPlaceUndoNetworkHandler.java`
- `config/BuilderUltPlaceConfig.java` (ou classe aninhada)

Client:
- `gui/UltPlaceConfigScreen.java`
- `data/UltPlaceClientState.java`
- `render/UltPlacePreview.java`

## Reuso de código existente

- **Outline rendering:** `VeinMinerPreview.renderOutlines(...)` já é pública e pode ser usada em `UltPlacePreview` só trocando a cor.
- **findBlockInInventory:** extrair método de `BuilderSkill.java:589-600` para um utilitário `utils/InventoryBlockFinder.java` para que UltPlaceHandler o consuma sem duplicar.
- **Shape calculation helpers:** `UltmineShapeCalculator.addPlanar`, `addLine`, `addStairs` — mover para um `ShapeMath` utilitário e reaproveitar no UltPlace (evita divergência futura entre os dois sistemas).
- **GUI widgets:** o layout visual/gradientes de `UltmineConfigScreen.java:422-778` pode virar um helper base para ambas as telas.

## Verificação

1. `./gradlew build` — compila sem erros.
2. `./gradlew runClient`:
   - Criar mundo criativo/sobrevivência.
   - Apertar `V` → mensagem/HUD indicando toggle on.
   - Apertar `B` → abre `UltPlaceConfigScreen`.
   - Selecionar PLANE 3×3 → colocar bloco → deve aparecer stamp 3×3 na face clicada.
   - Trocar para WALL 5×5 → parede vertical.
   - Testar STAIRS, COLUMN, TUNNEL_3x3, LINE, CIRCLE, SPHERE_SHELL.
   - Apertar `Z` → último placement é desfeito, blocos voltam ao inventário.
   - Esgotar inventário → feature só coloca até onde tem item, não duplica.
   - Testar preview: os outlines aparecem enquanto segurando `BlockItem` na mão, e somem ao trocar para ferramenta.
   - Apertar `V` de novo → toggle off, preview some, placements voltam ao normal.
3. Confirmar que `config/murilloskills.json` escreveu a nova seção `builderUltPlace`.
4. Confirmar em multiplayer (`./gradlew runServer` + client) que preview/toggle/undo funcionam via rede.
