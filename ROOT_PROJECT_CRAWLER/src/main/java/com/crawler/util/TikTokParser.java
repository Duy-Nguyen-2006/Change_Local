package com.crawler.util;

import com.google.gson.*;
import com.crawler.model.SocialPost;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TikTokParser {

    /**
     * Parse one search-response JSON page into a list of SocialPost objects.
     * Only keeps: platform, content, createdDate, reaction.
     */
    public static List<SocialPost> parseSearchResult(String json) {
        List<SocialPost> list = new ArrayList<>();

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject data = root.getAsJsonObject("data");
        if (data == null || !data.has("videos")) {
            return list; // empty result / error handling
        }
        JsonArray videos = data.getAsJsonArray("videos");

        for (JsonElement el : videos) {
            JsonObject v = el.getAsJsonObject();

            // caption / text
            String content = v.get("title").getAsString();

            // reaction = likes + shares + comments
            long likes = v.get("digg_count").getAsLong();
            long shares = v.get("share_count").getAsLong();
            long comments = v.get("comment_count").getAsLong();
            long reaction = likes + shares + comments;

            // unix epoch seconds -> local date
            long createdAt = v.get("create_time").getAsLong();
            LocalDate createdDate = Instant.ofEpochSecond(createdAt)
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toLocalDate();

            String sourceId = extractId(v);

            SocialPost item = new SocialPost(sourceId, content, "tiktok", createdDate, reaction);
            list.add(item);
        }
        return list;
    }

    private static String extractId(JsonObject video) {
        if (video.has("video_id")) {
            return video.get("video_id").getAsString();
        }
        if (video.has("aweme_id")) {
            return video.get("aweme_id").getAsString();
        }
        if (video.has("id")) {
            return video.get("id").getAsString();
        }
        return "";
    }

    /** Read the "cursor" field returned by the API to get the next page. */
    public static int extractNextCursor(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject data = root.getAsJsonObject("data");
        if (data == null || !data.has("cursor")) {
            return -1; // signal "no more"
        }
        return data.get("cursor").getAsInt();
    }

    /** Whether the API reports that more pages are available. */
    public static boolean hasMore(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject data = root.getAsJsonObject("data");
        return data != null && data.has("has_more") && data.get("has_more").getAsBoolean();
    }
}
