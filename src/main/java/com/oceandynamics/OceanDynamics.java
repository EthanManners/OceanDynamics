package com.oceandynamics;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OceanDynamics extends JavaPlugin {
    private CurrentField currentField;
    private WindManager windManager;
    private BoatMotionListener boatMotionListener;
    private CompassListener compassListener;
    private ActionbarTask actionbarTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.currentField = new CurrentField(this);
        this.currentField.reload();

        this.windManager = new WindManager(this);
        this.windManager.reload();
        this.windManager.start();

        this.boatMotionListener = new BoatMotionListener(this, currentField, windManager);
        this.compassListener = new CompassListener(boatMotionListener);

        getServer().getPluginManager().registerEvents(boatMotionListener, this);
        getServer().getPluginManager().registerEvents(compassListener, this);

        this.actionbarTask = new ActionbarTask(this, currentField, windManager, boatMotionListener);
        this.actionbarTask.start();

        OceanDynamicsCommand executor = new OceanDynamicsCommand(this, currentField, windManager, boatMotionListener);
        PluginCommand command = getCommand("oceandynamics");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("OceanDynamics enabled.");
    }

    @Override
    public void onDisable() {
        if (actionbarTask != null) {
            actionbarTask.stop();
        }
        if (windManager != null) {
            windManager.stop();
        }
        getLogger().info("OceanDynamics disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        currentField.reload();
        windManager.reload();
        windManager.start();
    }
}
