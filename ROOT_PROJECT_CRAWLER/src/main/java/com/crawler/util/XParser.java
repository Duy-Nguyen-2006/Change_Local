package com.crawler.util;

import com.google.gson.*;
import com.crawler.model.Post;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class XParser {

    // Example: "Fri Mar 22 22:44:22 +0000 2024"
    private static final DateTimeFormatter TWITTER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    /**
     * Parse one search-response JSON page into a list of Post.
     * This version just walks the entire JSON and collects any "legacy"
     * object that clearly looks like a tweet (has full_text/text).
     */
    public static List<Post> parseSearchResult(String json) {
        List<Post> posts = new ArrayList<>();

        JsonElement root = JsonParser.parseString(json);

        // Some RapidAPI endpoints wrap real payload inside "body"
        if (root.isJsonObject()
                && root.getAsJsonObject().has("body")
                && root.getAsJsonObject().get("body").isJsonObject()) {
            root = root.getAsJsonObject().get("body");
        }

        dfsCollectTweets(root, posts);
        return posts;
    }

    /**
     * Depth-first search: whenever we see a JsonObject with field "legacy"
     * that contains full_text/text, treat it as a tweet.
     */
    private static void dfsCollectTweets(JsonElement element, List<Post> posts) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // Tweet candidate?
            if (obj.has("legacy") && obj.get("legacy").isJsonObject()) {
                JsonObject legacy = obj.getAsJsonObject("legacy");

                boolean hasText =
                        legacy.has("full_text") || legacy.has("text");

                if (hasText) {
                    Post p = legacyToPost(legacy);
                    if (p != null) {
                        posts.add(p);
                    }
                }
            }

            // Recurse on all values
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                dfsCollectTweets(e.getValue(), posts);
            }

        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement child : arr) {
                dfsCollectTweets(child, posts);
            }
        }
    }

    /**
     * Convert a tweet "legacy" object into our Post {platform, content, createdDate, reaction}.
     */
    private static Post legacyToPost(JsonObject legacy) {
        // text
        String text = null;
        if (legacy.has("full_text")) {
            text = legacy.get("full_text").getAsString();
        } else if (legacy.has("text")) {
            text = legacy.get("text").getAsString();
        }
        if (text == null || text.isBlank()) return null;

        String createdDate = "";
        // created_at → yyyy-MM-dd in Asia/Ho_Chi_Minh if possible
        if (legacy.has("created_at")) {
            String createdAtStr = legacy.get("created_at").getAsString();
            try {
                ZonedDateTime zdt =
                        ZonedDateTime.parse(createdAtStr, TWITTER_DATE_FORMAT);
                createdDate = zdt.withZoneSameInstant(
                                ZoneId.of("Asia/Ho_Chi_Minh"))
                        .toLocalDate()
                        .toString();
            } catch (Exception e) {
                // If parsing fails, store raw string
                createdDate = createdAtStr;
            }
        }

        long likes = legacy.has("favorite_count")
                ? legacy.get("favorite_count").getAsLong()
                : 0L;
        long retweets = legacy.has("retweet_count")
                ? legacy.get("retweet_count").getAsLong()
                : 0L;
        long reaction = likes + retweets;

        return new Post("x", text, createdDate, reaction);
    }

    /**
     * Extract the "bottom" cursor from payload:
     * {
     *   "cursor": { "bottom": "...", "top": "..." },
     *   "result": { ... }
     * }
     * (possibly wrapped in "body").
     */
    public static String extractNextCursor(String json) {
        JsonElement root = JsonParser.parseString(json);

        if (root.isJsonObject()
                && root.getAsJsonObject().has("body")
                && root.getAsJsonObject().get("body").isJsonObject()) {
            root = root.getAsJsonObject().get("body");
        }

        if (!root.isJsonObject()) return null;
        JsonObject obj = root.getAsJsonObject();

        if (!obj.has("cursor") || !obj.get("cursor").isJsonObject()) return null;
        JsonObject cursor = obj.getAsJsonObject("cursor");

        if (cursor.has("bottom") && !cursor.get("bottom").isJsonNull()) {
            return cursor.get("bottom").getAsString();
        }
        return null;
    }
}
