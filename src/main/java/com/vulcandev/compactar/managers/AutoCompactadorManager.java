package com.vulcandev.compactar.managers;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.models.CompactableItem;
import com.vulcandev.compactar.utils.ColorUtils;
import com.vulcandev.compactar.utils.NBTUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gerenciador do Auto Compactador
 * Item que compacta automaticamente itens do inventário a cada X segundos
 */
public class AutoCompactadorManager {

    private final VKCompactar plugin;
    private BukkitTask autoCompactTask;

    public AutoCompactadorManager(VKCompactar plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia a task de auto compactação
     */
    public void startAutoCompactTask() {
        stopAutoCompactTask();
        
        int delayTicks = plugin.getConfigManager().getAutoCompactadorDelay() * 20; // Segundos para ticks
        
        autoCompactTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                processAutoCompact(player);
            }
        }, delayTicks, delayTicks);
        
        plugin.getLogger().info("Auto Compactador task iniciada (delay: " + 
            plugin.getConfigManager().getAutoCompactadorDelay() + "s)");
    }

    /**
     * Para a task de auto compactação
     */
    public void stopAutoCompactTask() {
        if (autoCompactTask != null) {
            autoCompactTask.cancel();
            autoCompactTask = null;
        }
    }

    /**
     * Processa auto compactação para um jogador
     */
    private void processAutoCompact(Player player) {
        // Verificar se o plugin está ativado
        if (!plugin.getConfigManager().isPluginAtivado()) {
            return;
        }
        
        // Verificar mundo
        if (!plugin.getConfigManager().isMundoPermitido(player.getWorld().getName())) {
            return;
        }
        
        // Verificar se o jogador tem o Auto Compactador no inventário
        if (!hasAutoCompactador(player)) {
            return;
        }
        
        // Verificar permissão básica
        if (!player.hasPermission("compact.autocompactador")) {
            return;
        }
        
        // Compactar itens (respeitando permissões individuais)
        compactWithPermissions(player);
    }

    /**
     * Verifica se o jogador tem o Auto Compactador no inventário
     */
    public boolean hasAutoCompactador(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAutoCompactador(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se um item é o Auto Compactador
     */
    public boolean isAutoCompactador(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Verificar via NBT
        if (NBTUtils.hasAutoCompactadorTag(item)) {
            return true;
        }
        
        // Fallback: verificar via lore
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                if (line.contains("§k§a§c")) { // Código oculto de identificação
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Compacta itens respeitando permissões individuais
     */
    private void compactWithPermissions(Player player) {
        CompactManager compactManager = plugin.getCompactManager();
        Map<Material, Integer> result = new HashMap<>();
        
        for (CompactableItem compactableItem : compactManager.getCompactableItems().values()) {
            Material material = compactableItem.getMaterial();
            String materialName = material.name().toLowerCase();
            
            // Verificar permissão individual do item
            String permission = "compact.item." + materialName;
            if (!player.hasPermission(permission) && !player.hasPermission("compact.item.*")) {
                continue;
            }
            
            // Verificar se o item está bloqueado
            if (plugin.getConfigManager().isItemBloqueado(material.name())) {
                continue;
            }
            
            // Tentar compactar
            int compacted = compactManager.compactItems(player, material);
            if (compacted > 0) {
                result.put(material, compacted);
            }
        }
        
        // Também tentar ultra compactar
        if (!result.isEmpty()) {
            compactManager.compactToUltra(player, result);
        }
    }

    /**
     * Cria o item Auto Compactador
     */
    public ItemStack createAutoCompactador() {
        String materialName = plugin.getConfigManager().getAutoCompactadorMaterial();
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.EMERALD;
            plugin.getLogger().warning("Material inválido para Auto Compactador: " + materialName + ". Usando EMERALD.");
        }
        
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) return item;
        
        // Nome
        String nome = plugin.getConfigManager().getAutoCompactadorNome();
        meta.setDisplayName(ColorUtils.colorize(nome));
        
        // Lore
        List<String> loreConfig = plugin.getConfigManager().getAutoCompactadorLore();
        List<String> lore = new ArrayList<>();
        
        for (String line : loreConfig) {
            lore.add(ColorUtils.colorize(line));
        }
        
        // Adicionar identificador oculto
        lore.add("§k§a§c" + UUID.randomUUID().toString().substring(0, 8));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        // Adicionar NBT tag
        item = NBTUtils.setAutoCompactadorTag(item);
        
        return item;
    }

    /**
     * Dá o Auto Compactador para um jogador
     */
    public void giveAutoCompactador(Player player) {
        ItemStack autoCompactador = createAutoCompactador();
        
        if (player.getInventory().firstEmpty() == -1) {
            // Inventário cheio, dropar no chão
            player.getWorld().dropItemNaturally(player.getLocation(), autoCompactador);
            plugin.getMessageManager().sendMessage(player, "autocompactador.recebido-dropado");
        } else {
            player.getInventory().addItem(autoCompactador);
            plugin.getMessageManager().sendMessage(player, "autocompactador.recebido");
        }
    }
}
