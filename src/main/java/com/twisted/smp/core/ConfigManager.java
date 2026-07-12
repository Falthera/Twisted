package com.twisted.smp.core;

import com.twisted.smp.TwistedSMP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class ConfigManager {

    private final TwistedSMP plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private final Map<String, FileConfiguration> twistConfigs = new java.util.HashMap<>();
    private final Map<String, FileConfiguration> eventConfigs = new java.util.HashMap<>();

    private ConfigManager(TwistedSMP plugin) {
        this.plugin = plugin;
    }

    public static ConfigLoadResult load(TwistedSMP plugin) {
        ConfigManager manager = new ConfigManager(plugin);
        try {
            manager.loadAll();
            return new ConfigLoadResult(manager, null);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            return new ConfigLoadResult(null, e.getMessage());
        }
    }

    private void loadAll() throws IOException {
        saveDefault("config.yml");
        saveDefault("messages.yml");

        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        File twistsDir = new File(plugin.getDataFolder(), "twists");
        if (!twistsDir.exists()) {
            twistsDir.mkdirs();
        }
        for (String twistId : new String[]{"voidwalker", "titan", "berserker", "phantom", "infernal", "frostborn"}) {
            File twistFile = new File(twistsDir, twistId + ".yml");
            if (!twistFile.exists()) {
                saveDefault("twists/" + twistId + ".yml", twistFile);
            }
            FileConfiguration twistConfig = YamlConfiguration.loadConfiguration(twistFile);
            twistConfigs.put(twistId, twistConfig);
        }

        File eventsDir = new File(plugin.getDataFolder(), "events");
        if (!eventsDir.exists()) {
            eventsDir.mkdirs();
        }
        for (String eventId : new String[]{"rift", "storm"}) {
            File eventFile = new File(eventsDir, eventId + ".yml");
            if (!eventFile.exists()) {
                saveDefault("events/" + eventId + ".yml", eventFile);
            }
            FileConfiguration eventConfig = YamlConfiguration.loadConfiguration(eventFile);
            eventConfigs.put(eventId, eventConfig);
        }
    }

    private void saveDefault(String resourcePath) throws IOException {
        saveDefault(resourcePath, new File(plugin.getDataFolder(), resourcePath));
    }

    private void saveDefault(String resourcePath, File target) throws IOException {
        if (!target.exists() || target.length() == 0) {
            target.getParentFile().mkdirs();
            try (InputStream input = plugin.getResource(resourcePath)) {
                if (input != null) {
                    Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public void reload() throws IOException {
        loadAll();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getTwistConfig(String twistId) {
        return twistConfigs.getOrDefault(twistId.toLowerCase(), YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "twists/" + twistId.toLowerCase() + ".yml")));
    }

    public FileConfiguration getEventConfig(String eventId) {
        return eventConfigs.getOrDefault(eventId.toLowerCase(), YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "events/" + eventId.toLowerCase() + ".yml")));
    }

    public Map<String, FileConfiguration> getAllTwistConfigs() {
        return java.util.Collections.unmodifiableMap(twistConfigs);
    }

    public String getMessage(String path) {
        String msg = messages.getString(path, path);
        return msg.replace("%prefix%", messages.getString("prefix", "<gray>[TwistedSMP]"));
    }

    public String getMessage(String path, Object... replacements) {
        String msg = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace("%" + replacements[i] + "%", String.valueOf(replacements[i + 1]));
            }
        }
        return msg;
    }

    public record ConfigLoadResult(ConfigManager config, String error) {
        public boolean success() {
            return error == null;
        }
    }
}
