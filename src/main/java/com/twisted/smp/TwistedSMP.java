package com.twisted.smp;

import com.twisted.smp.abilities.AbilityManager;
import com.twisted.smp.anti.AntiAbuseManager;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.DataManager;
import com.twisted.smp.core.DatabaseManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.crafting.RecipeManager;
import com.twisted.smp.events.RiftEvent;
import com.twisted.smp.events.TwistedStorm;
import com.twisted.smp.evolution.EvolutionManager;
import com.twisted.smp.energy.EssenceManager;
import com.twisted.smp.energy.InstabilityManager;
import com.twisted.smp.placeholders.TwistedSMPExpansion;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.twists.TwistManager;
import com.twisted.smp.commands.TwistCommand;
import com.twisted.smp.commands.TwistAdminCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TwistedSMP extends JavaPlugin {

    private static TwistedSMP instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DataManager dataManager;
    private TwistManager twistManager;
    private AbilityManager abilityManager;
    private EvolutionManager evolutionManager;
    private RecipeManager recipeManager;
    private TwistedStorm twistedStorm;
    private RiftEvent riftEvent;
    private AntiAbuseManager antiAbuseManager;
    private com.twisted.smp.energy.EnergyManager energyManager;
    private EssenceManager essenceManager;
    private InstabilityManager instabilityManager;
    private BukkitAudiences adventure;
    private com.twisted.smp.vfx.VFXManager vfxManager;
    private com.twisted.smp.listeners.PlayerListener playerListener;

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("TwistedSMP is starting...");

        adventure = BukkitAudiences.create(this);

        ConfigManager.ConfigLoadResult configResult = ConfigManager.load(this);
        if (!configResult.success()) {
            getLogger().severe("Failed to load configuration: " + configResult.error());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = configResult.config();

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        dataManager = new DataManager(this, databaseManager, configManager);
        twistManager = new TwistManager(this);
        vfxManager = new com.twisted.smp.vfx.VFXManager(this, configManager);
        abilityManager = new AbilityManager(this, configManager);
        evolutionManager = new EvolutionManager(this, configManager);
        energyManager = new com.twisted.smp.energy.EnergyManager(this, configManager);
        essenceManager = new EssenceManager(this, configManager);
        instabilityManager = new InstabilityManager(this, configManager);
        recipeManager = new RecipeManager(this, configManager);
        twistManager.loadTwistConfigs();
        recipeManager.registerRecipes();
        twistedStorm = new TwistedStorm(this, configManager, dataManager, energyManager, instabilityManager);
        riftEvent = new RiftEvent(this, configManager, dataManager);
        antiAbuseManager = new AntiAbuseManager(this, configManager, databaseManager);
        registerCommands();
        registerListeners();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TwistedSMPExpansion(this, dataManager, twistManager).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will be unavailable.");
        }

        for (Player online : getServer().getOnlinePlayers()) {
            dataManager.loadPlayerData(online.getUniqueId());
            antiAbuseManager.loadData(online.getUniqueId());
        }

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player online : getServer().getOnlinePlayers()) {
                PlayerData data = dataManager.getPlayerData(online.getUniqueId());
                if (data == null || !data.isTwistSelected()) continue;
                instabilityManager.tickInstability(data);
                if (data.getInstability() > 0) {
                    instabilityManager.applyInstabilityEffects(data);
                }
            }
            antiAbuseManager.tick();
        }, 20 * 60L, 20 * 60L);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player online : getServer().getOnlinePlayers()) {
                antiAbuseManager.addPlaytime(online.getUniqueId(), 1.0 / 60.0);
            }
        }, 20L, 20L);

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info("TwistedSMP enabled successfully in " + elapsed + "ms.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TwistedSMP is disabling...");

        if (twistedStorm != null && twistedStorm.isActive()) {
            twistedStorm.stop();
        }
        if (riftEvent != null && riftEvent.isActive()) {
            riftEvent.stop();
        }

        if (dataManager != null) {
            dataManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        if (adventure != null) {
            adventure.close();
        }

        instance = null;
        getLogger().info("TwistedSMP disabled.");
    }

    private void registerListeners() {
        playerListener = new com.twisted.smp.listeners.PlayerListener(this, dataManager, twistManager);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new com.twisted.smp.listeners.CombatListener(this, dataManager, twistManager, antiAbuseManager, configManager), this);
        getServer().getPluginManager().registerEvents(new com.twisted.smp.listeners.EntityListener(this, dataManager, twistManager, abilityManager), this);
        getServer().getPluginManager().registerEvents(new com.twisted.smp.listeners.WorldListener(this, configManager, twistManager, abilityManager), this);
    }

    private void registerCommands() {
        getCommand("twist").setExecutor(new TwistCommand(this));
        getCommand("twistadmin").setExecutor(new TwistAdminCommand(this));
    }

    public static TwistedSMP get() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public TwistManager getTwistManager() {
        return twistManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public com.twisted.smp.energy.EnergyManager getEnergyManager() {
        return energyManager;
    }

    public EssenceManager getEssenceManager() {
        return essenceManager;
    }

    public InstabilityManager getInstabilityManager() {
        return instabilityManager;
    }

    public EvolutionManager getEvolutionManager() {
        return evolutionManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public TwistedStorm getTwistedStorm() {
        return twistedStorm;
    }

    public RiftEvent getRiftEvent() {
        return riftEvent;
    }

    public com.twisted.smp.listeners.PlayerListener getPlayerListener() {
        return playerListener;
    }

    public AntiAbuseManager getAntiAbuseManager() {
        return antiAbuseManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public com.twisted.smp.vfx.VFXManager vfx() {
        return vfxManager;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }

    public Map<UUID, PlayerData> getPlayerDataCache() {
        return dataManager.getPlayerDataCache();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return dataManager.getPlayerData(uuid);
    }

    public PlayerData getPlayerData(Player player) {
        return dataManager.getPlayerData(player.getUniqueId());
    }

    public PlayerData getOrCreatePlayerData(UUID uuid) {
        return dataManager.getOrCreatePlayerData(uuid);
    }
}
