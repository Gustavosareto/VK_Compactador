# VK_Compactar

Plugin de compactação automática e manual de itens para Minecraft 1.8.8 (Spigot/Paper).

## 📋 Descrição

O VK_Compactar permite que jogadores compactem grandes quantidades de itens em um único item compactado, facilitando o armazenamento e organização do inventário. Sistema com item físico Auto Compactador que realiza a compactação automática.

## ✨ Funcionalidades

- **Compactação Manual**: Use `/compactar` para compactar todos os itens elegíveis do inventário
- **Auto Compactador (Item)**: Item físico que compacta automaticamente a cada 5 segundos quando no inventário
- **Ultra Compactação**: Sistema de compactação em dois níveis (Compactado e Ultra Compactado)
- **Permissões por Item**: Controle individual de permissões para cada tipo de item
- **Proteção contra Exploits**: Sistema de NBT tags com hash de verificação
- **Proteção de Bigorna**: Bloqueia renomeação de itens compactados
- **Feedback Visual/Sonoro**: Sons, partículas, títulos e ActionBar configuráveis
- **Totalmente Configurável**: Todos os itens, mensagens e configurações via YAML

## 📦 Sistema de Compactação

O plugin utiliza um sistema de compactação em dois níveis:

| Nível | Quantidade | Resultado |
|-------|------------|-----------|
| **Compactado** | 2240 itens normais | 1 Item Compactado |
| **Ultra Compactado** | 2240 itens compactados | 1 Item Ultra Compactado |

> **Nota**: 1 Item Ultra Compactado = 5.017.600 itens normais!
> **Quantidade ajustada**: 2240 itens (35 stacks) para caber no inventário com o Auto Compactador (1 slot)

## 🎮 Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/compactar` | Compacta todos os itens do inventário | `compact.use` |
| `/compact reload` | Recarrega as configurações | `compact.reload` |
| `/compact give <player> <item> [qtd]` | Dá item compactado para jogador | `compact.give` |
| `/compact autocompactador [player]` | Dá o item Auto Compactador | `compact.autocompactador.give` |
| `/compact info` | Mostra informações do plugin | - |

## 🔐 Permissões

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `compact.use` | Permite compactar itens manualmente | op |
| `compact.autocompactador` | Permite usar o Auto Compactador | op |
| `compact.autocompactador.give` | Permite dar o item Auto Compactador | op |
| `compact.admin` | Acesso total às funções administrativas | op |
| `compact.reload` | Permite recarregar configurações | op |
| `compact.give` | Permite dar itens compactados | op |
| `compact.item.*` | Permite compactar todos os itens | op |
| `compact.item.<material>` | Permite compactar um item específico (ex: `compact.item.diamond`) | op |

### Permissões Individuais por Item

Cada minério e plantação possui sua própria permissão:

**Minérios:**
- `compact.item.coal` - Carvão
- `compact.item.iron_ore` - Minério de Ferro
- `compact.item.gold_ore` - Minério de Ouro
- `compact.item.diamond` - Diamante
- `compact.item.emerald` - Esmeralda
- `compact.item.redstone` - Redstone
- `compact.item.lapis_lazuli` - Lápis-Lazúli
- `compact.item.quartz` - Quartzo
- E mais...

**Plantações:**
- `compact.item.wheat` - Trigo
- `compact.item.carrot_item` - Cenoura
- `compact.item.potato_item` - Batata
- `compact.item.sugar_cane` - Cana-de-Açúcar
- `compact.item.cactus` - Cacto
- E mais...

## 🎯 Auto Compactador

O Auto Compactador é um **item físico** que deve estar no inventário do jogador para ativar a compactação automática.

### Características:
- ✅ Compacta automaticamente a cada 5 segundos (configurável)
- ✅ Compactação **progressiva** - não precisa ter 2304 itens de uma vez
- ✅ Respeita as permissões individuais por item
- ✅ Visual customizável (material, nome, lore)
- ✅ Identificação via NBT e lore oculta
- ✅ Sistema de proteção anti-duplicação

### Como Funciona:
1. Obtenha o item com `/compact autocompactador`
2. Mantenha o item no inventário (ocupa 1 slot)
3. A cada 5 segundos, o sistema verifica seu inventário
4. Se tiver 2240+ de algum item, compacta automaticamente
5. Funciona de forma **progressiva** - vai compactando aos poucos conforme você coleta

### Exemplo Prático:
```
Você está minerando com Auto Compactador no inventário:
- Minera diamantes normalmente
- A cada 5 segundos o sistema verifica
- Quando atingir 2240 diamantes (35 stacks), compacta em 1 item
- Você continua minerando e o processo se repete
- Sistema otimizado: 35 slots de itens + 1 slot do Auto Compactador = 36 slots
```

### Como Usar:
1. Obtenha o item com `/compact autocompactador`
2. Mantenha o item no inventário
3. Itens serão compactados automaticamente conforme suas permissões

## 📁 Arquivos de Configuração

### config.yml
Configurações gerais do plugin:
```yaml
geral:
  ativado: true
  protecao-renomeacao: true
  protecao-duplicacao: true
  mundos-permitidos:
    - world
    - world_nether
    - world_the_end

auto-compactador:
  ativado: true
  delay-segundos: 5
  material: "EMERALD"
  nome: "&a&lAuto Compactador"
  lore:
    - "&7Tenha este item no inventário"
    - "&7para compactar automaticamente!"
```

### messages.yml
Todas as mensagens do plugin com suporte a cores (&).

### itens.yml
Configuração de itens compactáveis (apenas minérios e plantações):
```yaml
compactaveis:
  DIAMOND:
    quantidade: 2304
    nome: "&bDiamante Compactado"
    lore:
      - ""
      - "&7Contém: &f2304 Diamantes"
      - "&7Tipo: &fMinério"
    nome_ultra: "&5&lDiamante Ultra Compactado"
    lore_ultra:
      - ""
      - "&7Contém: &f2304 Diamantes Compactados"
      - "&7Total: &d5.308.416 Diamantes"
      - "&7Tipo: &5Ultra Minério"
```

## 🎯 Itens Suportados

### Minérios
- Carvão, Ferro, Ouro, Diamante, Esmeralda
- Redstone, Lápis-Lazúli, Quartzo
- Todos os minérios brutos (ore)

### Plantações
- Trigo, Cenoura, Batata
- Cana-de-Açúcar, Cacto
- Melancia, Abóbora
- Fungo do Nether, Cacau
- Sementes, Maçã

## 🛡️ Sistema de Proteção

O plugin possui várias camadas de proteção:

1. **NBT Tags Únicas**: Cada item compactado possui um ID único
2. **Hash de Verificação**: Valida integridade dos dados do item
3. **Proteção de Bigorna**: Bloqueia renomeação de itens compactados
4. **Proteção Creative**: Remove itens compactados ao entrar no modo criativo
5. **Logs de Exploit**: Registra tentativas de exploits
6. **Auto Compactador**: Identificação via NBT com lore oculta

## 📦 Instalação

1. Baixe o arquivo `VK_Compactar-1.2.0.jar`
2. Coloque na pasta `plugins` do seu servidor
3. Reinicie o servidor
4. Configure os arquivos YAML conforme necessário
5. Use `/compact autocompactador` para obter o item de auto compactação

## 🔧 Compilação

Requisitos:
- Java 8
- Maven

```bash
mvn clean package
```

O arquivo JAR será gerado em `target/VK_Compactar-1.2.0.jar`

## 📝 Compatibilidade

- Minecraft: 1.8.8
- Java: 8
- Servidores: Spigot, Paper

## 📋 Changelog

### v1.2.0
- ✅ Removida compactação automática por eventos (inventário/pickup)
- ✅ Adicionado sistema de Auto Compactador (item físico)
- ✅ Permissões individuais por item (compact.item.diamond, etc.)
- ✅ Todas as permissões agora são `op` por padrão
- ✅ Sistema de verificação de permissões no Auto Compactador
- ✅ Comando `/compact autocompactador` para dar o item
- ✅ Configuração completa do Auto Compactador no config.yml
- ✅ Quantidade ajustada para 2240 itens (35 stacks) para compatibilidade com Auto Compactador

### v1.1.0
- ✅ Removida funcionalidade de descompactação
- ✅ Sistema de Ultra Compactação
- ✅ Otimizações de código

### v1.0.0
- 🎉 Lançamento inicial
- ✅ Sistema de compactação manual e automática
- ✅ Proteção contra exploits
- ✅ Sistema de NBT com fallback

## 👤 Autor

Desenvolvido por **VulcanDev**

## 📄 Licença

Este projeto é de uso privado.
