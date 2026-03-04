package com.yourdomain.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import java.util.List;

public final class OceanPhysicsTask implements Runnable {
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
        double speed = horizontal.length();

        Vector2 referenceDir = speed > 1.0E-4 ? horizontal.normalize() : yawToDir(boat.getLocation().getYaw());
        Vector2 currentCellVec = cfg.getCurrentAt(boat.getLocation().getX(), boat.getLocation().getZ());
        Vector2 currentBasePush = currentCellVec.multiply(cfg.currentPush);
        Vector2 windBasePush = plugin.getWindManager().getWindVector();

        Vector2 pushed = horizontal;
        pushed = applyAlignedForce(pushed, referenceDir, currentBasePush, cfg.currentWithMultiplier, cfg.currentAgainstDrag);
        pushed = applyAlignedForce(pushed, referenceDir, windBasePush, cfg.windWithMultiplier, cfg.windAgainstDrag);

        double cap = cfg.maxHorizontalSpeed;
        if (hasCrewBonus(boat)) {
            cap *= cfg.crewBonusMultiplier;
            Vector2 forward = yawToDir(boat.getLocation().getYaw()).multiply(0.015 * (cfg.crewBonusMultiplier - 1.0) * 10.0);
            pushed = pushed.add(forward);
        }

        pushed = clampHorizontal(pushed, cap);
        boat.setVelocity(new Vector(pushed.x(), vel.getY(), pushed.z()));
    }

    private Vector2 applyAlignedForce(Vector2 velocity, Vector2 referenceDir, Vector2 basePush, double withMultiplier, double againstDrag) {
        if (basePush.length() < 1.0E-8) {
            return velocity;
        }
        Vector2 forceDir = basePush.normalize();
        double alignment = referenceDir.dot(forceDir);
        Vector2 push = basePush;
        Vector2 out = velocity;
        if (alignment > 0.0) {
            push = push.multiply(1.0 + alignment * (withMultiplier - 1.0));
        } else if (alignment < 0.0) {
            double dragFactor = Math.max(0.0, 1.0 - againstDrag * Math.abs(alignment));
            out = out.multiply(dragFactor);
        }
        return out.add(push);
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
