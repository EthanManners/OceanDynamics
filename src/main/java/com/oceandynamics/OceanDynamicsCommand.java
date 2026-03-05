package com.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OceanDynamicsCommand implements CommandExecutor, TabCompleter {

    private final OceanDynamics plugin;
    private final CompassListener compassListener;

    public OceanDynamicsCommand(OceanDynamics plugin, CompassListener compassListener) {
        this.plugin = plugin;
        this.compassListener = compassListener;
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
                    sender.sendMessage(Component.text("You do not have permission."));
                    return true;
                }
                plugin.reloadSystems();
                sender.sendMessage(Component.text("OceanDynamics config and systems reloaded."));
                return true;
            }
            case "wind" -> {
                Vector wind = plugin.getWindManager().getWindVector();
                double magnitude = Math.sqrt(wind.getX() * wind.getX() + wind.getZ() * wind.getZ());
                double deg = toCompassDegrees(wind);
                sender.sendMessage(Component.text(String.format("Wind: %s (%.0f°), strength %.2f", toCompassPoint(deg), deg, magnitude)));
                return true;
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can toggle debug."));
                    return true;
                }
                boolean enabled = compassListener.toggleDebug(player.getUniqueId());
                sender.sendMessage(Component.text("OceanDynamics debug " + (enabled ? "enabled" : "disabled") + "."));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand. Use /oceandynamics <reload|wind|debug>"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String candidate : List.of("reload", "wind", "debug")) {
                if (candidate.startsWith(prefix)) {
                    out.add(candidate);
                }
            }
            return out;
        }
        return List.of();
    }

    private static final String[] SIXTEEN_WIND = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    };

    private static double toCompassDegrees(Vector vector) {
        double raw = Math.toDegrees(Math.atan2(vector.getX(), -vector.getZ()));
        if (raw < 0) {
            raw += 360.0;
        }
        return raw;
    }

    private static String toCompassPoint(double degrees) {
        int index = (int) Math.round(degrees / 22.5) % 16;
        return SIXTEEN_WIND[index];
    }
}
