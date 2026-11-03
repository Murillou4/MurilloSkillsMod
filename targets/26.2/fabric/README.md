# MurilloSkills 2.2.6 · Fabric 26.2

Oito profissões, quatro escolhas e uma jornada longa de domínio. Alvo nativo próprio; as versões 1.x permanecem preservadas.

## Jogar
Download: [MurilloSkills 2.2.6 para Fabric 26.2](../../../dist/26.2/fabric/murilloskills-26.2-fabric-2.2.6.jar). A pasta `previous` preserva os JARs das etapas anteriores.

Minecraft **26.2**, Java **25**, Fabric Loader **0.20.1+** e Fabric API **0.161.0+26.2**, no cliente e no servidor. Nenhuma integração externa ou biblioteca de configuração é necessária.

Após a revisão geral, o pacote local fica em `PRONTO_26.2` na raiz. Instale o mod e a API dessa pasta em `mods`. `TESTAR_26.2.cmd` abre o cliente isolado; seus mundos ficam em `targets/26.2/fabric/run/saves`.

| Tecla padrão | Ação |
|---|---|
| O | Painel de profissões |
| R | Ativar / encerrar habilidade equipada, sem devolver recarga |
| V | Selecionar ativa entre as quatro profissões |
| G | Ferramentas no mesmo painel |
| Ponto | Segurar Ultmine enquanto quebra o primeiro bloco |
| Apóstrofo | Segurar radial do Ultmine, apontar e soltar |
| B | UltPlace |
| U | Desfazer colocação em área ainda válida |

Teclas já salvas continuam valendo, inclusive acento grave. Novos atalhos de utilidades e quatro ativas diretas começam **sem tecla**. Defina-os em **O → profissão → Controles** ou nas opções do Minecraft; Esc remove a associação durante a captura.

Ultmine/UltPlace liberam no 15. Ultmine oferece **segurar** ou **alternar**; a primeira quebra continua vanilla. UltPlace oferece execução pontual ou modo contínuo armado, usado com clique direito. Configure esses modos nos Controles. O HUD indica quando as ferramentas estão armadas.

A prévia do Ultmine continua branca semitransparente. O radial foi recomposto com seis cartões flutuantes, ícones e medidas alinhados, núcleo compacto e a mesma linguagem visual do painel de profissões: SkillsTheme, superfícies quentes e seleção dourada. O aro rasterizado e a faixa de largura total foram removidos. Aponte para uma opção ou sua direção e solte; a área central e a legenda são neutras para cancelar. Clique, navegação por teclado, ajustes pela roda do mouse e teclas remapeadas continuam disponíveis. Ajustes fica dentro do núcleo; a capacidade vem do servidor.

## Interface e efeitos

O card da habilidade equipada aparece **somente durante a ativa** por padrão. Em **O → Ajuda → Card da habilidade**, escolha **Só durante a ativa**, **Sempre** ou **Oculto**. A preferência fica salva neste cliente e não desliga os outros avisos. O controle geral do HUD continua em Apresentação.

Ao entrar pela primeira vez, o catálogo aguarda perfil e apresentação da mesma sessão. As oito profissões aparecem numa grade 4×2. Marque de uma a quatro, revise e confirme uma única vez. Você também pode escolher depois; completar posições livres é gratuito.

O abre as quatro posições do personagem, com nível, prestígio, XP, próximo marco e estado da ativa. Um clique entra nos detalhes; equipar e usar estão na própria posição. A última profissão, aba e rolagem são guardadas durante a sessão. Atualizações de XP não refazem a navegação.

Cada profissão reúne Resumo, Progressão, Especialização e Controles. O Resumo separa passivas, utilidades e efeitos da ativa. Progressão mostra os valores que mudam nos marcos; Especialização alinha diferenças dos dois ramos, nos patamares 25/50/75. Controles reúne funções e atalhos. Ferramentas oferece formatos em grade, dimensões, capacidade, filtros e ações. Contratos distingue pontos de atividade de quantidades de itens.

Os números vêm de definições executadas pelo servidor: valores, unidades, condições, limites, próximo nível e comparações. Potencial, efeito aplicado e composição com outras profissões são apresentados separadamente. A 2.2 preserva bônus, chances, durações, recargas, requisitos, limites e curva de XP da 2.1; corrige a aplicação de efeitos e sua comunicação. Não há uma categoria nova de ultimate.

Utilidades independentes: visão, tochas, radar sonoro, fusão, movimento, subida, plantio, bone meal, colheita, replantio, crescimento, cura no campo, fôlego, nado, reparo, fornalhas, reflexão, proteção contra fogo, vigor, escalada, queda lenta, recuperação e travessia do Nether. Desligar uma utilidade não remove as demais nem poções vanilla.

## Profissões
| Profissão | Recursos |
|---|---|
| Minerador | Ultmine, filtros, conservação, fortuna de recursos, fusão, pulso e radar sonoro |
| Guerreiro | Vida, dano, guarda, recuperação, roubo de vida e Fúria com velocidade/estabilidade |
| Arqueiro | Flechas velozes, dispersão reduzida, headshot, penetração e perfuração; Paragon com orientação durante Foco |
| Fazendeiro | Plantio, colheita, replantio e bone meal separados; crescimento, vitalidade, compostagem dupla |
| Pescador | Espera reduzida, sorte, captura extra, conservação da vara, utilidades aquáticas; Dança da chuva localizada |
| Ferreiro | Fornalhas vanilla, reparo, bigorna, encantamento e sobre-encantamento; proteção, reflexão e Aura |
| Construtor | UltPlace, formas, alcance, economia, fabricação decorativa, vigor, escalada e queda lenta; Pincel por dois cantos |
| Explorador | Deslocamento, subida, fôlego, orientação, descobertas e Busca; recuperação fora de combate, magma e areia das almas |

**Pincel:** ative Construtor, clique com um bloco em duas superfícies para marcar cantos; confira prévia e materiais e confirme com UltPlace. Exige todos os materiais na mão. Limpe as marcações no painel de ferramentas. Desfazer funciona durante o próprio Pincel, fora de combate; só remove blocos ainda iguais aos colocados. Uma tentativa sem colocação preserva o desfazer anterior.

Pulso e Busca mostram andamento, filtros, resultados e direção do sinal mais próximo. Crescimento percorre toda a área anunciada, distribuindo trabalho entre ticks. Anzóis lançados recebem o estado atual da profissão/ativa. Headshot usa o ponto de impacto; avisos de cura, reparo, bigorna e encantamento informam resultados efetivos. Sons de habilidade e avisos visuais podem ser desligados em Ajuda.

## Jornada
- Até quatro profissões; as quatro chegam a 100. Uma pode ser Paragon.
- Bônus por nível; 160 marcos, 16 ramos, oito ativas com cinco patamares.
- Ativa no 20; ramo no 25, melhorado no 50/75.
- Contratos persistentes: conclua os três e ganhe um selo. Sem prazo, limite diário de XP ou obrigação de login.
- Primeiras escolhas gratuitas. Ramo custa 1 selo para trocar; profissão ou Paragon, 3. Progresso preservado.
- Prestígio opcional até dez vezes: reinicia nível/XP; mantém ferramentas, ramos e patamares. +10% XP e +1% passivas por prestígio.
- Sinergias, conquistas e limites compartilhados preservam custos e perigos do survival.

## Dados e importação
Progressão: `<mundo>/murilloskills/v2/players/<UUID>.json`; proveniência: `murilloskills/v2/provenance.json`.
Configuração do servidor: `config/murilloskills-v2.json`; preferências locais: `config/murilloskills-client-v2.json`.

A atualização preserva dados e teclas 2.1. Dados 2.0 têm seus controles convertidos uma vez, com cópia `.pre-2.1.bak`. Níveis, XP, maior nível, prestígio, contratos, ramos e selos permanecem. Novas preferências têm padrões compatíveis e atalhos opcionais continuam sem tecla. O protocolo 2.2 exige atualizar cliente e servidor juntos.

Legado 1.x é opcional, antes de evoluir na v2: **Ajuda → Importar legado** ou `/murilloskills importlegacy`, depois `/murilloskills importlegacy confirm`. Prévia válida por 30 s, confirmação e backup. Preserva níveis, prestígio e fração de XP; novos ramos são escolhidos novamente. Sem selos retroativos ou aplicação do balanceamento antigo. Reconhece JSON, attachment Fabric 1.21.10 e cópia do NBT antigo em `murilloskills/legacy`. Os arquivos antigos não são alterados.

Operadores: `/murilloskills admin <jogador> level <skill> <0..100>`, `/murilloskills admin <jogador> seals <quantidade>`, `/murilloskills save`.

Para maximizar todas as profissões escolhidas: `/murilloskills admin maxselected` (em si) ou `/murilloskills admin <jogador> maxselected`. Exige permissão de administrador; ajusta nível/maior nível para 100 e XP para zero. Mantém prestígio, ramos, Paragon, contratos, selos e progresso das profissões inativas. Não escolhe ramos nem concede recompensas de primeira conclusão.

## Desenvolvimento
Java 25 e wrapper deste alvo:
```powershell
.\gradlew.bat --no-daemon --console=plain build
.\gradlew.bat runClient -PuseReleaseJar=true
.\gradlew.bat runServer -PuseReleaseJar=true
```

Revisão geral pela raiz: `tools/validate-26.2.ps1`. Compila, simula, executa o JAR real em mundo integrado e dedicado, reabre dados e salva evidências. Fixtures ficam fora do mod. Consulte [VALIDATION_STATUS.md](VALIDATION_STATUS.md).

A meta de 30–50 horas é uma estimativa de atividades. Não equivale a uma campanha humana medida. Partidas reais orientarão o balanceamento posterior.
