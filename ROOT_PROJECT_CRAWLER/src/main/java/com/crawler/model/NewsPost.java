package com.crawler.model;

import java.time.LocalDate;

/**
 * NewsPost - Đại diện cho bài viết TỪ BÁO CHÍ
 * QUAN HỆ IS-A: NewsPost LÀ AbstractPost (INHERITANCE)
 *
 * SRP: Chỉ chứa các trường và logic RIÊNG cho News
 * KHÔNG chứa reaction, createdDate (những thứ của Social Media)
 */
public class NewsPost extends AbstractPost {
    // Các trường RIÊNG của News (ENCAPSULATION - private)
    private LocalDate postDate;
    private String title;
    private int comments;

    public static final String[] HEADER = {"date", "title", "content", "platform", "comments", "engagement_score"};

    /**
     * Constructor cho NewsPost
     */
    public NewsPost(LocalDate postDate, String title, String content, String platform, int comments) {
        super(content, platform); // Gọi constructor của lớp cha
        this.postDate = postDate;
        this.title = title;
        this.comments = comments;
    }

    /**
     * Constructor mặc định
     */
    public NewsPost() {
        super();
        this.postDate = LocalDate.now();
        this.title = "";
        this.comments = 0;
    }

    // ========== GETTERS (IMMUTABLE FIELDS) + SETTER (MUTABLE FIELD) ==========
    // postDate và title là IMMUTABLE - không thể thay đổi sau khi crawl!
    // comments CÓ THỂ thay đổi (nếu cần update thống kê)

    public LocalDate getPostDate() {
        return postDate;
    }

    public String getTitle() {
        return title;
    }

    public int getComments() {
        return comments;
    }

    /**
     * Setter cho comments - CHO PHÉP cập nhật thống kê comments
     * (Đây là trường CÓ THỂ thay đổi theo thời gian)
     */
    public void setComments(int comments) {
        if (comments < 0) {
            throw new IllegalArgumentException("Comments không được âm!");
        }
        this.comments = comments;
    }

    // ========== OVERRIDE ABSTRACT METHODS (POLYMORPHISM) ==========

    /**
     * OVERRIDE: Trả về ngày dưới dạng LocalDate.toString()
     */
    @Override
    public String getDisplayDate() {
        return postDate != null ? postDate.toString() : "N/A";
    }

    /**
     * OVERRIDE: NewsPost dùng comments làm engagement score
     */
    @Override
    public long getEngagementScore() {
        return comments;
    }

    /**
     * OVERRIDE: Xuất dữ liệu News sang CSV format
     */
    @Override
    public String[] toCsvArray() {
        return new String[] {
            getDisplayDate(),
            title,
            getContent(),
            getPlatform(),
            Integer.toString(comments),
            Long.toString(getEngagementScore())
        };
    }

    /**
     * OVERRIDE: Cung cấp CSV Header cho NewsPost (FIX OCP VIOLATION)
     * POLYMORPHISM: Mỗi loại Post có header riêng
     */
    @Override
    public String[] getCsvHeader() {
        return HEADER;
    }

    @Override
    public String toString() {
        return String.format("NewsPost[platform=%s, title=%s, date=%s, comments=%d]",
                getPlatform(), title, getDisplayDate(), comments);
    }
}
