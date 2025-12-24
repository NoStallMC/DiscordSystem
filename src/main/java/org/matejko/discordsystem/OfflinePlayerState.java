package main.java.org.matejko.discordsystem;

import org.bukkit.World;
import org.jnbt.*;
import main.java.org.matejko.discordsystem.utils.ImgBuilder.ItemInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OfflinePlayerState {
    public static PlayerState readOfflinePlayerState(File datFile, Map<Integer, ItemInfo> inventory, World world) {
        try (FileInputStream fis = new FileInputStream(datFile);
             NBTInputStream nis = new NBTInputStream(fis)) {
            Tag t = nis.readTag();
            if (!(t instanceof CompoundTag)) return PlayerState.createDefault();
            CompoundTag root = (CompoundTag) t;
            Map<String, Tag> rootMap = root.getValue();
            double health = 20.0;
            Tag healthTag = rootMap.get("Health");
            if (healthTag instanceof ShortTag) {
                health = ((ShortTag) healthTag).getValue();
            } else if (healthTag instanceof IntTag) {
                health = ((IntTag) healthTag).getValue();
            } else if (healthTag instanceof FloatTag) {
                health = ((FloatTag) healthTag).getValue();
            } else if (healthTag instanceof DoubleTag) {
                health = ((DoubleTag) healthTag).getValue();
            }
         float totalArmor = 0f;
         for (int i = 0; i < 4; i++) {
             int slotKey = InventoryManager.ARMOR_SLOT_BASE + i; 
             ItemInfo info = inventory.get(slotKey);
             if (info == null || "0".equals(info.id)) continue;
             org.bukkit.Material material = getMaterialFromId(info.id);
             if (material == null) continue;
             int maxDurability = material.getMaxDurability();
             int currentDamage = info.damage; 
             if (maxDurability <= 0) {
                 totalArmor += PlayerState.getBaseArmorValue(material);
                 continue;
             }
             int remaining = maxDurability - currentDamage;
             if (remaining < 0) remaining = 0;
             float durabilityRatio = (float) remaining / (float) maxDurability;
             if (durabilityRatio < 0f) durabilityRatio = 0f;
             if (durabilityRatio > 1f) durabilityRatio = 1f;
             float baseArmor = PlayerState.getBaseArmorValue(material);
             totalArmor += baseArmor * durabilityRatio;
          }
            Position pos = Position.ORIGIN;
            Tag posTag = rootMap.get("Pos");
            if (posTag instanceof ListTag) {
                List<Tag> posList = ((ListTag) posTag).getValue();
                if (posList.size() >= 3) {
                    double x = getDoubleFromTag(posList.get(0), 0.0);
                    double y = getDoubleFromTag(posList.get(1), 64.0);
                    double z = getDoubleFromTag(posList.get(2), 0.0);
                    pos = new Position(x, y, z);
                }
            }
            return new PlayerState(health, 20.0, totalArmor, pos, world);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return PlayerState.createDefault();
    }
    private static double getDoubleFromTag(Tag tag, double def) {
        if (tag instanceof DoubleTag) return ((DoubleTag) tag).getValue();
        if (tag instanceof FloatTag) return ((FloatTag) tag).getValue();
        if (tag instanceof IntTag) return ((IntTag) tag).getValue();
        if (tag instanceof ShortTag) return ((ShortTag) tag).getValue();
        return def;
    }
    public static org.bukkit.Material getMaterialFromId(String idStr) {
        try {
            String[] parts = idStr.split("\\.");
            int id = Integer.parseInt(parts[0]);
            for (org.bukkit.Material mat : org.bukkit.Material.values()) {
                if (mat.getId() == id) {
                    return mat;
                }
            }
        } catch (NumberFormatException e) {
            try {
                return org.bukkit.Material.valueOf(idStr.toUpperCase());
            } catch (IllegalArgumentException e2) {
            }
        }
        return null;
    }
}