# 🎮 MurilloSkills Mod

<div align="center">

**Um mod de habilidades e progressão para Minecraft — Fabric 1.21.10**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-≥0.16.0-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📖 Sobre o Projeto

**MurilloSkills** é um mod de RPG para Minecraft que adiciona um sistema completo de **8 habilidades** com progressão, perks passivos e habilidades ativas. Cada skill oferece uma experiência única, incentivando diferentes estilos de jogo — seja um agricultor mestre, um guerreiro implacável ou um explorador ousado!

---

## ✨ Funcionalidades Principais

### 🌾 8 Habilidades Únicas

| Skill | Tema | Habilidade Nível 100 |
|:---:|:---|:---|
| 🌾 **Farmer** | Agricultura e cultivos | **Harvest Moon** — Colheita automática em área |
| 🎣 **Fisher** | Pesca e domínio aquático | **Rain Dance** — Chuva mágica com buffs de pesca |
| ⛏️ **Miner** | Mineração e cavernas | **Master Miner** — Pulso que revela minérios |
| 🏗️ **Builder** | Construção e arquitetura | **Creative Brush** — Preenchimento de área |
| 🔨 **Blacksmith** | Forja e resistência | **Titanium Aura** — Imunidade temporária |
| 🧭 **Explorer** | Exploração e viagens | **Treasure Hunter** — Ver baús através de paredes |
| ⚔️ **Warrior** | Combate corpo a corpo | **Master Warrior** — Vida extra permanente |
| 🏹 **Archer** | Combate à distância | **Master Ranger** — Flechas perfurantes e rastreáveis |

### 📈 Sistema de Progressão

- **100 níveis** por habilidade
- **XP baseado em ações** — Ganhe XP fazendo o que você ama no jogo
- **Bônus por nível** — Melhorias graduais conforme você evolui
- **Perks nos níveis 10, 25, 50, 75 e 100** — Habilidades especiais desbloqueáveis
- **Habilidades ativas** — Pressione `F` para ativar poderes especiais no nível 100

### 🎯 Escolha Seu Caminho

- Selecione até **3 habilidades** para focar
- **Tela de habilidades responsiva** e visualmente rica
- Resetar habilidades e escolher novos caminhos
- Sincronização multiplayer completa

---

## 📋 Pré-requisitos

| Requisito | Versão |
|:---|:---|
| **Minecraft** | 1.21.10 |
| **Fabric Loader** | ≥ 0.16.0 |
| **Fabric API** | ≥ 0.138.3+1.21.10 |
| **Java** | 21 ou superior |

---

## 📥 Instalação

### 1. Instalar o Fabric Loader

1. Baixe o [Fabric Installer](https://fabricmc.net/use/installer/)
2. Execute o instalador e selecione a versão **1.21.10**
3. Clique em "Install"

### 2. Instalar o Fabric API

1. Baixe o [Fabric API](https://modrinth.com/mod/fabric-api) compatível com 1.21.10
2. Coloque o arquivo `.jar` na pasta `mods/`

### 3. Instalar o MurilloSkills

1. Baixe a última versão do mod nas [Releases](../../releases)
2. Coloque o arquivo `murilloskills-X.X.X.jar` na pasta `mods/`
3. Inicie o Minecraft com o perfil Fabric

> **📁 Localização da pasta mods:**
> - Windows: `%appdata%\.minecraft\mods\`
> - Linux: `~/.minecraft/mods/`
> - macOS: `~/Library/Application Support/minecraft/mods/`

---

## 🎮 Como Usar

### Abrindo o Menu de Habilidades

1. Pressione `K` para abrir a tela de habilidades (ou a tecla configurada)
2. Selecione até 3 habilidades para desenvolver
3. Confirme sua escolha

### Ganhando XP

Cada habilidade ganha XP de formas diferentes:

- **Farmer**: Colher cultivos, plantar, compostar
- **Fisher**: Pescar peixes, tesouros e itens
- **Miner**: Quebrar pedras e minérios
- **Builder**: Colocar blocos de construção
- **Blacksmith**: Usar bigorna, encantar, fundir
- **Explorer**: Descobrir biomas, estruturas, abrir baús
- **Warrior**: Causar e receber dano, bloquear com escudo
- **Archer**: Acertar flechas, especialmente de longa distância

### Ativando Habilidades Especiais

No **nível 100**, pressione `F` para ativar a habilidade especial de cada skill!

---

## ⚙️ Configuração

O mod inclui valores configuráveis em código para ajustar a experiência:

- Multiplicadores de XP por ação
- Cooldowns de habilidades ativas
- Intensidade dos bônus por nível

---

## 🛠️ Compilação do Projeto

Para desenvolvedores que desejam compilar o mod:

```bash
# Clone o repositório
git clone https://github.com/Murillou4/MurilloSkillsMod.git
cd MurilloSkillsMod

# Compile o mod
./gradlew build

# O arquivo .jar estará em build/libs/
```

### Executando em Ambiente de Desenvolvimento

```bash
# Configurar ambiente
./gradlew genSources

# Executar cliente de teste
./gradlew runClient
```

---

## 📸 Screenshots

> *Em breve: capturas de tela da interface de habilidades e efeitos visuais*

---

## 🤝 Contribuições

Contribuições são bem-vindas! Para contribuir:

1. Faça um **Fork** do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/NovaFeature`)
5. Abra um **Pull Request**

### Reportando Bugs

Use a aba [Issues](../../issues) para reportar bugs ou sugerir melhorias.

---

## 📜 Licença

Este projeto está licenciado sob a [Licença MIT](LICENSE).

---

## 👨‍💻 Créditos

- **Desenvolvedor Principal**: Murillo
- **Framework**: [Fabric MC](https://fabricmc.net/)
- **Inspiração**: Mods de RPG clássicos e sistemas de skills

---

<div align="center">

**Feito com ❤️ para a comunidade Minecraft**

⭐ Se você gostou do projeto, considere dar uma estrela!

</div>
