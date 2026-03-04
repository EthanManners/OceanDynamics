package com.yourdomain.oceandynamics;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.Locale;

public final class OceanDynamicsCommand implements CommandExecutor {
    private final OceanDynamics plugin;

    public OceanDynamicsCommand(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "wind";
        switch (sub) {
            case "wind" -> {
                Vector2 d = plugin.getWindManager().getDirection();
                sender.sendMessage(String.format("Wind: %s (%.0f°), strength %.3f", d.toCardinal(), d.toDegrees(), plugin.getWindManager().getStrength()));
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("oceandynamics.admin")) {
                    sender.sendMessage("No permission.");
                    return true;
                }
                plugin.reloadPluginConfig();
                sender.sendMessage("OceanDynamics config reloaded.");
                return true;
            }
            case "debug" -> {
                if (!sender.hasPermission("oceandynamics.admin")) {
                    sender.sendMessage("No permission.");
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use debug.");
                    return true;
                }
                Entity vehicle = p.getVehicle();
                if (!(vehicle instanceof Vehicle boat) || !plugin.isSupportedBoat(boat)) {
                    sender.sendMessage("You are not in a supported boat/raft.");
                    return true;
                }
                OceanDynamicsConfig cfg = plugin.getPluginConfig();
                OceanDynamicsConfig.CellIndex cell = cfg.getCellIndex(boat.getLocation().getX(), boat.getLocation().getZ());
                Vector2 current = cfg.getCurrentAt(boat.getLocation().getX(), boat.getLocation().getZ());
                Vector2 wind = plugin.getWindManager().getDirection().multiply(plugin.getWindManager().getStrength());
                sender.sendMessage(String.format("Cell x=%d z=%d | Current=(%.3f, %.3f) | Wind=(%.3f, %.3f)", cell.x(), cell.z(), current.x(), current.z(), wind.x(), wind.z()));
                return true;
            }
            default -> {
                sender.sendMessage("Usage: /oceandynamics <wind|reload|debug>");
                return true;
            }
        }
    }
}
