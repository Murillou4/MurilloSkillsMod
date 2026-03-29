# Changelog - MurilloSkills Mod

Todas as mudanças importantes do mod serão documentadas aqui.

---

## [1.2.21] - 2026-03-29

### ⚡ Velocidade do Explorador

- **Toggle de Speed do Pathfinder (Nível 45)**: adicionado toggle (tecla B) para ativar/desativar o bônus de velocidade ao correr do Explorador. O efeito agora usa um attribute modifier customizado ao invés do Speed do Minecraft, eliminando o aumento de FOV indesejado. Equivalente a Speed II (+40%) enquanto corre, sem partículas e sem distorção visual.

---

## [1.2.20] - 2026-03-29

### 🐛 Bugfix

- **Farmer XP com cana de açúcar**: cana de açúcar agora é reconhecida como crop do Farmer, dando XP ao quebrar (2 XP base, configurável). Todas as passivas de colheita (colheita dupla, colheita dourada, sementes extras, colheita adjacente) agora se aplicam à cana.

---

## [1.2.19] - 2026-03-28

### 🐛 Bugfix

- **Ultmine agora aplica skills por bloco**: corrigido o fluxo do `Ultmine` para reutilizar os mesmos handlers da quebra manual em cada bloco adicional quebrado.
- **Farmer com passivas completas no Ultmine**: colheitas com `Ultmine` agora aplicam corretamente XP, streak, daily challenge, achievements e passivas de harvest do Farmer bloco por bloco, ficando consistente com quebrar um por um.
- **Miner com processamento consistente por bloco**: minérios quebrados pelo `Ultmine` agora passam pelo mesmo caminho individual do `BlockBreakHandler`, preservando o comportamento esperado por bloco e evitando diferenças entre quebra manual e quebra em área.
- **Fortune da picareta + Fortune do Miner mantidas no loot individual**: a quebra em área continua gerando os drops bloco por bloco, sem agregar o cálculo final em lote.

---

## [1.2.18] - 2026-03-28

### 🐛 Bugfix

- **Farmer XP com Ultmine**: corrigido bug onde o Farmer não ganhava XP ao quebrar crops, pumpkins, melons e outros itens de farm usando o Ultmine. O `VeinMinerHandler` agora concede XP de Farmer para blocos de crop quebrados, com diminishing returns e cap de 500 XP por uso (consistente com o Miner). Também rastreia progresso de daily challenges (`HARVEST_CROPS`) e achievement (`Mega Farmer`).

---

## [1.2.17] - 2026-03-23

### ⚔️ Warrior - Looting Passivo

- **Looting por nível**: o Warrior agora ganha bônus de Looting passivo baseado no nível (+2% por nível, máximo +2 níveis de Looting no nível 100). Funciona como a Fortune do Miner — aplica-se automaticamente ao atacar mobs.
- Tooltip na tela de skills exibe o bônus atual de Looting.

### ⚖️ Rebalanceamento - 5 Classes Buffadas

Classes menos populares (Farmer, Fisher, Blacksmith, Builder, Explorer) receberam **melhorias substanciais** com novas habilidades passivas nos níveis 35 e 60, além de buffs nos valores existentes.

#### 🌾 Farmer
- **Nível 35 - Vitalidade Natural**: Regeneração I ao pisar em terra arada, grama ou musgo
- **Nível 60 - Mestre das Sementes**: Haste I permanente (interações mais rápidas)
- Buffs: colheita dupla +100%, cultivo dourado +166%, duração ativa +100%, cooldown -25%

#### 🎣 Fisher
- **Nível 35 - Benção do Oceano**: Visão Noturna quando submerso
- **Nível 60 - Fortuna do Mar**: Luck I permanente
- Buffs: velocidade de pesca +100%, Epic Bundle +200%, cooldown -33%, Rain Dance +50%

#### 🔨 Blacksmith
- **Nível 35 - Domínio do Fogo**: Fire Resistance permanente
- **Nível 60 - Aura de Reparo**: auto-reparo do item na mão (1 durabilidade a cada 10s)
- Buffs: thorns +50% chance, reflexo +40%, duração Titanium +66%, cooldown -25%

#### 🏗️ Builder
- **Nível 35 - Vigor do Construtor**: Haste I permanente (mineração e construção mais rápida)
- **Nível 60 - Construção Leve**: Slow Falling ao agachar em altura (≥100Y)
- Buffs: alcance por nível +60%, redução de queda +100%, economia decorativa +50%, cooldown -25%

#### 🧭 Explorer
- **Nível 45 - Desbravador**: Speed II ao correr (sprint)
- **Nível 55 - Recuperação Rápida**: Regeneração I quando vida abaixo de 50%
- Buffs: velocidade +100%, redução de fome +60%, redução de queda +50%, XP por distância +43%

### 🌐 Localização

- 31 novas chaves de tradução adicionadas em `en_us`, `pt_br` e `es_es` (nomes, descrições e passivas das novas habilidades)

### 📖 UI/Guias

- Tooltips atualizados na tela de skills para todas as 5 classes
- Guia de progressão (nível 1-100) atualizada com as novas habilidades
- SKILL_PERKS e getMaxPassiveGuide atualizados para refletir os novos marcos

---

## [1.2.16] - 2026-03-21

### ⛏️ Ultmine - Configuração e Variantes

- **Tela de configuração do Ultmine**: nova tela acessível pelo botão ⛏ no card do Miner, permitindo configurar:
  - **Drops to Inventory**: toggle para enviar itens direto ao inventário
  - **Same Block Only**: toggle para minerar apenas blocos do mesmo tipo
  - **Depth/Length por shape**: ajuste individual de profundidade e comprimento para cada formato
  - **Variantes por shape**: seleção de variante (ex: escada subindo/descendo, 20x20 horizontal/vertical)
- **Variantes de shapes**: Stairs (subir/descer), Square 20x20 (horizontal/vertical N-S/vertical E-W), 2x1 (largo/alto)
- **Config persistente**: preferências salvas em `config/murilloskills_ultmine.json` no lado do cliente
- **Durabilidade da ferramenta corrigida**: ferramentas agora perdem durabilidade corretamente ao usar Ultmine e Vein Miner. Se a ferramenta quebrar no meio do shape, a mineração para automaticamente
- **Outline em líquidos corrigido**: o preview do Ultmine não destaca mais blocos de água/lava
- **Mensagem de "blocos inválidos" removida**: eliminado o spam de mensagem quando o Ultmine não encontrava blocos válidos

### ✨ XP Direto ao Jogador

- **Novo toggle "XP Direct"**: na tela de config do Ultmine, permite que os orbes de XP (de carvão, diamante, etc.) ao minerar com Ultmine/Vein Miner sejam concedidos diretamente ao jogador em vez de cair no chão
- Funciona tanto no Ultmine quanto no Vein Miner legado
- Sincronizado via rede — toggle na interface envia preferência ao servidor

### ⚙️ Limite de Skills Selecionadas Configurável

- **`maxSelectedSkills`**: novo campo em `murilloskills.json` > `general` que define o número máximo de skills que o jogador pode selecionar (padrão: 3, mínimo: 1, máximo: 8)
- O limite é sincronizado do servidor para o cliente via payload, garantindo que tela de seleção e validações estejam sempre consistentes
- Comandos de admin (`/murilloskills select`, `/murilloskills paragon`) também respeitam o novo limite

```json
"general": {
  "maxSelectedSkills": 3
}
```

### 🎨 OreFilterScreen - Redesign

- **Redesign completo**: tela de filtro de minérios reescrita com o tema premium dark do mod (ColorPalette + RenderingHelper)
- **Cards de minério com cor**: cada minério tem uma barra lateral com sua cor característica
- **Botões de modo estilizados**: seleção de modo (X-Ray, Visível, Próximo) com indicador visual e descrição

### 🌐 Localização

- Traduções da tela de config do Ultmine em `pt_br`, `en_us` e `es_es`
- Traduções do filtro de minérios adicionadas em `es_es` (estavam faltando)
- 8 chaves de variantes de shapes em 3 idiomas

---

## [1.2.15] - 2026-03-11

### 🐛 Fisher / Miner (Skyblock)

- **Tooltip da vara de pesca corrigido**: o Fisher agora mostra na Fishing Rod os bônus reais da skill, incluindo velocidade de pesca, redução de espera, Luck of the Sea total e bônus de tesouro/XP, junto dos encantamentos aplicados.
- **Miner funcionando em skyblock**: `cobblestone` e `cobbled_deepslate` agora também concedem XP de Miner, corrigindo mundos com geradores de pedra onde a skill parecia não evoluir.
- **Diagnóstico do save confirmado**: o problema não era corrupção de dados do player; as skills selecionadas e o level do Fisher estavam corretos no mundo analisado.

---

## [1.2.14] - 2026-03-11

### ✨ Guide / Tooltips / Builder / Ultmine

- **GUIDE expandida**: a antiga aba de perks virou uma GUIDE completa com visão geral da skill, motivo para escolher, como upar, habilidade mestre, snapshot do level 100 e progressão detalhada do nível 1 ao 100 para cada skill.
- **Tooltips com valores finais reais**: ferramentas e armas agora mostram os atributos já somados com bônus de skill e encantamentos. O Arqueiro também passou a exibir dano estimado à distância e bônus aplicados.
- **Exploit de XP do Builder corrigido**: XP de construção agora só é concedido após uma colocação de bloco realmente bem-sucedida, eliminando ganho indevido ao clicar com botão direito sem posicionar o bloco.
- **Ultmine reforçado**: seleção, preview e execução ficaram mais robustos, com validações melhores e configuração por shape para profundidade/comprimento e comportamento padrão.
- **Localização atualizada**: textos novos da GUIDE e dos tooltips adicionados em `pt_br`, `en_us` e `es_es`.

---
## [1.2.13] - 2026-02-27

### 🐛 Ultmine / Vein Miner

- **Ultmine desbloqueado em servidores dedicados**: corrigido default de `requireMinerMaster` que vinha como `true`, bloqueando silenciosamente todos os jogadores que não tinham Miner nível 100. Agora o default é `false`, permitindo todos os jogadores usarem shapes e preview sem restrição de classe ou nível.

> **Nota:** ao atualizar, delete o `config/murilloskills.json` do servidor para regenerar com o novo default.

---

## [1.2.12] - 2026-02-26

### 🛠️ Ultmine / Vein Miner

- **Outline fantasma corrigido**: o preview agora descarta posições stale/no ar e limpa preview inválido ao trocar de alvo, evitando shape invisível no ar.
- **Sincronização do radial reforçada**: mudança de shape por scroll já sincroniza com o servidor, reduzindo divergência entre seleção e quebra real.
- **Linha reta estabilizada**: `LINE` em mineração horizontal passa a manter direção horizontal (sem drift vertical), cavando sequência consistente de blocos.
- **Escadaria restaurada para cima**: `STAIRS` volta a subir por degraus progressivos (com profundidade maior), mantendo o estilo clássico do shape.
- **Defaults ampliados para escavação longa**: `lineLengthDefault` aumentado para `12` e `stairsDepthDefault` para `16`.

---

## [1.2.11] - 2026-02-26

### 🛠️ Ultmine / Vein Miner

- **Preview de shape corrigido**: o outline do Ultmine agora marca todos os blocos do shape atual (não apenas o bloco mirado).
- **Direção de preview alinhada**: o preview usa a mesma direção da quebra real, evitando divergência entre contorno e resultado final.
- **Coleta do bloco inicial reforçada**: com drops para inventário ativo, o primeiro bloco quebrado também é coletado de forma confiável.
- **Coleta atrasada de drops adicionada**: sweep por alguns ticks após a quebra para capturar itens que spawnam ligeiramente depois do evento inicial.

---

## [1.2.10] - 2026-02-26

### 🛠️ Ultmine / Vein Miner

- **Modo legado restaurado no radial**: adicionado shape `Legacy` (modo antigo) junto dos demais formatos do Ultmine.
- **Execução e preview do Legacy corrigidos**: `Legacy` agora usa novamente a lógica clássica de blocos conectados.
- **Limite do Legacy ajustado**: aumento leve de limite (+25%), respeitando o teto global de `ultmine.maxBlocksPerUse`.
- **Seleção de shape corrigida**: removido comportamento que forçava voltar para `3x3` ao interagir com o radial.
- **Drop do bloco inicial corrigido**: no modo com drops para inventário, o primeiro bloco quebrado também é coletado corretamente.
- **Ordem de shapes estabilizada**: shape `Legacy` mantido no radial sem quebrar compatibilidade de seleção entre cliente/servidor.
- **Outline do preview corrigido**: removidas linhas duplicadas e adicionado fallback visual para manter contorno visível enquanto o preview sincroniza.
- **Localização atualizada**: nova tradução de shape legado em `pt_br`, `en_us` e `es_es`.

---

## [1.2.9] - 2026-02-26

### ✨ Ultmine (novo modo de mineração em área)

- **Menu radial de shapes**: nova UI no cliente para escolher formato de mineração (`'` por padrão), com seleção por mouse/scroll e confirmação ao soltar a tecla.
- **Novos formatos suportados**: `3x3`, `2x1`, `Line`, `Stairs` e `Square 20x20 (d1)`.
- **Preview validado no servidor**: o cliente envia requisições periódicas e renderiza apenas blocos aprovados pelo servidor.
- **Execução segura no servidor**: mineração em área respeita permissões, whitelist/blacklist, ferramenta válida, proteção de bloco e limite máximo por uso.
- **Feedback de resultado**: mensagens de sucesso/erro no action bar (cooldown, limite excedido, XP insuficiente, sem blocos válidos, etc).

### ⚙️ Configuração

Novo bloco no `murilloskills.json`:
```json
"ultmine": {
  "enabled": true,
  "maxBlocksPerUse": 500,
  "permissionLevel": 0,
  "requireMinerMaster": true,
  "cooldownTicks": 0,
  "lineLengthDefault": 5,
  "stairsDepthDefault": 5,
  "previewRequestIntervalTicks": 4,
  "xpCostPerUse": 0,
  "blockWhitelist": [],
  "blockBlacklist": ["minecraft:bedrock", "..."],
  "costs": {
    "shape3x3": 5,
    "shape2x1": 2,
    "lineCostPerBlock": 1,
    "stairs": 10,
    "square20x20d1": 50
  }
}
```

### 🌐 Localização

- Novas traduções de Ultmine adicionadas em `pt_br`, `en_us` e `es_es` (keybind, nomes de shape e mensagens de resultado).

### 🧪 Testes

- **JUnit 5 habilitado no Gradle** (`useJUnitPlatform`).
- **Testes do `UltmineShapeCalculator` adicionados** cobrindo plano 3x3, linha diagonal, escadaria e quadrado 20x20.

---

## [1.2.8] - 2026-02-20

### 🐛 Correções

- **Builder: Shulker Boxes agora podem ser colocadas**: Corrigido crash (NPE) ao colocar blocos com solidez dinâmica (como Shulker Boxes). Adicionado reconhecimento de Shulker Boxes como blocos decorativos para dar XP ao Builder. Adicionado try-catch no handler de placement para que erros nunca impeçam a colocação de blocos.

- **Archer: Flechas com piercing não compoundam mais o dano**: Flechas que atravessavam múltiplas entidades tinham o multiplicador de dano aplicado repetidamente (base → base×1.3 → base×1.3×1.3). Agora o dano original é armazenado e sempre usado como base para o cálculo.

- **Archer: Penetração de armadura implementada**: Antes, o nível 50 do Archer só dava piercing (atravessar entidades). Agora também ignora uma porcentagem da armadura do alvo (15% no nível 50, escalando até 30% no nível 100). Configurável via `armorPenetrationPercent` no `murilloskills.json`.

- **Builder: Removida mensagem falsa de "inventário cheio"**: A sinergia Master Crafter (Builder + Blacksmith) exibia uma mensagem de inventário cheio mesmo quando o item era dropado com sucesso. Agora sempre mostra a mensagem de sucesso, independente de o item ir para o inventário ou ser dropado.

### ⚔️ Balanceamento

- **Warrior: Dano por nível aumentado de 0.05 para 0.20**: O dano adicional do Warrior era muito baixo (+5 no nível 100). Agora é +20 no nível 100, resultando em 28 de dano total com espada de Netherite. Escalável com multiplicador de prestígio.

### ⚙️ Configuração

Novo campo no `murilloskills.json`:
```json
"archer": {
  "armorPenetrationPercent": 0.30  // % de armadura ignorada no nível 100 (escala a partir do nível 50)
}
```

Valor alterado:
```json
"warrior": {
  "damagePerLevel": 0.20  // Era 0.05, agora 0.20
}
```

---

## [1.2.7] - 2026-02-03

### ⛏️ Vein Miner - Correções e Melhorias

- **Tecla para alternar drops no inventário**: nova keybind `,` (vírgula) para alternar entre drops no inventário e drops no chão, configurável em Controles > MurilloSkills. Preferência é por jogador.
- **Default alterado**: `dropsToInventory` agora é `true` por padrão (itens vão direto para o inventário).
- **Redstone Ore corrigido**: blocos lit/unlit agora são tratados corretamente como o mesmo tipo no BFS.
- **Equivalência de deepslate**: minérios deepslate agora conectam com suas variantes normais (ex: `deepslate_iron_ore` + `iron_ore` são minerados juntos).
- **Glowstone e outros blocos**: removida qualquer restrição implícita — o Vein Miner funciona com qualquer bloco quebrável.
- **Algoritmo BFS corrigido**: blocos agora são marcados como visitados no momento da descoberta (eager), eliminando duplicatas na fila e garantindo limite consistente de `maxBlocks`.

---

## [1.2.6] - 2026-01-12

### ⛏️ Vein Miner

- **Drops para inventário**: nova opção `dropsToInventory` no `murilloskills.json` para enviar itens quebrados direto ao inventário do jogador (com fallback para dropar no chão se não couber).

---

## [1.2.4 - 1.2.5] - 2026-01-10

### ⛏️ Vein Miner

- **Vein Miner global**: adicionada quebra em lote de blocos conectados do mesmo tipo, com limite configurável via `murilloskills.json`.
- **XP do Minerador e drops consistentes**: a mineração em sequência aplica XP por bloco e respeita a ferramenta/fortuna usada.

---

## [1.2.3] - 2026-01-08

### 🎯 Desafios Diários - Melhorias

- **Reset por tempo de jogo**: Os desafios agora resetam a cada **1 dia do Minecraft** (~20 minutos reais) em vez de a cada dia real. Jogue no seu ritmo!
  
- **XP Aumentado**: 
  - Recompensa base por desafio: 500 → **800 XP**
  - Bônus por completar todos: 1000 → **1500 XP**

- **Dificuldade Adaptativa**: As metas dos desafios agora escalam com o seu nível médio de skills. Jogadores iniciantes terão metas mais fáceis, enquanto veteranos terão desafios mais intensos.

- **Notificação de Novos Desafios**: Ao resetar, você receberá a mensagem "🎯 Novos desafios disponíveis!" para saber que há novos objetivos.

- **Timer de Reset**: O cliente agora recebe informação do tempo restante até o próximo reset (base para futuro timer na GUI).

### ⚙️ Configuração

Novos campos no `murilloskills.json`:
```json
"dailyChallenges": {
  "resetIntervalTicks": 24000,      // Intervalo de reset em ticks (24000 = 1 dia MC)
  "baseXpReward": 800,              // XP por desafio completado
  "bonusXpAllComplete": 1500,       // XP bônus por completar todos
  "difficultyScalingEnabled": true, // Escalar dificuldade por nível
  "minTargetMultiplier": 0.5,       // Multiplicador mínimo (nível 0)
  "maxTargetMultiplier": 2.0        // Multiplicador máximo (nível 100)
}
```

---

## [1.2.2] - 2026-01-04

### 🐛 Correções
- **Corrigido valores de XP para testes**: Valores de XP para mineração de pedra (Miner) e colocação de blocos estruturais (Builder) foram restaurados para os valores padrão. Eles estavam incorretamente definidos como 150.000 XP (valor de teste) ao invés dos valores corretos:
  - Miner (Pedra/Deepslate): 150.000 → **1 XP**
  - Builder (Blocos Estruturais): 150.000 → **15 XP**

### 📚 Documentação
- Adicionado arquivo `DEFAULT_XP_VALUES.txt` com referência completa de todos os valores padrão de XP para cada skill.

---

## [1.0.0] - 2025-12-14

### ✨ Funcionalidades
- **8 Skills completas**: Miner, Warrior, Archer, Farmer, Fisher, Blacksmith, Builder e Explorer
- **Sistema de Level**: Até nível 100 com progressão baseada em XP
- **Sistema de Prestígio**: Até 10 níveis de prestígio por skill com bônus permanentes
- **Habilidades Ativas**: Cada skill possui uma habilidade ativa no nível 100
- **Passivas Desbloqueáveis**: Novas habilidades são desbloqueadas em marcos (10, 25, 50, 75, 100)
- **Sinergias**: Combinações de 2 skills ativas concedem bônus especiais
- **Desafios Diários**: 3 desafios por dia com recompensas de XP
- **Sistema de Streak**: Bônus de XP por ações consecutivas
- **Tela de Skills**: Interface gráfica completa para visualizar progresso
- **Tela de Informações**: Detalhes sobre mecânicas, sinergias e perks
- **Notificações Toast**: Feedback visual de ganho de XP
- **Configuração Externa**: Todos os valores são configuráveis via `murilloskills.json`
- **Suporte a Idiomas**: Português (BR) e Inglês (US)

### 🎮 Skills Disponíveis

| Skill | Ações que dão XP |
|-------|------------------|
| ⛏️ Miner | Minerar blocos e minérios |
| ⚔️ Warrior | Matar mobs hostis |
| 🏹 Archer | Acertar/matar com flechas |
| 🌾 Farmer | Colher plantações maduras |
| 🎣 Fisher | Pescar (peixes, tesouros, lixo) |
| 🔨 Blacksmith | Usar bigorna, mesa de encantamento, fornalha |
| 🏗️ Builder | Colocar blocos de construção |
| 🧭 Explorer | Explorar biomas, estruturas, distância percorrida |

---

## Notas de Uso

### Arquivo de Configuração
O arquivo `config/murilloskills.json` é gerado automaticamente na primeira execução. Todos os valores de XP, cooldowns e bônus podem ser personalizados por lá.

### Resetar para Padrões
Para restaurar os valores padrão, basta deletar o arquivo `murilloskills.json` e reiniciar o jogo.
