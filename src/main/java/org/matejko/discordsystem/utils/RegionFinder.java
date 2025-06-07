package main.java.org.matejko.discordsystem.utils;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RegionFinder {
    private final WorldGuardPlugin wg;

    public RegionFinder() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (plugin instanceof WorldGuardPlugin) {
            this.wg = (WorldGuardPlugin) plugin;
        } else {
        	this.wg = null;
        }
    }

    public String getRegion(Player player) {
        if (wg == null) {
            return "NOTFOUND";
        }
        Location loc = player.getLocation();
        World world = loc.getWorld();
        RegionManager manager = wg.getRegionManager(world);
        if (manager == null) {
            return "NOTFOUND";
        }
        ApplicableRegionSet set = manager.getApplicableRegions(loc);
        if (set.size() == 0) {
            return "";
        }
        ProtectedRegion topRegion = null;
        for (ProtectedRegion region : set) {
            if (topRegion == null || region.getPriority() > topRegion.getPriority()) {
                topRegion = region;
            }
        }
        return "[" + topRegion.getId() + "]";
    }
    public boolean isHooked() {
        return this.wg != null;
    }
}
