# Changelog

## 2.2.6
- Recompõe completamente o radial Ultmine: seis cartões flutuantes ao redor de um núcleo informativo, com o mesmo SkillsTheme/dourado das outras telas.
- Remove aro/divisórias rasterizados e rodapé de largura total; ícones, nomes, medidas e estado em uso ficam juntos. Destaque suave da direção apontada, separado da escolha salva.
- Área central e legenda neutras; clique nas opções e entre elas usa a mesma seleção radial de apontar/soltar. Foco por teclado não mantém outra opção iluminada durante o uso do mouse.
- Mantém prévia branca, capacidade recebida do servidor, atalhos, ajustes e gameplay. Build sem testes; runtime e inspeção visual no jogo ficam para o usuário nesta entrega.

## 2.2.5
- Prévia do Ultmine branca semitransparente: contorno com 50% de opacidade, largura de 1 px e preenchimento discreto. Demais marcações preservadas.
- Radial reorganizado com paleta neutra, setores mais compactos, centro legível, ícones distintos, seleção/hover claros e instruções separadas das medidas.
- Ícones de formatos compartilhados entre radial e ferramentas; capacidade continua recebida do servidor. Sem mudanças em números, formas, consumo ou atalhos.

## 2.2.4
- Card da profissão equipada deixa de ficar fixo por padrão: aparece só durante sua ativa.
- Opção própria no início de Ajuda: Só durante a ativa / Sempre / Oculto. Preferência local persistente, independente de XP, ferramentas, avisos e HUD geral.
- Configurações anteriores recebem o padrão contextual; preferências e teclas existentes preservadas. Alteração de apresentação no cliente, sem mudanças de gameplay ou protocolo.

## 2.2.3
- Marcos incluem os valores reais durante a ativa, além dos bônus comuns e duração/recarga; a trilha mostra todos os benefícios com rolagem, sem depender de tooltip cortado.
- Ajuste de espaçamento visível nas formas Linha de colocação, Coluna e Túnel, que já usavam essa preferência salva.
- Água, lava e agachamento deixam de manter efeitos próprios depois que a condição termina; preserva efeitos compartilhados e poções vanilla.
- Descrições de fogo do Explorador e altura de queda lenta corrigidas, com valores compartilhados com a implementação.
- Sem alterações na curva de XP, chances, magnitudes, custos ou recargas. Somente correções para Fabric 26.2.

## 2.2.2
- Revisão de textos e números: unidades corretas para alvos/níveis, multiplicadores com precisão suficiente, XP fracionário e sinal positivo único nos bônus da ativa.
- Radial de Ultmine lê a capacidade do servidor, incluindo ativa e limite configurado. UltPlace e Pincel descrevem seus limites próprios existentes separadamente.
- Ajuda e rodapé mostram teclas remapeadas; versão vem dos metadados. Custos, bônus de prestígio, limites compartilhados e recompensa de contratos usam as mesmas definições do gameplay.
- Efeitos exclusivos das ativas separados das passivas; condições aquáticas/combate e requisitos de ramo corrigidos na apresentação. Ativas bloqueadas mostram os valores futuros do marco 20.
- Descrições corrigidas para Prospecção, plantio, queda, Busca, reflexão e radial de equipar. Mecânicas, curva de XP e balanceamento preservados. Atualizar cliente e servidor juntos.

## 2.2.1
- Tooltips de seleção compactos, com valores essenciais, moldura da interface e posicionamento limitado à tela; tooltips das linhas também respeitam largura/altura disponíveis.
- Novo comando de administrador: `/murilloskills admin maxselected` para si ou `/murilloskills admin <jogador> maxselected` para um alvo. Ajusta somente profissões escolhidas ao nível 100; preserva prestígio, ramos, Paragon, contratos e selos.

## 2.2.0
- Seleção inicial sincronizada por sessão/revisão, catálogo completo 4×2 e confirmação atômica de uma a quatro profissões.
- Início com quatro posições e ações diretas; detalhes separados em componentes, navegação fixa independente da rolagem, memória de aba/posição durante a sessão.
- Passivas/utilidades/ativa separadas, marcos concretos, comparação alinhada de ramos, controles por função, formas em grade e contratos com unidades.
- Respostas estruturadas do servidor e andamento/resultados das varreduras, ferramentas e habilidades; sons e avisos configuráveis.
- Crescimento cobre as extremidades da área; anzóis atualizam sorte em uso; headshots usam impacto real; cura/reparo/custos informam resultados efetivos.
- Pincel mostra etapas e materiais, permite limpar pontos e desfazer fora de combate durante a ativa; tentativa sem colocação preserva o desfazer anterior.
- Sem alterações dos números de balanceamento, curva de XP, salvamentos ou teclas existentes. Apenas Fabric 26.2; atualizar cliente e servidor juntos.

## 2.1.0
- Painel único de profissões, com navegação persistente, rolagem e abas de resumo, progressão, ramos e controles.
- Definições numéricas compartilhadas por gameplay e resumos enviados pelo servidor; comparações de especialização e condições explícitas.
- Controles independentes e persistentes para utilidades, atalhos novos sem tecla, captura no painel e modos de segurar/alternar Ultmine e UltPlace contínuo.
- Encerramento antecipado da ativa sem reembolso de recarga e sincronização de todas as ativas em andamento.
- Entrada antes dos atalhos globais da 26.2: O abre/fecha o painel em vez da lista de amigos; captura de teclas preservada.
- Comparações após prestígio usam o nível atual e os patamares já conquistados; o próximo nível numérico é exibido corretamente.
- Recuperação de recursos próprios das oito profissões; consulte CORE_PARITY_2.1.md.
- Preserva progressão 2.0 e migra controles com backup; nenhuma integração externa adicionada.

## 2.0.1
- Radial próprio do Ultmine e sincronização da tecla antes da primeira quebra vanilla.
