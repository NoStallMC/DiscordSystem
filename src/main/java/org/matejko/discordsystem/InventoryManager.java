package main.java.org.matejko.discordsystem;

import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.utils.ImgBuilder.ItemInfo;
import main.java.org.matejko.discordsystem.utils.WorldResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InventoryManager {
	@SuppressWarnings("unused")
	private static File SERVER_ROOT;
	private static File PLAYERS_DIR;
	private static File UUID_CACHE_FILE;
	private static DiscordPlugin plugin;
	private static Config config;
    public static final int ARMOR_SLOT_BASE = 100;
    private static final Set<Integer> SUBID_ITEMS = new HashSet<>();
    static {
        SUBID_ITEMS.add(6);
        SUBID_ITEMS.add(18);
        SUBID_ITEMS.add(31);
        SUBID_ITEMS.add(35);
        SUBID_ITEMS.add(43);
        SUBID_ITEMS.add(44);
        SUBID_ITEMS.add(351);
    }
    public static void init(DiscordPlugin plugin, Config config) {
        InventoryManager.plugin = plugin;
        InventoryManager.config = config;
        File pluginData = plugin.getDataFolder();
        String pluginPath = pluginData.getAbsolutePath();
        String suffix = "/plugins/DiscordSystem";
        File serverRoot;
        if (pluginPath.endsWith(suffix)) {
            String rootPath = pluginPath.substring(0, pluginPath.length() - suffix.length());
            serverRoot = new File(rootPath);
        } else {
            plugin.getLogger().warning("[WARN] Unexpected plugin folder structure!");
            serverRoot = pluginData;
        }
        SERVER_ROOT = serverRoot;
        PLAYERS_DIR = new File(serverRoot, "world/players");
        UUID_CACHE_FILE = new File(serverRoot, "uuidcache.json");
    }
    public static PlayerData getPlayerData(String playerName) {
        Map<Integer, ItemInfo> inventory = getInventoryForPlayerName(playerName);
        PlayerState state = getPlayerState(playerName);
        String uuid = null;
        Player p = Bukkit.getPlayerExact(playerName);
        if (p != null) {
            uuid = p.getUniqueId().toString();
        } else if (UUID_CACHE_FILE.exists()) {
            try {
                UUIDCache cache = new UUIDCache(UUID_CACHE_FILE);
                uuid = cache.getUUIDForName(playerName);
            } catch (Exception ignored) {}
        }
        World world = null;
        if (p != null) {
            world = p.getWorld();
        } else if (uuid != null) {
            File uuidDat = new File(PLAYERS_DIR, uuid + ".dat");
            if (uuidDat.exists()) {
                WorldResolver resolver = new WorldResolver(plugin, config);
                world = resolver.resolveWorld(uuidDat);
            }
        }
        return new PlayerData(playerName, uuid, inventory, state, world);
    }
    public static Map<Integer, ItemInfo> getInventoryForPlayerName(String playerName) {
        Player p = Bukkit.getPlayerExact(playerName);
        if (p != null) return fetchFromOnlinePlayer(p);
        File pnDat = new File(PLAYERS_DIR, playerName + ".dat");
        if (pnDat.exists()) {
            try { return PlayerDataReader.readPlayerDat(pnDat); } catch (Exception e) { e.printStackTrace(); return Collections.emptyMap(); }
        }
        if (UUID_CACHE_FILE.exists()) {
            try {
                UUIDCache cache = new UUIDCache(UUID_CACHE_FILE);
                String uuid = cache.getUUIDForName(playerName);
                if (uuid != null) {
                    File uuidDat = new File(PLAYERS_DIR, uuid + ".dat");
                    if (uuidDat.exists()) return PlayerDataReader.readPlayerDat(uuidDat);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return Collections.emptyMap();
    }
    public static PlayerState getPlayerState(String playerName) {
        Player p = Bukkit.getPlayerExact(playerName);
        if (p != null) {
            return PlayerState.fromOnlinePlayer(p);
        }
        Map<Integer, ItemInfo> inventory = getInventoryForPlayerName(playerName);
        File nameDat = new File(PLAYERS_DIR, playerName + ".dat");
        if (nameDat.exists()) {
            return OfflinePlayerState.readOfflinePlayerState(nameDat, inventory, null);
        }
        if (UUID_CACHE_FILE.exists()) {
            try {
                UUIDCache cache = new UUIDCache(UUID_CACHE_FILE);
                String uuid = cache.getUUIDForName(playerName);
                if (uuid != null) {
                    File uuidDat = new File(PLAYERS_DIR, uuid + ".dat");
                    if (uuidDat.exists()) return OfflinePlayerState.readOfflinePlayerState(uuidDat, inventory, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("[PlayerState] Invalid player " + playerName + " - using default state");
        return PlayerState.createDefault();
    }

    private static Map<Integer, ItemInfo> fetchFromOnlinePlayer(Player p) {
        Map<Integer, ItemInfo> out = new HashMap<>();
        ItemStack[] contents = p.getInventory().getContents();
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it == null) continue;
                ItemInfo info = convertItem(it);
                out.put(i, info);
            }
        }
        try {
            ItemStack[] armor = p.getInventory().getArmorContents();
            for (int i = 0; i < 4; i++) {
                ItemStack a = (armor != null && i < armor.length) ? armor[i] : null;
                ItemInfo info;
                int slotIndex = 3 - i;
                if (a != null && a.getType() != Material.AIR) {
                    info = convertItem(a);
                    switch (a.getType()) {
                        case LEATHER_HELMET:
                        case GOLD_HELMET:
                        case CHAINMAIL_HELMET:
                        case IRON_HELMET:
                        case DIAMOND_HELMET:
                            slotIndex = 0; break;
                        case LEATHER_CHESTPLATE:
                        case GOLD_CHESTPLATE:
                        case CHAINMAIL_CHESTPLATE:
                        case IRON_CHESTPLATE:
                        case DIAMOND_CHESTPLATE:
                            slotIndex = 1; break;
                        case LEATHER_LEGGINGS:
                        case GOLD_LEGGINGS:
                        case CHAINMAIL_LEGGINGS:
                        case IRON_LEGGINGS:
                        case DIAMOND_LEGGINGS:
                            slotIndex = 2; break;
                        case LEATHER_BOOTS:
                        case GOLD_BOOTS:
                        case CHAINMAIL_BOOTS:
                        case IRON_BOOTS:
                        case DIAMOND_BOOTS:
                            slotIndex = 3; break;
                        default:
                    }
                } else {
                    info = new ItemInfo("0", 0, 0, 0);
                }
                out.put(ARMOR_SLOT_BASE + slotIndex, info);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return out;
    }
    private static ItemInfo convertItem(ItemStack it) {
        int typeId = it.getTypeId();
        String id = String.valueOf(typeId);
        int damage = it.getDurability();
        int maxDur = 0;
        if (SUBID_ITEMS.contains(typeId)) {
            id = id + "." + damage;
            damage = 0;
        }
        try {
            Material mat = it.getType();
            maxDur = mat.getMaxDurability();
            if (maxDur <= 0) maxDur = 0;
        } catch (Throwable ignored) {
            maxDur = 0;
        }
        return new ItemInfo(id, it.getAmount(), damage, maxDur);
    }
    public static boolean playerExists(String playerName) {
        String uuid = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        if (UUID_CACHE_FILE.exists()) {
            try {
                UUIDCache cache = new UUIDCache(UUID_CACHE_FILE);
                uuid = cache.getUUIDForName(playerName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (uuid != null) {
            File uuidDat = new File(PLAYERS_DIR, uuid + ".dat");
            if (uuidDat.exists()) return true;
        }
        File nameDat = new File(PLAYERS_DIR, playerName + ".dat");
        return nameDat.exists();
    }
}