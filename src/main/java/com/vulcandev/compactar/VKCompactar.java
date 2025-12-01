package com.vulcandev.compactar;

import com.vulcandev.compactar.commands.CompactCommand;
import com.vulcandev.compactar.commands.CompactarCommand;
import com.vulcandev.compactar.listeners.AnvilListener;
import com.vulcandev.compactar.listeners.InventoryListener;
import com.vulcandev.compactar.listeners.ItemPickupListener;
import com.vulcandev.compactar.listeners.PlayerListener;
import com.vulcandev.compactar.managers.AutoCompactadorManager;
import com.vulcandev.compactar.managers.CompactManager;
import com.vulcandev.compactar.managers.ConfigManager;
import com.vulcandev.compactar.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin principal VK_Compactar
 * Plugin de compactação automática e manual de itens para Minecraft 1.8.8
 * 
 * @author VulcanDev
 * @version 1.2.0
 */
public class VKCompactar extends JavaPlugin {

    private static VKCompactar instance;
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private CompactManager compactManager;
    private AutoCompactadorManager autoCompactadorManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inicializar managers
        initializeManagers();
        
        // Registrar comandos
        registerCommands();
        
        // Registrar listeners
        registerListeners();
        
        // Iniciar Auto Compactador
        if (configManager.isAutoCompactadorAtivado()) {
            autoCompactadorManager.startAutoCompactTask();
        }
        
        getLogger().info("§a══════════════════════════════════════════");
        getLogger().info("§a VK_Compactar v" + getDescription().getVersion());
        getLogger().info("§a Plugin carregado com sucesso!");
        getLogger().info("§a Desenvolvido por VulcanDev");
        getLogger().info("§a══════════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        // Parar Auto Compactador
        if (autoCompactadorManager != null) {
            autoCompactadorManager.stopAutoCompactTask();
        }
        
        getLogger().info("§c VK_Compactar desativado!");
        instance = null;
    }

    /**
     * Inicializa todos os managers do plugin
     */
    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.compactManager = new CompactManager(this);
        this.autoCompactadorManager = new AutoCompactadorManager(this);
        
        // Carregar configurações
        configManager.loadConfigs();
        messageManager.loadMessages();
        compactManager.loadCompactableItems();
    }

    /**
     * Registra todos os comandos do plugin
     */
    private void registerCommands() {
        getCommand("compactar").setExecutor(new CompactarCommand(this));
        getCommand("compact").setExecutor(new CompactCommand(this));
    }

    /**
     * Registra todos os listeners do plugin
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemPickupListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnvilListener(this), this);
    }

    /**
     * Recarrega todas as configurações do plugin
     */
    public void reloadPlugin() {
        // Parar task do auto compactador antes de recarregar
        if (autoCompactadorManager != null) {
            autoCompactadorManager.stopAutoCompactTask();
        }
        
        configManager.loadConfigs();
        messageManager.loadMessages();
        compactManager.loadCompactableItems();
        
        // Reiniciar auto compactador se ativado
        if (configManager.isAutoCompactadorAtivado()) {
            autoCompactadorManager.startAutoCompactTask();
        }
    }

    /**
     * Obtém a instância do plugin
     * @return Instância do VKCompactar
     */
    public static VKCompactar getInstance() {
        return instance;
    }

    /**
     * Obtém o gerenciador de configurações
     * @return ConfigManager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Obtém o gerenciador de mensagens
     * @return MessageManager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Obtém o gerenciador de compactação
     * @return CompactManager
     */
    public CompactManager getCompactManager() {
        return compactManager;
    }

    /**
     * Obtém o gerenciador de auto compactador
     * @return AutoCompactadorManager
     */
    public AutoCompactadorManager getAutoCompactadorManager() {
        return autoCompactadorManager;
    }
}
