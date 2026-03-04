package com.yourdomain.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import java.util.List;

public final class OceanPhysicsTask implements Runnable {
    private static final double ALIGNMENT_TOLERANCE_DEGREES = 10.0;
    private static final double ALIGNMENT_BAND_DEGREES = 10.0;

    private final OceanDynamics plugin;

    public OceanPhysicsTask(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        OceanDynamicsConfig cfg = plugin.getPluginConfig();
        long timeoutMillis = cfg.activeTimeoutSeconds * 1000L;
        plugin.getActiveBoatManager().prune(timeoutMillis, 0.03);

        for (ActiveBoatManager.ActiveBoatEntry entry : plugin.getActiveBoatManager().snapshot()) {
            Vehicle boat = entry.vehicle();
            if (boat == null || !boat.isValid() || boat.isDead() || !plugin.isSupportedBoat(boat)) {
                continue;
            }
            Player driver = ActiveBoatManager.getDriver(boat);
            if (driver == null || !boat.isInWater()) {
                continue;
            }

            plugin.getActiveBoatManager().markActive(boat);
            applyBoatPhysics(boat, cfg);
            showCompassGuidance(boat, cfg);
        }
    }

    private void applyBoatPhysics(Vehicle boat, OceanDynamicsConfig cfg) {
        Vector vel = boat.getVelocity();
        Vector2 horizontal = new Vector2(vel.getX(), vel.getZ());
        Vector2 forwardDir = yawToDir(boat.getLocation().getYaw());

        Vector2 currentBaseEffect = cfg.getCurrentAt(boat.getLocation().getX(), boat.getLocation().getZ()).multiply(cfg.currentPush);
        Vector2 windBaseEffect = plugin.getWindManager().getWindVector();

        Vector2 pushed = horizontal;
        pushed = applyAlignedSpeedEffect(pushed, forwardDir, currentBaseEffect, cfg.currentWithMultiplier, cfg.currentAgainstDrag);
        pushed = applyAlignedSpeedEffect(pushed, forwardDir, windBaseEffect, cfg.windWithMultiplier, cfg.windAgainstDrag);

        double cap = cfg.maxHorizontalSpeed;
        if (hasCrewBonus(boat)) {
            cap *= cfg.crewBonusMultiplier;
            Vector2 forward = forwardDir.multiply(0.015 * (cfg.crewBonusMultiplier - 1.0) * 10.0);
            pushed = pushed.add(forward);
        }

        pushed = clampHorizontal(pushed, cap);
        boat.setVelocity(new Vector(pushed.x(), vel.getY(), pushed.z()));
    }

    private Vector2 applyAlignedSpeedEffect(Vector2 velocity, Vector2 forwardDir, Vector2 baseEffect, double withMultiplier, double againstDrag) {
        double baseMagnitude = baseEffect.length();
        if (baseMagnitude < 1.0E-8) {
            return velocity;
        }

        Vector2 effectDirection = baseEffect.normalize();
        double alignment = mapAlignment(forwardDir, effectDirection);
        if (Math.abs(alignment) < 1.0E-8) {
            return velocity;
        }

        double forwardSpeed = velocity.dot(forwardDir);
        Vector2 lateralComponent = velocity.add(forwardDir.multiply(-forwardSpeed));

        double deltaSpeed;
        if (alignment > 0.0) {
            double boostScale = 1.0 + alignment * (withMultiplier - 1.0);
            deltaSpeed = baseMagnitude * boostScale;
        } else {
            double slowdown = Math.max(0.0, forwardSpeed) * againstDrag * Math.abs(alignment);
            deltaSpeed = -Math.min(Math.max(0.0, forwardSpeed), slowdown);
        }

        double nextForwardSpeed = forwardSpeed + deltaSpeed;
        return forwardDir.multiply(nextForwardSpeed).add(lateralComponent);
    }

    private double mapAlignment(Vector2 referenceDir, Vector2 effectDirection) {
        double dot = Math.max(-1.0, Math.min(1.0, referenceDir.dot(effectDirection)));
        double angle = Math.toDegrees(Math.acos(dot));
        double snappedAngle = Math.round(angle / ALIGNMENT_BAND_DEGREES) * ALIGNMENT_BAND_DEGREES;

        if (snappedAngle <= ALIGNMENT_TOLERANCE_DEGREES) {
            return 1.0;
        }
        if (snappedAngle < 90.0) {
            return (90.0 - snappedAngle) / (90.0 - ALIGNMENT_TOLERANCE_DEGREES);
        }
        if (snappedAngle < 180.0 - ALIGNMENT_TOLERANCE_DEGREES) {
            return -((snappedAngle - 90.0) / (90.0 - ALIGNMENT_TOLERANCE_DEGREES));
        }
        return -1.0;
    }

    private void showCompassGuidance(Vehicle boat, OceanDynamicsConfig cfg) {
        List<Entity> passengers = boat.getPassengers();
        for (Entity passenger : passengers) {
            if (!(passenger instanceof Player player)) {
                continue;
            }
            if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) {
                continue;
            }
            CompassMode mode = plugin.getMode(player.getUniqueId());
            Vector2 vec;
            String label;
            if (mode == CompassMode.CURRENT) {
                vec = cfg.getCurrentAt(boat.getLocation().getX(), boat.getLocation().getZ());
                label = "CURRENT";
            } else {
                vec = plugin.getWindManager().getDirection().multiply(plugin.getWindManager().getStrength());
                label = "WIND";
            }
            double mag = vec.length();
            String message = String.format("%s %s (%.0f°) strength %.3f", label, vec.toCardinal(), vec.toDegrees(), mag);
            player.sendActionBar(Component.text(message));
        }
    }

    private boolean hasCrewBonus(Vehicle boat) {
        Player driver = ActiveBoatManager.getDriver(boat);
        if (driver == null) {
            return false;
        }
        return boat.getPassengers().stream().anyMatch(e -> e instanceof Player p && !p.getUniqueId().equals(driver.getUniqueId()));
    }

    private Vector2 yawToDir(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vector2(-Math.sin(rad), Math.cos(rad)).normalize();
    }

    private Vector2 clampHorizontal(Vector2 v, double maxSpeed) {
        double len = v.length();
        if (len > maxSpeed && len > 1.0E-8) {
            return v.multiply(maxSpeed / len);
        }
        return v;
    }
}
