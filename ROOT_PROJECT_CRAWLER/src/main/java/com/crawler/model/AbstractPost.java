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

    // Webhook metadata - MUTABLE (có thể được cập nhật từ webhook)
    private String sentiment;  // "positive", "negative", "neutral"
    private String location;   // Vị trí địa lý được phân tích từ nội dung
    private String focus;      // Chủ đề chính của bài viết

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

    // ========== GETTERS ONLY - NO SETTERS (DATA INTEGRITY) ==========
    // Content và Platform là IMMUTABLE - không thể thay đổi sau khi tạo!

    public String getContent() {
        return content;
    }

    public String getPlatform() {
        return platform;
    }

    // ========== WEBHOOK METADATA - GETTERS & SETTERS ==========
    // Các trường này MUTABLE - được cập nhật từ Webhook sau khi crawl

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

    /**
     * BẮT BUỘC lớp con cung cấp header riêng cho việc Export
     * (Loại bỏ nhu cầu sử dụng instanceof trong Exporter - FIX OCP)
     * POLYMORPHISM: Mỗi loại Post có header khác nhau
     * @return CSV Header array
     */
    public abstract String[] getCsvHeader();
}
