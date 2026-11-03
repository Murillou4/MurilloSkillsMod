# MurilloSkills 2.2.6 / Fabric 26.2

Entrega de 03/11/2026. Radial Ultmine recomposto estruturalmente com seis cartões flutuantes ao redor de um núcleo compacto, no mesmo SkillsTheme/dourado das telas de profissões e ferramentas.

O usuário aprovou visualmente o novo radial em 03/11/2026 e autorizou a publicação. Esse retorno não substitui as verificações automatizadas ou de gameplay listadas abaixo.

## Implementação
- Aro, divisórias scanline e faixa de largura total removidos.
- Ícone, nome e medidas agrupados por opção; escolha salva marcada em dourado, direção apontada com destaque suave.
- Núcleo com forma apontada, capacidade recebida do servidor e acesso a Ajustes; legenda local de apontar/soltar, roda e Esc.
- Seleção angular também entre cartões; hitbox do cartão tem prioridade. Núcleo e legenda são neutros para cancelar ao soltar.
- Clique nativo, foco por teclado e seleção por apontamento usam o mesmo estado. A última posição do mouse é lida no evento de soltar.
- Prévia branca, números, formas, limites, consumo, dados e atalhos preservados.

## Resultado
| Área | Resultado |
|---|---|
| Java25 / compileJava / compileClientJava / empacotamento | PASS |
| Revisão de fonte: layout, textos, foco, eventos e sobreposições | Concluída por inspeção; não equivale a exercício em jogo |
| Cópias build/libs, dist/26.2/fabric e PRONTO_26.2 | PASS; hashes idênticos |
| Conteúdo do JAR, sem fixtures/runtime externo | PASS |
| Testes JVM | NOT_RUN; excluídos com -x test |
| Cliente, mundo integrado/dedicado, gameplay e UI GUI2/3 | NOT_RUN; usuário fará a avaliação no jogo |
| Capturas do novo radial em Minecraft | NOT_RUN; cliente não foi aberto pelo agente |
| Outros alvos/loaders e CI remota | NOT_RUN; fora do escopo |

Comando em targets/26.2/fabric: gradlew.bat --no-daemon --console=plain build -x test, Java 25.

As evidências PASS da 2.2.5 pertencem ao histórico e não validam este radial. Nenhum cliente/servidor Minecraft foi aberto nesta entrega.

## Evidências e entrega
- [Build final](artifacts/radial-20261103-154322/build.log)
- [Resumo](artifacts/radial-20261103-154322/summary.json)
- murilloskills-26.2-fabric-2.2.6.jar
- SHA256: fb77d4447658fad64d82a3583264ea3adc97c950b7f688432709957947cd0d05

Versões anteriores preservadas em dist/26.2/fabric/previous. Teste manual pelo usuário via TESTAR_26.2.cmd ou instalação do novo JAR com Fabric API.
