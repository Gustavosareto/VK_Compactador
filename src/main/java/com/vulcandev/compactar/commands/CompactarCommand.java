package com.vulcandev.compactar.commands;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.CompactManager;
import com.vulcandev.compactar.managers.ConfigManager;
import com.vulcandev.compactar.managers.MessageManager;
import com.vulcandev.compactar.utils.FeedbackUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Comando /compactar
 * Permite ao jogador compactar todos os itens compactáveis do inventário
 */
public class CompactarCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final CompactManager compactManager;

    public CompactarCommand(VKCompactar plugin) {
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.compactManager = plugin.getCompactManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;

        // Verificar se o plugin está ativado
        if (!configManager.isPluginAtivado()) {
            messageManager.sendMessage(player, "erros.plugin-desativado");
            return true;
        }

        // Verificar permissão
        if (!player.hasPermission("compact.use")) {
            messageManager.sendMessage(player, "permissoes.sem-permissao");
            return true;
        }

        // Verificar mundo
        if (!configManager.isMundoPermitido(player.getWorld().getName())) {
            messageManager.sendMessage(player, "erros.mundo-desativado");
            return true;
        }

        // Verificar cooldown
        if (compactManager.isOnCooldown(player)) {
            return true;
        }

        // Compactar todos os itens
        Map<Material, Integer> compactedItems = compactManager.compactAllItems(player);

        if (compactedItems.isEmpty()) {
            messageManager.sendMessage(player, "compactacao.erro-quantidade");
            return true;
        }

        // Definir cooldown
        compactManager.setCooldown(player);

        // Calcular total compactado
        int totalCompactado = compactedItems.values().stream().mapToInt(Integer::intValue).sum();

        // Enviar mensagem de sucesso
        String itemsTexto = compactedItems.size() == 1 
            ? compactedItems.keySet().iterator().next().name() 
            : compactedItems.size() + " tipos de itens";
        
        messageManager.sendMessage(player, "compactacao.sucesso",
            new String[]{"quantidade", "item"},
            new String[]{String.valueOf(totalCompactado), itemsTexto}
        );

        // Enviar feedback visual/sonoro
        FeedbackUtils.sendCompactFeedback(player, itemsTexto, totalCompactado);

        return true;
    }
}
