package com.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OceanDynamicsCommand implements CommandExecutor, TabCompleter {
    private final OceanDynamics plugin;
    private final CurrentField currentField;
    private final WindManager windManager;
    private final BoatMotionListener boatMotionListener;

    public OceanDynamicsCommand(OceanDynamics plugin,
                                CurrentField currentField,
                                WindManager windManager,
                                BoatMotionListener boatMotionListener) {
        this.plugin = plugin;
        this.currentField = currentField;
        this.windManager = windManager;
        this.boatMotionListener = boatMotionListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/oceandynamics <reload|wind|debug>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("oceandynamics.admin")) {
                    sender.sendMessage(Component.text("No permission."));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(Component.text("OceanDynamics config and systems reloaded."));
                return true;
            }
            case "wind" -> {
                sender.sendMessage(Component.text("Wind: " + windManager.formatSummary()));
                return true;
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this."));
                    return true;
                }
                boolean state = boatMotionListener.toggleDebug(player.getUniqueId());
                player.sendMessage(Component.text("OceanDynamics debug " + (state ? "enabled" : "disabled") + "."));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand."));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "wind", "debug");
        }
        return new ArrayList<>();
    }
}
