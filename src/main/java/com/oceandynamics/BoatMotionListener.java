package com.oceandynamics;

import org.bukkit.ChatColor;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BoatMotionListener implements Listener {
    public enum CompassMode {
        WIND,
        CURRENT
    }

    private final OceanDynamics plugin;
    private final CurrentField currentField;
    private final WindManager windManager;

    private final Map<UUID, CompassMode> compassModes = new HashMap<>();
    private final Map<UUID, Boolean> debugPlayers = new HashMap<>();
    private final Map<UUID, Double> lastSpeed = new HashMap<>();

    public BoatMotionListener(OceanDynamics plugin, CurrentField currentField, WindManager windManager) {
        this.plugin = plugin;
        this.currentField = currentField;
        this.windManager = windManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) {
            return;
        }
        if (!boat.isInWater()) {
            clearBoatCache(boat.getUniqueId());
            return;
        }

        Player driver = getDriver(boat);
        if (driver == null) {
            clearBoatCache(boat.getUniqueId());
            return;
        }
        if (!isDriverActivelyPropelling(boat)) {
            clearBoatCache(boat.getUniqueId());
            return;
        }

        Vector fwd = boat.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 1e-9) {
            return;
        }
        fwd.normalize();

        Vector current = currentField.getCurrentVector(boat.getLocation().getX(), boat.getLocation().getZ());
        Vector windVec = windManager.getWindVector();

        double currentAlign = alignment(fwd, current);
        double windAlign = alignment(fwd, windVec);

        double baseSpeed = plugin.getConfig().getDouble("baseSpeed", 0.85);
        double minSpeed = plugin.getConfig().getDouble("minSpeed", 0.25);
        double maxSpeed = plugin.getConfig().getDouble("maxSpeed", 2.2);

        double speed = baseSpeed;
        speed += plugin.getConfig().getDouble("currentPush", 0.75)
                * Math.max(0.0, currentAlign)
                * plugin.getConfig().getDouble("currentWithMultiplier", 2.2);
        speed += plugin.getConfig().getDouble("windPush", 0.35)
                * Math.max(0.0, windAlign)
                * plugin.getConfig().getDouble("windWithMultiplier", 1.5);

        speed -= plugin.getConfig().getDouble("currentAgainstPenalty", 0.9) * Math.max(0.0, -currentAlign);
        speed -= plugin.getConfig().getDouble("windAgainstPenalty", 0.35) * Math.max(0.0, -windAlign);

        speed = clamp(speed, minSpeed, maxSpeed);

        if (countPlayerPassengers(boat) >= 2) {
            speed *= plugin.getConfig().getDouble("crewBonusMultiplier", 1.15);
        }

        speed = clamp(speed, minSpeed, maxSpeed);

        double smoothingFactor = clamp(plugin.getConfig().getDouble("smoothingFactor", 0.15), 0.0, 1.0);
        UUID boatId = boat.getUniqueId();
        if (smoothingFactor > 0.0) {
            double prev = lastSpeed.getOrDefault(boatId, speed);
            speed = lerp(prev, speed, smoothingFactor);
        }
        lastSpeed.put(boatId, speed);

        Vector drift = current.clone().multiply(plugin.getConfig().getDouble("driftScaleCurrent", 0.0))
                .add(windVec.clone().multiply(plugin.getConfig().getDouble("driftScaleWind", 0.0)));

        Vector currentVelocity = boat.getVelocity();
        Vector horizontal = fwd.multiply(speed).add(drift).setY(0);

        double maxHorizontal = plugin.getConfig().getDouble("maxHorizontalSpeed", maxSpeed);
        if (horizontal.length() > maxHorizontal) {
            horizontal.normalize().multiply(maxHorizontal);
        }

        Vector newVelocity = horizontal.setY(currentVelocity.getY());
        boat.setVelocity(newVelocity);

        if (isDebugEnabled(driver.getUniqueId())) {
            driver.sendActionBar(ChatColor.AQUA + "spd=" + fmt(speed)
                    + ChatColor.GRAY + " curAlign=" + fmt(currentAlign)
                    + ChatColor.GRAY + " windAlign=" + fmt(windAlign));
        }
    }

    public CompassMode toggleCompassMode(UUID uuid) {
        CompassMode next = getCompassMode(uuid) == CompassMode.WIND ? CompassMode.CURRENT : CompassMode.WIND;
        compassModes.put(uuid, next);
        return next;
    }

    public CompassMode getCompassMode(UUID uuid) {
        return compassModes.getOrDefault(uuid, CompassMode.WIND);
    }

    public boolean toggleDebug(UUID uuid) {
        boolean next = !debugPlayers.getOrDefault(uuid, false);
        debugPlayers.put(uuid, next);
        return next;
    }

    public boolean isDebugEnabled(UUID uuid) {
        return debugPlayers.getOrDefault(uuid, false);
    }

    private Player getDriver(Boat boat) {
        if (boat.getPassengers().isEmpty()) {
            return null;
        }
        Entity first = boat.getPassengers().get(0);
        return first instanceof Player player ? player : null;
    }

    private int countPlayerPassengers(Boat boat) {
        int count = 0;
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Player) {
                count++;
            }
        }
        return count;
    }

    private void clearBoatCache(UUID boatId) {
        lastSpeed.remove(boatId);
    }

    private boolean isDriverActivelyPropelling(Boat boat) {
        double activationMinVanillaSpeed = plugin.getConfig().getDouble("activationMinVanillaSpeed", 0.04);
        Vector vel = boat.getVelocity().clone().setY(0);
        return vel.lengthSquared() >= activationMinVanillaSpeed * activationMinVanillaSpeed;
    }

    private double alignment(Vector forward, Vector effect) {
        Vector flat = effect.clone().setY(0);
        if (flat.lengthSquared() < 1e-9) {
            return 0.0;
        }
        return forward.dot(flat.normalize());
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double lerp(double a, double b, double t) {
        return a + ((b - a) * t);
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
