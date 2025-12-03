package com.crawler.model;

/**
 * Lớp trừu tượng gốc cho TẤT CẢ các loại bài viết
 * ÁP DỤNG INHERITANCE - Đây là lớp cha chứa các trường CHUNG
 *
 * ENCAPSULATION: Tất cả fields đều private
 * ABSTRACTION: Định nghĩa phương thức trừu tượng bắt buộc lớp con override
 * SRP: Chỉ chứa các trường và hành vi CHUNG cho mọi loại Post
 */
public abstract class AbstractPost {
    // Các trường CHUNG cho TẤT CẢ loại bài viết (ENCAPSULATION - private)
    private String content;
    private String platform;

    /**
     * Constructor chung cho lớp con gọi
     * @param content Nội dung bài viết
     * @param platform Nền tảng (VNExpress, TikTok, X, Dantri...)
     */
    public AbstractPost(String content, String platform) {
        this.content = content;
        this.platform = platform;
    }

    /**
     * Constructor mặc định
     */
    public AbstractPost() {
        this.content = "";
        this.platform = "";
    }

    // ========== GETTERS/SETTERS (ENCAPSULATION) ==========

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content không được rỗng!");
        }
        this.content = content;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException("Platform không được rỗng!");
        }
        this.platform = platform;
    }

    // ========== PHƯƠNG THỨC TRỪU TƯỢNG (ABSTRACTION) ==========
    // BẮT BUỘC LỚP CON PHẢI OVERRIDE

    /**
     * Lấy ngày hiển thị của bài viết
     * Mỗi loại Post có cách format ngày khác nhau
     * @return Ngày dưới dạng String
     */
    public abstract String getDisplayDate();

    /**
     * Tính điểm tương tác (engagement score)
     * NewsPost dùng comments, SocialPost dùng reaction
     * @return Điểm tương tác
     */
    public abstract long getEngagementScore();

    /**
     * Xuất dữ liệu dưới dạng mảng String cho CSV
     * @return Mảng String chứa dữ liệu
     */
    public abstract String[] toCsvArray();
}
