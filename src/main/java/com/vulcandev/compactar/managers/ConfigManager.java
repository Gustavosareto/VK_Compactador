package com.vulcandev.compactar.managers;

import com.vulcandev.compactar.VKCompactar;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador de configurações do plugin
 * Responsável por carregar e gerenciar todas as configurações
 */
public class ConfigManager {

    private final VKCompactar plugin;
    
    private FileConfiguration config;
    private FileConfiguration itensConfig;
    
    // Configurações gerais
    private boolean pluginAtivado;
    private boolean protecaoRenomeacao;
    private boolean protecaoDuplicacao;
    private List<String> mundosPermitidos;
    private int delayCompactacao;
    
    // Configurações de feedback - Som
    private boolean somAtivado;
    private Sound tipoSom;
    private float somVolume;
    private float somPitch;
    
    // Configurações de feedback - Partículas
    private boolean particulasAtivado;
    private String tipoParticulas;
    private int particulasQuantidade;
    
    // Configurações de feedback - Título
    private boolean tituloAtivado;
    private String tituloTexto;
    private String subtituloTexto;
    private int tituloFadeIn;
    private int tituloStay;
    private int tituloFadeOut;
    
    // Configurações de feedback - ActionBar
    private boolean actionbarAtivado;
    private String actionbarMensagem;
    
    // Configurações de performance
    private boolean usarAsync;
    private int limitePorTick;
    private int cacheTempo;
    
    // Configurações do Auto Compactador Item
    private boolean autoCompactadorAtivado;
    private int autoCompactadorDelay;
    private String autoCompactadorMaterial;
    private String autoCompactadorNome;
    private List<String> autoCompactadorLore;
    
    // Configurações de proteção
    private boolean bloquearItensRenomeados;
    private boolean verificarNBT;
    private boolean logarExploits;
    
    // Itens bloqueados
    private List<String> itensBloqueados;

    public ConfigManager(VKCompactar plugin) {
        this.plugin = plugin;
    }

    /**
     * Carrega todas as configurações
     */
    public void loadConfigs() {
        // Salvar configs padrão se não existirem
        plugin.saveDefaultConfig();
        saveDefaultItensConfig();
        
        // Recarregar configurações
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Carregar itens.yml
        File itensFile = new File(plugin.getDataFolder(), "itens.yml");
        itensConfig = YamlConfiguration.loadConfiguration(itensFile);
        
        // Carregar valores
        loadGeneralSettings();
        loadFeedbackSettings();
        loadPerformanceSettings();
        loadAutoCompactadorSettings();
        loadProtectionSettings();
    }

    /**
     * Carrega configurações gerais
     */
    private void loadGeneralSettings() {
        pluginAtivado = config.getBoolean("geral.ativado", true);
        protecaoRenomeacao = config.getBoolean("geral.protecao-renomeacao", true);
        protecaoDuplicacao = config.getBoolean("geral.protecao-duplicacao", true);
        mundosPermitidos = config.getStringList("geral.mundos-permitidos");
        delayCompactacao = config.getInt("geral.delay-compactacao", 10);
    }

    /**
     * Carrega configurações de feedback
     */
    private void loadFeedbackSettings() {
        // Som
        somAtivado = config.getBoolean("feedback.som.ativado", true);
        String soundName = config.getString("feedback.som.tipo", "LEVEL_UP");
        try {
            tipoSom = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            tipoSom = Sound.LEVEL_UP;
            plugin.getLogger().warning("Som inválido: " + soundName + ". Usando LEVEL_UP.");
        }
        somVolume = (float) config.getDouble("feedback.som.volume", 1.0);
        somPitch = (float) config.getDouble("feedback.som.pitch", 1.0);
        
        // Partículas
        particulasAtivado = config.getBoolean("feedback.particulas.ativado", true);
        tipoParticulas = config.getString("feedback.particulas.tipo", "VILLAGER_HAPPY");
        particulasQuantidade = config.getInt("feedback.particulas.quantidade", 20);
        
        // Título
        tituloAtivado = config.getBoolean("feedback.titulo.ativado", true);
        tituloTexto = config.getString("feedback.titulo.titulo", "&a&lItem Compactado!");
        subtituloTexto = config.getString("feedback.titulo.subtitulo", "&7{item} x{quantidade}");
        tituloFadeIn = config.getInt("feedback.titulo.fade-in", 10);
        tituloStay = config.getInt("feedback.titulo.stay", 40);
        tituloFadeOut = config.getInt("feedback.titulo.fade-out", 10);
        
        // ActionBar
        actionbarAtivado = config.getBoolean("feedback.actionbar.ativado", true);
        actionbarMensagem = config.getString("feedback.actionbar.mensagem", "&a✔ &7Você compactou &a{quantidade}x {item}");
    }

    /**
     * Carrega configurações de performance
     */
    private void loadPerformanceSettings() {
        usarAsync = config.getBoolean("performance.usar-async", false);
        limitePorTick = config.getInt("performance.limite-por-tick", 100);
        cacheTempo = config.getInt("performance.cache-tempo", 300);
    }

    /**
     * Carrega configurações do Auto Compactador Item
     */
    private void loadAutoCompactadorSettings() {
        autoCompactadorAtivado = config.getBoolean("auto-compactador.ativado", true);
        autoCompactadorDelay = config.getInt("auto-compactador.delay-segundos", 5);
        autoCompactadorMaterial = config.getString("auto-compactador.material", "EMERALD");
        autoCompactadorNome = config.getString("auto-compactador.nome", "&a&lAuto Compactador");
        autoCompactadorLore = config.getStringList("auto-compactador.lore");
        
        if (autoCompactadorLore == null || autoCompactadorLore.isEmpty()) {
            autoCompactadorLore = new ArrayList<>();
            autoCompactadorLore.add("&7Tenha este item no inventário");
            autoCompactadorLore.add("&7para compactar automaticamente!");
            autoCompactadorLore.add("");
            autoCompactadorLore.add("&eDelay: &f" + autoCompactadorDelay + " segundos");
        }
    }

    /**
     * Carrega configurações de proteção
     */
    private void loadProtectionSettings() {
        bloquearItensRenomeados = config.getBoolean("protecao.bloquear-itens-renomeados", false);
        verificarNBT = config.getBoolean("protecao.verificar-nbt", true);
        logarExploits = config.getBoolean("protecao.logar-exploits", true);
        itensBloqueados = config.getStringList("itens-bloqueados");
        
        if (itensBloqueados == null) {
            itensBloqueados = new ArrayList<>();
        }
    }

    /**
     * Salva o arquivo itens.yml padrão
     */
    private void saveDefaultItensConfig() {
        File itensFile = new File(plugin.getDataFolder(), "itens.yml");
        if (!itensFile.exists()) {
            plugin.saveResource("itens.yml", false);
        }
    }

    // ============================================
    // GETTERS
    // ============================================

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getItensConfig() {
        return itensConfig;
    }

    public boolean isPluginAtivado() {
        return pluginAtivado;
    }

    public boolean isProtecaoRenomeacao() {
        return protecaoRenomeacao;
    }

    public boolean isProtecaoDuplicacao() {
        return protecaoDuplicacao;
    }

    public List<String> getMundosPermitidos() {
        return mundosPermitidos;
    }

    public int getDelayCompactacao() {
        return delayCompactacao;
    }

    public boolean isSomAtivado() {
        return somAtivado;
    }

    public Sound getTipoSom() {
        return tipoSom;
    }

    public float getSomVolume() {
        return somVolume;
    }

    public float getSomPitch() {
        return somPitch;
    }

    public boolean isParticulasAtivado() {
        return particulasAtivado;
    }

    public String getTipoParticulas() {
        return tipoParticulas;
    }

    public int getParticulasQuantidade() {
        return particulasQuantidade;
    }

    public boolean isTituloAtivado() {
        return tituloAtivado;
    }

    public String getTituloTexto() {
        return tituloTexto;
    }

    public String getSubtituloTexto() {
        return subtituloTexto;
    }

    public int getTituloFadeIn() {
        return tituloFadeIn;
    }

    public int getTituloStay() {
        return tituloStay;
    }

    public int getTituloFadeOut() {
        return tituloFadeOut;
    }

    public boolean isActionbarAtivado() {
        return actionbarAtivado;
    }

    public String getActionbarMensagem() {
        return actionbarMensagem;
    }

    public boolean isUsarAsync() {
        return usarAsync;
    }

    public int getLimitePorTick() {
        return limitePorTick;
    }

    public int getCacheTempo() {
        return cacheTempo;
    }

    public boolean isAutoCompactadorAtivado() {
        return autoCompactadorAtivado;
    }

    public int getAutoCompactadorDelay() {
        return autoCompactadorDelay;
    }

    public String getAutoCompactadorMaterial() {
        return autoCompactadorMaterial;
    }

    public String getAutoCompactadorNome() {
        return autoCompactadorNome;
    }

    public List<String> getAutoCompactadorLore() {
        return autoCompactadorLore;
    }

    public boolean isBloquearItensRenomeados() {
        return bloquearItensRenomeados;
    }

    public boolean isVerificarNBT() {
        return verificarNBT;
    }

    public boolean isLogarExploits() {
        return logarExploits;
    }

    public List<String> getItensBloqueados() {
        return itensBloqueados;
    }

    /**
     * Verifica se um mundo é permitido
     */
    public boolean isMundoPermitido(String mundo) {
        if (mundosPermitidos == null || mundosPermitidos.isEmpty()) {
            return true;
        }
        return mundosPermitidos.contains(mundo);
    }

    /**
     * Verifica se um item está bloqueado
     */
    public boolean isItemBloqueado(String material) {
        return itensBloqueados.contains(material);
    }
}
