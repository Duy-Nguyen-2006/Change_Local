package com.crawler.util;

import com.google.gson.*;
import com.crawler.model.SocialPost;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class XParser {

    private static final DateTimeFormatter TWITTER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    public static List<SocialPost> parseSearchResult(String json) {
        List<SocialPost> posts = new ArrayList<>();

        JsonElement root = JsonParser.parseString(json);

        if (root.isJsonObject()
                && root.getAsJsonObject().has("body")
                && root.getAsJsonObject().get("body").isJsonObject()) {
            root = root.getAsJsonObject().get("body");
        }

        dfsCollectTweets(root, posts);
        return posts;
    }

    private static void dfsCollectTweets(JsonElement element, List<SocialPost> posts) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("legacy") && obj.get("legacy").isJsonObject()) {
                JsonObject legacy = obj.getAsJsonObject("legacy");

                boolean hasText =
                        legacy.has("full_text") || legacy.has("text");

                if (hasText) {
                    SocialPost p = legacyToPost(legacy);
                    if (p != null) {
                        posts.add(p);
                    }
                }
            }

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

    private static SocialPost legacyToPost(JsonObject legacy) {
        String text = null;
        if (legacy.has("full_text")) {
            text = legacy.get("full_text").getAsString();
        } else if (legacy.has("text")) {
            text = legacy.get("text").getAsString();
        }
        if (text == null || text.isBlank()) return null;

        LocalDate createdDate = null;
        if (legacy.has("created_at")) {
            String createdAtStr = legacy.get("created_at").getAsString();
            try {
                ZonedDateTime zdt =
                        ZonedDateTime.parse(createdAtStr, TWITTER_DATE_FORMAT);
                createdDate = zdt.withZoneSameInstant(
                                ZoneId.of("Asia/Ho_Chi_Minh"))
                        .toLocalDate();
            } catch (Exception ignored) {
                createdDate = null;
            }
        }

        long likes = legacy.has("favorite_count")
                ? legacy.get("favorite_count").getAsLong()
                : 0L;
        long retweets = legacy.has("retweet_count")
                ? legacy.get("retweet_count").getAsLong()
                : 0L;
        long reaction = likes + retweets;

        String sourceId = "";
        if (legacy.has("id_str")) {
            sourceId = legacy.get("id_str").getAsString();
        } else if (legacy.has("id")) {
            sourceId = legacy.get("id").getAsString();
        }

        return new SocialPost(sourceId, text, "x", createdDate, reaction);
    }

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
