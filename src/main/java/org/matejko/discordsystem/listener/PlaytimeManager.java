package main.java.org.matejko.discordsystem.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.configuration.Config;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlaytimeManager implements Listener {
    private final Map<String, Long> playtimes = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionStartTimes = new ConcurrentHashMap<>();
	private static Config config;
	private static DiscordPlugin plugin;

	public PlaytimeManager(Config config, DiscordPlugin plugin) {
		PlaytimeManager.plugin = plugin;
	    PlaytimeManager.config = config;
	}
	
    /////////////////////////////////////////////////////////////////////////////
    // Reset playtime and session start time for all currently online players
    /////////////////////////////////////////////////////////////////////////////
    public void resetAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName().toLowerCase();
            playtimes.put(name, 0L);
            sessionStartTimes.put(name, System.nanoTime());
            if (config.debugEnabled()) {
            	getLogger().info("[DEBUG] " + name + " playtime and session start reset on plugin enable.");
            }
        }
    }
    
    /////////////////////////////////////////////////////////////////////////////
    // Handle Player Join Event
    /////////////////////////////////////////////////////////////////////////////
    @EventHandler
    public void handleJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String name = player.getName().toLowerCase();
        // Reset playtime and session start
        playtimes.put(name, 0L);
        sessionStartTimes.put(name, System.nanoTime());
        if (config.debugEnabled()) {
        	getLogger().info("[DEBUG] " + name + " joined, playtime and session start reset.");
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // Handle Player Quit Event
    /////////////////////////////////////////////////////////////////////////////
    @EventHandler
    public void handleQuit(PlayerQuitEvent e) {
        String name = e.getPlayer().getName().toLowerCase();
        Long start = sessionStartTimes.remove(name);
        if (start != null) {
            long duration = System.nanoTime() - start;
            long millis = duration / 1_000_000;
            playtimes.put(name, playtimes.getOrDefault(name, 0L) + millis);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // Get Player's Session Time
    /////////////////////////////////////////////////////////////////////////////
    public String getSessionTime(Player player) {
        String name = player.getName().toLowerCase();
        long sessionMillis = 0L;
        if (sessionStartTimes.containsKey(name)) {
            sessionMillis = (System.nanoTime() - sessionStartTimes.get(name)) / 1_000_000;
        }
        long totalMillis = playtimes.getOrDefault(name, 0L) + sessionMillis;
        long seconds = totalMillis / 1000;
        long mins = (seconds / 60) % 60;
        long hrs = seconds / 3600;
        return String.format("%dh %02dmin", hrs, mins);
    }
    public static Logger getLogger() {
        return plugin.getLogger();
    }
}
