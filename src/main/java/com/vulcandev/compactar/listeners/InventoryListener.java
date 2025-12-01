package com.vulcandev.compactar.listeners;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Listener para eventos de inventário
 * A compactação automática agora é feita pelo item Auto Compactador
 */
public class InventoryListener implements Listener {

    private final ConfigManager configManager;

    public InventoryListener(VKCompactar plugin) {
        this.configManager = plugin.getConfigManager();
    }

    /**
     * Evento de fechamento de inventário
     * A compactação automática agora é feita pelo item Auto Compactador (a cada 5 segundos)
     * Este evento pode ser usado para outras funcionalidades no futuro
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        // A compactação automática agora é feita pelo item Auto Compactador
        // Este listener é mantido para possíveis futuras implementações
        
        // Verificar se o plugin está ativado
        if (!configManager.isPluginAtivado()) {
            return;
        }
    }
}
