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
    private BukkitTask task;

    private double strength;
    private double minStrength;
    private double maxStrength;
    private double gradualStepDeg;
    private double currentAngleDeg;
    private DirectionMode directionMode;

    public WindManager(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.minStrength = config.getDouble("windStrengthMin", 0.3);
        this.maxStrength = Math.max(minStrength, config.getDouble("windStrengthMax", 1.0));
        this.gradualStepDeg = Math.max(0.0, config.getDouble("windGradualStepDegrees", 22.5));

        String modeRaw = config.getString("windDirectionMode", "RANDOM");
        try {
            this.directionMode = DirectionMode.valueOf(modeRaw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            this.directionMode = DirectionMode.RANDOM;
        }

        if (strength <= 0.0) {
            this.currentAngleDeg = ThreadLocalRandom.current().nextDouble(0, 360);
            this.strength = randomStrength();
        }
    }

    public void start() {
        stop();
        long minutes = Math.max(1, plugin.getConfig().getLong("windChangeMinutes", 20));
        long ticks = minutes * 60L * 20L;

        rerollWind();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::rerollWind, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void rerollWind() {
        if (directionMode == DirectionMode.RANDOM) {
            this.currentAngleDeg = ThreadLocalRandom.current().nextDouble(0, 360);
        } else {
            double sign = ThreadLocalRandom.current().nextBoolean() ? 1.0 : -1.0;
            this.currentAngleDeg = normalizeAngle(currentAngleDeg + sign * gradualStepDeg);
        }
        this.strength = randomStrength();

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Wind updated: " + formatSummary());
        }
    }

    public Vector getDirectionUnit() {
        double radians = Math.toRadians(currentAngleDeg);
        return new Vector(Math.cos(radians), 0, Math.sin(radians)).normalize();
    }

    public Vector getWindVector() {
        return getDirectionUnit().multiply(strength);
    }

    public double getStrength() {
        return strength;
    }

    public double getAngleDeg() {
        return currentAngleDeg;
    }

    public String formatSummary() {
        Vector dir = getDirectionUnit();
        return DirectionUtil.toCompass(dir) + " (" + DirectionUtil.toDegrees(dir) + "°), strength=" + String.format("%.2f", strength);
    }

    private double randomStrength() {
        if (Math.abs(maxStrength - minStrength) < 1e-9) {
            return minStrength;
        }
        return ThreadLocalRandom.current().nextDouble(minStrength, maxStrength);
    }

    private double normalizeAngle(double value) {
        double out = value % 360.0;
        return out < 0 ? out + 360.0 : out;
    }
}
