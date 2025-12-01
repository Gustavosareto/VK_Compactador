package com.vulcandev.compactar.commands;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.CompactManager;
import com.vulcandev.compactar.managers.ConfigManager;
import com.vulcandev.compactar.managers.MessageManager;
import com.vulcandev.compactar.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando /compact
 * Comandos administrativos do plugin
 */
public class CompactCommand implements CommandExecutor, TabCompleter {

    private final VKCompactar plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final CompactManager compactManager;

    public CompactCommand(VKCompactar plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.compactManager = plugin.getCompactManager();
        
        // Registrar tab completer
        plugin.getCommand("compact").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "autocompactador":
            case "ac":
                handleAutoCompactador(sender, args);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "help":
            case "ajuda":
                sendHelp(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * Recarrega as configurações do plugin
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("compact.reload") && !sender.hasPermission("compact.admin")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "permissoes.sem-permissao");
            } else {
                sender.sendMessage(ColorUtils.colorize("&cVocê não tem permissão!"));
            }
            return;
        }

        plugin.reloadPlugin();

        if (sender instanceof Player) {
            messageManager.sendMessage((Player) sender, "comandos.reload-sucesso");
        } else {
            sender.sendMessage(ColorUtils.colorize("&aConfigurações recarregadas com sucesso!"));
        }
    }

    /**
     * Dá o Auto Compactador para um jogador
     */
    private void handleAutoCompactador(CommandSender sender, String[] args) {
        if (!sender.hasPermission("compact.autocompactador.give") && !sender.hasPermission("compact.admin")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "permissoes.sem-permissao");
            } else {
                sender.sendMessage(ColorUtils.colorize("&cVocê não tem permissão!"));
            }
            return;
        }

        Player target;
        
        if (args.length >= 2) {
            // Dar para outro jogador
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                if (sender instanceof Player) {
                    messageManager.sendMessage((Player) sender, "comandos.give-erro-player");
                } else {
                    sender.sendMessage(ColorUtils.colorize("&cJogador não encontrado!"));
                }
                return;
            }
        } else {
            // Dar para si mesmo
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtils.colorize("&eUso: &7/compact autocompactador <jogador>"));
                return;
            }
            target = (Player) sender;
        }

        // Criar e dar o Auto Compactador
        ItemStack autoCompactador = plugin.getAutoCompactadorManager().createAutoCompactador();
        target.getInventory().addItem(autoCompactador);

        // Mensagens
        if (sender.equals(target)) {
            sender.sendMessage(ColorUtils.colorize("&aVocê recebeu o &eAuto Compactador&a!"));
        } else {
            sender.sendMessage(ColorUtils.colorize("&aVocê deu o &eAuto Compactador &apara &e" + target.getName() + "&a!"));
            target.sendMessage(ColorUtils.colorize("&aVocê recebeu o &eAuto Compactador&a!"));
        }
    }

    /**
     * Dá itens compactados para um jogador
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("compact.give") && !sender.hasPermission("compact.admin")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "permissoes.sem-permissao");
            } else {
                sender.sendMessage(ColorUtils.colorize("&cVocê não tem permissão!"));
            }
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&eUso: &7/compact give <jogador> <item> [quantidade]"));
            return;
        }

        // Buscar jogador
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "comandos.give-erro-player");
            } else {
                sender.sendMessage(ColorUtils.colorize("&cJogador não encontrado!"));
            }
            return;
        }

        // Verificar item
        String itemName = args[2].toUpperCase();
        if (compactManager.getCompactableItem(itemName) == null) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "comandos.give-erro-item");
            } else {
                sender.sendMessage(ColorUtils.colorize("&cItem inválido!"));
            }
            return;
        }

        // Quantidade
        int quantidade = 64;
        if (args.length >= 4) {
            try {
                quantidade = Integer.parseInt(args[3]);
                if (quantidade < 1) quantidade = 64;
            } catch (NumberFormatException e) {
                quantidade = 64;
            }
        }

        // Criar e dar o item
        ItemStack compactedItem = compactManager.createCompactedItem(itemName, quantidade);
        if (compactedItem == null) {
            sender.sendMessage(ColorUtils.colorize("&cErro ao criar item compactado!"));
            return;
        }

        target.getInventory().addItem(compactedItem);

        // Mensagens
        if (sender instanceof Player) {
            messageManager.sendMessage((Player) sender, "comandos.give-sucesso",
                new String[]{"quantidade", "item", "player"},
                new String[]{String.valueOf(quantidade), itemName, target.getName()}
            );
        } else {
            sender.sendMessage(ColorUtils.colorize("&aVocê deu &e" + quantidade + "x " + itemName + " compactado &apara &e" + target.getName() + "&a."));
        }

        messageManager.sendMessage(target, "comandos.give-recebido",
            new String[]{"quantidade", "item"},
            new String[]{String.valueOf(quantidade), itemName}
        );
    }

    /**
     * Mostra informações do plugin
     */
    private void handleInfo(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(ColorUtils.colorize("&a&l═══════════ VK_Compactar ═══════════"));
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(ColorUtils.colorize(" &7Versão: &a" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtils.colorize(" &7Autor: &aVulcanDev"));
        sender.sendMessage(ColorUtils.colorize(" &7Itens Compactáveis: &a" + compactManager.getTotalCompactableItems()));
        sender.sendMessage(ColorUtils.colorize(" &7Status: " + (configManager.isPluginAtivado() ? "&aAtivado" : "&cDesativado")));
        sender.sendMessage(ColorUtils.colorize(" &7Auto Compactador: " + (configManager.isAutoCompactadorAtivado() ? "&aAtivado" : "&cDesativado")));
        sender.sendMessage(ColorUtils.colorize(" &7Delay Auto Compact: &a" + configManager.getAutoCompactadorDelay() + "s"));
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(ColorUtils.colorize("&a&l═══════════════════════════════════"));
        sender.sendMessage(ColorUtils.colorize(""));
    }

    /**
     * Envia a mensagem de ajuda
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(ColorUtils.colorize("&a&l═══════ VK_Compactar - Ajuda ═══════"));
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(ColorUtils.colorize(" &a/compactar &8- &7Compacta todos os itens"));
        sender.sendMessage(ColorUtils.colorize(" &a/compact reload &8- &7Recarrega configurações"));
        sender.sendMessage(ColorUtils.colorize(" &a/compact give <player> <item> [qtd] &8- &7Dar item compactado"));
        sender.sendMessage(ColorUtils.colorize(" &a/compact autocompactador [player] &8- &7Dar auto compactador"));
        sender.sendMessage(ColorUtils.colorize(" &a/compact info &8- &7Informações do plugin"));
        sender.sendMessage(ColorUtils.colorize(""));
        sender.sendMessage(ColorUtils.colorize("&a&l═════════════════════════════════════"));
        sender.sendMessage(ColorUtils.colorize(""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "give", "autocompactador", "ac", "info", "help");
            String arg = args[0].toLowerCase();
            completions = subCommands.stream()
                .filter(s -> s.startsWith(arg))
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Jogadores online
            String arg = args[1].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(arg))
                .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("autocompactador") || args[0].equalsIgnoreCase("ac"))) {
            // Jogadores online para autocompactador
            String arg = args[1].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(arg))
                .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Itens compactáveis
            String arg = args[2].toLowerCase();
            completions = compactManager.getCompactableItems().keySet().stream()
                .filter(item -> item.toLowerCase().startsWith(arg))
                .collect(Collectors.toList());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // Quantidades sugeridas
            completions = Arrays.asList("64", "128", "256", "512", "576");
        }

        return completions;
    }
}
