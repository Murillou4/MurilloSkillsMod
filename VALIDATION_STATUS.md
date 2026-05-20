# MurilloSkills Validation Status

Atualizado em 2026-05-19.

## Forge 1.12.2 runtime/UI/skills

Rodada executada com JDK 8 portatil em `.codex-temp/jdks/jdk8/jdk8u492-b09` e Gradle 4.9:

```powershell
.\.codex-temp\smoke\mdk-1122\gradlew.bat --no-daemon --console=plain -p targets/1.12.2/forge clean build
$env:MURILLOSKILLS_SELFTEST='1'; $env:MURILLOSKILLS_AUTOWORLD='1'; $env:MURILLOSKILLS_CLIENT_UI_SELFTEST='1'; $env:MURILLOSKILLS_CLIENT_UI_SELFTEST_EXIT='1'; .\.codex-temp\smoke\mdk-1122\gradlew.bat --no-daemon --console=plain -p targets/1.12.2/forge runClient
```

| Versao | Loader | Status |
| --- | --- | --- |
| 1.12.2 | Forge 14.23.5.2859 | PASS |

Evidencias:

- Build: `targets/1.12.2/forge/build/libs/murilloskills-1.12.2-forge-1.2.75.jar`
- Dist: `dist/1.12.2/forge/murilloskills-1.12.2-forge-1.2.75.jar`
- Server self-test: `targets/1.12.2/forge/run-clean/saves/CodexSmoke112/murilloskills/selftest-result.txt` = `PASS`
- UI screenshots: `targets/1.12.2/forge/run-clean/screenshots/murilloskills-112-skills.png`, `murilloskills-112-ore_filter.png`, `murilloskills-112-ultmine_config.png`, `murilloskills-112-trash_picker.png`, `murilloskills-112-classic_picker.png`, `murilloskills-112-ultmine_radial.png`, `murilloskills-112-controls.png`, `murilloskills-112-ultmine_preview_world.png`
- Runtime log evidence: `ClientUiSelfTest PASS` and client closed automatically.

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

NeoForge e Legacy Fabric nao foram revalidados nesta rodada. Eles precisam de smoke proprio por loader e nao devem herdar resultado Fabric/Forge.
