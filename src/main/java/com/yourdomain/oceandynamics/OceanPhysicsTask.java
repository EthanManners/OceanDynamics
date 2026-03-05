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
            applyBoatPhysics(boat, entry, cfg);
            showCompassGuidance(boat, cfg);
        }
    }

    private void applyBoatPhysics(Vehicle boat, ActiveBoatManager.ActiveBoatEntry entry, OceanDynamicsConfig cfg) {
        Vector vel = boat.getVelocity();
        Vector2 horizontal = new Vector2(vel.getX(), vel.getZ());
        Vector2 forwardDir = yawToDir(boat.getLocation().getYaw());

        Vector2 currentCellVec = entry.cachedCurrentAt(cfg, boat.getLocation().getX(), boat.getLocation().getZ());
        Vector2 currentBasePush = currentCellVec.multiply(cfg.currentPush);
        Vector2 windBasePush = plugin.getWindManager().getWindVector();

        Vector2 boost = alignedBoost(currentBasePush, forwardDir, cfg.currentWithMultiplier, cfg.currentAgainstDrag)
                .add(alignedBoost(windBasePush, forwardDir, cfg.windWithMultiplier, cfg.windAgainstDrag));

        boolean crewBonus = hasCrewBonus(boat);
        if (crewBonus) {
            boost = boost.add(forwardDir.multiply(0.004 * (cfg.crewBonusMultiplier - 1.0) * 10.0));
        }

        boost = clampMagnitude(boost, cfg.maxAccelPerTick);

        double speedCap = crewBonus ? cfg.maxHorizontalSpeed * cfg.crewBonusMultiplier : cfg.maxHorizontalSpeed;
        Vector2 target = clampHorizontal(horizontal.add(boost), speedCap);
        Vector2 next = lerp(horizontal, target, cfg.velocitySmoothing);

        boat.setVelocity(new Vector(next.x(), vel.getY(), next.z()));
    }

    private Vector2 alignedBoost(Vector2 basePush, Vector2 forwardDir, double withMultiplier, double againstDrag) {
        if (basePush.length() < 1.0E-8) {
            return Vector2.ZERO;
        }

        Vector2 push = basePush;
        double alignment = forwardDir.dot(basePush.normalize());
        if (alignment > 0.0) {
            return push.multiply(1.0 + alignment * (withMultiplier - 1.0));
        }
        if (alignment < 0.0) {
            return push.multiply(Math.max(0.0, 1.0 - againstDrag * Math.abs(alignment)));
        }
        return push;
    }

    private Vector2 lerp(Vector2 from, Vector2 to, double alpha) {
        double t = Math.max(0.0, Math.min(1.0, alpha));
        return from.multiply(1.0 - t).add(to.multiply(t));
    }

    private Vector2 clampMagnitude(Vector2 v, double maxLen) {
        if (maxLen <= 0.0) {
            return Vector2.ZERO;
        }
        double len = v.length();
        if (len > maxLen && len > 1.0E-8) {
            return v.multiply(maxLen / len);
        }
        return v;
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
