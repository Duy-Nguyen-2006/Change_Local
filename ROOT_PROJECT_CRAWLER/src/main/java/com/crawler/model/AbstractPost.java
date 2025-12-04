package com.crawler.model;

import java.util.Objects;

/**
 * Lớp trừu tượng gốc cho tất cả các loại bài viết.
 *
 * ENCAPSULATION:
 * - Mọi field đều private, chỉ expose getter cho core data.
 *
 * ABSTRACTION:
 * - Định nghĩa hành vi chung cần lớp con override.
 *
 * SRP:
 * - Core data (sourceId, content, platform) tách biệt với enrichment metadata (PostMetadata).
 */
public abstract class AbstractPost {
    // Trường chung (core data)
    private String sourceId;
    private String content;
    private String platform;

    // Webhook metadata được gom vào 1 value object riêng
    private PostMetadata metadata;

    /**
     * Constructor chung cho lớp con gọi.
     *
     * @param sourceId ID duy nhất từ nguồn (URL/ID gốc)
     * @param content  Nội dung bài viết
     * @param platform Nền tảng (VNExpress, TikTok, X, Dantri...)
     */
    public AbstractPost(String sourceId, String content, String platform) {
        this.sourceId = Objects.requireNonNullElse(sourceId, "");
        this.content = Objects.requireNonNullElse(content, "");
        this.platform = Objects.requireNonNullElse(platform, "");
        this.metadata = new PostMetadata(); // metadata rỗng mặc định
    }

    /**
     * Constructor mặc định (cần cho Gson / ORM).
     */
    public AbstractPost() {
        this("", "", "");
    }

    // ========== GETTERS ONLY CHO CORE DATA ==========
    public String getSourceId() {
        return sourceId;
    }

    public String getContent() {
        return content;
    }

    public String getPlatform() {
        return platform;
    }

    // Cho phép Gson/deserializer set lại core data nếu cần
    public void setSourceId(String sourceId) {
        this.sourceId = Objects.requireNonNullElse(sourceId, "");
    }

    public void setContent(String content) {
        this.content = Objects.requireNonNullElse(content, "");
    }

    public void setPlatform(String platform) {
        this.platform = Objects.requireNonNullElse(platform, "");
    }

    // ========== METADATA (ENRICHMENT DATA) - PROXY QUA PostMetadata (RESTORED SRP & ENCAPSULATION) ==========

    // Giữ PostMetadata ở mức internal để đảm bảo ENCAPSULATION,
    // client chỉ tương tác qua các convenience methods phía dưới.
    protected PostMetadata getMetadata() {
        if (metadata == null) {
            metadata = new PostMetadata();
        }
        return metadata;
    }

    protected void setMetadata(PostMetadata metadata) {
        this.metadata = (metadata != null) ? metadata : new PostMetadata();
    }

    // Convenience getters
    public String getSentiment() {
        return getMetadata().getSentiment();
    }

    public void setSentiment(String sentiment) {
        getMetadata().setSentiment(sentiment);
    }

    public String getLocation() {
        return getMetadata().getLocation();
    }

    public void setLocation(String location) {
        getMetadata().setLocation(location);
    }

    public String getFocus() {
        return getMetadata().getFocus();
    }

    public void setFocus(String focus) {
        getMetadata().setFocus(focus);
    }

    public String getDirection() {
        return getMetadata().getDirection();
    }

    public void setDirection(String direction) {
        getMetadata().setDirection(direction);
    }

    public String getDamageCategory() {
        return getMetadata().getDamageCategory();
    }

    public void setDamageCategory(String damageCategory) {
        getMetadata().setDamageCategory(damageCategory);
    }

    public String getRescueGoods() {
        return getMetadata().getRescueGoods();
    }

    public void setRescueGoods(String rescueGoods) {
        getMetadata().setRescueGoods(rescueGoods);
    }

    // ========== PHƯƠNG THỨC TRỪU TƯỢNG ==========
    public abstract String getDisplayDate();

    public abstract long getEngagementScore();

    public abstract String[] toCsvArray();

    public abstract String[] getCsvHeader();
}