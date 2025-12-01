package com.vulcandev.compactar.utils;

import org.bukkit.ChatColor;

/**
 * Utilitário para manipulação de cores
 */
public class ColorUtils {

    /**
     * Converte códigos de cor & para códigos do Minecraft
     * @param text Texto com códigos de cor
     * @return Texto colorido
     */
    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Remove todas as cores de um texto
     * @param text Texto com cores
     * @return Texto sem cores
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    /**
     * Converte códigos de cor do Minecraft para &
     * @param text Texto com códigos do Minecraft
     * @return Texto com códigos &
     */
    public static String decolorize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(ChatColor.COLOR_CHAR, '&');
    }
}
