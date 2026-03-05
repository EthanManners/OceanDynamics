package com.oceandynamics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public final class WindManager {

    public enum DirectionMode {
        RANDOM,
        GRADUAL
    }

    private final OceanDynamics plugin;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private Vector windDirection = new Vector(1, 0, 0);
    private double windStrength = 0.5;
    private DirectionMode directionMode = DirectionMode.RANDOM;

    private BukkitTask task;

    public WindManager(OceanDynamics plugin) {
        this.plugin = plugin;
        applyConfigAndRollInitial();
    }

    public void start() {
        FileConfiguration config = plugin.getConfig();
        long minutes = Math.max(1L, config.getLong("windChangeMinutes", 20));
        long ticks = minutes * 60L * 20L;

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateWind, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public synchronized Vector getWindDirection() {
        return windDirection.clone();
    }

    public synchronized double getWindStrength() {
        return windStrength;
    }

    public synchronized Vector getWindVector() {
        return windDirection.clone().multiply(windStrength);
    }

    private void applyConfigAndRollInitial() {
        FileConfiguration config = plugin.getConfig();
        this.directionMode = parseDirectionMode(config.getString("windDirectionMode", "RANDOM"));
        updateWind();
    }

    public synchronized void updateWind() {
        FileConfiguration config = plugin.getConfig();

        boolean fixed = config.getBoolean("windFixedStrength.enabled", false);
        if (fixed) {
            windStrength = Math.max(0.0, config.getDouble("windFixedStrength.value", 0.5));
        } else {
            double min = config.getDouble("windStrengthMin", 0.3);
            double max = config.getDouble("windStrengthMax", 1.0);
            if (max < min) {
                double tmp = min;
                min = max;
                max = tmp;
            }
            windStrength = random.nextDouble(min, max + 1.0E-9);
        }

        if (directionMode == DirectionMode.GRADUAL) {
            rotateGradually();
        } else {
            windDirection = randomUnitDirection();
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format("Wind updated: dir=(%.3f, %.3f) strength=%.3f", windDirection.getX(), windDirection.getZ(), windStrength));
        }
    }

    private void rotateGradually() {
        double currentAngle = Math.atan2(windDirection.getZ(), windDirection.getX());
        double delta = Math.toRadians(random.nextDouble(-30.0, 30.0));
        double next = currentAngle + delta;
        windDirection = new Vector(Math.cos(next), 0, Math.sin(next)).normalize();
    }

    private Vector randomUnitDirection() {
        double angle = random.nextDouble(0, Math.PI * 2.0);
        return new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();
    }

    private DirectionMode parseDirectionMode(String raw) {
        if (raw == null) {
            return DirectionMode.RANDOM;
        }
        try {
            return DirectionMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown windDirectionMode '" + raw + "', defaulting to RANDOM.");
            return DirectionMode.RANDOM;
        }
    }
}
