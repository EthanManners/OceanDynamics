package com.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Locale;

public final class ActionbarTask {
    private final OceanDynamics plugin;
    private final CurrentField currentField;
    private final WindManager windManager;
    private final BoatMotionListener motionListener;
    private BukkitTask task;

    public ActionbarTask(OceanDynamics plugin, CurrentField currentField, WindManager windManager, BoatMotionListener motionListener) {
        this.plugin = plugin;
        this.currentField = currentField;
        this.windManager = windManager;
        this.motionListener = motionListener;
    }

    public void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getVehicle() instanceof Boat boat)) {
                continue;
            }
            if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) {
                continue;
            }

            BoatMotionListener.CompassMode mode = motionListener.getCompassMode(player.getUniqueId());
            if (mode == BoatMotionListener.CompassMode.WIND) {
                Vector wind = windManager.getWindVector();
                String message = "WIND " + DirectionUtil.toCompass(wind) + " " + DirectionUtil.toDegrees(wind)
                        + "° | str " + fmt(wind.length());
                player.sendActionBar(Component.text(message));
            } else {
                Vector current = currentField.getCurrentVector(boat.getLocation().getX(), boat.getLocation().getZ());
                String message = "CURRENT " + DirectionUtil.toCompass(current) + " " + DirectionUtil.toDegrees(current)
                        + "° | mag " + fmt(current.length());
                player.sendActionBar(Component.text(message));
            }
        }
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
