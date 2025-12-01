package com.vulcandev.compactar.utils;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Utilitário para manipulação de NBT em Minecraft 1.8.8
 * Usa reflection para maior compatibilidade com diferentes builds de Spigot/Paper
 * Possui fallback para armazenamento via lore caso NMS não funcione
 */
public class NBTUtils {

    private static final String VERSION;
    private static boolean nmsAvailable = false;
    
    // Cache de classes e métodos NMS
    private static Class<?> craftItemStackClass;
    private static Class<?> nmsItemStackClass;
    private static Class<?> nbtTagCompoundClass;
    private static Method asNMSCopyMethod;
    private static Method asBukkitCopyMethod;
    private static Method hasTagMethod;
    private static Method getTagMethod;
    private static Method setTagMethod;

    // Chave principal para identificar itens compactados
    private static final String COMPACTED_KEY = "VKCompacted";
    private static final String MATERIAL_KEY = "Material";
    private static final String QUANTITY_KEY = "Quantity";
    private static final String MULTIPLIER_KEY = "Multiplier";
    private static final String UNIQUE_ID_KEY = "UniqueId";
    private static final String TIMESTAMP_KEY = "Timestamp";
    private static final String HASH_KEY = "Hash";
    private static final String ULTRA_KEY = "Ultra";
    
    // Chave para Auto Compactador
    private static final String AUTO_COMPACTADOR_KEY = "VKAutoCompactador";
    
    // Prefixo para dados ocultos na lore (fallback)
    private static final String LORE_DATA_PREFIX = ChatColor.COLOR_CHAR + "k" + ChatColor.COLOR_CHAR + "c" + ChatColor.COLOR_CHAR + "d";

    static {
        // Obter versão do servidor
        String packageName = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
        
        try {
            initNMS();
            nmsAvailable = true;
        } catch (Exception e) {
            nmsAvailable = false;
        }
    }

    /**
     * Inicializa classes NMS via reflection
     */
    private static void initNMS() throws Exception {
        craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory.CraftItemStack");
        nmsItemStackClass = Class.forName("net.minecraft.server." + VERSION + ".ItemStack");
        nbtTagCompoundClass = Class.forName("net.minecraft.server." + VERSION + ".NBTTagCompound");
        
        asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
        
        hasTagMethod = nmsItemStackClass.getMethod("hasTag");
        getTagMethod = nmsItemStackClass.getMethod("getTag");
        setTagMethod = nmsItemStackClass.getMethod("setTag", nbtTagCompoundClass);
    }

    /**
     * Define os dados de compactação em um item
     * @param itemStack Item a ser modificado
     * @param materialName Nome do material original
     * @param quantity Quantidade base
     * @param multiplier Multiplicador
     * @param uniqueId ID único
     * @return ItemStack com NBT aplicado
     */
    public static ItemStack setCompactedData(ItemStack itemStack, String materialName, 
                                              int quantity, int multiplier, String uniqueId) {
        return setCompactedData(itemStack, materialName, quantity, multiplier, uniqueId, false);
    }
    
    /**
     * Define os dados de compactação em um item
     * @param itemStack Item a ser modificado
     * @param materialName Nome do material original
     * @param quantity Quantidade base
     * @param multiplier Multiplicador
     * @param uniqueId ID único
     * @param isUltra Se é um item ultra compactado
     * @return ItemStack com NBT aplicado
     */
    public static ItemStack setCompactedData(ItemStack itemStack, String materialName, 
                                              int quantity, int multiplier, String uniqueId, boolean isUltra) {
        if (nmsAvailable) {
            try {
                return setCompactedDataNMS(itemStack, materialName, quantity, multiplier, uniqueId, isUltra);
            } catch (Exception e) {
                // Fallback para lore
            }
        }
        
        return setCompactedDataLore(itemStack, materialName, quantity, multiplier, uniqueId, isUltra);
    }

    /**
     * Define dados via NMS
     */
    private static ItemStack setCompactedDataNMS(ItemStack itemStack, String materialName,
                                                   int quantity, int multiplier, String uniqueId, boolean isUltra) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        Object tag;
        
        if (hasTag) {
            tag = getTagMethod.invoke(nmsItem);
        } else {
            Constructor<?> constructor = nbtTagCompoundClass.getConstructor();
            tag = constructor.newInstance();
        }
        
        // Criar compound para dados
        Constructor<?> compoundConstructor = nbtTagCompoundClass.getConstructor();
        Object compactedData = compoundConstructor.newInstance();
        
        // Métodos para setar valores
        Method setString = nbtTagCompoundClass.getMethod("setString", String.class, String.class);
        Method setInt = nbtTagCompoundClass.getMethod("setInt", String.class, int.class);
        Method setLong = nbtTagCompoundClass.getMethod("setLong", String.class, long.class);
        Method setBoolean = nbtTagCompoundClass.getMethod("setBoolean", String.class, boolean.class);
        Method set = nbtTagCompoundClass.getMethod("set", String.class, 
            Class.forName("net.minecraft.server." + VERSION + ".NBTBase"));
        
        setString.invoke(compactedData, MATERIAL_KEY, materialName);
        setInt.invoke(compactedData, QUANTITY_KEY, quantity);
        setInt.invoke(compactedData, MULTIPLIER_KEY, multiplier);
        setString.invoke(compactedData, UNIQUE_ID_KEY, uniqueId);
        setLong.invoke(compactedData, TIMESTAMP_KEY, System.currentTimeMillis());
        setBoolean.invoke(compactedData, ULTRA_KEY, isUltra);
        
        // Hash
        String hashData = materialName + quantity + multiplier + uniqueId;
        setInt.invoke(compactedData, HASH_KEY, hashData.hashCode());
        
        set.invoke(tag, COMPACTED_KEY, compactedData);
        setTagMethod.invoke(nmsItem, tag);
        
        return (ItemStack) asBukkitCopyMethod.invoke(null, nmsItem);
    }

    /**
     * Define dados via lore (fallback)
     */
    private static ItemStack setCompactedDataLore(ItemStack itemStack, String materialName,
                                                    int quantity, int multiplier, String uniqueId, boolean isUltra) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Remover dados antigos se existirem
        lore.removeIf(line -> line.startsWith(LORE_DATA_PREFIX));
        
        // Codificar dados em Base64 (incluindo flag ultra)
        String data = materialName + "|" + quantity + "|" + multiplier + "|" + uniqueId + "|" + System.currentTimeMillis() + "|" + (isUltra ? "1" : "0");
        String hashData = materialName + quantity + multiplier + uniqueId;
        data += "|" + hashData.hashCode();
        
        String encoded = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        
        // Adicionar dados ocultos
        lore.add(LORE_DATA_PREFIX + encoded);
        
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        
        return itemStack;
    }

    /**
     * Verifica se um item possui a tag de compactação
     */
    public static boolean hasCompactedTag(ItemStack itemStack) {
        if (itemStack == null) return false;
        
        if (nmsAvailable) {
            try {
                return hasCompactedTagNMS(itemStack);
            } catch (Exception e) {
                // Fallback
            }
        }
        
        return hasCompactedTagLore(itemStack);
    }

    private static boolean hasCompactedTagNMS(ItemStack itemStack) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        if (nmsItem == null) return false;
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        if (!hasTag) return false;
        
        Object tag = getTagMethod.invoke(nmsItem);
        Method hasKeyMethod = nbtTagCompoundClass.getMethod("hasKey", String.class);
        return (boolean) hasKeyMethod.invoke(tag, COMPACTED_KEY);
    }

    private static boolean hasCompactedTagLore(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasLore()) return false;
        
        for (String line : meta.getLore()) {
            if (line.startsWith(LORE_DATA_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtém o nome do material compactado
     */
    public static String getCompactedMaterial(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                return getCompactedDataNMS(itemStack, MATERIAL_KEY, String.class);
            } catch (Exception e) {
                // Fallback
            }
        }
        return getDataFromLore(itemStack, 0);
    }

    /**
     * Obtém a quantidade base do item compactado
     */
    public static int getCompactedQuantity(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                Integer result = getCompactedDataNMS(itemStack, QUANTITY_KEY, Integer.class);
                return result != null ? result : -1;
            } catch (Exception e) {
                // Fallback
            }
        }
        String data = getDataFromLore(itemStack, 1);
        try {
            return data != null ? Integer.parseInt(data) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Obtém o multiplicador do item compactado
     */
    public static int getCompactedMultiplier(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                Integer result = getCompactedDataNMS(itemStack, MULTIPLIER_KEY, Integer.class);
                return result != null ? result : -1;
            } catch (Exception e) {
                // Fallback
            }
        }
        String data = getDataFromLore(itemStack, 2);
        try {
            return data != null ? Integer.parseInt(data) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Obtém o ID único do item compactado
     */
    public static String getUniqueId(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                return getCompactedDataNMS(itemStack, UNIQUE_ID_KEY, String.class);
            } catch (Exception e) {
                // Fallback
            }
        }
        return getDataFromLore(itemStack, 3);
    }
    
    /**
     * Verifica se o item é ultra compactado
     */
    public static boolean isUltraCompacted(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                Boolean result = getCompactedDataNMS(itemStack, ULTRA_KEY, Boolean.class);
                return result != null && result;
            } catch (Exception e) {
                // Fallback
            }
        }
        String data = getDataFromLore(itemStack, 5);
        return data != null && data.equals("1");
    }

    @SuppressWarnings("unchecked")
    private static <T> T getCompactedDataNMS(ItemStack itemStack, String key, Class<T> type) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        if (nmsItem == null) return null;
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        if (!hasTag) return null;
        
        Object tag = getTagMethod.invoke(nmsItem);
        Method hasKeyMethod = nbtTagCompoundClass.getMethod("hasKey", String.class);
        if (!(boolean) hasKeyMethod.invoke(tag, COMPACTED_KEY)) return null;
        
        Method getCompoundMethod = nbtTagCompoundClass.getMethod("getCompound", String.class);
        Object compactedData = getCompoundMethod.invoke(tag, COMPACTED_KEY);
        
        if (type == String.class) {
            Method getStringMethod = nbtTagCompoundClass.getMethod("getString", String.class);
            return (T) getStringMethod.invoke(compactedData, key);
        } else if (type == Integer.class) {
            Method getIntMethod = nbtTagCompoundClass.getMethod("getInt", String.class);
            return (T) getIntMethod.invoke(compactedData, key);
        } else if (type == Boolean.class) {
            Method getBooleanMethod = nbtTagCompoundClass.getMethod("getBoolean", String.class);
            return (T) getBooleanMethod.invoke(compactedData, key);
        }
        return null;
    }

    private static String getDataFromLore(ItemStack itemStack, int index) {
        String[] parts = parseDataFromLore(itemStack);
        return parts != null && parts.length > index ? parts[index] : null;
    }

    private static String[] parseDataFromLore(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasLore()) return null;
        
        for (String line : meta.getLore()) {
            if (line.startsWith(LORE_DATA_PREFIX)) {
                try {
                    String encoded = line.substring(LORE_DATA_PREFIX.length());
                    String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                    return decoded.split("\\|");
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Valida a integridade do item compactado
     */
    public static boolean validateCompactedItem(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                return validateCompactedItemNMS(itemStack);
            } catch (Exception e) {
                // Fallback
            }
        }
        return validateCompactedItemLore(itemStack);
    }

    private static boolean validateCompactedItemNMS(ItemStack itemStack) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        if (nmsItem == null) return false;
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        if (!hasTag) return false;
        
        Object tag = getTagMethod.invoke(nmsItem);
        Method hasKeyMethod = nbtTagCompoundClass.getMethod("hasKey", String.class);
        if (!(boolean) hasKeyMethod.invoke(tag, COMPACTED_KEY)) return false;
        
        Method getCompoundMethod = nbtTagCompoundClass.getMethod("getCompound", String.class);
        Object compactedData = getCompoundMethod.invoke(tag, COMPACTED_KEY);
        
        // Verificar campos
        String[] requiredKeys = {MATERIAL_KEY, QUANTITY_KEY, MULTIPLIER_KEY, UNIQUE_ID_KEY, HASH_KEY};
        for (String reqKey : requiredKeys) {
            if (!(boolean) hasKeyMethod.invoke(compactedData, reqKey)) return false;
        }
        
        // Verificar hash
        Method getStringMethod = nbtTagCompoundClass.getMethod("getString", String.class);
        Method getIntMethod = nbtTagCompoundClass.getMethod("getInt", String.class);
        
        String material = (String) getStringMethod.invoke(compactedData, MATERIAL_KEY);
        int quantity = (int) getIntMethod.invoke(compactedData, QUANTITY_KEY);
        int multiplier = (int) getIntMethod.invoke(compactedData, MULTIPLIER_KEY);
        String uniqueId = (String) getStringMethod.invoke(compactedData, UNIQUE_ID_KEY);
        int storedHash = (int) getIntMethod.invoke(compactedData, HASH_KEY);
        
        String hashData = material + quantity + multiplier + uniqueId;
        return storedHash == hashData.hashCode();
    }

    private static boolean validateCompactedItemLore(ItemStack itemStack) {
        String[] data = parseDataFromLore(itemStack);
        if (data == null || data.length < 6) return false;
        
        try {
            String material = data[0];
            int quantity = Integer.parseInt(data[1]);
            int multiplier = Integer.parseInt(data[2]);
            String uniqueId = data[3];
            int storedHash = Integer.parseInt(data[5]);
            
            String hashData = material + quantity + multiplier + uniqueId;
            return storedHash == hashData.hashCode();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Remove os dados de compactação de um item
     */
    public static ItemStack removeCompactedData(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                return removeCompactedDataNMS(itemStack);
            } catch (Exception e) {
                // Fallback
            }
        }
        return removeCompactedDataLore(itemStack);
    }

    private static ItemStack removeCompactedDataNMS(ItemStack itemStack) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        if (nmsItem == null) return itemStack;
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        if (!hasTag) return itemStack;
        
        Object tag = getTagMethod.invoke(nmsItem);
        Method removeMethod = nbtTagCompoundClass.getMethod("remove", String.class);
        removeMethod.invoke(tag, COMPACTED_KEY);
        setTagMethod.invoke(nmsItem, tag);
        
        return (ItemStack) asBukkitCopyMethod.invoke(null, nmsItem);
    }

    private static ItemStack removeCompactedDataLore(ItemStack itemStack) {
        if (!itemStack.hasItemMeta()) return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasLore()) return itemStack;
        
        List<String> lore = new ArrayList<>(meta.getLore());
        lore.removeIf(line -> line.startsWith(LORE_DATA_PREFIX));
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        
        return itemStack;
    }

    /**
     * Clona o item com novos dados de compactação
     */
    public static ItemStack cloneWithNewId(ItemStack itemStack) {
        String material = getCompactedMaterial(itemStack);
        int quantity = getCompactedQuantity(itemStack);
        int multiplier = getCompactedMultiplier(itemStack);
        
        if (material == null || quantity <= 0 || multiplier <= 0) {
            return itemStack.clone();
        }
        
        String newId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return setCompactedData(itemStack.clone(), material, quantity, multiplier, newId);
    }
    
    // ══════════════════════════════════════
    //         AUTO COMPACTADOR
    // ══════════════════════════════════════
    
    /**
     * Define a tag de Auto Compactador em um item
     */
    public static ItemStack setAutoCompactadorTag(ItemStack itemStack) {
        if (nmsAvailable) {
            try {
                return setAutoCompactadorTagNMS(itemStack);
            } catch (Exception e) {
                // Fallback - a identificação será feita pela lore
            }
        }
        return itemStack;
    }
    
    private static ItemStack setAutoCompactadorTagNMS(ItemStack itemStack) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        Object tag;
        
        if (hasTag) {
            tag = getTagMethod.invoke(nmsItem);
        } else {
            java.lang.reflect.Constructor<?> constructor = nbtTagCompoundClass.getConstructor();
            tag = constructor.newInstance();
        }
        
        Method setBoolean = nbtTagCompoundClass.getMethod("setBoolean", String.class, boolean.class);
        setBoolean.invoke(tag, AUTO_COMPACTADOR_KEY, true);
        
        setTagMethod.invoke(nmsItem, tag);
        
        return (ItemStack) asBukkitCopyMethod.invoke(null, nmsItem);
    }
    
    /**
     * Verifica se um item possui a tag de Auto Compactador
     */
    public static boolean hasAutoCompactadorTag(ItemStack itemStack) {
        if (itemStack == null) return false;
        
        if (nmsAvailable) {
            try {
                return hasAutoCompactadorTagNMS(itemStack);
            } catch (Exception e) {
                // Fallback
            }
        }
        
        return false;
    }
    
    private static boolean hasAutoCompactadorTagNMS(ItemStack itemStack) throws Exception {
        Object nmsItem = asNMSCopyMethod.invoke(null, itemStack);
        if (nmsItem == null) return false;
        
        boolean hasTag = (boolean) hasTagMethod.invoke(nmsItem);
        if (!hasTag) return false;
        
        Object tag = getTagMethod.invoke(nmsItem);
        Method hasKeyMethod = nbtTagCompoundClass.getMethod("hasKey", String.class);
        return (boolean) hasKeyMethod.invoke(tag, AUTO_COMPACTADOR_KEY);
    }
}
