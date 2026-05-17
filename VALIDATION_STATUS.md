# MurilloSkills Validation Status

Atualizado em 2026-05-17.

## Fabric runtime/UI/skills

Rodada final executada com:

```powershell
.\tools\validate-fabric-runtime.ps1 -Versions 1.21.10,1.21.1,1.20.1,1.19.2,1.18.2,1.16.5 -SkipBuild -PostWorldWaitSeconds 30
```

Todas as versoes abaixo abriram cliente real, entraram em mundo single-player, abriram o menu principal de skills e terminaram os self-tests de skill/runtime e UI com `PASS`.

| Versao | Loader | Status |
| --- | --- | --- |
| 1.21.10 | Fabric | PASS |
| 1.21.1 | Fabric | PASS |
| 1.20.1 | Fabric | PASS |
| 1.19.2 | Fabric | PASS |
| 1.18.2 | Fabric | PASS |
| 1.16.5 | Fabric | PASS |

## Builder max reach

Validado explicitamente em todas as versoes Fabric acima.

| Versao | Servidor | Cliente |
| --- | --- | --- |
| 1.21.10 | `passive.BUILDER.reach_runtime_range` PASS | `passive.BUILDER.client_reach value=18.5 bonus=14.0` PASS |
| 1.21.1 | `passive.BUILDER.reach_runtime_range` PASS | `passive.BUILDER.client_reach value=18.5 bonus=14.0` PASS |
| 1.20.1 | `passive.BUILDER.reach_server_distance` PASS | `passive.BUILDER.client_reach value=18.5 bonus=14.0` PASS |
| 1.19.2 | `passive.BUILDER.reach_server_distance` PASS | `passive.BUILDER.client_reach value=18.5 bonus=14.0` PASS |
| 1.18.2 | `passive.BUILDER.reach_server_distance` PASS | `passive.BUILDER.client_reach value=18.5 bonus=14.0` PASS |
| 1.16.5 | `passive.BUILDER.reach_server_distance` PASS | `passive.BUILDER.client_reach value=18.5 bonus=14.0` PASS |

## Evidence

Resumo JSON atual: `.codex-temp/runtime-validation-summary.json`

Cada entrada aponta para:

- `murilloskills-skill-selftest.log`
- `murilloskills-ui-selftest.log`
- screenshot do mundo carregado
- screenshot do menu de skills aberto

## Smoke automation

O smoke de Fabric agora usa OCR local do Windows para reconhecer botoes como `Singleplayer` antes de cair no fallback por pixels. A tela `Caution: Third-Party Online Play`, que aparecia quando algum clique caia no fluxo de Multiplayer, tambem e reconhecida e tratada para voltar ao menu em vez de travar a validacao.

## Fora desta rodada

Forge, NeoForge e Legacy Fabric nao foram revalidados nesta rodada. Eles precisam de smoke proprio por loader e nao devem herdar resultado Fabric.
