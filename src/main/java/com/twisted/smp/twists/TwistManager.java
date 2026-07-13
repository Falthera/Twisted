package com.twisted.smp.twists;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.vfx.HologramManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class TwistManager {

    private final TwistedSMP plugin;
    private final Map<String, org.bukkit.configuration.ConfigurationSection> twistConfigs = new HashMap<>();

    public TwistManager(TwistedSMP plugin) {
        this.plugin = plugin;
    }

    public void loadTwistConfigs() {
        twistConfigs.clear();
        for (Twist twist : Twist.getAllTwists()) {
            twistConfigs.put(twist.configId(), plugin.getConfigManager().getTwistConfig(twist.configId()));
            loadEvolutionDataFromConfig(twist);
        }
        plugin.getLogger().info("Loaded " + twistConfigs.size() + " twist configurations.");
    }

    private void loadEvolutionDataFromConfig(Twist twist) {
        org.bukkit.configuration.ConfigurationSection config = twistConfigs.get(twist.configId());
        if (config == null) return;

        for (int stage = 1; stage <= 3; stage++) {
            String sectionPath = "evolution.stage-" + stage;
            if (config.contains(sectionPath)) {
                org.bukkit.configuration.ConfigurationSection evoSec = config.getConfigurationSection(sectionPath);
                if (evoSec != null) {
                    twist.setEvolutionStageName(stage, evoSec.getString("name", twist.displayName()));
                    twist.setEvolutionEnergyReq(stage, evoSec.getInt("energy-required", 0));
                    twist.setEvolutionEssenceReq(stage, evoSec.getInt("essence-required", 0));
                }
            }
        }
    }

    public org.bukkit.configuration.ConfigurationSection getTwistConfig(String twistId) {
        return twistConfigs.get(twistId.toLowerCase());
    }

    public org.bukkit.configuration.ConfigurationSection getTwistConfig(Twist twist) {
        return twistConfigs.get(twist.configId());
    }

    public String getTwistDisplayName(Twist twist) {
        org.bukkit.configuration.ConfigurationSection config = twistConfigs.get(twist.configId());
        if (config != null) {
            return format(config.getString("display-name", twist.displayName()));
        }
        return twist.displayName();
    }

    public String getTwistDescription(Twist twist) {
        org.bukkit.configuration.ConfigurationSection config = twistConfigs.get(twist.configId());
        return config != null ? config.getString("description", "") : "";
    }

    public org.bukkit.Material getTwistGuiMaterial(Twist twist) {
        org.bukkit.configuration.ConfigurationSection config = twistConfigs.get(twist.configId());
        if (config != null && config.contains("material")) {
            try {
                return org.bukkit.Material.valueOf(config.getString("material"));
            } catch (IllegalArgumentException e) {
                // fallback
            }
        }
        return twist.guiMaterial();
    }

    public java.util.List<String> getSelectionLore(Twist twist) {
        org.bukkit.configuration.ConfigurationSection config = twistConfigs.get(twist.configId());
        java.util.List<String> lore = new ArrayList<>();
        lore.add("<gray>Passives:");
        if (config != null && config.contains("passive")) {
            org.bukkit.configuration.ConfigurationSection passive = config.getConfigurationSection("passive");
            if (passive != null) {
                for (String key : passive.getKeys(false)) {
                    lore.add(" <gray>- <white>" + key.replace("-", " ") + "</white>");
                }
            }
        }
        lore.add(" ");
        lore.add(getTwistDescription(twist));
        return lore;
    }

    public org.bukkit.inventory.ItemStack createTwistMenuItem(Twist twist) {
        org.bukkit.configuration.ConfigurationSection config = twistConfigs.get(twist.configId());
        org.bukkit.Material mat = getTwistGuiMaterial(twist);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = getTwistDisplayName(twist);
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(displayName));
            meta.lore(getSelectionLore(twist).stream()
                .map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(s))
                .toList()
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public String format(String input) {
        if (input == null || input.isEmpty()) return "";
        try {
            net.kyori.adventure.text.Component parsed = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(input);
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(parsed);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse MiniMessage in twist display name: " + input + " (" + e.getMessage() + ")");
            return input.replaceAll("<[^>]+>", "");
        }
    }

    public void openTwistSelectionGUI(org.bukkit.entity.Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        java.util.List<Twist> choices = java.util.Collections.emptyList();
        if (!data.isTwistSelected()) {
            choices = Twist.getRandomTwists(Twist.getAllTwists().size());
        }

        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, "§5Select Your Twist");
        for (int i = 0; i < choices.size() && i < 9; i++) {
            Twist twist = choices.get(i);
            org.bukkit.inventory.ItemStack item = createTwistMenuItem(twist);
            item.editMeta(meta -> {
                if (meta instanceof org.bukkit.inventory.meta.ItemMeta itemMeta) {
                    java.util.List<net.kyori.adventure.text.Component> lore = itemMeta.lore();
                    if (lore != null) {
                        java.util.ArrayList<net.kyori.adventure.text.Component> newLore = new ArrayList<>(lore);
                        newLore.add(net.kyori.adventure.text.Component.text(" "));
                        newLore.add(net.kyori.adventure.text.Component.text("§a§lClick to lock in!"));
                        itemMeta.lore(newLore);
                    }
                }
            });
            inv.setItem(i * 2 + 1, item);
        }

        player.openInventory(inv);

        HologramManager holo = plugin.vfx().holograms();
        ScreenShake shake = plugin.vfx().shake();
        SoundDesigner sounds = plugin.vfx().sounds();
        Location loc = player.getLocation();

        shake.shake(player, ScreenShake.Intensity.LIGHT);
        sounds.playTwistSelectSound(player);
        holo.spawnTextHologram(loc.clone().add(0, 2.6, 0), "§5§lCHOOSE YOUR TWIST", 50, ParticlePatterns.Color.VOID.toAdventure());
        holo.spawnOrbitalText(loc, "§f", 2, 60);

        for (int i = 0; i < 30; i++) {
            double angle = (2 * Math.PI / 30) * i;
            double r = 2.0;
            Location l = loc.clone().add(Math.cos(angle) * r, 1.0, Math.sin(angle) * r);
            l.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, l, 3, 0.1, 0.1, 0.1, 0.04);
            l.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, l, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    public boolean handleTwistSelection(org.bukkit.entity.Player player, Twist twist) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || data.isTwistSelected()) {
            return false;
        }
        data.setTwist(twist);
        data.setTwistSelected(true);
        data.writeToPersistentData(player.getPersistentDataContainer());

        HologramManager holo = plugin.vfx().holograms();
        ScreenShake shake = plugin.vfx().shake();
        SoundDesigner sounds = plugin.vfx().sounds();
        Location loc = player.getLocation();

        ParticlePatterns.explosion(loc.clone().add(0, 0.8, 0), ParticlePatterns.Color.VOID, 2.5f);
        shake.shakeWorld(player.getWorld(), ScreenShake.Intensity.MEDIUM);
        sounds.playTwistSelectSound(player);

        net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("Twist Chosen")
            .color(net.kyori.adventure.text.format.TextColor.color(0xa29bfe));
        net.kyori.adventure.text.Component subtitle = net.kyori.adventure.text.Component.text("Your destiny awaits...")
            .color(net.kyori.adventure.text.format.TextColor.color(0xffffff));
        player.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(200),
            Duration.ofMillis(600), Duration.ofMillis(200))));

        String twistName = getTwistDisplayName(twist);
        holo.spawnTextHologram(loc.clone().add(0, 2.6, 0), "§d§l" + twistName, 45, ParticlePatterns.Color.RIFT.toAdventure());
        holo.spawnTextHologram(loc.clone().add(0, 1.8, 0), "§f§lLOCKED IN", 35, ParticlePatterns.Color.EVOLUTION.toAdventure());
        holo.spawnOrbitalText(loc, "§a", 3, 65);
        loc.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 40, 1.5, 1.5, 1.5, 0.05);
        loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.clone().add(0, 1, 0), 25, 1.0, 2.0, 1.0, 0.02);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 2.0f);

        plugin.getDataManager().savePlayerData(data, true);
        return true;
    }
}
