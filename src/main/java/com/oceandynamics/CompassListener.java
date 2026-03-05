package com.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class CompassListener implements Listener {
    private final BoatMotionListener motionListener;

    public CompassListener(BoatMotionListener motionListener) {
        this.motionListener = motionListener;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        BoatMotionListener.CompassMode mode = motionListener.toggleCompassMode(player.getUniqueId());
        player.sendActionBar(Component.text("OceanDynamics: " + mode.name() + " mode"));
    }
}
