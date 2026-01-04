# Changelog - MurilloSkills Mod

Todas as mudanças importantes do mod serão documentadas aqui.

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
