package com.vulcandev.compactar.listeners;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * Listener para evento de pegar itens
 * A compactação automática agora é feita pelo item Auto Compactador
 * Este listener é mantido para futuras implementações
 */
public class ItemPickupListener implements Listener {

    private final ConfigManager configManager;

    public ItemPickupListener(VKCompactar plugin) {
        this.configManager = plugin.getConfigManager();
    }

    /**
     * Evento de pegar item
     * A compactação automática agora é feita pelo item Auto Compactador (a cada 5 segundos)
     * Este evento pode ser usado para outras funcionalidades no futuro
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        // A compactação automática agora é feita pelo item Auto Compactador
        // Este listener é mantido para possíveis futuras implementações
        
        // Verificar se o plugin está ativado
        if (!configManager.isPluginAtivado()) {
            return;
        }
    }
}
