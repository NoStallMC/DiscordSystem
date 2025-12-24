package main.java.org.matejko.discordsystem.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.bukkit.World;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.PlayerData;
import main.java.org.matejko.discordsystem.PlayerState;

public class ImgBuilder {
    public static class ItemInfo {
        public final String id;
        public final int count;
        public final int damage;
        public final int maxDurability;
        public ItemInfo(String id, int count, int damage, int maxDurability) {
            this.id = id;
            this.count = count;
            this.damage = damage;
            this.maxDurability = maxDurability;
        }
    }
    public static final int ARMOR_SLOT_BASE = 100;
    private static final int SLOT_SIZE = 32;
    private static final int COL_STEP = 36;
    private static final int ARMOR_X = 16;
    private static final int ARMOR_Y_START = 16;
    private static final int WORLD_X = 202;
    private static final int WORLD_Y_START = 20;
    private static final int INV_X_START = 16;
    private static final int INV_Y_START = 168;
    private static final int HOTBAR_Y = 284;
    private static final int COLUMNS = 9;
    private static final int INV_ROWS = 3;

    private static final String BG_PATH = "/assets/inventory.png";
    private static final String TILE_PATH = "/assets/tiles/";
    private static final String NUM_FONT_PATH = "/assets/num.png";
    private static final String HEART_FULL_PATH = "/assets/icons/full.png";
    private static final String HEART_HALF_PATH = "/assets/icons/half.png";
    private static final String ARMOR_FULL_PATH = "/assets/icons/full_armor.png";
    private static final String ARMOR_HALF_PATH = "/assets/icons/half_armor.png";

    private static BufferedImage numFont;
    private static BufferedImage cachedBg;
    private static BufferedImage heartFull;
    private static BufferedImage heartHalf;
    private static BufferedImage ARMORFull;
    private static BufferedImage ARMORHalf;

    private static final int NUM_WIDTH = 10;
    private static final int NUM_HEIGHT = 14;
    private static final int NUM_SPACING = 6;

    private static final int HEARTS_X1 = 164;
    private static final int HEARTS_Y1 = 100;
    private static final int HEART_COUNT = 10;
    private static final int HEART_SIZE = 18;

    private static final int ARMOR_X1 = 164;
    private static final int ARMOR_Y1 = 136;
    private static final int ARMOR_COUNT = 10;
    private static final int ARMOR_SIZE = 18;

    private static final Map<String, BufferedImage> itemSprites = new HashMap<>();
    private static final Map<Integer, Point> fontOffsets = new HashMap<>();
    static {
        for (int i = 0; i < 4; i++) fontOffsets.put(ARMOR_SLOT_BASE + i, new Point(0, 0));
        for (int i = 0; i < 36; i++) fontOffsets.put(i, new Point(0, 0));}
    static {
        Function<String, InputStream> getResStream = (path) -> {
            String absolutePath = path.startsWith("/") ? path : "/" + path;
            return ImgBuilder.class.getResourceAsStream(absolutePath);
        };
        try {
            InputStream is;
            if ((is = getResStream.apply(NUM_FONT_PATH)) != null) numFont = ImageIO.read(is);
            else System.err.println("Missing: " + NUM_FONT_PATH);
            if ((is = getResStream.apply(BG_PATH)) != null) cachedBg = ImageIO.read(is);
            else System.err.println("Missing: " + BG_PATH);
            if ((is = getResStream.apply(HEART_FULL_PATH)) != null) heartFull = ImageIO.read(is);
            if ((is = getResStream.apply(HEART_HALF_PATH)) != null) heartHalf = ImageIO.read(is);
            if ((is = getResStream.apply(ARMOR_FULL_PATH)) != null) ARMORFull = ImageIO.read(is);
            if ((is = getResStream.apply(ARMOR_HALF_PATH)) != null) ARMORHalf = ImageIO.read(is);
            URL codeSource = ImgBuilder.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSource != null) {
                String path = java.net.URLDecoder.decode(codeSource.getPath(), "UTF-8");
                File jarFile = new File(path);
                if (jarFile.isFile()) {
                    try (JarFile jar = new JarFile(jarFile)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        String prefix = TILE_PATH.startsWith("/") ? TILE_PATH.substring(1) : TILE_PATH;
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(prefix) && name.endsWith(".png")) {
                                try (InputStream tileIs = getResStream.apply("/" + name)) {
                                    if (tileIs != null) {
                                        String id = name.substring(name.lastIndexOf('/') + 1, name.length() - 4);
                                        BufferedImage img = ImageIO.read(tileIs);
                                        if (img != null) itemSprites.put(id, img);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    URL url = ImgBuilder.class.getResource(TILE_PATH);
                    if (url != null) scanTileFolder(Paths.get(url.toURI()));
                }
            }
            System.out.println("[DiscordSystem] Logic complete. Loaded " + itemSprites.size() + " tiles.");
        } catch (Exception e) {
            System.err.println("[DiscordSystem] Failed to initialize images!");
            e.printStackTrace();
        }
    }
    private static DiscordPlugin plugin;
    public ImgBuilder(DiscordPlugin plugin) {
    	ImgBuilder.plugin = plugin;
    }
    private static void scanTileFolder(Path folder) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.png")) {
            for (Path entry : stream) {
                try (InputStream is = Files.newInputStream(entry)) {
                    String id = entry.getFileName().toString().replace(".png", "");
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) itemSprites.put(id, img);
                }
            }
        }
    }
    public static byte[] renderInventory(PlayerData playerData) throws IOException {
        Map<Integer, ItemInfo> slotMap = playerData.getInventory();
        PlayerState state = playerData.getState();
        return renderInventory(
                slotMap,
                state.getHealth(),
                state.getMaxHealth(),
                state.getArmor(),
                state.getX(),
                state.getY(),
                state.getZ(),
                playerData.getUuid(),
                playerData.getPlayerName(),
                playerData.getWorld()
        );
    }
    public static byte[] renderInventory(
            Map<Integer, ItemInfo> slotMap,
            double health,
            double maxHealth,
            double armor,
            int posX,
            int posY,
            int posZ,
            String uuid,
            String playerName,
            World world
    ) throws IOException {
    	BufferedImage bg;
        try (InputStream is = ImgBuilder.class.getResourceAsStream(BG_PATH)) {
            if (is == null) throw new IOException("Missing file: " + BG_PATH);
            bg = ImageIO.read(is);
        }
        int width = bg.getWidth();
        int height = bg.getHeight();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(cachedBg, 0, 0, null);
        drawHealth(g, health, maxHealth);
        drawArmor(g, armor);
        drawWorld(g, world);
        renderPlayerSkin(g, uuid, playerName);
        drawPosition(g, posX, posY, posZ, 180, 36);
        drawArmorSlots(g, slotMap);
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slot = 9 + row * COLUMNS + col;
                drawItem(g, slotMap, slot, INV_X_START + col * COL_STEP, INV_Y_START + row * COL_STEP);
            }
        }
        for (int s = 0; s < COLUMNS; s++) {
            drawItem(g, slotMap, s, INV_X_START + s * COL_STEP, HOTBAR_Y);
        }
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
    public static void drawWorld(Graphics2D g, World world) {
        if (g == null || world == null) return;
        g.setColor(Color.BLACK);
        FontToBitmap.drawText(g, world.getName(), WORLD_X, WORLD_Y_START);
    }
    public static BufferedImage loadSkinFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream is = conn.getInputStream()) {
                BufferedImage img = ImageIO.read(is);
                if (img == null) plugin.getLogger().info("Failed to read image from: " + urlString);
                return img;
            }
        } catch (Exception e) {
            plugin.getLogger().info("Error loading skin: " + e.getMessage());
            return null;
        }
    }
    public static void renderPlayerSkin(Graphics2D g, String uuid, String playerName) {
        if (g == null) return;
        final int destX = 75;
        final int destY = 18;
        final int skinHeight = 56;
        BufferedImage skin = null;
        try {
            if (uuid != null && !uuid.isEmpty()) {
                skin = loadSkinFromUrl("https://mc-heads.net/body/" + uuid + "/" + skinHeight);
            }
            if (skin == null && playerName != null && !playerName.isEmpty()) {
                skin = loadSkinFromUrl("https://mc-heads.net/body/" + playerName + "/" + skinHeight);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (skin == null) return;
        g.drawImage(skin, destX, destY, null);
    }
    public static void drawArmor(Graphics2D g, double armor) {
        if (g == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        double armorIcons = armor / 2.0;
        double roundedArmorIcons = Math.ceil(armorIcons * 2) / 2.0;
        int startX = ARMOR_X1;
        int startY = ARMOR_Y1;
        for (int i = 0; i < ARMOR_COUNT; i++) {
            double iconThreshold = i + 1;
            int drawX = startX + i * ARMOR_SIZE;
            int drawY = startY;
            boolean isFull = roundedArmorIcons >= iconThreshold;
            boolean isHalf = roundedArmorIcons > i && roundedArmorIcons < iconThreshold;
            if (isFull) {
                if (ARMORFull != null) {
                    g.drawImage(ARMORFull, drawX, drawY, ARMOR_SIZE, ARMOR_SIZE, null);
                } else {
                    g.setColor(new Color(0x6666FF));
                    g.fillRect(drawX, drawY, ARMOR_SIZE, ARMOR_SIZE);
                }
            } else if (isHalf) {
                if (ARMORHalf != null) {
                    g.drawImage(ARMORHalf, drawX, drawY, ARMOR_SIZE, ARMOR_SIZE, null);
                } else {
                    g.setColor(new Color(0x6666FF));
                    g.fillRect(drawX, drawY, ARMOR_SIZE / 2, ARMOR_SIZE);
                    g.setColor(new Color(0x333333));
                    g.fillRect(drawX + ARMOR_SIZE / 2, drawY, ARMOR_SIZE / 2, ARMOR_SIZE);
                }
            } else {
                // Empty
            }
        }
    }
    private static void drawArmorSlots(Graphics2D g, Map<Integer, ItemInfo> slotMap) {
        for (int i = 0; i < 4; i++) {
            int slotKey = ARMOR_SLOT_BASE + i;
            int x = ARMOR_X;
            int y = ARMOR_Y_START + i * COL_STEP;
            drawItem(g, slotMap, slotKey, x, y);
        }
    }
    private static void drawItem(Graphics2D g, Map<Integer, ItemInfo> slotMap, int slot, int x, int y) {
        ItemInfo it = slotMap.get(slot);
        if (it == null) return;
        String lookupId = it.id;
        if (it.id.equals("17")) {
            lookupId = it.id + "." + it.damage;
        }
        BufferedImage sprite = itemSprites.get(lookupId);
        if (sprite != null) {
            g.drawImage(sprite, x, y, SLOT_SIZE, SLOT_SIZE, null);
        } else {
            int cx = x + SLOT_SIZE / 2;
            int cy = y + SLOT_SIZE / 2;
            g.setColor(new Color(180, 180, 180));
            g.fillOval(cx - 10, cy - 10, 20, 20);
        }
        if (it.count > 1) {
            Point offset = fontOffsets.getOrDefault(slot, new Point(0, 0));
            int textX = x + SLOT_SIZE - 3 + offset.x;
            int textY = y + SLOT_SIZE - 3 + offset.y;
            drawNumber(g, it.count, textX, textY);
        }
        if (!it.id.equals("17") && it.maxDurability > 0 && it.damage > 0 && it.damage < it.maxDurability) {
            drawDurabilityBar(g, it, x, y);
        }
    }
    private static void drawNumber(Graphics2D g, int number, int x, int y) {
        String text = String.valueOf(number);
        Color shadowColor = new Color(0x3f3f3f);
        int drawX = x - NUM_WIDTH + 3;
        int drawY = y - NUM_HEIGHT + 3;
        for (int idx = text.length() - 1; idx >= 0; idx--) {
            int digit = text.charAt(idx) - '0';
            int srcX = digit * (NUM_WIDTH + NUM_SPACING);
            BufferedImage digitImg = numFont.getSubimage(srcX, 0, NUM_WIDTH, NUM_HEIGHT);
            g.drawImage(colorize(digitImg, shadowColor), drawX + 2, drawY + 2, null);
            g.drawImage(digitImg, drawX, drawY, null);
            drawX -= (NUM_WIDTH + 2);
        }
    }
    public static void drawPosition(Graphics2D g, int x, int y, int z, int startX, int startY) {
        if (g == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        drawNumberPlain(g, x, startX, startY);
        drawNumberPlain(g, y, startX, startY + NUM_HEIGHT + 2);
        drawNumberPlain(g, z, startX, startY + (NUM_HEIGHT + 2) * 2);
    }
    private static void drawNumberPlain(Graphics2D g, int number, int x, int y) {
        String text = String.valueOf(number);
        int drawX = x;
        int drawY = y;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '-') {
                g.setColor(Color.BLACK);
                int minusWidth = NUM_WIDTH - 4;
                g.fillRect(drawX, drawY + NUM_HEIGHT / 2, minusWidth, 2);
                drawX += minusWidth + 2;
                continue;
            }
            int digit = c - '0';
            int srcX = digit * (NUM_WIDTH + NUM_SPACING);
            BufferedImage digitImg = numFont.getSubimage(srcX, 0, NUM_WIDTH, NUM_HEIGHT);
            g.drawImage(colorize(digitImg, Color.BLACK), drawX, drawY, null);
            drawX += NUM_WIDTH + 2;
        }
    }
    private static BufferedImage colorize(BufferedImage src, Color color) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int w = src.getWidth(), h = src.getHeight();
        int rgb = color.getRGB() & 0x00ffffff;
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int pixel = src.getRGB(xx, yy);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha != 0) out.setRGB(xx, yy, (alpha << 24) | rgb);
            }
        }
        return out;
    }
    private static void drawDurabilityBar(Graphics2D g, ItemInfo item, int x, int y) {
        int barWidth = SLOT_SIZE - 4;
        int barHeight = 3;
        int offsetX = x + 2;
        int offsetY = y + SLOT_SIZE - barHeight - 2;
        int max = Math.max(0, item.maxDurability);
        int dmg = Math.max(0, Math.min(item.damage, max));
        if (max <= 0) return;
        float remaining = (max - dmg) / (float) max;
        remaining = Math.max(0f, Math.min(1f, remaining));
        g.setColor(new Color(0x333333));
        g.fillRect(offsetX, offsetY, barWidth, barHeight);
        int filled = Math.round(barWidth * remaining);
        if (filled <= 0) return;
        Color col = durabilityColor(remaining);
        g.setColor(col);
        g.fillRect(offsetX, offsetY, filled, barHeight);
        g.setColor(new Color(0x000000));
        g.drawRect(offsetX, offsetY, barWidth, barHeight);
    }
    private static Color durabilityColor(float remaining) {
        if (remaining >= 0.5f) {
            float t = (remaining - 0.5f) / 0.5f;
            return lerpColor(new Color(0xFFFF00), new Color(0x00FF00), t);
        } else {
            float t = remaining / 0.5f;
            return lerpColor(new Color(0xFF0000), new Color(0xFFFF00), t);
        }
    }
    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
    public static void drawHealth(Graphics2D g, double health) {
        drawHealth(g, health, 20.0);
    }
    public static void drawHealth(Graphics2D g, double health, double maxHealth) {
        if (g == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        double heartsTotal = HEART_COUNT;
        double heartsAvailable = Math.max(0.0, Math.min(heartsTotal, (health / Math.max(1.0, maxHealth)) * heartsTotal));
        int startX = HEARTS_X1;
        int startY = HEARTS_Y1;
        for (int i = 0; i < HEART_COUNT; i++) {
            double heartIndex = i + 1;
            int drawX = startX + i * HEART_SIZE;
            int drawY = startY;
            if (heartsAvailable >= heartIndex) {
                if (heartFull != null) {
                    g.drawImage(heartFull, drawX, drawY, HEART_SIZE, HEART_SIZE, null);
                } else {
                    g.setColor(new Color(0xFF6666));
                    g.fillRect(drawX, drawY, HEART_SIZE, HEART_SIZE);
                }
            } else if (heartsAvailable > i && heartsAvailable < heartIndex) {
                if (heartHalf != null) {
                    g.drawImage(heartHalf, drawX, drawY, HEART_SIZE, HEART_SIZE, null);
                } else {
                    g.setColor(new Color(0xFF6666));
                    g.fillRect(drawX, drawY, HEART_SIZE / 2, HEART_SIZE);
                    g.setColor(new Color(0x333333));
                    g.fillRect(drawX + HEART_SIZE / 2, drawY, HEART_SIZE / 2, HEART_SIZE);
                }
            } else {
            }
        }
    }
    public static void setFontOffset(int slot, int offsetX, int offsetY) {
        fontOffsets.put(slot, new Point(offsetX, offsetY));
    }
}
