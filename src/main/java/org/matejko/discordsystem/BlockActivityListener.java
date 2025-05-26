package main.java.org.matejko.discordsystem;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

import main.java.org.matejko.discordsystem.utils.ActivityManager;

public class BlockActivityListener implements Listener {
    ////////////////////////////////////////////////////////////////////////////////
	// Detects Activites
    ////////////////////////////////////////////////////////////////////////////////
	
    // Detects block placement events
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ActivityManager.handlePlace(e.getPlayer(), e.getBlockPlaced().getType());
    }

    // Detects block breaking events
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        ActivityManager.handleBreak(e.getPlayer(), e.getBlock().getType());
    }

    // Detects item pickup (used to infer gathering activity)
    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        ActivityManager.handlePickup(e.getPlayer(), e.getItem().getItemStack().getType());
    }

    // Detects successful fishing attempts
    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            ActivityManager.handleFish(e.getPlayer());
        }
    }

    // Detects interactions with specific farmland-related blocks
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            Material mat = e.getClickedBlock().getType();
            if (mat == Material.SOIL || mat == Material.CROPS || mat == Material.SEEDS) {
                ActivityManager.handleFarmland(e.getPlayer(), mat);
            }
        }
    }

    // Detects player movement and checks the block type below
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() != null && !e.getTo().getBlock().equals(e.getFrom().getBlock())) {
            Material below = e.getTo().subtract(0, 1, 0).getBlock().getType();
            ActivityManager.handleMove(e.getPlayer(), below);
        }
    }
}
