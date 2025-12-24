package main.java.org.matejko.discordsystem;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;

public class UUIDCache {
    private final JSONArray entries;
    public UUIDCache(File jsonFile) throws Exception {
        String content = new String(Files.readAllBytes(jsonFile.toPath()));
        entries = new JSONArray(content);
    }
    public String getUUIDForName(String playerName) {
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (entry.getString("name").equalsIgnoreCase(playerName)) {
                return entry.getString("uuid");
            }
        }
        return null;
    }
}
