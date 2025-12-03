package com.crawler.client;

import com.crawler.model.Post;
import java.util.List;

/**
 * ĐÂY LÀ HỢP ĐỒNG (CONTRACT) MÀ TẤT CẢ CÁC CRAWLER PHẢI TUÂN THEO
 *
 * ABSTRACTION - Interface định nghĩa hành vi chung cho tất cả các crawler client
 * OCP (Open/Closed Principle) - Mở cho mở rộng, đóng cho sửa đổi
 * DIP (Dependency Inversion Principle) - Phụ thuộc vào abstraction, không phụ thuộc vào concrete class
 */
public interface ISearchClient {
    /**
     * Mọi client tìm kiếm phải có phương thức search.
     * @param query Từ khóa tìm kiếm
     * @param limit Số lượng kết quả tối đa
     * @return Danh sách các bài viết
     */
    List<Post> search(String query, int limit) throws Exception;

    /**
     * Mọi client phải có phương thức để khởi tạo driver/kết nối.
     */
    void initialize();

    /**
     * Mọi client phải có phương thức để dọn dẹp driver/kết nối.
     */
    void close();
}
