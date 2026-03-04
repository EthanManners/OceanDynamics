package com.yourdomain.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public final class OceanDynamicsListener implements Listener {
    private final OceanDynamics plugin;

    public OceanDynamicsListener(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }
        if (!plugin.isSupportedBoat(event.getVehicle())) {
            return;
        }
        plugin.getActiveBoatManager().markActive(event.getVehicle());
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) {
            return;
        }
        if (!plugin.isSupportedBoat(event.getVehicle())) {
            return;
        }
        plugin.getActiveBoatManager().onPlayerExit(event.getVehicle());
    }

    @EventHandler
    public void onCompassToggle(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Vehicle v) || !plugin.isSupportedBoat(v)) {
            return;
        }
        CompassMode mode = plugin.toggleMode(player.getUniqueId());
        player.sendActionBar(Component.text("OceanDynamics Compass Mode: " + mode));
        event.setCancelled(true);
    }
}
