# Changelog - MurilloSkills Mod

Todas as mudanças importantes do mod serão documentadas aqui.

---

## [1.2.10] - 2026-02-26

### 🛠️ Ultmine / Vein Miner

- **Modo legado restaurado no radial**: adicionado shape `Legacy` (modo antigo) junto dos demais formatos do Ultmine.
- **Execução e preview do Legacy corrigidos**: `Legacy` agora usa novamente a lógica clássica de blocos conectados.
- **Limite do Legacy ajustado**: aumento leve de limite (+25%), respeitando o teto global de `ultmine.maxBlocksPerUse`.
- **Seleção de shape corrigida**: removido comportamento que forçava voltar para `3x3` ao interagir com o radial.
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
