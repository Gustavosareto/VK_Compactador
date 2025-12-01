package com.vulcandev.compactar.models;

import org.bukkit.Material;

import java.util.List;

/**
 * Modelo que representa um item compactável
 * Suporta compactação normal e ultra compactação
 */
public class CompactableItem {

    private final String key;
    private final Material material;
    private final int quantidade;
    private final String nome;
    private final List<String> lore;
    private final short data;
    
    // Ultra compactado
    private final String nomeUltra;
    private final List<String> loreUltra;

    public CompactableItem(String key, Material material, int quantidade, String nome, List<String> lore, short data) {
        this(key, material, quantidade, nome, lore, data, null, null);
    }
    
    public CompactableItem(String key, Material material, int quantidade, String nome, List<String> lore, short data,
                           String nomeUltra, List<String> loreUltra) {
        this.key = key;
        this.material = material;
        this.quantidade = quantidade;
        this.nome = nome;
        this.lore = lore;
        this.data = data;
        this.nomeUltra = nomeUltra;
        this.loreUltra = loreUltra;
    }

    /**
     * Obtém a chave/identificador do item
     */
    public String getKey() {
        return key;
    }

    /**
     * Obtém o material do item
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Obtém a quantidade necessária para compactar
     */
    public int getQuantidade() {
        return quantidade;
    }

    /**
     * Obtém o nome personalizado do item compactado
     */
    public String getNome() {
        return nome;
    }

    /**
     * Obtém a lore (descrição) do item compactado
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * Obtém o data value do item
     */
    public short getData() {
        return data;
    }
    
    /**
     * Obtém o nome do item ultra compactado
     */
    public String getNomeUltra() {
        return nomeUltra;
    }
    
    /**
     * Obtém a lore do item ultra compactado
     */
    public List<String> getLoreUltra() {
        return loreUltra;
    }
    
    /**
     * Verifica se o item tem configuração de ultra compactado
     */
    public boolean hasUltraConfig() {
        return nomeUltra != null && !nomeUltra.isEmpty();
    }

    @Override
    public String toString() {
        return "CompactableItem{" +
                "key='" + key + '\'' +
                ", material=" + material +
                ", quantidade=" + quantidade +
                ", nome='" + nome + '\'' +
                ", data=" + data +
                '}';
    }
}
