package com.crawler.processor;

import java.util.List;

import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;

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
 * POLYMORPHISM + GENERICS: Sử dụng Generics <T> để ép kiểu và loại bỏ 'instanceof'.
 * T BẮT BUỘC phải là AbstractPost hoặc lớp con của nó.
 */
public interface IDataProcessor<T extends AbstractPost> {

    /**
     * Xử lý và làm giàu dữ liệu posts
     * POLYMORPHISM + GENERICS: Nhận và trả về List<T extends AbstractPost>
     *
     * @param rawPosts Danh sách posts gốc từ crawler
     * @return Danh sách posts đã được xử lý và làm giàu
     * @throws CrawlerException Nếu xử lý thất bại (ví dụ: webhook timeout)
     */
    List<T> process(List<T> rawPosts) throws CrawlerException;
}