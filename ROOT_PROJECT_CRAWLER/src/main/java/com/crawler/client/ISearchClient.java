package com.crawler.client;

import com.crawler.model.AbstractPost;
import java.time.LocalDate;
import java.util.List;

/**
 * ĐÂY LÀ HỢP ĐỒNG (CONTRACT) MÀ TẤT CẢ CÁC CRAWLER PHẢI TUÂN THEO
 *
 * ABSTRACTION - Interface định nghĩa hành vi chung cho tất cả các crawler client
 * OCP (Open/Closed Principle) - Mở cho mở rộng, đóng cho sửa đổi
 * DIP (Dependency Inversion Principle) - Phụ thuộc vào abstraction, không phụ thuộc vào concrete class
 * LSP (Liskov Substitution Principle) - Tất cả crawler đều có thể thay thế cho nhau
 */
public interface ISearchClient {
    /**
     * Mọi client tìm kiếm phải có phương thức search.
     * @param query Từ khóa tìm kiếm
     * @param startDate Ngày bắt đầu lọc
     * @param endDate Ngày kết thúc lọc
     * @return Danh sách các bài viết (NewsPost hoặc SocialPost)
     * @throws CrawlerException Nếu có lỗi khi crawl
     */
    List<? extends AbstractPost> search(String query, LocalDate startDate, LocalDate endDate) throws CrawlerException;

    /**
     * Mọi client phải có phương thức để khởi tạo driver/kết nối.
     */
    void initialize();

    /**
     * Mọi client phải có phương thức để dọn dẹp driver/kết nối.
     */
    void close();
}
