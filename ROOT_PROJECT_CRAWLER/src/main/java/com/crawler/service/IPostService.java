package com.crawler.service;

import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import java.util.List;

/**
 * IPostService - CONTRACT cho Service Layer (Business Logic Layer)
 *
 * SERVICE LAYER PATTERN:
 * - Đây là nơi chứa BUSINESS LOGIC (caching, orchestration, workflow)
 * - Phối hợp giữa Repository (DAL) và Processor (Processing Layer)
 * - Controller/UI chỉ gọi Service, KHÔNG gọi trực tiếp Repository hay Crawler
 *
 * DIP: Controller phụ thuộc vào interface này, KHÔNG phụ thuộc vào PostService cụ thể
 * SRP: Chỉ có MỘT trách nhiệm - Điều phối logic nghiệp vụ (Orchestration)
 * OCP: Có thể thay đổi cách implement caching mà KHÔNG ẢNH HƯỞNG Controller
 *
 * WORKFLOW:
 * 1. Kiểm tra cache (Repository.isCached)
 * 2. Nếu có cache → Load từ Repository
 * 3. Nếu không → Crawl từ ISearchClient → Process qua IDataProcessor → Save vào Repository
 */
public interface IPostService {

    /**
     * Lấy danh sách posts với caching logic
     * POLYMORPHISM: Trả về List<? extends AbstractPost> (có thể là NewsPost hoặc SocialPost)
     *
     * Luồng xử lý:
     * 1. Check repository.isCached(keyword)
     * 2. Nếu cached → return repository.load(keyword)
     * 3. Nếu không:
     *    a. crawler.search(keyword, limit)
     *    b. processor.process(rawPosts)  // Gọi webhook để lấy metadata
     *    c. repository.save(enrichedPosts, keyword)
     *    d. return enrichedPosts
     *
     * @param keyword Từ khóa tìm kiếm
     * @param limit Số lượng bài viết tối đa
     * @return Danh sách bài viết (từ cache hoặc crawl mới)
     * @throws CrawlerException Nếu crawl hoặc xử lý thất bại
     */
    List<? extends AbstractPost> getPosts(String keyword, int limit) throws CrawlerException;
}
