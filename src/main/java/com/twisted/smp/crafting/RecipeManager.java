package com.twisted.smp.crafting;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RecipeManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final Set<NamespacedKey> registeredKeys = new HashSet<>();

    public RecipeManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void registerRecipes() {
        if (!configManager.getConfig().getBoolean("recipes.enabled", true)) return;
        registerTwistedCore();
        registerStabilityCrystal();
        registerEssenceExtractor();
        registerRiftKey();
        plugin.getLogger().info("Registered " + registeredKeys.size() + " custom crafting recipes.");
    }

    private void registerRecipe(NamespacedKey key, Recipe recipe) {
        if (registeredKeys.contains(key)) return;
        plugin.getServer().addRecipe(recipe);
        registeredKeys.add(key);
    }

    private void registerTwistedCore() {
        NamespacedKey key = new NamespacedKey(plugin, "twisted_core");
        ShapedRecipe recipe = new ShapedRecipe(key, createTwistedCoreItem());
        recipe.shape("DBE", "NSN", "DBE");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('B', Material.NETHERITE_BLOCK);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);
        registerRecipe(key, recipe);
    }

    private void registerStabilityCrystal() {
        NamespacedKey key = new NamespacedKey(plugin, "stability_crystal");
        ShapedRecipe recipe = new ShapedRecipe(key, createStabilityCrystalItem());
        recipe.shape("AEA", "ENE", "AEA");
        recipe.setIngredient('A', Material.AMETHYST_BLOCK);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);
        registerRecipe(key, recipe);
    }

    private void registerEssenceExtractor() {
        NamespacedKey key = new NamespacedKey(plugin, "essence_extractor");
        ShapedRecipe recipe = new ShapedRecipe(key, createEssenceExtractorItem());
        recipe.shape("SDS", "DBD", "SDS");
        recipe.setIngredient('S', Material.NETHERITE_SCRAP);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('B', Material.BEACON);
        registerRecipe(key, recipe);
    }

    private void registerRiftKey() {
        NamespacedKey key = new NamespacedKey(plugin, "rift_key");
        ShapedRecipe recipe = new ShapedRecipe(key, createRiftKeyItem());
        recipe.shape("EAE", "ENE", "EAE");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('A', Material.ENDER_EYE);
        recipe.setIngredient('N', Material.NETHER_STAR);
        registerRecipe(key, recipe);
    }

    private ItemStack createTwistedCoreItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Twisted Core")
            .color(net.kyori.adventure.text.format.TextColor.color(0xa29bfe)));
        meta.lore(List.of(
            net.kyori.adventure.text.Component.text("Change your Twist for 100 Essence.")
                .color(net.kyori.adventure.text.format.TextColor.color(0xaaaaaa))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStabilityCrystalItem() {
        ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Stability Crystal")
            .color(net.kyori.adventure.text.format.TextColor.color(0x74b9ff)));
        meta.lore(List.of(
            net.kyori.adventure.text.Component.text("Reduces Instability by 50%.")
                .color(net.kyori.adventure.text.format.TextColor.color(0xaaaaaa))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEssenceExtractorItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Essence Extractor")
            .color(net.kyori.adventure.text.format.TextColor.color(0xfdcb6e)));
        meta.lore(List.of(
            net.kyori.adventure.text.Component.text("Use this to extract Essence from ores.")
                .color(net.kyori.adventure.text.format.TextColor.color(0xaaaaaa))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRiftKeyItem() {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Rift Key")
            .color(net.kyori.adventure.text.format.TextColor.color(0xff6b6b)));
        meta.lore(List.of(
            net.kyori.adventure.text.Component.text("Use to start a Rift Event.")
                .color(net.kyori.adventure.text.format.TextColor.color(0xaaaaaa))
        ));
        item.setItemMeta(meta);
        return item;
    }
}
