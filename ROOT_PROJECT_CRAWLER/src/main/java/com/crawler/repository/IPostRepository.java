package com.crawler.repository;

import com.crawler.model.AbstractPost;
import java.util.List;

/**
 * IPostRepository - CONTRACT cho Data Access Layer (DAL)
 *
 * DIP: Các layer cao (Service) phụ thuộc vào interface này, KHÔNG phụ thuộc vào SQLitePostRepository
 * SRP: Chỉ có MỘT trách nhiệm - Truy cập dữ liệu (CRUD operations)
 * OCP: Có thể thay SQLite bằng MySQL, MongoDB... KHÔNG CẦN sửa code Service
 *
 * REPOSITORY PATTERN:
 * - Tách logic truy cập dữ liệu khỏi business logic
 * - Service Layer không biết dữ liệu lưu ở đâu (SQLite? MySQL? File?)
 * - Dễ dàng test bằng Mock Repository
 */
public interface IPostRepository {

    /**
     * Lưu danh sách posts vào storage
     * POLYMORPHISM: Có thể nhận List<NewsPost> hoặc List<SocialPost>
     *
     * @param posts Danh sách bài viết cần lưu
     * @param keyword Từ khóa tìm kiếm (dùng làm cache key)
     */
    void save(List<? extends AbstractPost> posts, String keyword);

    /**
     * Load danh sách posts từ storage
     * POLYMORPHISM: Trả về List<? extends AbstractPost> (có thể là NewsPost hoặc SocialPost)
     *
     * @param keyword Từ khóa tìm kiếm (cache key)
     * @return Danh sách bài viết đã lưu, null nếu không tìm thấy
     */
    List<? extends AbstractPost> load(String keyword);

    /**
     * Kiểm tra xem keyword đã được cache chưa
     *
     * @param keyword Từ khóa tìm kiếm
     * @return true nếu đã có dữ liệu trong cache
     */
    boolean isCached(String keyword);
}
