package com.vulcandev.compactar.managers;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de mensagens do plugin
 * Responsável por carregar e enviar mensagens personalizadas
 */
public class MessageManager {

    private final VKCompactar plugin;
    private FileConfiguration messagesConfig;
    private String prefixo;
    
    // Cache de mensagens
    private final Map<String, String> messageCache;

    public MessageManager(VKCompactar plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
    }

    /**
     * Carrega todas as mensagens
     */
    public void loadMessages() {
        saveDefaultMessages();
        
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Limpar cache
        messageCache.clear();
        
        // Carregar prefixo
        prefixo = ColorUtils.colorize(messagesConfig.getString("prefixo", "&8[&aCompactar&8] "));
        
        // Pré-carregar mensagens comuns
        preloadCommonMessages();
    }

    /**
     * Pré-carrega mensagens mais usadas
     */
    private void preloadCommonMessages() {
        String[] commonPaths = {
            "compactacao.sucesso",
            "compactacao.erro-quantidade",
            "compactacao.erro-item",
            "descompactacao.sucesso",
            "permissoes.sem-permissao"
        };
        
        for (String path : commonPaths) {
            String message = messagesConfig.getString(path, "");
            messageCache.put(path, ColorUtils.colorize(message));
        }
    }

    /**
     * Salva o arquivo de mensagens padrão
     */
    private void saveDefaultMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    /**
     * Obtém uma mensagem do arquivo de configuração
     * @param path Caminho da mensagem
     * @return Mensagem colorida
     */
    public String getMessage(String path) {
        // Verificar cache
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }
        
        String message = messagesConfig.getString(path, "&cMensagem não encontrada: " + path);
        message = ColorUtils.colorize(message);
        
        // Adicionar ao cache
        messageCache.put(path, message);
        
        return message;
    }

    /**
     * Obtém uma mensagem com variáveis substituídas
     * @param path Caminho da mensagem
     * @param placeholders Variáveis a substituir
     * @return Mensagem colorida com variáveis substituídas
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }

    /**
     * Obtém uma mensagem com variáveis substituídas (método simplificado)
     * @param path Caminho da mensagem
     * @param keys Chaves das variáveis
     * @param values Valores das variáveis
     * @return Mensagem colorida com variáveis substituídas
     */
    public String getMessage(String path, String[] keys, String[] values) {
        String message = getMessage(path);
        
        if (keys != null && values != null && keys.length == values.length) {
            for (int i = 0; i < keys.length; i++) {
                message = message.replace("{" + keys[i] + "}", values[i]);
            }
        }
        
        return message;
    }

    /**
     * Envia uma mensagem para o jogador
     * @param player Jogador que receberá a mensagem
     * @param path Caminho da mensagem
     */
    public void sendMessage(Player player, String path) {
        player.sendMessage(prefixo + getMessage(path));
    }

    /**
     * Envia uma mensagem para o jogador com variáveis
     * @param player Jogador que receberá a mensagem
     * @param path Caminho da mensagem
     * @param placeholders Variáveis a substituir
     */
    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(prefixo + getMessage(path, placeholders));
    }

    /**
     * Envia uma mensagem para o jogador com variáveis (método simplificado)
     * @param player Jogador que receberá a mensagem
     * @param path Caminho da mensagem
     * @param keys Chaves das variáveis
     * @param values Valores das variáveis
     */
    public void sendMessage(Player player, String path, String[] keys, String[] values) {
        player.sendMessage(prefixo + getMessage(path, keys, values));
    }

    /**
     * Envia uma mensagem raw (sem prefixo)
     * @param player Jogador que receberá a mensagem
     * @param path Caminho da mensagem
     */
    public void sendRawMessage(Player player, String path) {
        player.sendMessage(getMessage(path));
    }

    /**
     * Envia uma mensagem raw com variáveis
     * @param player Jogador que receberá a mensagem
     * @param path Caminho da mensagem
     * @param keys Chaves das variáveis
     * @param values Valores das variáveis
     */
    public void sendRawMessage(Player player, String path, String[] keys, String[] values) {
        player.sendMessage(getMessage(path, keys, values));
    }

    /**
     * Obtém o prefixo configurado
     * @return Prefixo colorido
     */
    public String getPrefixo() {
        return prefixo;
    }

    /**
     * Obtém a configuração de mensagens
     * @return FileConfiguration das mensagens
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
