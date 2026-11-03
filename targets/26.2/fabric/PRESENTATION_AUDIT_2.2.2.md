# Revisão corretiva de apresentação / 2.2.2

Escopo: Fabric 26.2, textos e números das profissões, ativas, ramos, ferramentas, HUD, ajuda e mensagens. Sem novos recursos nem mudança da curva de XP.

## Divergências corrigidas
- Ultmine radial usava toolLimit no cliente e ignorava a ativa/limite configurado. Agora lê o valor avaliado e sincronizado pelo servidor.
- UltPlace podia usar até o limite configurado, enquanto Pincel mantinha teto próprio de 256. A apresentação agora distingue os dois; o cálculo real de cada ferramenta permanece igual.
- Custos de ramo/profissão/Paragon, máximo e bônus de prestígio, limite de dano durante ativa, sinergias e selos de contratos deixam de repetir números nos textos.
- Multiplicadores mantêm até quatro casas para não mostrar um aumento real como dois valores arredondados iguais. XP pequeno/fracionário mantém casas decimais.
- Sorte adicional da ativa usa um único sinal positivo. Perfuração usa alvos; desconto de encantamento usa níveis; bloco/item no singular.
- Ativa bloqueada apresenta a referência real do marco 20, em vez de duração/recarga do patamar zero.
- Pulso, Surto, Busca e cura ao iniciar Fúria são classificados como efeitos da ativa. Respiração e recuperação do Explorador informam condições de água/combate.
- Prospecção melhora alcance do Pulso, não o radar sonoro fixo. Plantio usa a pilha na mão; queda combina com Explorador em ambos os ramos; Busca encontra os contêineres elegíveis carregados; reflexão descreve seu limite real.
- Radial de habilidades diz que o clique equipa; ramos bloqueados mostram requisito; Paragon já escolhida não oferece uma troca redundante.
- Ajuda e rodapé leem as teclas atuais; comando de orientação aponta para Controles. Versão de menu, log e catálogo vem dos metadados do mod.

## Fonte dos números
Regras de gameplay continuam explícitas em suas implementações e serviços. Valores fixos de layout e regras deliberadas não foram removidos indiscriminadamente. O alvo da correção foi eliminar duplicação que podia fazer o texto divergir do servidor.

## Evidências
Revisão final: artifacts/general-20261103-140340/index.html.
27 testes JVM; 869 verificações de gameplay/layout; 42 capturas; mundo integrado, reabertura e DedicatedServer local PASS.

75 valores/requisitos de referência da 2.1 preservados; uma descrição adicional expõe o limite já existente do Pincel. Comparação no nível100/ramo A/sem ativa, não todas as builds. Testes adicionais conferem formatação, os limites separados com configuração customizada e o radial durante ativa com teto de 17 blocos.

Atualizar cliente e servidor juntos. Outras versões/loaders não foram alterados ou validados.
