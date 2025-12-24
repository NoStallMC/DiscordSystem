package main.java.org.matejko.discordsystem.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jnbt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.configuration.Config;

public class WorldResolver {
    private final Map<Integer, World> dimensionMap = new HashMap<>();
    private static final int CUSTOM_WORLD_BASE_ID = 12;
    private static DiscordPlugin plugin;
    private static Config config;

    public WorldResolver(DiscordPlugin plugin, Config config) {
        WorldResolver.plugin = plugin;
        WorldResolver.config = config;
        registerVanillaWorlds();
        File configFile = new File("plugins/Multiverse-Core/worlds.yml");
        if (!configFile.exists()) {
            plugin.getLogger().info("[DiscordSystem] Multiverse-Core worlds.yml not found at " +
                    configFile.getAbsolutePath() + ", using fallback.");
            return;
        }
        if (config.debugEnabled()) {
        	plugin.getLogger().info("[DiscordSystem] Found Multiverse-Core config at " + configFile.getAbsolutePath());
        }
        initFromConfig(configFile);
    }

    private void registerVanillaWorlds() {
        World overworld = Bukkit.getWorld("world");
        World nether = Bukkit.getWorld("world_nether");
        if (overworld != null) dimensionMap.put(0, overworld);
        if (nether != null) dimensionMap.put(-1, nether);
        if (config.debugEnabled()) {
            plugin.getLogger().info("[DiscordSystem] Vanilla dimensions registered: " +
                    (overworld != null ? "0=world " : "") +
                    (nether != null ? "-1=world_nether " : ""));
        }
    }

    @SuppressWarnings("unchecked")
    private void initFromConfig(File configFile) {
        try (FileReader reader = new FileReader(configFile)) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map)) {
                plugin.getLogger().warning("[DiscordSystem] worlds.yml is not a Map! Falling back.");
                initFallback();
                return;
            }
            Map<String, Object> topMap = (Map<String, Object>) loaded;
            Object worldsObj = topMap.get("worlds");
            if (!(worldsObj instanceof Map)) {
            	if (config.debugEnabled()) {
            		plugin.getLogger().warning("[DiscordSystem] 'worlds' section missing or invalid! Falling back.");
            	}
                initFallback();
                return;
            }

            Map<String, Object> worldsMap = (Map<String, Object>) worldsObj;
            if (config.debugEnabled()) {
            plugin.getLogger().info("[DiscordSystem] worlds.yml contains " + worldsMap.size() + " worlds.");
            }
            int idCounter = CUSTOM_WORLD_BASE_ID;
            for (Map.Entry<String, Object> entry : worldsMap.entrySet()) {
                String worldName = entry.getKey();
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    dimensionMap.put(idCounter, world);
                    if (config.debugEnabled()) {
                    	plugin.getLogger().info("[DiscordSystem] Registered custom world " + worldName + " with fake dimension ID " + idCounter);
                    }
                    idCounter++;
                } else {
                	if (config.debugEnabled()) {
                		plugin.getLogger().warning("[DiscordSystem] Failed to load world '" + worldName + "'");
                	}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            initFallback();
        }
    }
    private void initFallback() {
        World overworld = Bukkit.getWorld("world");
        World nether = Bukkit.getWorld("world_nether");
        World end = Bukkit.getWorld("world_the_end");
        if (overworld != null) dimensionMap.put(0, overworld);
        if (nether != null) dimensionMap.put(-1, nether);
        if (end != null) dimensionMap.put(1, end);
        if (config.debugEnabled()) {
        	plugin.getLogger().info("[DiscordSystem] Fallback worlds registered: " +
        			(overworld != null ? "world " : "") +
        			(nether != null ? "world_nether " : "") +
        			(end != null ? "world_the_end" : ""));
        }
    }
    public World resolveWorld(File datFile) {
        int dimension = 0;
        try (FileInputStream fis = new FileInputStream(datFile);
             NBTInputStream nis = new NBTInputStream(fis)) {
            Tag t = nis.readTag();
            if (!(t instanceof CompoundTag)) {
            	if (config.debugEnabled()) {
            		plugin.getLogger().warning("[DiscordSystem] .dat root tag is not CompoundTag. Using default world.");	
            	}
                return dimensionMap.getOrDefault(0, null);
            }
            CompoundTag root = (CompoundTag) t;
            Tag dimTag = root.getValue().get("Dimension");
            if (dimTag instanceof IntTag) {
                dimension = ((IntTag) dimTag).getValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        World resolved = dimensionMap.getOrDefault(dimension, dimensionMap.getOrDefault(0, null));
        if (config.debugEnabled()) {
        	plugin.getLogger().info("[DiscordSystem] .dat file dimension: " + dimension + " -> Resolved world: " + (resolved != null ? resolved.getName() : "null"));
        }
        return resolved;
    }
}
