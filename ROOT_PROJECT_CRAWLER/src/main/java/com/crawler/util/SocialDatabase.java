package com.crawler.util;

import com.crawler.model.AbstractPost;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Tiny helper for storing generic AbstractPost rows into a local SQLite file.
 * POLYMORPHISM: Có thể lưu cả NewsPost và SocialPost (DIP - Depend on Abstraction)
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
                    engagement_score INTEGER
                );
                """;
            stmt.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a batch of posts (from any platform) to the DB.
     * POLYMORPHISM: Accepts List<? extends AbstractPost> - có thể là NewsPost hoặc SocialPost
     * This APPENDS rows; it does not overwrite existing data.
     */
    public static void savePosts(List<? extends AbstractPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO posts(platform, content, date, engagement_score) VALUES(?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (AbstractPost p : posts) {
                // POLYMORPHISM - getEngagementScore() hoạt động khác nhau cho NewsPost vs SocialPost
                ps.setString(1, p.getPlatform());
                ps.setString(2, p.getContent());
                ps.setString(3, p.getDisplayDate());
                ps.setLong(4, p.getEngagementScore());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

            System.out.println("Đã lưu " + posts.size() + " posts vào database.");

        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu vào database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
