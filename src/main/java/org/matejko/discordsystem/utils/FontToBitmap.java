package main.java.org.matejko.discordsystem.utils;

import javax.imageio.ImageIO;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.configuration.Config;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontToBitmap {
    private static final int GRID_W = 14;
    private static final int GRID_H = 16;
    private static final int NUM_WIDTH = 10;
    private static final int NUM_HEIGHT = 14;
    private static final int NUM_SPACING = 6;
    private static final int MAX_X = 333;
    private static final Map<Character, CharacterData> fontMap = new HashMap<>();
    private static DiscordPlugin plugin;
    private static Config config;
    private static boolean initialized = false;

    private static class CharacterData {
        byte[] bits;
        int width, height;
        CharacterData(byte[] bits, int w, int h) { 
            this.bits = bits; 
            this.width = w; 
            this.height = h; 
        }
    }
    public static void init(DiscordPlugin p, Config c) {
        if (initialized) return;
        plugin = p;
        config = c;
        plugin.getLogger().info("[DiscordSystem] loading bitmap...");
        loadAndCropFont();
        loadNumbers();
        initialized = true;
        if (!fontMap.isEmpty()) {
        	plugin.getLogger().info("[DiscordSystem] Successfully loaded bitmap.");
        }
    }
    private static void loadAndCropFont() {
        try (InputStream is = FontToBitmap.class.getResourceAsStream("/assets/font.png")) {
            if (is == null) {
            	plugin.getLogger().severe("[DiscordSystem] FONT ERROR: /assets/font.png not found!");
                return;
            }
            BufferedImage sheet = ImageIO.read(is);
            String[] rows = {
                "ABCDEFGHIJKLMNOP", 
                "QRSTUVWXYZ_abcdef", 
                "ghijklmnopqrstuv", 
                "wxyz"               
            };
            int loadedCount = 0;
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                String rowChars = rows[rowIndex];
                for (int colIndex = 0; colIndex < rowChars.length(); colIndex++) {
                    char c = rowChars.charAt(colIndex);
                    int sx = colIndex * GRID_W;
                    int sy = rowIndex * GRID_H;
                    if (sx + GRID_W <= sheet.getWidth() && sy + GRID_H <= sheet.getHeight()) {
                        BufferedImage cell = sheet.getSubimage(sx, sy, GRID_W, GRID_H);
                        CharacterData data = cropAndConvertToBits(cell, GRID_W, GRID_H);
                        if (data.bits.length > 0) {
                            fontMap.put(c, data);
                            loadedCount++;
                        }
                    }
                }
            }
            if (config.debugEnabled()) {
            	plugin.getLogger().info("[DiscordSystem] Loaded " + loadedCount + " letters.");
            }
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
    }
    private static void loadNumbers() {
        try (InputStream is = FontToBitmap.class.getResourceAsStream("/assets/num.png")) {
            if (is == null) {
                plugin.getLogger().severe("[DiscordSystem] FONT ERROR: /assets/num.png not found!");
                return;
            }
            BufferedImage sheet = ImageIO.read(is);
            int count = 0;
            for (int i = 0; i <= 9; i++) {
                char c = (char) ('0' + i);
                int sx = i * (NUM_WIDTH + NUM_SPACING);
                if (sx + NUM_WIDTH <= sheet.getWidth()) {
                    BufferedImage cell = sheet.getSubimage(sx, 0, NUM_WIDTH, NUM_HEIGHT);
                    CharacterData data = cropAndConvertToBits(cell, NUM_WIDTH, NUM_HEIGHT);
                    if (data.bits.length > 0) {
                        fontMap.put(c, data);
                        count++;
                    }
                }
            }
            if (config.debugEnabled()) {
            	plugin.getLogger().info("[DiscordSystem] Loaded " + count + " numbers.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static CharacterData cropAndConvertToBits(BufferedImage img, int maxWidth, int maxHeight) {
        int minX = maxWidth, maxX = 0;
        boolean found = false;
        for (int y = 0; y < maxHeight; y++) {
            for (int x = 0; x < maxWidth; x++) {
                if (isWhite(img.getRGB(x, y))) {
                    minX = Math.min(minX, x); 
                    maxX = Math.max(maxX, x);
                    found = true;
                }
            }
        }
        if (!found) return new CharacterData(new byte[0], 4, maxHeight); 
        int w = (maxX - minX) + 1;
        int h = maxHeight; 
        byte[] bits = new byte[(w * h + 7) / 8];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isWhite(img.getRGB(x + minX, y))) {
                    int bitPos = y * w + x;
                    bits[bitPos / 8] |= (1 << (7 - (bitPos % 8)));
                }
            }
        }
        return new CharacterData(bits, w, h);
    }
    private static boolean isWhite(int rgb) {
        int alpha = (rgb >> 24) & 0xFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (alpha > 50) && (r > 240 && g > 240 && b > 240);
    }
    public static void drawText(Graphics2D g, String text, int startX, int startY) {
        if (!initialized) {
            System.err.println("[DiscordSystem] FontToBitmap not initialized! Call init(plugin) first.");
            return;
        }
        if (text == null) return;
        int currentX = startX;
        boolean truncated = false;
        for (char c : text.toCharArray()) {
            if (c == ' ') { 
                currentX += 8; 
                continue; 
            }
            CharacterData data = fontMap.get(c);
            if (data == null || data.bits.length == 0) {
                currentX += 4; 
                continue;
            }
            if (currentX > MAX_X) { 
                truncated = true;
                break;
            }
            for (int i = 0; i < data.width * data.height; i++) {
                int byteIdx = i / 8;
                int bitIdx = 7 - (i % 8);
                if (byteIdx < data.bits.length && ((data.bits[byteIdx] >> bitIdx) & 1) == 1) {
                    g.fillRect(currentX + (i % data.width), startY + (i / data.width), 1, 1);
                }
            }
            currentX += data.width + 2; 
        }
        if (truncated) {
            g.setColor(Color.BLACK); 
            int dotY = startY + 13;
            g.fillRect(currentX, dotY, 2, 2);
            g.fillRect(currentX + 4, dotY, 2, 2);
            g.fillRect(currentX + 8, dotY, 2, 2);
        }
    }
}