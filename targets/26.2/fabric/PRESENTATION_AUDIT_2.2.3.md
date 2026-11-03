# Revisão corretiva adicional / 2.2.3

Escopo: Fabric 26.2; revisão de descrições, valores exibidos e condições já existentes.

## Corrigido
- A trilha calculava os marcos com a ativa desligada. Agora também mostra os valores reais durante a habilidade: dano de Fúria/Foco, mineração, capacidade e sorte, quando houver mudança.
- Benefícios longos de marcos eram limitados a duas linhas e podiam depender de tooltip truncado. A trilha mede e mostra o texto completo com rolagem.
- Linha de colocação, Coluna e Túnel já aplicavam o espaçamento salvo, mas escondiam seu controle. O painel agora permite conferir/corrigir esse valor.
- Queda lenta passa a descrever exatamente Y>=100; a redução de fogo existente do Explorador é informada com o mesmo multiplicador da implementação.
- Efeitos próprios podiam persistir depois de sair da água/lava ou soltar o agachamento. A limpeza agora identifica a origem profissão+utilidade. Outras origens e poções vanilla continuam preservadas.

## Verificação
tools/validate-26.2.ps1: build Java25, 28 testes JVM, mundo integrado, reabertura, DedicatedServer local, UI GUI2/3 PASS. 912 verificações de gameplay/layout e 43 capturas.

Os testes nativos conferem visão compartilhada Minerador/Pescador, respiração compartilhada, remoção por condição, queda lenta e preservação de poção vanilla. Um clique real no controle de espaçamento foi confirmado pelo servidor.

76 valores/requisitos de referência da 2.2.2 continuam iguais; teste conhecido confere os marcos 20/40 da Fúria, Foco e sorte. Magnitudes, chances, custos, recargas e curva de XP preservados. Remover uma utilidade ao terminar sua condição é correção de aplicação.

Relatório: artifacts/general-20261103-142915/index.html.
Comparação: artifacts/general-20261103-142915/compatibility-2.2.2.json.
Referência numérica no nível100/ramoA/sem ativa, sem alegar todas as builds possíveis.
