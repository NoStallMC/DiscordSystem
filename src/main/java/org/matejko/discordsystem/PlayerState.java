package main.java.org.matejko.discordsystem;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerState {
    private final double health;
    private final double maxHealth;
    private final double armor;
    private final Position position;
    private final World world;

    public PlayerState(double health, double maxHealth, double armor, Position position, World world) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.armor = armor;
        this.position = position;
        this.world = world;
    }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public double getArmor() { return armor; }
    public Position getPosition() { return position; }
    public World getWorld() { return world; }
    public int getX() { return (int) Math.floor(getPosition().getX());}
    public int getY() { return (int) Math.floor(getPosition().getY());}
    public int getZ() { return (int) Math.floor(getPosition().getZ());}

    public static PlayerState createDefault() {
        return new PlayerState(20.0, 20.0, 0.0, Position.ORIGIN, null);
    }
    public static PlayerState fromOnlinePlayer(Player player) {
        double health = player.getHealth();
        double maxHealth = 20.0;
        double armor = getCurrentArmorLevel(player);
        World world = player.getWorld();
        Position position = Position.fromLocation(player.getLocation());
        return new PlayerState(health, maxHealth, armor, position, world);
    }
    public static float getCurrentArmorLevel(Player player) {
        float totalArmor = 0.0f;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null) continue;
            int maxDurability = armor.getType().getMaxDurability();
            int currentDamage = armor.getDurability();
            int remaining = maxDurability - currentDamage;
            float durabilityRatio = (float) remaining / (float) maxDurability;
            float baseArmor = getBaseArmorValue(armor.getType());
            totalArmor += baseArmor * durabilityRatio;
            }
        return totalArmor;
    }
    public static float getBaseArmorValue(Material type) {
        switch (type) {
            case LEATHER_HELMET: return 1f;
            case LEATHER_CHESTPLATE: return 3f;
            case LEATHER_LEGGINGS: return 2f;
            case LEATHER_BOOTS: return 1f;

            case IRON_HELMET: return 2f;
            case IRON_CHESTPLATE: return 6f;
            case IRON_LEGGINGS: return 5f;
            case IRON_BOOTS: return 2f;

            case DIAMOND_HELMET: return 3f;
            case DIAMOND_CHESTPLATE: return 8f;
            case DIAMOND_LEGGINGS: return 6f;
            case DIAMOND_BOOTS: return 3f;

            case GOLD_HELMET: return 2f;
            case GOLD_CHESTPLATE: return 5f;
            case GOLD_LEGGINGS: return 3f;
            case GOLD_BOOTS: return 1f;

            case CHAINMAIL_HELMET: return 2f;
            case CHAINMAIL_CHESTPLATE: return 5f;
            case CHAINMAIL_LEGGINGS: return 4f;
            case CHAINMAIL_BOOTS: return 1f;

            default: return 0f;
        }
    }
}