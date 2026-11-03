# Experiência 2.2 / Fabric 26.2

Este documento descreve o escopo implementado; o resultado da execução está em VALIDATION_STATUS.md.

## Interface
- Catálogo de oito profissões derivado de SkillType, independente do perfil; seleção em lote validada no servidor.
- Profile State e Summaries identificados por sessão e revisão. Abertura automática somente com conjunto coerente; catálogo exibe carregamento se aberto antes.
- Quatro posições, equipar/usar diretamente, detalhes por profissão, scroll com propriedade explícita dos widgets.
- Resumo separa passivas, utilidades e ativa. Progressão compara cada marco com o nível anterior, incluindo melhorias condicionais.
- Ramos usam pares de valores alinhados, nos patamares conquistados; prestígio usa o nível atual.
- Controles por função; formatos de ferramentas em grade, filtros próximos; contratos em pontos/unidades apropriados.

## Gameplay e feedback
- Minerador: Pulso mostra progresso/filtros/sinal; radar comunica alvo novo ou aproximação.
- Guerreiro: início/término da Fúria e limites compartilhados visíveis; cura somente quando recupera vida.
- Arqueiro: ponto de impacto nativo determina headshot; aviso desconta limite de dano já aplicado.
- Fazendeiro: iteração completa da área, processamento distribuído; cultivos afetados e usos reais.
- Pescador: sorte em anzóis existentes acompanha seleção, ramo, ativa e encerramento.
- Ferreiro: durabilidade reparada, equipamento ausente e níveis realmente consumidos.
- Construtor: dois pontos, prévia, materiais e confirmação; limpar; desfazer na ativa fora de combate e preservado em tentativa vazia.
- Explorador: Busca mostra progresso/direção; condições e leases de efeitos continuam sob controle do servidor.

## Compatibilidade
Gameplay e apresentação compartilham definições e composição. Os limites de float preservam exatamente a expressão da 2.1. Os testes de valores conhecidos, duração, recarga, área e curva detectam mudanças numéricas acidentais.

Não há novos talentos, categoria de ultimate, integração externa ou alteração automática de balanceamento. Dados persistentes 2.1, contratos, selos, ramos e atalhos continuam válidos. Estado visual é transitório. Atualização conjunta de cliente e servidor necessária.

Revisão concentrada no final: build/JVM, mundo integrado com abertura automática real, GUI 2/3, entrada física, efeitos, ferramentas, reabertura e conexão em dedicado. As fixtures não entram no JAR distribuído.
