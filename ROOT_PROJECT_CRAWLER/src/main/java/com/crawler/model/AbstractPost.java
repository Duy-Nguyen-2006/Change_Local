package com.crawler.model;

/**
 * Lớp trừu tượng gốc cho tất cả các loại bài viết.
 * ENCAPSULATION: mọi field đều private.
 * ABSTRACTION: định nghĩa hành vi chung cần lớp con override.
 */
public abstract class AbstractPost {
    // Trường chung
    private String sourceId;
    private String content;
    private String platform;

    // Webhook metadata - MUTABLE
    private String sentiment;
    private String location;
    private String focus;
    private String direction;

    /**
     * Constructor chung cho lớp con gọi.
     * @param sourceId ID duy nhất từ nguồn (URL/ID gốc)
     * @param content Nội dung bài viết
     * @param platform Nền tảng (VNExpress, TikTok, X, Dantri...)
     */
    public AbstractPost(String sourceId, String content, String platform) {
        this.sourceId = sourceId;
        this.content = content;
        this.platform = platform;
    }

    /**
     * Constructor mặc định.
     */
    public AbstractPost() {
        this("", "", "");
    }

    // ========== GETTERS ONLY - NO SETTERS (DATA INTEGRITY) ==========
    public String getSourceId() {
        return sourceId;
    }

    public String getContent() {
        return content;
    }

    public String getPlatform() {
        return platform;
    }

    // ========== WEBHOOK METADATA - GETTERS & SETTERS ==========
    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    // ========== PHƯƠNG THỨC TRỪU TƯỢNG ==========
    public abstract String getDisplayDate();

    public abstract long getEngagementScore();

    public abstract String[] toCsvArray();

    public abstract String[] getCsvHeader();
}
