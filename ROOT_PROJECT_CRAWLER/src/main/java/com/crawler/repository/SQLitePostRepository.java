package com.crawler.repository;

import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import com.crawler.model.SocialPost;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public SQLitePostRepository() {
        this.gson = new Gson();
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
        JsonArray jsonArray = new JsonArray();
        for (AbstractPost post : posts) {
            JsonObject jsonPost = serializePost(post);
            jsonArray.add(jsonPost);
        }

        String postsJson = gson.toJson(jsonArray);

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
                String postType = rs.getString("post_type");
                String postsJson = rs.getString("posts_json");

                // Deserialize JSON array
                JsonArray jsonArray = JsonParser.parseString(postsJson).getAsJsonArray();

                // POLYMORPHISM - Tạo NewsPost hoặc SocialPost dựa vào post_type
                if ("NewsPost".equals(postType)) {
                    return deserializeNewsPosts(jsonArray);
                } else if ("SocialPost".equals(postType)) {
                    return deserializeSocialPosts(jsonArray);
                } else {
                    System.err.println("✗ Unknown post type: " + postType);
                    return null;
                }
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

    // ========== HELPER METHODS - SERIALIZATION ==========

    /**
     * Serialize AbstractPost (NewsPost hoặc SocialPost) sang JsonObject
     * POLYMORPHISM: Xử lý cả NewsPost và SocialPost
     */
    private JsonObject serializePost(AbstractPost post) {
        JsonObject json = new JsonObject();

        // Trường chung từ AbstractPost
        json.addProperty("content", post.getContent());
        json.addProperty("platform", post.getPlatform());
        json.addProperty("sentiment", post.getSentiment());
        json.addProperty("location", post.getLocation());
        json.addProperty("focus", post.getFocus());

        // Trường riêng của từng loại Post
        if (post instanceof NewsPost) {
            NewsPost newsPost = (NewsPost) post;
            json.addProperty("postDate", newsPost.getPostDate() != null ? newsPost.getPostDate().toString() : null);
            json.addProperty("title", newsPost.getTitle());
            json.addProperty("comments", newsPost.getComments());
        } else if (post instanceof SocialPost) {
            SocialPost socialPost = (SocialPost) post;
            json.addProperty("createdDate", socialPost.getCreatedDate());
            json.addProperty("reaction", socialPost.getReaction());
        }

        return json;
    }

    /**
     * Deserialize JsonArray thành List<NewsPost>
     */
    private List<NewsPost> deserializeNewsPosts(JsonArray jsonArray) {
        List<NewsPost> posts = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject json = jsonArray.get(i).getAsJsonObject();

            String postDateStr = json.has("postDate") && !json.get("postDate").isJsonNull()
                    ? json.get("postDate").getAsString() : null;
            LocalDate postDate = postDateStr != null ? LocalDate.parse(postDateStr) : LocalDate.now();

            String title = json.has("title") ? json.get("title").getAsString() : "";
            String content = json.has("content") ? json.get("content").getAsString() : "";
            String platform = json.has("platform") ? json.get("platform").getAsString() : "";
            int comments = json.has("comments") ? json.get("comments").getAsInt() : 0;

            NewsPost post = new NewsPost(postDate, title, content, platform, comments);

            // Restore webhook metadata
            if (json.has("sentiment") && !json.get("sentiment").isJsonNull()) {
                post.setSentiment(json.get("sentiment").getAsString());
            }
            if (json.has("location") && !json.get("location").isJsonNull()) {
                post.setLocation(json.get("location").getAsString());
            }
            if (json.has("focus") && !json.get("focus").isJsonNull()) {
                post.setFocus(json.get("focus").getAsString());
            }

            posts.add(post);
        }

        return posts;
    }

    /**
     * Deserialize JsonArray thành List<SocialPost>
     */
    private List<SocialPost> deserializeSocialPosts(JsonArray jsonArray) {
        List<SocialPost> posts = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject json = jsonArray.get(i).getAsJsonObject();

            String content = json.has("content") ? json.get("content").getAsString() : "";
            String platform = json.has("platform") ? json.get("platform").getAsString() : "";
            String createdDate = json.has("createdDate") ? json.get("createdDate").getAsString() : "";
            long reaction = json.has("reaction") ? json.get("reaction").getAsLong() : 0;

            SocialPost post = new SocialPost(content, platform, createdDate, reaction);

            // Restore webhook metadata
            if (json.has("sentiment") && !json.get("sentiment").isJsonNull()) {
                post.setSentiment(json.get("sentiment").getAsString());
            }
            if (json.has("location") && !json.get("location").isJsonNull()) {
                post.setLocation(json.get("location").getAsString());
            }
            if (json.has("focus") && !json.get("focus").isJsonNull()) {
                post.setFocus(json.get("focus").getAsString());
            }

            posts.add(post);
        }

        return posts;
    }
}
