package com.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ActionbarTask implements Runnable {

    private static final String[] SIXTEEN_WIND = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    };

    private final OceanDynamics plugin;
    private final CompassListener compassListener;
    private final BoatMotionListener boatMotionListener;

    public ActionbarTask(OceanDynamics plugin, CompassListener compassListener, BoatMotionListener boatMotionListener) {
        this.plugin = plugin;
        this.compassListener = compassListener;
        this.boatMotionListener = boatMotionListener;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getVehicle() instanceof Boat boat)) {
                continue;
            }

            if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) {
                continue;
            }

            CompassListener.CompassMode mode = compassListener.getMode(player.getUniqueId());
            String text;
            if (mode == CompassListener.CompassMode.WIND) {
                Vector wind = plugin.getWindManager().getWindVector();
                text = formatWindText(wind);
            } else {
                Vector current = plugin.getCurrentField().getCurrentAt(boat.getLocation().getX(), boat.getLocation().getZ());
                text = formatCurrentText(current);
            }

            if (compassListener.isDebugEnabled(player.getUniqueId())) {
                text += " | cell=" + boatMotionListener.getLastCellText(boat.getUniqueId());
            }

            player.sendActionBar(Component.text(text));
        }
    }

    private String formatWindText(Vector wind) {
        double mag = horizontalMagnitude(wind);
        double deg = toCompassDegrees(wind);
        return String.format("WIND %s (%.0f°) | strength %.2f", toCompassPoint(deg), deg, mag);
    }

    private String formatCurrentText(Vector current) {
        double mag = horizontalMagnitude(current);
        if (mag < 1.0E-8) {
            return "CURRENT Calm | magnitude 0.00";
        }
        double deg = toCompassDegrees(current);
        return String.format("CURRENT %s (%.0f°) | magnitude %.2f", toCompassPoint(deg), deg, mag);
    }

    private static double horizontalMagnitude(Vector v) {
        return Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
    }

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
