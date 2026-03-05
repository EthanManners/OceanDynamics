package com.oceandynamics;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class OceanDynamics extends JavaPlugin {

    private CurrentField currentField;
    private WindManager windManager;
    private BoatMotionListener boatMotionListener;
    private CompassListener compassListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSystems();

        getServer().getPluginManager().registerEvents(boatMotionListener, this);
        getServer().getPluginManager().registerEvents(compassListener, this);

        ActionbarTask actionbarTask = new ActionbarTask(this, compassListener, boatMotionListener);
        Bukkit.getScheduler().runTaskTimer(this, actionbarTask, 20L, 5L);

        OceanDynamicsCommand command = new OceanDynamicsCommand(this, compassListener);
        if (getCommand("oceandynamics") != null) {
            getCommand("oceandynamics").setExecutor(command);
            getCommand("oceandynamics").setTabCompleter(command);
        }

        getLogger().info("OceanDynamics enabled.");
    }

    @Override
    public void onDisable() {
        if (windManager != null) {
            windManager.stop();
        }
        getLogger().info("OceanDynamics disabled.");
    }

    public void reloadSystems() {
        reloadConfig();

        if (windManager != null) {
            windManager.stop();
        }

        this.currentField = CurrentField.fromConfig(getConfig(), getLogger());
        this.windManager = new WindManager(this);
        this.windManager.start();

        if (boatMotionListener == null) {
            this.boatMotionListener = new BoatMotionListener(this);
        } else {
            this.boatMotionListener.clearCachedState();
        }

        if (compassListener == null) {
            this.compassListener = new CompassListener(this);
        }
    }

    public CurrentField getCurrentField() {
        return currentField;
    }

    public WindManager getWindManager() {
        return windManager;
    }

    public BoatMotionListener getBoatMotionListener() {
        return boatMotionListener;
    }
}
