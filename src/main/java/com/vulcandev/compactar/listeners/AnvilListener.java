package com.vulcandev.compactar.listeners;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.CompactManager;
import com.vulcandev.compactar.managers.ConfigManager;
import com.vulcandev.compactar.managers.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Listener para eventos de bigorna
 * Responsável por bloquear renomeação de itens compactados
 */
public class AnvilListener implements Listener {

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final CompactManager compactManager;

    public AnvilListener(VKCompactar plugin) {
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.compactManager = plugin.getCompactManager();
    }

    /**
     * Evento de clique em inventário de bigorna
     * Proteção contra renomeação de itens compactados
     * Nota: PrepareAnvilEvent não existe em 1.8.8, então usamos InventoryClickEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!configManager.isProtecaoRenomeacao()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Verificar se é uma bigorna
        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Verificar item no cursor (sendo colocado na bigorna)
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            if (compactManager.isValidCompactedItem(cursorItem)) {
                // Slot de resultado
                if (event.getSlot() == 2 || event.getRawSlot() == 2) {
                    event.setCancelled(true);
                    messageManager.sendMessage(player, "protecao.renomeacao-bloqueada");
                    return;
                }
            }
        }

        // Verificar item sendo clicado (resultado da bigorna)
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            // Slot de resultado da bigorna
            if (event.getSlot() == 2 || event.getRawSlot() == 2) {
                // Verificar se os itens de entrada são compactados
                ItemStack firstSlot = event.getInventory().getItem(0);
                ItemStack secondSlot = event.getInventory().getItem(1);

                if ((firstSlot != null && compactManager.isValidCompactedItem(firstSlot)) ||
                    (secondSlot != null && compactManager.isValidCompactedItem(secondSlot))) {
                    event.setCancelled(true);
                    messageManager.sendMessage(player, "protecao.renomeacao-bloqueada");
                    return;
                }
            }
        }

        // Verificar se está tentando colocar item compactado na bigorna
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() == InventoryType.ANVIL) {
            
            // Verificar se o item sendo movido é compactado
            ItemStack movedItem = null;
            
            if (event.isShiftClick() && event.getCurrentItem() != null) {
                movedItem = event.getCurrentItem();
            } else if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                movedItem = cursorItem;
            }

            if (movedItem != null && compactManager.isValidCompactedItem(movedItem)) {
                // Permitir apenas remover, não adicionar
                if (event.getRawSlot() < 3) { // Slots da bigorna
                    // Verificar se está tentando colocar
                    if (cursorItem != null && cursorItem.getType() != Material.AIR && 
                        compactManager.isValidCompactedItem(cursorItem)) {
                        event.setCancelled(true);
                        messageManager.sendMessage(player, "protecao.renomeacao-bloqueada");
                    }
                }
            }
        }
    }
}
