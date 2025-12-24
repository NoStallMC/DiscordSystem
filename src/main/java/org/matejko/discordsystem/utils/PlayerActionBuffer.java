package main.java.org.matejko.discordsystem.utils;

import org.bukkit.Material;

import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;

import java.util.*;

public class PlayerActionBuffer {

    /////////////////////////////////////////////////////////////////
    // Action lists for tracking various types of activities
    /////////////////////////////////////////////////////////////////
    public final List<BlockAction> gatheringBlocks = new ArrayList<>();
    public final List<BlockAction> miningBlocks = new ArrayList<>();
    public final List<BlockAction> placedBlocks = new ArrayList<>();
    public final List<BlockAction> brokenBlocks = new ArrayList<>();
    public final List<Material> pickedUpItems = new ArrayList<>();
    public final List<BlockAction> farmlandBlocks = new ArrayList<>();

    /////////////////////////////////////////////////////////////////
    // Counters and time tracking
    /////////////////////////////////////////////////////////////////
    public int afkSeconds = 0;
    public int hunting = 0;
    public int fishCaught = 0;
    public int netherDistance = 0;
    public long lastTickTime = System.currentTimeMillis();
    private final int DECAY_WINDOW_TIME = ActivityTrackerConfig.getInt("Activity-Clear", 10) * 1000;

    /////////////////////////////////////////////////////////////////
    // Represents a single action with time and frequency tracking
    /////////////////////////////////////////////////////////////////
    public static class BlockAction {
        public final Material type;
        public final long time;
        public long lastUpdatedTime;
        public int count = 1;

        public BlockAction(Material type, long time) {
            this.type = type;
            this.time = time;
            this.lastUpdatedTime = time;
        }

        public void updateTime() {
            this.lastUpdatedTime = System.currentTimeMillis();
            this.count++;
        }
    }

    /////////////////////////////////////////////////////////////////
    // Removes stale data from all tracked action lists
    /////////////////////////////////////////////////////////////////
    public void cleanup() {
        long now = System.currentTimeMillis();
        placedBlocks.removeIf(b -> now - b.lastUpdatedTime > DECAY_WINDOW_TIME);
        brokenBlocks.removeIf(b -> now - b.lastUpdatedTime > DECAY_WINDOW_TIME);
        farmlandBlocks.removeIf(b -> now - b.lastUpdatedTime > DECAY_WINDOW_TIME);
        gatheringBlocks.removeIf(b -> now - b.lastUpdatedTime > DECAY_WINDOW_TIME);
        miningBlocks.removeIf(b -> now - b.lastUpdatedTime > DECAY_WINDOW_TIME);
    }

    /////////////////////////////////////////////////////////////////
    // Adds a new action or updates an existing one
    /////////////////////////////////////////////////////////////////
    public void addOrUpdate(List<BlockAction> actionList, Material type) {
        for (BlockAction action : actionList) {
            if (action.type == type) {
                action.updateTime();
                return;
            }
        }
        actionList.add(new BlockAction(type, System.currentTimeMillis()));
    }
}
