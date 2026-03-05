package com.oceandynamics;

import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BoatMotionListener implements Listener {

    private final OceanDynamics plugin;
    private final Map<UUID, Double> lastSpeedByBoat = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastCellByBoat = new ConcurrentHashMap<>();

    public BoatMotionListener(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Entity vehicleEntity = event.getVehicle();
        if (!(vehicleEntity instanceof Boat boat)) {
            return;
        }

        if (!isDriverPlayer(boat)) {
            return;
        }

        if (!boat.isInWater()) {
            return;
        }

        Location location = boat.getLocation();
        Vector fwd = location.getDirection().setY(0);
        if (fwd.lengthSquared() < 1.0E-8) {
            return;
        }
        fwd.normalize();

        CurrentField currentField = plugin.getCurrentField();
        Vector current = currentField.getCurrentAt(location.getX(), location.getZ());
        Vector windDir = plugin.getWindManager().getWindDirection();
        double windStrength = plugin.getWindManager().getWindStrength();
        Vector wind = windDir.clone().multiply(windStrength);

        double currentAlign = 0.0;
        double currentMag = current.length();
        if (currentMag > 1.0E-8) {
            currentAlign = fwd.dot(current.clone().normalize());
        }

        double windAlign = fwd.dot(windDir);

        double speed = plugin.getConfig().getDouble("baseSpeed", 0.85);
        speed += plugin.getConfig().getDouble("currentPush", 0.55) * Math.max(0.0, currentAlign)
                * plugin.getConfig().getDouble("currentWithMultiplier", 2.0);
        speed += plugin.getConfig().getDouble("windPush", 0.18) * Math.max(0.0, windAlign)
                * plugin.getConfig().getDouble("windWithMultiplier", 1.4);

        speed -= plugin.getConfig().getDouble("currentAgainstPenalty", 0.45) * Math.max(0.0, -currentAlign);
        speed -= plugin.getConfig().getDouble("windAgainstPenalty", 0.18) * Math.max(0.0, -windAlign);

        speed = Math.max(plugin.getConfig().getDouble("minSpeed", 0.25), speed);
        speed = Math.min(plugin.getConfig().getDouble("maxSpeed", 1.90), speed);

        if (countPassengerPlayers(boat) >= 2) {
            speed *= plugin.getConfig().getDouble("crewBonusMultiplier", 1.15);
            speed = Math.min(plugin.getConfig().getDouble("maxSpeed", 1.90), speed);
        }

        double smoothing = clamp(plugin.getConfig().getDouble("smoothingFactor", 0.15), 0.0, 1.0);
        UUID boatId = boat.getUniqueId();
        if (smoothing > 0.0) {
            Double prev = lastSpeedByBoat.get(boatId);
            if (prev != null) {
                speed = lerp(prev, speed, smoothing);
            }
            lastSpeedByBoat.put(boatId, speed);
        }

        Vector drift = current.multiply(plugin.getConfig().getDouble("driftScaleCurrent", 0.0))
                .add(wind.multiply(plugin.getConfig().getDouble("driftScaleWind", 0.0)));

        Vector vel = boat.getVelocity();
        Vector newHorizontal = fwd.multiply(speed).add(new Vector(drift.getX(), 0, drift.getZ()));
        double maxHorizontal = plugin.getConfig().getDouble("maxHorizontalSpeed", 2.20);
        double horizLen = Math.sqrt(newHorizontal.getX() * newHorizontal.getX() + newHorizontal.getZ() * newHorizontal.getZ());
        if (horizLen > maxHorizontal && horizLen > 1.0E-8) {
            double scale = maxHorizontal / horizLen;
            newHorizontal.multiply(scale);
        }

        Vector newVel = newHorizontal.setY(vel.getY());
        boat.setVelocity(newVel);

        if (plugin.getConfig().getBoolean("debug", false)) {
            int cellX = currentField.getCellX(location.getX());
            int cellZ = currentField.getCellZ(location.getZ());
            lastCellByBoat.put(boatId, cellX + "," + cellZ);
        }
    }

    public void clearCachedState() {
        lastSpeedByBoat.clear();
        lastCellByBoat.clear();
    }

    public String getLastCellText(UUID boatId) {
        return lastCellByBoat.getOrDefault(boatId, "n/a");
    }

    private boolean isDriverPlayer(Boat boat) {
        if (boat.getPassengers().isEmpty()) {
            return false;
        }
        return boat.getPassengers().get(0) instanceof Player;
    }

    private int countPassengerPlayers(Boat boat) {
        int players = 0;
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Player) {
                players++;
            }
        }
        return players;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
