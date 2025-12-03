package com.crawler.model;

import java.time.LocalDate;
import com.crawler.util.CSVFormat;

/**
 * Lớp Post để đựng thông tin bài viết - ÁP DỤNG ENCAPSULATION
 * Tất cả các trường đã được đổi thành PRIVATE và cung cấp getter/setter
 */
public class Post implements CSVFormat {
    // 1. CHUYỂN TẤT CẢ THÀNH PRIVATE (FIX LỖI ENCAPSULATION)
    private LocalDate postDate;
    private String title;
    private String content;
    private String platform;
    private int comments;
    private long reaction;
    private String createdDate;

    public static final String[] HEADER = {"date", "title", "content", "platform", "comments", "reaction"};

    // Constructor cho news crawler (VNExpress, Dantri)
    public Post(LocalDate postDate, String title, String content, String platform, int comments) {
        this.postDate = postDate;
        this.title = title;
        this.content = content;
        this.platform = platform;
        this.comments = comments;
        this.reaction = 0;
        this.createdDate = postDate != null ? postDate.toString() : "";
    }

    // Constructor cho social media crawler (TikTok, X)
    public Post(String platform, String content, String createdDate, long reaction) {
        this.platform = platform;
        this.content = content;
        this.createdDate = createdDate;
        this.reaction = reaction;
        this.comments = 0;
        this.title = "";
        this.postDate = null;
    }

    // No-arg constructor
    public Post() {
        this.platform = "";
        this.content = "";
        this.createdDate = "";
        this.reaction = 0;
        this.comments = 0;
        this.title = "";
        this.postDate = null;
    }

    // 2. CUNG CẤP CÁC PHƯƠNG THỨC GETTER (READ-ONLY ACCESS)
    public LocalDate getPostDate() {
        return postDate;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getPlatform() {
        return platform;
    }

    public int getComments() {
        return comments;
    }

    public long getReaction() {
        return reaction;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    // 3. CUNG CẤP CÁC PHƯƠNG THỨC SETTER (CONTROLLED WRITE ACCESS)
    public void setPostDate(LocalDate postDate) {
        this.postDate = postDate;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title không được rỗng!");
        }
        this.title = title;
    }

    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content không được rỗng!");
        }
        this.content = content;
    }

    public void setPlatform(String platform) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException("Platform không được rỗng!");
        }
        this.platform = platform;
    }

    public void setComments(int comments) {
        if (comments < 0) {
            throw new IllegalArgumentException("Comments không được âm!");
        }
        this.comments = comments;
    }

    public void setReaction(long reaction) {
        if (reaction < 0) {
            throw new IllegalArgumentException("Reaction không được âm!");
        }
        this.reaction = reaction;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Dành cho việc viết vào file .csv.
     * @return Array of strings for CSV export
     */
    @Override
    public String[] csvParse() {
        String dateStr = postDate != null ? postDate.toString() : createdDate;
        return new String[] {
            dateStr,
            title,
            content,
            platform,
            Integer.toString(comments),
            Long.toString(reaction)
        };
    }
}
