package main.java.org.matejko.discordsystem;

import main.java.org.matejko.discordsystem.utils.ImgBuilder.ItemInfo;
import org.jnbt.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerDataReader {
    public static final int ARMOR_SLOT_BASE = 100;
    private static final java.util.Set<Integer> SUBID_ITEMS = new java.util.HashSet<>();
    static {
        SUBID_ITEMS.add(6);
        SUBID_ITEMS.add(18);
        SUBID_ITEMS.add(31);
        SUBID_ITEMS.add(35);
        SUBID_ITEMS.add(43);
        SUBID_ITEMS.add(44);
        SUBID_ITEMS.add(351);
    }
    public static Map<Integer, ItemInfo> readPlayerDat(File datFile) throws IOException {
        Map<Integer, ItemInfo> out = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(datFile);
             NBTInputStream nis = new NBTInputStream(fis)) {
            Tag rootTag = nis.readTag();
            if (!(rootTag instanceof CompoundTag)) {
                throw new IOException("Unexpected root tag in dat file: " + datFile.getAbsolutePath());
            }
            CompoundTag root = (CompoundTag) rootTag;
            Map<String, Tag> rootMap = root.getValue();
            Tag invTag = rootMap.get("Inventory");
            if (invTag instanceof ListTag) {
                List<Tag> items = ((ListTag) invTag).getValue();
                for (Tag itemTag : items) {
                    if (!(itemTag instanceof CompoundTag)) continue;
                    Map<String, Tag> m = ((CompoundTag) itemTag).getValue();
                    int slot = -1;
                    int count = 1;
                    int damage = 0;
                    String idStr = null;
                    Tag slotTag = m.get("Slot");
                    if (slotTag instanceof ByteTag) slot = ((ByteTag) slotTag).getValue();
                    else if (slotTag instanceof ShortTag) slot = ((ShortTag) slotTag).getValue();
                    else if (slotTag instanceof IntTag) slot = ((IntTag) slotTag).getValue();
                    Tag countTag = m.get("Count");
                    if (countTag instanceof ByteTag) count = ((ByteTag) countTag).getValue();
                    else if (countTag instanceof IntTag) count = ((IntTag) countTag).getValue();
                    Tag damageTag = m.get("Damage");
                    if (damageTag instanceof ShortTag) damage = ((ShortTag) damageTag).getValue();
                    else if (damageTag instanceof IntTag) damage = ((IntTag) damageTag).getValue();
                    Tag idTag = m.get("id");
                    if (idTag instanceof ShortTag) {
                        idStr = Short.toString(((ShortTag) idTag).getValue());
                    } else if (idTag instanceof IntTag) {
                        idStr = Integer.toString(((IntTag) idTag).getValue());
                    } else if (idTag instanceof StringTag) {
                        idStr = ((StringTag) idTag).getValue();
                    }
                    if (slot >= 0 && idStr != null) {
                        int maxDur = getMaxDurabilityFromIdStr(idStr);
                        if (isSubIdItem(idStr)) {
                            String[] parts = idStr.split("\\.");
                            if (parts.length > 0) {
                                idStr = parts[0] + "." + damage;
                                damage = 0;
                            }
                        }
                        int slotKey = convertSlot(slot, idStr);
                        out.put(slotKey, new ItemInfo(idStr, count, damage, maxDur));
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                int slotKey = ARMOR_SLOT_BASE + i;
                if (!out.containsKey(slotKey)) {
                    out.put(slotKey, new ItemInfo("0", 0, 0, 0));
                }
            }
        }
        return out;
    }
    private static int convertSlot(int nbtSlot, String idStr) {
        if (nbtSlot >= 100 && nbtSlot <= 103) {
            int armorIndex = nbtSlot - 100; 
            int reversedIndex = 3 - armorIndex; 
            org.bukkit.Material material = OfflinePlayerState.getMaterialFromId(idStr);
            if (material != null && isArmor(material)) {
                int slotIndex;
                switch (material) {
                    case LEATHER_HELMET:
                    case GOLD_HELMET:
                    case CHAINMAIL_HELMET:
                    case IRON_HELMET:
                    case DIAMOND_HELMET:
                        slotIndex = 0; 
                        break;
                    case LEATHER_CHESTPLATE:
                    case GOLD_CHESTPLATE:
                    case CHAINMAIL_CHESTPLATE:
                    case IRON_CHESTPLATE:
                    case DIAMOND_CHESTPLATE:
                        slotIndex = 1; 
                        break;
                    case LEATHER_LEGGINGS:
                    case GOLD_LEGGINGS:
                    case CHAINMAIL_LEGGINGS:
                    case IRON_LEGGINGS:
                    case DIAMOND_LEGGINGS:
                        slotIndex = 2; 
                        break;
                    case LEATHER_BOOTS:
                    case GOLD_BOOTS:
                    case CHAINMAIL_BOOTS:
                    case IRON_BOOTS:
                    case DIAMOND_BOOTS:
                        slotIndex = 3; 
                        break;
                    default:
                        slotIndex = reversedIndex; 
                }
                return ARMOR_SLOT_BASE + slotIndex;
            }
            return ARMOR_SLOT_BASE + reversedIndex;
        }
        return nbtSlot;
    }
    private static boolean isArmor(org.bukkit.Material material) {
        return material.toString().endsWith("_HELMET") ||
               material.toString().endsWith("_CHESTPLATE") ||
               material.toString().endsWith("_LEGGINGS") ||
               material.toString().endsWith("_BOOTS");
    }
    private static boolean isSubIdItem(String idStr) {
        try {
            String[] parts = idStr.split("\\.");
            int id = Integer.parseInt(parts[0]);
            return SUBID_ITEMS.contains(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private static int getMaxDurabilityFromIdStr(String idStr) {
        org.bukkit.Material material = OfflinePlayerState.getMaterialFromId(idStr);
        if (material != null) {
            int maxDur = material.getMaxDurability();
            return maxDur > 0 ? maxDur : 0;
        }
        return 0;
    }
}