package com.twisted.smp.twists;

import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.TwistedSMP;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;

import java.util.*;

public enum Twist {

    VOID("voidwalker", "Voidwalker", Material.ENDER_PEARL),
    TITAN("titan", "Titan", Material.SHIELD),
    BERSERKER("berserker", "Berserker", Material.IRON_SWORD),
    PHANTOM("phantom", "Phantom", Material.FEATHER),
    INFERNAL("infernal", "Infernal", Material.FIRE_CHARGE),
    FROSTBORN("frostborn", "Frostborn", Material.ICE);

    private final String configId;
    private final String displayName;
    private final Material guiMaterial;
    private final Map<Integer, String> evolutionNames = new HashMap<>();
    private final Map<Integer, Integer> evolutionEnergyReqs = new HashMap<>();
    private final Map<Integer, Integer> evolutionEssenceReqs = new HashMap<>();

    Twist(String configId, String displayName, Material guiMaterial) {
        this.configId = configId;
        this.displayName = displayName;
        this.guiMaterial = guiMaterial;
    }

    public String configId() { return configId; }
    public String displayName() { return displayName; }
    public Material guiMaterial() { return guiMaterial; }

    public void setEvolutionStageName(int stage, String name) {
        evolutionNames.put(stage, name);
    }
    public void setEvolutionEnergyReq(int stage, int req) {
        evolutionEnergyReqs.put(stage, req);
    }
    public void setEvolutionEssenceReq(int stage, int req) {
        evolutionEssenceReqs.put(stage, req);
    }

    public String getEvolutionName(int stage) {
        return evolutionNames.getOrDefault(stage, displayName);
    }
    public int getEvolutionEnergyReq(int stage) {
        return evolutionEnergyReqs.getOrDefault(stage, 0);
    }
    public int getEvolutionEssenceReq(int stage) {
        return evolutionEssenceReqs.getOrDefault(stage, 0);
    }

    public String getFormattedName() {
        return displayName();
    }

    public static Twist fromConfigId(String configId) {
        for (Twist twist : values()) {
            if (twist.configId.equalsIgnoreCase(configId)) {
                return twist;
            }
        }
        return VOID;
    }

    public static List<Twist> getAllTwists() {
        return Arrays.asList(values());
    }

    public static List<Twist> getRandomTwists(int amount) {
        List<Twist> all = new ArrayList<>(Arrays.asList(values()));
        java.util.Collections.shuffle(all);
        return all.subList(0, Math.min(amount, all.size()));
    }
}
