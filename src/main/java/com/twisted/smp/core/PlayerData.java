package com.twisted.smp.core;

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData implements Cloneable {

    public static final NamespacedKey ENERGY_KEY = new NamespacedKey("twistedsmp", "twist_energy");
    public static final NamespacedKey ESSENCE_KEY = new NamespacedKey("twistedsmp", "twist_essence");
    public static final NamespacedKey STAGE_KEY = new NamespacedKey("twistedsmp", "twist_stage");
    public static final NamespacedKey TWIST_KEY = new NamespacedKey("twistedsmp", "twist_type");

    private final UUID uuid;
    private com.twisted.smp.twists.Twist twist;
    private double energy;
    private double essence;
    private double instability;
    private int evolutionStage;
    private int kills;
    private int deaths;
    private final Map<String, Long> abilityCooldowns;
    private boolean twistSelected;
    private final long firstJoin;
    private double maxEnergy;

    public PlayerData(UUID uuid, com.twisted.smp.twists.Twist twist, double energy, double essence,
                      double instability, int evolutionStage, int kills, int deaths,
                      Map<String, Long> abilityCooldowns, boolean twistSelected, long firstJoin) {
        this(uuid, twist, energy, essence, instability, evolutionStage, kills, deaths, abilityCooldowns, twistSelected, firstJoin, 200.0);
    }

    public PlayerData(UUID uuid, com.twisted.smp.twists.Twist twist, double energy, double essence,
                      double instability, int evolutionStage, int kills, int deaths,
                      Map<String, Long> abilityCooldowns, boolean twistSelected, long firstJoin, double maxEnergy) {
        this.uuid = Objects.requireNonNull(uuid);
        this.twist = twist;
        this.energy = clampEnergy(energy, maxEnergy);
        this.essence = Math.max(0, essence);
        this.instability = clampInstability(instability);
        this.evolutionStage = evolutionStage;
        this.kills = kills;
        this.deaths = deaths;
        this.abilityCooldowns = new ConcurrentHashMap<>(abilityCooldowns);
        this.twistSelected = twistSelected;
        this.firstJoin = firstJoin;
    }

    private static double clampEnergy(double energy, double maxEnergy) {
        return Math.max(0, Math.min(maxEnergy, energy));
    }

    private static double clampInstability(double instability) {
        return Math.max(0, Math.min(100, instability));
    }

    public UUID getUuid() { return uuid; }
    public com.twisted.smp.twists.Twist getTwist() { return twist; }
    public double getEnergy() { return energy; }
    public double getEssence() { return essence; }
    public double getInstability() { return instability; }
    public int getEvolutionStage() { return evolutionStage; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public Map<String, Long> getAbilityCooldowns() { return Collections.unmodifiableMap(abilityCooldowns); }
    public boolean isTwistSelected() { return twistSelected; }
    public long getFirstJoin() { return firstJoin; }
    public double getMaxEnergy() { return maxEnergy; }

    public void setTwist(com.twisted.smp.twists.Twist twist) {
        this.twist = twist;
    }

    public void setEnergy(double energy) {
        this.energy = clampEnergy(energy, maxEnergy);
    }

    public void addEnergy(double amount) {
        this.energy = clampEnergy(this.energy + amount, maxEnergy);
    }

    public void subtractEnergy(double amount) {
        this.energy = clampEnergy(this.energy - Math.abs(amount), maxEnergy);
    }

    public void setEssence(double essence) {
        this.essence = Math.max(0, essence);
    }

    public void addEssence(double amount) {
        this.essence += Math.max(0, amount);
    }

    public void subtractEssence(double amount) {
        this.essence = Math.max(0, this.essence - Math.abs(amount));
    }

    public void setInstability(double instability) {
        this.instability = clampInstability(instability);
    }

    public void addInstability(double amount) {
        this.instability = clampInstability(this.instability + amount);
    }

    public void subtractInstability(double amount) {
        this.instability = Math.max(0, this.instability - Math.abs(amount));
    }

    public void setEvolutionStage(int stage) {
        this.evolutionStage = Math.max(1, Math.min(3, stage));
    }

    public void addKill() {
        this.kills++;
    }

    public void addDeath() {
        this.deaths++;
    }

    public void putCooldown(String ability, long endTime) {
        abilityCooldowns.put(ability, endTime);
    }

    public void removeCooldown(String ability) {
        abilityCooldowns.remove(ability);
    }

    public long getCooldownRemaining(String ability) {
        Long endTime = abilityCooldowns.get(ability);
        if (endTime == null) return 0;
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public boolean isOnCooldown(String ability) {
        Long endTime = abilityCooldowns.get(ability);
        return endTime != null && endTime > System.currentTimeMillis();
    }

    public void purgeExpiredCooldowns() {
        long now = System.currentTimeMillis();
        abilityCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    public void setTwistSelected(boolean selected) {
        this.twistSelected = selected;
    }

    public String getEnergyStageName() {
        double e = this.energy;
        if (e <= 50) return "Dormant";
        if (e <= 100) return "Awakened";
        if (e <= 150) return "Enhanced";
        if (e < 200) return "Corrupted";
        return "Ascended";
    }

    public String getFormattedEnergy() {
        return String.format("%.0f%%", energy);
    }

    public void setMaxEnergy(double maxEnergy) {
        this.maxEnergy = maxEnergy;
        this.energy = clampEnergy(energy, maxEnergy);
    }

    public PlayerData snapshot() {
        Map<String, Long> cd = new ConcurrentHashMap<>(abilityCooldowns);
        PlayerData copy = new PlayerData(uuid, twist, energy, essence, instability, evolutionStage, kills, deaths, cd, twistSelected, firstJoin, maxEnergy);
        return copy;
    }

    public void writeToPersistentData(PersistentDataContainer container) {
        container.set(TWIST_KEY, PersistentDataType.STRING, twist.name());
        container.set(ENERGY_KEY, PersistentDataType.DOUBLE, energy);
        container.set(ESSENCE_KEY, PersistentDataType.DOUBLE, essence);
        container.set(STAGE_KEY, PersistentDataType.INTEGER, evolutionStage);
    }
}
