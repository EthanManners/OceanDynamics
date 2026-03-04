package com.yourdomain.oceandynamics;

import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OceanDynamics extends org.bukkit.plugin.java.JavaPlugin {
    private OceanDynamicsConfig pluginConfig;
    private final ActiveBoatManager activeBoatManager = new ActiveBoatManager();
    private final Map<UUID, CompassMode> compassModes = new ConcurrentHashMap<>();
    private WindManager windManager;
    private BukkitTask physicsTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        windManager = new WindManager(this);
        windManager.start(pluginConfig);

        getServer().getPluginManager().registerEvents(new OceanDynamicsListener(this), this);

        PluginCommand cmd = getCommand("oceandynamics");
        if (cmd != null) {
            cmd.setExecutor(new OceanDynamicsCommand(this));
        }

        startPhysicsTask();
        getLogger().info("OceanDynamics enabled.");
    }

    @Override
    public void onDisable() {
        if (physicsTask != null) {
            physicsTask.cancel();
        }
        if (windManager != null) {
            windManager.stop();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.pluginConfig = OceanDynamicsConfig.from(getConfig());
        if (windManager != null) {
            windManager.start(pluginConfig);
        }
        startPhysicsTask();
    }

    private void startPhysicsTask() {
        if (physicsTask != null) {
            physicsTask.cancel();
        }
        physicsTask = getServer().getScheduler().runTaskTimer(this, new OceanPhysicsTask(this), pluginConfig.tickInterval, pluginConfig.tickInterval);
    }

    public OceanDynamicsConfig getPluginConfig() {
        return pluginConfig;
    }

    public ActiveBoatManager getActiveBoatManager() {
        return activeBoatManager;
    }

    public WindManager getWindManager() {
        return windManager;
    }

    public CompassMode toggleMode(UUID uuid) {
        CompassMode next = compassModes.getOrDefault(uuid, CompassMode.WIND).toggle();
        compassModes.put(uuid, next);
        return next;
    }

    public CompassMode getMode(UUID uuid) {
        return compassModes.getOrDefault(uuid, CompassMode.WIND);
    }

    public boolean isSupportedBoat(Vehicle vehicle) {
        if (vehicle instanceof Boat) {
            return isOverworld(vehicle);
        }
        EntityType type = vehicle.getType();
        String key = type.getKey().toString().toLowerCase(Locale.ROOT);
        return isOverworld(vehicle) && (
                key.endsWith("boat")
                        || key.endsWith("chest_boat")
                        || key.contains("raft")
        );
    }

    private boolean isOverworld(Vehicle vehicle) {
        World.Environment env = vehicle.getWorld().getEnvironment();
        return env == World.Environment.NORMAL;
    }
}
