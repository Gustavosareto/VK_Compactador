package com.vulcandev.compactar.utils;

import com.vulcandev.compactar.VKCompactar;
import com.vulcandev.compactar.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Utilitário para feedback visual e sonoro
 * Usa reflection para compatibilidade com diferentes versões
 */
public class FeedbackUtils {

    private static final String VERSION;
    private static boolean nmsAvailable = false;
    
    // Cache de classes e métodos
    private static Class<?> craftPlayerClass;
    private static Class<?> entityPlayerClass;
    private static Class<?> playerConnectionClass;
    private static Class<?> packetClass;
    private static Class<?> chatSerializerClass;
    private static Class<?> iChatBaseComponentClass;
    private static Class<?> packetPlayOutChatClass;
    private static Class<?> packetPlayOutTitleClass;
    private static Class<?> enumTitleActionClass;
    private static Method getHandleMethod;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
        
        try {
            initNMS();
            nmsAvailable = true;
        } catch (Exception e) {
            nmsAvailable = false;
        }
    }

    private static void initNMS() throws Exception {
        craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer");
        entityPlayerClass = Class.forName("net.minecraft.server." + VERSION + ".EntityPlayer");
        playerConnectionClass = Class.forName("net.minecraft.server." + VERSION + ".PlayerConnection");
        packetClass = Class.forName("net.minecraft.server." + VERSION + ".Packet");
        iChatBaseComponentClass = Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent");
        packetPlayOutChatClass = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutChat");
        packetPlayOutTitleClass = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutTitle");
        
        // Chat Serializer
        try {
            chatSerializerClass = Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent$ChatSerializer");
        } catch (ClassNotFoundException e) {
            chatSerializerClass = Class.forName("net.minecraft.server." + VERSION + ".ChatSerializer");
        }
        
        // Enum Title Action
        enumTitleActionClass = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutTitle$EnumTitleAction");
        
        getHandleMethod = craftPlayerClass.getMethod("getHandle");
    }

    /**
     * Envia feedback completo de compactação (som, partículas, título, actionbar)
     */
    public static void sendCompactFeedback(Player player) {
        sendCompactFeedback(player, "Itens", 0);
    }
    
    /**
     * Envia feedback completo de compactação (som, partículas, título, actionbar)
     */
    public static void sendCompactFeedback(Player player, String itemName, int quantidade) {
        VKCompactar plugin = VKCompactar.getInstance();
        ConfigManager config = plugin.getConfigManager();

        // Som
        if (config.isSomAtivado()) {
            try {
                player.playSound(player.getLocation(), config.getTipoSom(), 
                    config.getSomVolume(), config.getSomPitch());
            } catch (Exception e) {
                // Ignorar erros de som
            }
        }

        // Partículas
        if (config.isParticulasAtivado()) {
            try {
                spawnParticles(player.getLocation(), config.getTipoParticulas(), 
                    config.getParticulasQuantidade());
            } catch (Exception e) {
                // Ignorar erros de partículas
            }
        }

        // Título
        if (config.isTituloAtivado()) {
            String titulo = config.getTituloTexto()
                .replace("{item}", itemName)
                .replace("{quantidade}", String.valueOf(quantidade));
            String subtitulo = config.getSubtituloTexto()
                .replace("{item}", itemName)
                .replace("{quantidade}", String.valueOf(quantidade));
            
            sendTitle(player, titulo, subtitulo, 
                config.getTituloFadeIn(), config.getTituloStay(), config.getTituloFadeOut());
        }

        // ActionBar
        if (config.isActionbarAtivado()) {
            String mensagem = config.getActionbarMensagem()
                .replace("{item}", itemName)
                .replace("{quantidade}", String.valueOf(quantidade));
            
            sendActionBar(player, mensagem);
        }
    }

    /**
     * Envia feedback silencioso para compactação automática (apenas actionbar)
     */
    public static void sendAutoCompactFeedback(Player player, String itemName, int quantidade) {
        VKCompactar plugin = VKCompactar.getInstance();
        ConfigManager config = plugin.getConfigManager();

        // Apenas ActionBar para compactação automática
        if (config.isActionbarAtivado()) {
            String mensagem = config.getActionbarMensagem()
                .replace("{item}", itemName)
                .replace("{quantidade}", String.valueOf(quantidade));
            
            sendActionBar(player, mensagem);
        }
    }

    /**
     * Envia título para o jogador usando Reflection
     */
    public static void sendTitle(Player player, String title, String subtitle, 
                                  int fadeIn, int stay, int fadeOut) {
        if (!nmsAvailable) {
            // Fallback
            player.sendMessage(ColorUtils.colorize(title));
            if (subtitle != null && !subtitle.isEmpty()) {
                player.sendMessage(ColorUtils.colorize(subtitle));
            }
            return;
        }
        
        try {
            Object entityPlayer = getHandleMethod.invoke(craftPlayerClass.cast(player));
            Object playerConnection = entityPlayerClass.getField("playerConnection").get(entityPlayer);
            Method sendPacket = playerConnectionClass.getMethod("sendPacket", packetClass);
            
            // Método para criar componente de chat
            Method chatSerializerA = chatSerializerClass.getMethod("a", String.class);
            
            // Obter enum actions
            Object titleAction = null;
            Object subtitleAction = null;
            for (Object enumConstant : enumTitleActionClass.getEnumConstants()) {
                if (enumConstant.toString().equals("TITLE")) titleAction = enumConstant;
                if (enumConstant.toString().equals("SUBTITLE")) subtitleAction = enumConstant;
            }
            
            // Constructor para título
            Constructor<?> titleConstructor = packetPlayOutTitleClass.getConstructor(
                enumTitleActionClass, iChatBaseComponentClass);
            Constructor<?> timesConstructor = packetPlayOutTitleClass.getConstructor(
                int.class, int.class, int.class);
            
            // Criar componentes
            Object titleComponent = chatSerializerA.invoke(null, 
                "{\"text\":\"" + ColorUtils.colorize(title).replace("\"", "\\\"") + "\"}");
            Object subtitleComponent = chatSerializerA.invoke(null, 
                "{\"text\":\"" + ColorUtils.colorize(subtitle).replace("\"", "\\\"") + "\"}");
            
            // Criar pacotes
            Object timesPacket = timesConstructor.newInstance(fadeIn, stay, fadeOut);
            Object titlePacket = titleConstructor.newInstance(titleAction, titleComponent);
            Object subtitlePacket = titleConstructor.newInstance(subtitleAction, subtitleComponent);
            
            // Enviar pacotes
            sendPacket.invoke(playerConnection, timesPacket);
            sendPacket.invoke(playerConnection, titlePacket);
            sendPacket.invoke(playerConnection, subtitlePacket);
            
        } catch (Exception e) {
            // Fallback: usar mensagem normal
            player.sendMessage(ColorUtils.colorize(title));
            if (subtitle != null && !subtitle.isEmpty()) {
                player.sendMessage(ColorUtils.colorize(subtitle));
            }
        }
    }

    /**
     * Envia ActionBar para o jogador usando Reflection
     */
    public static void sendActionBar(Player player, String message) {
        if (!nmsAvailable) {
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            Object entityPlayer = getHandleMethod.invoke(craftPlayerClass.cast(player));
            Object playerConnection = entityPlayerClass.getField("playerConnection").get(entityPlayer);
            Method sendPacket = playerConnectionClass.getMethod("sendPacket", packetClass);
            
            // Criar componente de chat
            Method chatSerializerA = chatSerializerClass.getMethod("a", String.class);
            Object chatComponent = chatSerializerA.invoke(null, 
                "{\"text\":\"" + ColorUtils.colorize(message).replace("\"", "\\\"") + "\"}");
            
            // Criar pacote - byte 2 = action bar
            Constructor<?> chatConstructor = packetPlayOutChatClass.getConstructor(
                iChatBaseComponentClass, byte.class);
            Object packet = chatConstructor.newInstance(chatComponent, (byte) 2);
            
            // Enviar pacote
            sendPacket.invoke(playerConnection, packet);
            
        } catch (Exception e) {
            // Fallback: usar mensagem normal
            player.sendMessage(ColorUtils.colorize(message));
        }
    }

    /**
     * Spawna partículas na localização
     */
    private static void spawnParticles(Location location, String particleType, int quantidade) {
        try {
            Effect effect;
            try {
                effect = Effect.valueOf(particleType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Fallback para efeito padrão em 1.8
                effect = Effect.HAPPY_VILLAGER;
            }

            for (int i = 0; i < quantidade; i++) {
                double offsetX = (Math.random() - 0.5) * 0.5;
                double offsetY = Math.random() * 0.5;
                double offsetZ = (Math.random() - 0.5) * 0.5;
                
                Location particleLocation = location.clone().add(offsetX, offsetY + 0.5, offsetZ);
                location.getWorld().playEffect(particleLocation, effect, 0);
            }
        } catch (Exception e) {
            // Ignorar erros de partículas
        }
    }
}
