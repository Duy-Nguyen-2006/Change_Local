package com.crawler.model;

/**
 * SocialPost - Đại diện cho bài viết TỪ MẠNG XÃ HỘI
 * QUAN HỆ IS-A: SocialPost LÀ AbstractPost (INHERITANCE)
 *
 * SRP: Chỉ chứa các trường và logic RIÊNG cho Social Media
 * KHÔNG chứa title, postDate, comments (những thứ của News)
 */
public class SocialPost extends AbstractPost {
    // Các trường RIÊNG của Social Media (ENCAPSULATION - private)
    private long reaction;
    private String createdDate; // Giữ String vì social API trả về format khác nhau

    public static final String[] HEADER = {"date", "content", "platform", "reaction", "engagement_score"};

    /**
     * Constructor cho SocialPost
     */
    public SocialPost(String content, String platform, String createdDate, long reaction) {
        super(content, platform); // Gọi constructor của lớp cha
        this.createdDate = createdDate;
        this.reaction = reaction;
    }

    /**
     * Constructor mặc định
     */
    public SocialPost() {
        super();
        this.createdDate = "";
        this.reaction = 0;
    }

    // ========== GETTERS/SETTERS (ENCAPSULATION) ==========

    public long getReaction() {
        return reaction;
    }

    public void setReaction(long reaction) {
        if (reaction < 0) {
            throw new IllegalArgumentException("Reaction không được âm!");
        }
        this.reaction = reaction;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    // ========== OVERRIDE ABSTRACT METHODS (POLYMORPHISM) ==========

    /**
     * OVERRIDE: Trả về createdDate string
     */
    @Override
    public String getDisplayDate() {
        return createdDate != null && !createdDate.isEmpty() ? createdDate : "N/A";
    }

    /**
     * OVERRIDE: SocialPost dùng reaction làm engagement score
     */
    @Override
    public long getEngagementScore() {
        return reaction;
    }

    /**
     * OVERRIDE: Xuất dữ liệu Social sang CSV format
     */
    @Override
    public String[] toCsvArray() {
        return new String[] {
            getDisplayDate(),
            getContent(),
            getPlatform(),
            Long.toString(reaction),
            Long.toString(getEngagementScore())
        };
    }

    @Override
    public String toString() {
        return String.format("SocialPost[platform=%s, date=%s, reaction=%d]",
                getPlatform(), getDisplayDate(), reaction);
    }
}
