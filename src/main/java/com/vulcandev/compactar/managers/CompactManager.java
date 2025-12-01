package com.vulcandev.compactar.managers;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.models.CompactableItem;
import com.vulcandev.compactar.utils.ColorUtils;
import com.vulcandev.compactar.utils.NBTUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Gerenciador de compactação de itens
 * Responsável por todas as operações de compactação e descompactação
 */
public class CompactManager {

    private final VKCompactar plugin;
    
    // Mapa de itens compactáveis
    private final Map<String, CompactableItem> compactableItems;
    
    // Cache de verificação de itens
    private final Map<UUID, Long> playerCooldowns;

    public CompactManager(VKCompactar plugin) {
        this.plugin = plugin;
        this.compactableItems = new HashMap<>();
        this.playerCooldowns = new HashMap<>();
    }

    /**
     * Carrega todos os itens compactáveis do arquivo itens.yml
     */
    public void loadCompactableItems() {
        compactableItems.clear();
        
        FileConfiguration itensConfig = plugin.getConfigManager().getItensConfig();
        ConfigurationSection compactaveisSection = itensConfig.getConfigurationSection("compactaveis");
        
        if (compactaveisSection == null) {
            plugin.getLogger().warning("Nenhum item compactável encontrado em itens.yml!");
            return;
        }
        
        for (String key : compactaveisSection.getKeys(false)) {
            try {
                ConfigurationSection itemSection = compactaveisSection.getConfigurationSection(key);
                
                if (itemSection == null) continue;
                
                int quantidade = itemSection.getInt("quantidade", 2304);
                String nome = ColorUtils.colorize(itemSection.getString("nome", "&f" + key + " Compactado"));
                List<String> loreRaw = itemSection.getStringList("lore");
                List<String> lore = new ArrayList<>();
                
                for (String line : loreRaw) {
                    lore.add(ColorUtils.colorize(line));
                }
                
                // Carregar configuração Ultra
                String nomeUltra = ColorUtils.colorize(itemSection.getString("nome_ultra", ""));
                List<String> loreUltraRaw = itemSection.getStringList("lore_ultra");
                List<String> loreUltra = new ArrayList<>();
                
                for (String line : loreUltraRaw) {
                    loreUltra.add(ColorUtils.colorize(line));
                }
                
                int data = itemSection.getInt("data", 0);
                String materialName = itemSection.getString("material", key);
                
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    try {
                        material = Material.valueOf(key.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Material inválido: " + key);
                        continue;
                    }
                }
                
                CompactableItem compactableItem = new CompactableItem(
                    key,
                    material,
                    quantidade,
                    nome,
                    lore,
                    (short) data,
                    nomeUltra,
                    loreUltra
                );
                
                compactableItems.put(key.toUpperCase(), compactableItem);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar item: " + key + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Carregados " + compactableItems.size() + " itens compactáveis.");
    }

    /**
     * Verifica se um material é compactável
     */
    public boolean isCompactable(Material material) {
        return compactableItems.containsKey(material.name());
    }

    /**
     * Verifica se um material é compactável (com data value)
     */
    public boolean isCompactable(Material material, short data) {
        String key = material.name();
        if (compactableItems.containsKey(key)) {
            CompactableItem item = compactableItems.get(key);
            return item.getData() == data || item.getData() == 0;
        }
        return false;
    }

    /**
     * Obtém as informações de um item compactável
     */
    public CompactableItem getCompactableItem(Material material) {
        return compactableItems.get(material.name());
    }

    /**
     * Obtém as informações de um item compactável
     */
    public CompactableItem getCompactableItem(String materialName) {
        return compactableItems.get(materialName.toUpperCase());
    }

    /**
     * Compacta itens do inventário do jogador
     * @param player Jogador
     * @param material Material a ser compactado
     * @return Quantidade de itens compactados (0 se não foi possível)
     */
    public int compactItems(Player player, Material material) {
        if (!isCompactable(material)) {
            return 0;
        }
        
        CompactableItem compactableItem = getCompactableItem(material);
        int quantidadeNecessaria = compactableItem.getQuantidade();
        
        // Contar itens no inventário
        int totalItems = countItems(player, material, compactableItem.getData());
        
        if (totalItems < quantidadeNecessaria) {
            return 0;
        }
        
        // Calcular quantas compactações podemos fazer
        int compactacoes = totalItems / quantidadeNecessaria;
        int itensRemover = compactacoes * quantidadeNecessaria;
        
        // Remover itens
        removeItems(player, material, compactableItem.getData(), itensRemover);
        
        // Criar item compactado
        ItemStack compactedItem = createCompactedItem(compactableItem, compactacoes);
        
        // Adicionar ao inventário
        addItemToInventory(player, compactedItem);
        
        return itensRemover;
    }

    /**
     * Compacta todos os itens compactáveis do inventário do jogador
     * Também compacta itens compactados em ultra compactados
     * @param player Jogador
     * @return Mapa com material e quantidade compactada
     */
    public Map<Material, Integer> compactAllItems(Player player) {
        Map<Material, Integer> result = new HashMap<>();
        
        // Primeiro, compactar itens normais
        for (CompactableItem compactableItem : compactableItems.values()) {
            Material material = compactableItem.getMaterial();
            
            // Verificar se o item está bloqueado
            if (plugin.getConfigManager().isItemBloqueado(material.name())) {
                continue;
            }
            
            int compacted = compactItems(player, material);
            
            if (compacted > 0) {
                result.put(material, compacted);
            }
        }
        
        // Depois, compactar itens compactados em ultra compactados
        compactToUltra(player, result);
        
        return result;
    }
    
    /**
     * Compacta itens compactados em ultra compactados
     */
    public void compactToUltra(Player player, Map<Material, Integer> result) {
        for (CompactableItem compactableItem : compactableItems.values()) {
            if (!compactableItem.hasUltraConfig()) continue;
            
            Material material = compactableItem.getMaterial();
            int quantidadeNecessaria = compactableItem.getQuantidade();
            
            // Contar itens compactados (não ultra) no inventário
            int totalCompacted = countCompactedItems(player, material);
            
            if (totalCompacted < quantidadeNecessaria) continue;
            
            // Calcular quantas ultra compactações podemos fazer
            int ultraCompactacoes = totalCompacted / quantidadeNecessaria;
            int itensRemover = ultraCompactacoes * quantidadeNecessaria;
            
            // Remover itens compactados
            removeCompactedItems(player, material, itensRemover);
            
            // Criar item ultra compactado
            ItemStack ultraItem = createUltraCompactedItem(compactableItem, ultraCompactacoes);
            
            // Adicionar ao inventário
            addItemToInventory(player, ultraItem);
            
            // Adicionar ao resultado (quantidade convertida para itens originais)
            long totalOriginal = (long) itensRemover * quantidadeNecessaria;
            result.merge(material, (int) Math.min(totalOriginal, Integer.MAX_VALUE), Integer::sum);
        }
    }
    
    /**
     * Conta itens compactados (não ultra) de um tipo específico no inventário
     */
    private int countCompactedItems(Player player, Material material) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() != material) continue;
            if (!isValidCompactedItem(item)) continue;
            
            // Verificar se NÃO é ultra compactado
            if (NBTUtils.isUltraCompacted(item)) continue;
            
            // Obter multiplicador para contar a quantidade real de compactados
            int multiplier = NBTUtils.getCompactedMultiplier(item);
            if (multiplier > 0) {
                count += multiplier * item.getAmount();
            }
        }
        
        return count;
    }
    
    /**
     * Remove itens compactados do inventário (para criar ultra)
     */
    private void removeCompactedItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            
            if (item == null) continue;
            if (item.getType() != material) continue;
            if (!isValidCompactedItem(item)) continue;
            if (NBTUtils.isUltraCompacted(item)) continue;
            
            int multiplier = NBTUtils.getCompactedMultiplier(item);
            if (multiplier <= 0) continue;
            
            int itemValue = multiplier * item.getAmount();
            
            if (itemValue <= remaining) {
                player.getInventory().setItem(i, null);
                remaining -= itemValue;
            } else {
                // Precisa remover parcialmente
                int stacksToRemove = remaining / multiplier;
                if (stacksToRemove > 0) {
                    item.setAmount(item.getAmount() - stacksToRemove);
                    remaining -= stacksToRemove * multiplier;
                }
                // Se sobrar algo que não é múltiplo exato, ignorar (raro acontecer)
                if (remaining > 0 && item.getAmount() > 0) {
                    // Remover mais um stack e devolver a diferença como itens normais
                    remaining = 0;
                }
            }
        }
        
        player.updateInventory();
    }

    /**
     * Cria um item compactado
     */
    public ItemStack createCompactedItem(CompactableItem compactableItem, int multiplier) {
        return createCompactedItem(compactableItem, multiplier, false);
    }
    
    /**
     * Cria um item compactado ou ultra compactado
     */
    public ItemStack createCompactedItem(CompactableItem compactableItem, int multiplier, boolean isUltra) {
        ItemStack item = new ItemStack(compactableItem.getMaterial(), 1, compactableItem.getData());
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) return item;
        
        // Definir nome baseado no tipo (normal ou ultra)
        String nome;
        List<String> loreBase;
        
        if (isUltra && compactableItem.hasUltraConfig()) {
            nome = compactableItem.getNomeUltra();
            loreBase = compactableItem.getLoreUltra();
        } else {
            nome = compactableItem.getNome();
            loreBase = compactableItem.getLore();
        }
        
        if (multiplier > 1) {
            nome = nome + " &7(x" + multiplier + ")";
        }
        meta.setDisplayName(ColorUtils.colorize(nome));
        
        // Definir lore com quantidade real
        List<String> lore = new ArrayList<>();
        int baseQuantidade = compactableItem.getQuantidade();
        int totalQuantidade = baseQuantidade * multiplier;
        
        for (String line : loreBase) {
            String processedLine = line.replace("{quantidade}", String.valueOf(totalQuantidade));
            lore.add(processedLine);
        }
        
        // Adicionar identificador único na lore
        lore.add("");
        lore.add(ColorUtils.colorize("&8ID: " + generateUniqueId()));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        // Adicionar NBT tags (incluindo flag de ultra)
        item = NBTUtils.setCompactedData(item, 
            compactableItem.getMaterial().name(),
            compactableItem.getQuantidade(),
            multiplier,
            generateUniqueId(),
            isUltra
        );
        
        return item;
    }

    /**
     * Cria um item ultra compactado
     */
    public ItemStack createUltraCompactedItem(CompactableItem compactableItem, int multiplier) {
        return createCompactedItem(compactableItem, multiplier, true);
    }

    /**
     * Cria um item compactado com quantidade específica (para comando /compact give)
     */
    public ItemStack createCompactedItem(String materialName, int quantidade) {
        CompactableItem compactableItem = getCompactableItem(materialName);
        if (compactableItem == null) {
            return null;
        }
        
        int multiplier = quantidade / compactableItem.getQuantidade();
        if (multiplier < 1) multiplier = 1;
        
        return createCompactedItem(compactableItem, multiplier);
    }

    /**
     * Verifica se um item é um item compactado válido
     */
    public boolean isValidCompactedItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        
        // Verificar NBT
        if (!NBTUtils.hasCompactedTag(itemStack)) {
            return false;
        }
        
        // Verificar integridade do NBT
        if (plugin.getConfigManager().isVerificarNBT()) {
            if (!NBTUtils.validateCompactedItem(itemStack)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Conta itens de um tipo específico no inventário
     */
    private int countItems(Player player, Material material, short data) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() != material) continue;
            
            // Verificar data value
            if (data != 0 && item.getDurability() != data) continue;
            
            // Verificar se não é um item compactado
            if (isValidCompactedItem(item)) continue;
            
            // Verificar se não é um item renomeado (se configurado)
            if (plugin.getConfigManager().isBloquearItensRenomeados()) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    continue;
                }
            }
            
            count += item.getAmount();
        }
        
        return count;
    }

    /**
     * Remove itens do inventário
     */
    private void removeItems(Player player, Material material, short data, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            
            if (item == null) continue;
            if (item.getType() != material) continue;
            if (data != 0 && item.getDurability() != data) continue;
            if (isValidCompactedItem(item)) continue;
            
            if (plugin.getConfigManager().isBloquearItensRenomeados()) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    continue;
                }
            }
            
            int itemAmount = item.getAmount();
            
            if (itemAmount <= remaining) {
                player.getInventory().setItem(i, null);
                remaining -= itemAmount;
            } else {
                item.setAmount(itemAmount - remaining);
                remaining = 0;
            }
        }
        
        player.updateInventory();
    }

    /**
     * Adiciona item ao inventário ou dropa no chão
     */
    private void addItemToInventory(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        
        if (!leftover.isEmpty()) {
            // Dropar itens que não couberam
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            
            plugin.getMessageManager().sendMessage(player, "compactacao.item-dropado");
        }
        
        player.updateInventory();
    }

    /**
     * Gera um ID único para o item compactado
     */
    private String generateUniqueId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Verifica se o jogador está em cooldown
     */
    public boolean isOnCooldown(Player player) {
        if (!playerCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        
        long lastCompact = playerCooldowns.get(player.getUniqueId());
        int delay = plugin.getConfigManager().getDelayCompactacao() * 50; // Converter ticks para ms
        
        return System.currentTimeMillis() - lastCompact < delay;
    }

    /**
     * Define cooldown para o jogador
     */
    public void setCooldown(Player player) {
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Remove cooldown do jogador
     */
    public void removeCooldown(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }

    /**
     * Obtém todos os itens compactáveis
     */
    public Map<String, CompactableItem> getCompactableItems() {
        return Collections.unmodifiableMap(compactableItems);
    }

    /**
     * Obtém a quantidade total de itens compactáveis
     */
    public int getTotalCompactableItems() {
        return compactableItems.size();
    }
}
