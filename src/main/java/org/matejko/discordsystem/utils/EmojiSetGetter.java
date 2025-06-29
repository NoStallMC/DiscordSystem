package main.java.org.matejko.discordsystem.utils;

import main.java.org.matejko.discordsystem.DiscordPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EmojiSetGetter {
    private static final String EMOJI_JSON_URL = "https://raw.githubusercontent.com/NoStallMC/Utilis/refs/heads/main/discord-emojis.json";
    private static Map<String, String> emojiMap;

    private static File getLocalFile() {
        return new File(DiscordPlugin.instance().getDataFolder(), "discord-emojis.json");
    }

    private static void downloadIfNeeded() throws IOException {
        File file = getLocalFile();
        if (file.exists()) return;
        URL url = new URL(EMOJI_JSON_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (InputStream in = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(file)) {
            	byte[] buffer = new byte[8192];
            	int bytesRead;
            	while ((bytesRead = in.read(buffer)) != -1) {
            		out.write(buffer, 0, bytesRead);
            }
        }
    }

    private static Map<String, String> loadEmojiMapFromJson() throws Exception {
        downloadIfNeeded();
        StringBuilder jsonText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(getLocalFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }
        }
        JSONObject root = new JSONObject(jsonText.toString());
        Map<String, String> map = new HashMap<>();
        for (String categoryKey : root.keySet()) {
            JSONArray emojiArray = root.getJSONArray(categoryKey);
            for (int i = 0; i < emojiArray.length(); i++) {
                JSONObject emojiObj = emojiArray.getJSONObject(i);
                JSONArray namesArray = emojiObj.optJSONArray("names");
                String surrogates = emojiObj.optString("surrogates", "");
                if (namesArray == null || surrogates.isEmpty()) continue;
                for (int j = 0; j < namesArray.length(); j++) {
                    String name = namesArray.getString(j);
                    map.put(name, surrogates);
                }
            }
        }
        return map;
    }

    public static String translateEmojis(String input) {
        if (input == null || input.isEmpty()) return input;
        if (emojiMap == null) {
            try {
                emojiMap = loadEmojiMapFromJson();
            } catch (Exception e) {
                e.printStackTrace();
                emojiMap = Collections.emptyMap();
            }
        }
        if (emojiMap.isEmpty()) return input;
        String result = input;
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            result = result.replace(":" + entry.getKey() + ":", entry.getValue());
        }
        return result;
    }

    public static Map<String, String> getEmojiMap() {
        if (emojiMap == null) {
            try {
                emojiMap = loadEmojiMapFromJson();
            } catch (Exception e) {
                e.printStackTrace();
                emojiMap = Collections.emptyMap();
            }
        }
        return emojiMap;
    }
}
