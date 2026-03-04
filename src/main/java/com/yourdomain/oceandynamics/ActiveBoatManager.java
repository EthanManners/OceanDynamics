package com.yourdomain.oceandynamics;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveBoatManager {
    private final Map<UUID, ActiveBoatEntry> activeBoats = new ConcurrentHashMap<>();

    public void markActive(Vehicle boat) {
        if (boat == null) {
            return;
        }
        activeBoats.compute(boat.getUniqueId(), (id, existing) -> {
            if (existing == null) {
                return new ActiveBoatEntry(boat);
            }
            existing.lastSeenMovingMillis = System.currentTimeMillis();
            existing.vehicle = boat;
            return existing;
        });
    }

    public void onPlayerExit(Vehicle boat) {
        ActiveBoatEntry entry = activeBoats.get(boat.getUniqueId());
        if (entry != null) {
            entry.lastDriverExitMillis = System.currentTimeMillis();
        }
    }

    public Iterable<ActiveBoatEntry> snapshot() {
        return new ArrayList<>(activeBoats.values());
    }

    public void prune(long timeoutMillis, double minSpeed) {
        long now = System.currentTimeMillis();
        activeBoats.entrySet().removeIf(e -> shouldRemove(e.getValue(), now, timeoutMillis, minSpeed));
    }

    private boolean shouldRemove(ActiveBoatEntry entry, long now, long timeoutMillis, double minSpeed) {
        Vehicle vehicle = entry.vehicle;
        if (vehicle == null || !vehicle.isValid() || vehicle.isDead()) {
            return true;
        }
        Player driver = getDriver(vehicle);
        Vector vel = vehicle.getVelocity();
        double speed = Math.hypot(vel.getX(), vel.getZ());
        if (driver != null || speed > minSpeed) {
            entry.lastSeenMovingMillis = now;
            return false;
        }
        return (now - Math.max(entry.lastSeenMovingMillis, entry.lastDriverExitMillis)) > timeoutMillis;
    }

    public static Player getDriver(Vehicle vehicle) {
        Entity passenger = vehicle.getPassengers().isEmpty() ? null : vehicle.getPassengers().get(0);
        if (passenger instanceof Player player) {
            return player;
        }
        return null;
    }

    public static final class ActiveBoatEntry {
        private Vehicle vehicle;
        private long lastSeenMovingMillis;
        private long lastDriverExitMillis;

        private ActiveBoatEntry(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.lastSeenMovingMillis = System.currentTimeMillis();
            this.lastDriverExitMillis = 0;
        }

        public Vehicle vehicle() {
            return vehicle;
        }
    }
}
