# Version Porting TODO

This file tracks changes intentionally made in only one Minecraft target so they are not forgotten when the rest of the version matrix is updated.

## Policy

- When a user asks for a fix or feature in a single Minecraft version first, add an entry here for the other supported versions before calling the work done.
- Keep the entry until the same behavior is ported and validated in each relevant loader/version target.

## Pending

- [ ] Avaliar o novo radial Ultmine 2.2.6 somente em futuros alvos 2.x.
  - Alterado apenas `targets/26.2/fabric/client/UltmineRadialScreen`: seis cartões flutuantes e núcleo compacto com SkillsTheme, remoção do aro/rodapé largo, seleção angular com área neutra e foco coerente.
  - Sem mudança de gameplay. Outras versões/loaders preservados. Build sem testes; runtime/UI NOT_RUN a pedido do usuário, que fará a avaliação no jogo.

- [ ] Avaliar a apresentação do Ultmine 2.2.5 somente em futuros alvos 2.x.
  - Alterado apenas `targets/26.2/fabric`: `MarkerRenderer`, `UltmineRadialScreen`, `ShapeChoice` e `SkillsButton`.
  - Prévia branca semitransparente e radial neutro com ícones compartilhados, capacidade real e instruções organizadas. Gameplay e teclas preservados.
  - Demais versões/loaders não foram alterados. Resultado específico em `targets/26.2/fabric/VALIDATION_STATUS.md`.

- [ ] Avaliar o controle do card do HUD da 2.2.4 somente em futuros alvos 2.x.
  - Alterado `targets/26.2/fabric`: `client/ClientPreferences`, `SkillsHud` e `ProfessionExtras`.
  - Card equipado contextual por padrão, com modos Sempre/Oculto e configuração local persistente em Ajuda; outros avisos e gameplay preservados.
  - Outros alvos/loaders não foram alterados. Validação específica em `targets/26.2/fabric/VALIDATION_STATUS.md`.

- [ ] Avaliar as correções 2.2.3 somente em futuros alvos 2.x.
  - Alvo alterado: `targets/26.2/fabric`; `SummaryService`, `ProfessionRows/Details/Tools`, `OwnedEffects`, `FisherSkill`, `ExplorerSkill` e `BuilderSkill`.
  - Marcos mostram valores durante a ativa e texto completo; controles de espaçamento refletem formas existentes; efeitos condicionais limpam apenas sua própria origem. Descrições numéricas corrigidas sem rebalancear.
  - Versões 1.x e outros loaders permanecem preservados. Validação deste alvo em `targets/26.2/fabric/VALIDATION_STATUS.md`.

- [ ] Avaliar as correções de apresentação 2.2.2 somente em futuros alvos 2.x.
  - Alterado `targets/26.2/fabric`: definições/formatos em `skills/effects`, custos/recompensas compartilhados, textos das oito profissões, UI/radiais e metadados de versão.
  - Números e mecânicas preservados; capacidade do radial passa a vir do servidor, limites existentes de UltPlace/Pincel são exibidos separadamente, atalhos seguem remapeamentos, unidades e condições foram corrigidas.
  - Versões 1.x e outros loaders permanecem preservados. Ver `targets/26.2/fabric/VALIDATION_STATUS.md` para validação deste alvo.

- [ ] Avaliar tooltip limitado à tela e comando maxselected da 2.2.1 em futuros alvos 2.x.
  - Alterado somente `targets/26.2/fabric`: `client/ProfessionTooltip`, `ProfessionSelection`, `ProfessionScreen` e `commands/SkillCommands`.
  - Tooltips compactos de seleção e quebra de linhas com limites de tela; comando administrativo para nível 100 apenas nas profissões escolhidas, preservando demais dados.
  - Versões 1.x e outros loaders permanecem preservados, sem port automático. Validação específica em `targets/26.2/fabric/VALIDATION_STATUS.md`.

- [ ] Avaliar a experiência 2.2 somente em futuros alvos 2.x.
  - Alterado: `targets/26.2/fabric`: componentes `Profession*`, estado/sessão/revisão de apresentação, seleção inicial atômica, respostas estruturadas, definições de efeitos/marcos e composição compartilhada, oito implementações, ferramentas e adaptadores nativos.
  - Comportamento: catálogo completo 4×2, início com quatro posições, ramos comparáveis, controles por função, formas em grade, resultados reais das ativas, cultivo completo, pesca atualizada, headshot pelo impacto e desfazer durante Pincel.
  - Números, curva de XP e dados 2.1 preservados. Cliente e servidor devem atualizar juntos. Apenas Fabric 26.2 recebe 2.2.0; versões 1.x e demais loaders permanecem preservados. Status: `targets/26.2/fabric/VALIDATION_STATUS.md`.

- [ ] Avaliar a UI, controles e recuperacoes do core 2.1 somente em futuros alvos 2.x.
  - Alterado: `targets/26.2/fabric`: oito `impl/*Skill`, definicoes em `skills/effects`, `ProfessionScreen`, entrada, persistencia, rede, ferramentas e mixins nativos.
  - Painel unico, valores do servidor, comparacoes de ramos, utilidades independentes e recursos proprios recuperados da 1.21.10. Apenas Fabric 26.2 recebe 2.1.0.
  - Versoes 1.x e demais loaders permanecem preservados, sem port automatico. Validacao 2.1 em `targets/26.2/fabric/VALIDATION_STATUS.md`.

- [ ] Avaliar a correção de entrada/radial da 2.0.1 somente quando outros alvos receberem a linha 2.x.
  - Fonte alterada: `targets/26.2/fabric`, especialmente `client/MiningInput`, `UltmineRadialScreen`, `MurilloSkillsClient`, HUD, callbacks e validação com teclado/mouse reais.
  - Comportamento: radial próprio de Ultmine, tecla segurada sincronizada antes do primeiro ataque vanilla, suporte a remapeamento e feedback de bloqueio. As versões antigas e seus radiais próprios permanecem preservados; nenhum outro loader foi alterado nesta correção.

- [ ] Avaliar futuros ports da linha 2.0 após a estreia Fabric 26.2.
  - Alvo desta entrega: `targets/26.2/fabric` (2.0.0), com API/registro, oito implementações, progressão, dados, ferramentas, rede e cliente nativos.
  - Mudanças: quatro profissões, marcos a cada cinco níveis, ramos, ativas progressivas, contratos persistentes, selos, prestígio, Paragon, sinergias, importação explícita e nova UI baseada na identidade da 1.21.10.
  - Todas as versões 1.x, Fabric antigo, Forge, NeoForge e Legacy permanecem preservadas. Não recebem automaticamente estas mudanças nem contam como validadas por este alvo. Novos ports dependem de escopo próprio.
  - Build/validação 26.2: `tools/validate-26.2.ps1`; status em `targets/26.2/fabric/VALIDATION_STATUS.md`. O empacotador reflexivo 1.x `tools/build-all.ps1` não deve gerar a 26.2.

- [ ] Decide whether the Forge 1.12.2 native split should be mirrored in other maintained legacy loader targets.
  - Source target changed now: `targets/1.12.2/forge/src/main/java/com/murilloskills/forge112/`
  - Changes: replaced the old single-file Forge 1.12.2 port with focused files for lifecycle, events, passives, skill math, timed effects, mining/Ultmine tools, environment effects, abilities, player/storage helpers, command, self-test, client hooks, client input, HUD, world render, UI screens, and client config. The target now also has an original-style `api/AbstractSkill`, `api/SkillRegistry`, and `impl/*Skill` strategy layer so each skill owns its active ability, passives, tick behavior, and event hooks. Client parity was expanded with XP toasts, notification cards, daily challenge cards, active-mode HUD cards, Paragon ability cards, UltPlace config, storage/trash/classic pickers, terminal amount dialogs, client state mirrors, first-time hints, achievements, and synergy/daily challenge services.
  - Remaining targets to check/port: 1.12.2 Legacy Fabric and any other maintained legacy-native loader target if they later receive the same native/full-client treatment.
  - Validation note: Forge 1.12.2 was built and runtime-validated on 2026-05-19 with server/UI self-tests passing. The note remains open only for deciding whether to mirror the native split into other legacy targets.

- [ ] Port the 1.20.1 Fabric Skills UI polish to the rest of the supported targets.
  - Source target changed now: `.codex-temp/native-fabric/1.20.1-fabric/src/client/java/com/murilloskills/gui/SkillsScreen.java`
  - Changes: Miner Filter and Ultmine Config buttons use smaller item icons instead of text labels, sit directly to the left of the reset button away from the perk markers, and expanded skill tooltip lines no longer add manual leading spaces.
  - Remaining targets to check/port: 1.21.10 Fabric, 1.21.1 Fabric, 1.19.2 Fabric, 1.18.2 Fabric, 1.16.5 Fabric, plus Forge/NeoForge/Legacy targets if those UI sources are maintained separately.
