# MurilloSkills 2.0 / Minecraft 26.2 Fabric

Este alvo substitui as premissas de gameplay da raiz apenas nesta pasta.
- Java 25, Loom sem remapeamento, nomes oficiais. Não usar runtime reflexivo 1.x.
- 8 profissões, até 4 escolhidas; todas chegam a 100; uma Paragon.
- Uma implementação por profissão em impl, registro EnumMap em api; eventos/mixins só adaptam chamadas.
- Progressão/marcos/ramos/contratos são definições compartilhadas; cliente é apenas cópia sincronizada.
- Dados v2 separados. Importação explícita com prévia, confirmação e backups; preservar originais.
- Não incluir integrações externas, dependências/testes de outras versões nem fixtures no JAR.
- Novas ações e custos são validados no servidor.
- O pedido atual é concluir a implementação antes de uma revisão geral, sem ciclos de testes a cada pequena edição.
- Comando de revisão: tools/validate-26.2.ps1 na raiz. Testes nativos somente com murilloskills.selftest=true e mundos descartáveis sob .codex-temp.
- Atualize VALIDATION_STATUS.md e VERSION_PORTING_TODO.md ao finalizar. Não afirmar validação de outras versões.
