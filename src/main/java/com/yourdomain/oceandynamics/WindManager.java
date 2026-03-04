package com.yourdomain.oceandynamics;

import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

public final class WindManager {
    private final OceanDynamics plugin;
    private BukkitTask task;
    private Vector2 direction = new Vector2(1, 0);
    private double strength = 0.5;

    public WindManager(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    public void start(OceanDynamicsConfig cfg) {
        stop();
        reroll(cfg);
        long intervalTicks = Math.max(20L, cfg.windChangeMinutes * 60L * 20L);
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> reroll(plugin.getPluginConfig()), intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reroll(OceanDynamicsConfig cfg) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double angle = r.nextDouble(0, Math.PI * 2);
        this.direction = new Vector2(Math.cos(angle), Math.sin(angle)).normalize();
        if (cfg.windFixedStrength) {
            this.strength = cfg.windFixedValue;
        } else {
            this.strength = r.nextDouble(cfg.windStrengthMin, cfg.windStrengthMax + 1.0E-9);
        }
    }

    public Vector2 getWindVector() {
        OceanDynamicsConfig cfg = plugin.getPluginConfig();
        return direction.multiply(strength * cfg.windPush);
    }

    public Vector2 getDirection() {
        return direction;
    }

    public double getStrength() {
        return strength;
    }
}
