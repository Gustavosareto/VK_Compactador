package com.vulcandev.compactar.listeners;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.CompactManager;
import com.vulcandev.compactar.managers.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener para eventos gerais do jogador
 * Responsável por proteções e limpeza de dados
 */
public class PlayerListener implements Listener {

    private final VKCompactar plugin;
    private final ConfigManager configManager;
    private final CompactManager compactManager;

    public PlayerListener(VKCompactar plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.compactManager = plugin.getCompactManager();
    }

    /**
     * Evento de entrada do jogador
     * Limpa cooldowns antigos
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        compactManager.removeCooldown(event.getPlayer());
    }

    /**
     * Evento de saída do jogador
     * Remove cooldowns
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        compactManager.removeCooldown(event.getPlayer());
    }

    /**
     * Evento de mudança de gamemode
     * Proteção contra duplicação em creative
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!configManager.isProtecaoDuplicacao()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Se está mudando PARA creative, verificar itens compactados
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            // Verificar se o jogador tem itens compactados
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && compactManager.isValidCompactedItem(item)) {
                    // Remover itens compactados ao entrar no creative (proteção)
                    if (!player.hasPermission("compact.admin")) {
                        player.getInventory().remove(item);
                        
                        if (configManager.isLogarExploits()) {
                            plugin.getLogger().warning("[Proteção] Itens compactados removidos de " + 
                                player.getName() + " ao entrar no Creative.");
                        }
                    }
                }
            }
            
            player.updateInventory();
        }
    }

    /**
     * Evento de interação com item
     * Proteção contra exploits e descompactação por clique
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Verificar se é um item compactado
        if (!compactManager.isValidCompactedItem(item)) {
            return;
        }

        // Bloquear uso de itens compactados como blocos/itens normais
        if (event.getAction().name().contains("RIGHT") || event.getAction().name().contains("LEFT")) {
            // Permitir apenas em inventário, não no mundo
            if (event.hasBlock()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Evento de dropar item
     * Validação de itens dropados
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        // Verificar se é um item compactado
        if (compactManager.isValidCompactedItem(item)) {
            // Apenas logar para monitoramento se configurado
            if (configManager.isLogarExploits()) {
                plugin.getLogger().info("[Log] " + event.getPlayer().getName() + 
                    " dropou item compactado: " + item.getType().name());
            }
        }
    }
}
