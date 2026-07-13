package com.twisted.smp.commands;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.twists.TwistManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class TwistCommand implements CommandExecutor, TabCompleter {

    private final TwistedSMP plugin;
    private final TwistManager twistManager;
    private final com.twisted.smp.energy.EnergyManager energyManager;
    private final com.twisted.smp.evolution.EvolutionManager evolutionManager;
    private final com.twisted.smp.energy.EssenceManager essenceManager;

    public TwistCommand(TwistedSMP plugin) {
        this.plugin = plugin;
        this.twistManager = plugin.getTwistManager();
        this.energyManager = plugin.getEnergyManager();
        this.evolutionManager = plugin.getEvolutionManager();
        this.essenceManager = plugin.getEssenceManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stats" -> handleStats(sender, args);
            case "ability" -> handleAbility(sender);
            case "evolve" -> handleEvolve(sender);
            case "gui" -> handleGUI(sender);
            case "withdraw" -> handleWithdraw(sender, args);
            case "reroll" -> handleReroll(sender);
            default -> showHelp(sender);
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        Component message = Component.text("TwistedSMP Commands", NamedTextColor.GOLD)
            .append(Component.newline())
            .append(Component.text("/twist gui", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.text("Open twist selection GUI"))).append(Component.space()))
            .append(Component.text("/twist stats", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.text("View your stats"))).append(Component.space()))
            .append(Component.newline())
            .append(Component.text("/twist ability", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.text("Use your Twist ability"))))
            .append(Component.text("/twist evolve", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.text("Evolve your Twist"))))
            .append(Component.text("/twist reroll", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.text("Reroll your Twist for 100 Essence"))))
            .append(Component.text("/twist withdraw <amount>", NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.text("Convert essence to items"))));
        player.sendMessage(message);
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Players only."));
            return;
        }
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Data not loaded."));
            return;
        }
        Twist twist = data.getTwist();
        String twistName = twistManager.getTwistDisplayName(twist);
        String stageName = energyManager.getStageName(data.getEnergy());
        String energyColor = energyManager.getStageColor(data.getEnergy());

        Component twistNameComponent = MiniMessage.miniMessage().deserialize(twistName);
        Component header = Component.text("Twist Stats".toUpperCase(), NamedTextColor.GOLD);
        Component body = Component.empty()
            .append(Component.text("Twist: ", NamedTextColor.GRAY).append(twistNameComponent))
            .append(Component.newline())
            .append(statsLine("Stage", stageName, NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(statsLine("Energy", String.format("%.0f%%", data.getEnergy()), NamedTextColor.AQUA))
            .append(Component.newline())
            .append(statsLine("Essence", String.format("%.0f", data.getEssence()), NamedTextColor.LIGHT_PURPLE))
            .append(Component.newline())
            .append(statsLine("Instability", String.format("%.0f%%", data.getInstability()), NamedTextColor.RED))
            .append(Component.newline())
            .append(statsLine("Kills", String.valueOf(data.getKills()), NamedTextColor.GREEN))
            .append(Component.newline())
            .append(statsLine("Deaths", String.valueOf(data.getDeaths()), NamedTextColor.DARK_RED));

        player.sendMessage(header);
        player.sendMessage(body);
    }

    private Component statsLine(String label, String value, NamedTextColor color) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(Component.text(value, color));
    }

    private void handleAbility(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet! Use /twist gui"));
            return;
        }
        plugin.getAbilityManager().useAbility(player, data.getTwist());
    }

    private void handleEvolve(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet! Use /twist gui"));
            return;
        }
        boolean success = evolutionManager.evolve(data);
        if (!success) {
            var reqs = evolutionManager.getRequirements(data);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<red>Cannot evolve yet. Need: <gold>" + reqs.energyRequired() + "</gold> Energy and <dark_purple>" + reqs.essenceRequired() + "</dark_purple> Essence"));
        }
    }

    private void handleWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet! Use /twist gui"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twist withdraw <amount|all>"));
            return;
        }

        double amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = data.getEssence();
        } else {
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid amount."));
                return;
            }
        }

        if (amount <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Withdraw amount must be positive."));
            return;
        }

        if (data.getEssence() < amount) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                plugin.getConfigManager().getMessage("withdraw-fail", "amount", String.valueOf((int) amount))));
            return;
        }

        data.subtractEssence(amount);
        plugin.getDataManager().savePlayerData(data, true);

        int totalItems = (int) Math.floor(amount);
        int stacks = totalItems / 64;
        int remainder = totalItems % 64;

        for (int i = 0; i < stacks; i++) {
            org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 64);
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text("Twisted Essence", net.kyori.adventure.text.format.TextColor.color(0xa29bfe)));
                java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.text("Worth: 64 Essence", net.kyori.adventure.text.format.TextColor.color(0xbb86fc)));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            player.getInventory().addItem(stack);
        }
        if (remainder > 0) {
            org.bukkit.inventory.ItemStack remStack = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, remainder);
            org.bukkit.inventory.meta.ItemMeta meta = remStack.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text("Twisted Essence", net.kyori.adventure.text.format.TextColor.color(0xa29bfe)));
                java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.text("Worth: " + remainder + " Essence", net.kyori.adventure.text.format.TextColor.color(0xbb86fc)));
                meta.lore(lore);
                remStack.setItemMeta(meta);
            }
            player.getInventory().addItem(remStack);
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            plugin.getConfigManager().getMessage("withdraw-success", "amount", String.valueOf((int) amount))));
    }

    private void handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) return;
        if (data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have already selected a twist: " + twistManager.getTwistDisplayName(data.getTwist())));
            return;
        }
        twistManager.openTwistSelectionGUI(player);
    }

    private void handleReroll(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet!"));
            return;
        }

        double rerollCost = plugin.getConfigManager().getConfig().getDouble("essence.twist-change-cost", 100);
        if (data.getEssence() < rerollCost) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                plugin.getConfigManager().getMessage("essence-insufficient", "amount", String.valueOf((int) rerollCost), "have", String.valueOf((int) data.getEssence()))));
            return;
        }

        java.util.List<Twist> available = new java.util.ArrayList<>(Twist.getAllTwists());
        available.remove(data.getTwist());
        if (available.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No other twists available to reroll to."));
            return;
        }
        Twist newTwist = available.get(new java.util.Random().nextInt(available.size()));

        data.subtractEssence(rerollCost);
        data.setTwist(newTwist);
        data.setTwistSelected(true);
        data.writeToPersistentData(player.getPersistentDataContainer());
        plugin.getDataManager().savePlayerData(data, true);

        String newName = twistManager.getTwistDisplayName(newTwist);
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "<green>Rerolled! Your new twist is <white>" + newName + "</white>. Cost: <dark_purple>" + (int) rerollCost + " Essence</dark_purple>."));
        plugin.vfx().sounds().playTwistSelectSound(player);
        plugin.vfx().holograms().spawnTextHologram(player.getLocation().clone().add(0, 2.0, 0),
            "§d§lTWIST REROLL", 40, net.kyori.adventure.text.format.TextColor.color(0xa29bfe));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) {
            return Arrays.asList("stats", "ability", "evolve", "gui", "reroll", "withdraw").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
