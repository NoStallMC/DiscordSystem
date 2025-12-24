package main.java.org.matejko.discordsystem.utils;

import main.java.org.matejko.discordsystem.DiscordPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiSetGetter {
    private static final String EMOJI_URL = "https://raw.githubusercontent.com/github/gemoji/master/db/emoji.json";
    private static Map<String, String> emojiMap;
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(":([a-z0-9_+-]+):");
    public static Map<String, String> getEmojiMap() {
        if (emojiMap != null && !emojiMap.isEmpty()) {
            return emojiMap;
        }
        emojiMap = new HashMap<>();
        try {
            DiscordPlugin.instance().getLogger().info("[DiscordSystem] Fetching Gemoji map from web.");
            URL url = new URL(EMOJI_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            StringBuilder jsonText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonText.append(line);
                }
            }
            JSONArray rootArray = new JSONArray(jsonText.toString());
            for (int i = 0; i < rootArray.length(); i++) {
                JSONObject obj = rootArray.getJSONObject(i);
                if (!obj.has("emoji") || !obj.has("aliases")) continue;
                String unicode = obj.getString("emoji");
                JSONArray aliases = obj.getJSONArray("aliases");
                for (int j = 0; j < aliases.length(); j++) {
                    String alias = aliases.getString(j);
                    emojiMap.put(alias, unicode);
                }
            }
            DiscordPlugin.instance().getLogger().info("[DiscordSystem] Successfully loaded " + emojiMap.size() + " emojis.");
        } catch (Exception e) {
            e.printStackTrace();
            DiscordPlugin.instance().getLogger().severe("[DiscordSystem] Failed to load emojis!");
        }
        return emojiMap;
    }
    public static String translateEmojis(String input) {
        if (input == null || input.isEmpty()) return input;
        getEmojiMap();
        Matcher matcher = SHORTCODE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String code = matcher.group(1);
            String replacement = emojiMap.getOrDefault(code, ":" + code + ":");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}