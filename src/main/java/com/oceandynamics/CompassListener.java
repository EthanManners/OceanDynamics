package com.oceandynamics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CompassListener implements Listener {

    public enum CompassMode {
        WIND,
        CURRENT
    }

    private final OceanDynamics plugin;
    private final Map<UUID, CompassMode> playerModes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> debugPlayers = new ConcurrentHashMap<>();

    public CompassListener(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Boat)) {
            return;
        }

        CompassMode nextMode = getMode(player.getUniqueId()) == CompassMode.WIND ? CompassMode.CURRENT : CompassMode.WIND;
        playerModes.put(player.getUniqueId(), nextMode);

        player.sendActionBar(Component.text("OceanDynamics: " + nextMode + " mode"));
    }

    public CompassMode getMode(UUID playerId) {
        return playerModes.getOrDefault(playerId, CompassMode.WIND);
    }

    public boolean toggleDebug(UUID playerId) {
        boolean next = !debugPlayers.getOrDefault(playerId, false);
        debugPlayers.put(playerId, next);
        return next;
    }

    public boolean isDebugEnabled(UUID playerId) {
        return debugPlayers.getOrDefault(playerId, false);
    }
}
