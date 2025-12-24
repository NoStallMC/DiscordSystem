package main.java.org.matejko.discordsystem;

import main.java.org.matejko.discordsystem.utils.ImgBuilder.ItemInfo;
import java.util.Map;
import org.bukkit.World;

public class PlayerData {
    private final String playerName;
    private final String uuid;
    private final Map<Integer, ItemInfo> inventory;
    private final PlayerState state;
    private final World world;

    public PlayerData(String playerName, String uuid, Map<Integer, ItemInfo> inventory, PlayerState state, World world) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.inventory = inventory;
        this.state = state;
        this.world = world;
    }
    public String getDisplayName() {
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (p != null) {
            String dn = p.getDisplayName();
            if (dn != null && !dn.isEmpty()) return dn;
        }
        return playerName;
    }
    public Map<Integer, ItemInfo> getInventory() { 
        return inventory; 
    }
    public PlayerState getState() { 
        return state; 
    }
    public String getPlayerName() { 
        return playerName; 
    }
    public double getHealth() { 
        return state.getHealth(); 
    }
    public double getMaxHealth() { 
        return state.getMaxHealth(); 
    }
    public double getArmor() { 
        return state.getArmor(); 
    }
    public Position getPosition() { 
        return state.getPosition(); 
    }
    public String getUuid() {
        return uuid;
    }
    public World getWorld() {
        return world;
    }
}
