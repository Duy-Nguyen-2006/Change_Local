package com.crawler.util;

import com.crawler.model.Post;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Tiny helper for storing generic Post rows into a local SQLite file.
 * Used by TikTok crawler, X crawler, etc.
 */
public class SocialDatabase {

    // Same DB file as before – do NOT change if you already have data there.
    private static final String DB_URL = "jdbc:sqlite:disaster_post_data.db";

    static {
        // Initialize table on first class load
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sql = """
                CREATE TABLE IF NOT EXISTS posts (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    platform TEXT    NOT NULL,
                    content  TEXT    NOT NULL,
                    date     TEXT,
                    reaction INTEGER
                );
                """;
            stmt.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a batch of posts (from any platform) to the DB.
     * This APPENDS rows; it does not overwrite existing data.
     */
    public static void savePosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO posts(platform, content, date, reaction) VALUES(?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (Post p : posts) {
                // Make sure platform is set by caller ("tiktok", "x", ...)
                ps.setString(1, p.getPlatform());
                ps.setString(2, p.getContent());
                ps.setString(3, p.getCreatedDate());
                ps.setLong(4, p.getReaction());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
