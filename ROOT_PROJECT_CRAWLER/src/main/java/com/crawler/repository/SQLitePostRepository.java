package com.crawler.repository;

import com.crawler.model.AbstractPost;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.lang.reflect.Type;

/**
 * SQLitePostRepository - CONCRETE IMPLEMENTATION của IPostRepository
 *
 * REPOSITORY PATTERN:
 * - Tách logic truy cập database ra khỏi Business Logic
 * - Service Layer KHÔNG biết dữ liệu lưu ở SQLite
 * - Dễ dàng thay SQLite bằng MySQL/MongoDB mà KHÔNG ẢNH HƯỞNG Service
 *
 * DIP: Implement interface IPostRepository (abstraction)
 * SRP: Chỉ có MỘT trách nhiệm - Lưu trữ và truy xuất dữ liệu từ SQLite
 *
 * DATABASE SCHEMA:
 * CREATE TABLE post_cache (
 *   keyword TEXT PRIMARY KEY,
 *   post_type TEXT NOT NULL,        -- "NewsPost" hoặc "SocialPost"
 *   posts_json TEXT NOT NULL,       -- JSON array chứa tất cả posts
 *   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 */
public class SQLitePostRepository implements IPostRepository {

    private static final String DB_URL = "jdbc:sqlite:crawler_cache.db";
    private static final String TABLE_NAME = "post_cache";
    private final Gson gson;
    private final Type listType = new TypeToken<List<AbstractPost>>() {}.getType();

    public SQLitePostRepository() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeHierarchyAdapter(AbstractPost.class, new PostTypeAdapter())
                .create();
        initDatabase();
    }

    /**
     * Khởi tạo database và table nếu chưa tồn tại
     */
    private void initDatabase() {
        String createTableSQL = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
            "  keyword TEXT PRIMARY KEY," +
            "  post_type TEXT NOT NULL," +
            "  posts_json TEXT NOT NULL," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")", TABLE_NAME
        );

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            System.out.println("✓ SQLite database initialized: " + DB_URL);

        } catch (SQLException e) {
            System.err.println("✗ Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lưu danh sách posts vào SQLite
     * POLYMORPHISM: Nhận List<? extends AbstractPost> (NewsPost hoặc SocialPost)
     *
     * Strategy:
     * 1. Xác định post type (NewsPost hay SocialPost)
     * 2. Serialize toàn bộ list sang JSON
     * 3. Lưu vào SQLite với keyword làm cache key
     */
    @Override
    public void save(List<? extends AbstractPost> posts, String keyword) {
        if (posts == null || posts.isEmpty()) {
            System.out.println("No posts to save for keyword: " + keyword);
            return;
        }

        // Xác định post type từ phần tử đầu tiên
        String postType = posts.get(0).getClass().getSimpleName();

        // Serialize list sang JSON
        String postsJson = gson.toJson(posts, listType);

        // INSERT hoặc REPLACE vào SQLite
        String sql = String.format(
            "INSERT OR REPLACE INTO %s (keyword, post_type, posts_json) VALUES (?, ?, ?)",
            TABLE_NAME
        );

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, keyword);
            pstmt.setString(2, postType);
            pstmt.setString(3, postsJson);
            pstmt.executeUpdate();

            System.out.println("✓ Saved " + posts.size() + " posts to cache (keyword: " + keyword + ")");

        } catch (SQLException e) {
            System.err.println("✗ Failed to save posts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load danh sách posts từ SQLite
     * POLYMORPHISM: Trả về List<NewsPost> hoặc List<SocialPost> dựa vào post_type
     */
    @Override
    public List<? extends AbstractPost> load(String keyword) {
        String sql = String.format(
            "SELECT post_type, posts_json FROM %s WHERE keyword = ?",
            TABLE_NAME
        );

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, keyword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String postsJson = rs.getString("posts_json");
                return gson.fromJson(postsJson, listType);
            } else {
                System.out.println("No cached data found for keyword: " + keyword);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("✗ Failed to load posts: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Kiểm tra xem keyword đã có trong cache chưa
     */
    @Override
    public boolean isCached(String keyword) {
        String sql = String.format(
            "SELECT 1 FROM %s WHERE keyword = ?",
            TABLE_NAME
        );

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, keyword);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("✗ Failed to check cache: " + e.getMessage());
            return false;
        }
    }

}
