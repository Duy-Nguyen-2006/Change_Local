package com.crawler.processor;

import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import java.util.List;

/**
 * IDataProcessor - CONTRACT cho Data Processing Layer
 *
 * CHAIN OF RESPONSIBILITY PATTERN:
 * - Xử lý dữ liệu qua nhiều bước (webhook enrichment, filtering, validation...)
 * - Có thể thêm processor mới mà KHÔNG SỬA code cũ (OCP)
 *
 * DIP: Service phụ thuộc vào interface này, KHÔNG phụ thuộc vào WebhookProcessor cụ thể
 * SRP: Chỉ có MỘT trách nhiệm - Xử lý và làm giàu dữ liệu (Data Enrichment)
 * OCP: Có thể thêm FilterProcessor, ValidationProcessor... KHÔNG CẦN sửa code Service
 *
 * Ví dụ sử dụng:
 * 1. WebhookProcessor - Gọi API để lấy sentiment, location, focus
 * 2. FilterProcessor - Lọc bỏ posts spam (nếu cần)
 * 3. ValidationProcessor - Kiểm tra data integrity (nếu cần)
 */
public interface IDataProcessor {

    /**
     * Xử lý và làm giàu dữ liệu posts
     * POLYMORPHISM: Nhận và trả về List<? extends AbstractPost>
     *
     * WebhookProcessor sẽ:
     * - Gọi HTTP POST đến webhook API
     * - Nhận về sentiment, location, focus
     * - Cập nhật các trường này vào từng Post (setSentiment, setLocation, setFocus)
     *
     * @param rawPosts Danh sách posts gốc từ crawler
     * @return Danh sách posts đã được xử lý và làm giàu
     * @throws CrawlerException Nếu xử lý thất bại (ví dụ: webhook timeout)
     */
    List<? extends AbstractPost> process(List<? extends AbstractPost> rawPosts) throws CrawlerException;
}
