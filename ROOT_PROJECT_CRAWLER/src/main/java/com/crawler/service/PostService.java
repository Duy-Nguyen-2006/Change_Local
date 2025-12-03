package com.crawler.service;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.model.AbstractPost;
import com.crawler.processor.IDataProcessor;
import com.crawler.repository.IPostRepository;

import java.util.List;

/**
 * PostService - CONCRETE IMPLEMENTATION của IPostService
 *
 * SERVICE LAYER PATTERN:
 * - Đây là nơi chứa BUSINESS LOGIC (caching, orchestration, workflow)
 * - Phối hợp giữa Repository (DAL), Crawler (Data Source), và Processor (Enrichment)
 * - Controller/UI chỉ gọi Service, KHÔNG gọi trực tiếp Repository hay Crawler
 *
 * DEPENDENCY INJECTION (Constructor Injection):
 * - Nhận IPostRepository, ISearchClient, IDataProcessor qua constructor
 * - DIP: Phụ thuộc vào ABSTRACTION, không phụ thuộc vào concrete class
 * - Dễ dàng test bằng Mock objects
 *
 * CACHING WORKFLOW:
 * 1. Check repository.isCached(keyword)
 * 2. Nếu cached → Load từ Repository (nhanh, không tốn API calls)
 * 3. Nếu không:
 *    a. Crawl từ ISearchClient
 *    b. Enrich qua IDataProcessor (gọi webhook)
 *    c. Save vào Repository
 *    d. Return enriched posts
 *
 * SOLID PRINCIPLES:
 * - SRP: Chỉ có MỘT trách nhiệm - Orchestration và caching logic
 * - OCP: Có thể thay đổi Repository/Processor implementation mà KHÔNG SỬA code này
 * - LSP: Mọi implementation của IPostRepository/IDataProcessor đều hoạt động
 * - DIP: Phụ thuộc vào interface, không phụ thuộc vào concrete class
 */
public class PostService implements IPostService {

    private final IPostRepository repository;
    private final ISearchClient crawler;
    private final IDataProcessor processor;

    /**
     * Constructor Injection - DEPENDENCY INJECTION PATTERN
     * Client phải cung cấp dependencies khi tạo PostService
     *
     * @param repository Repository để lưu/load posts
     * @param crawler Crawler để crawl posts mới
     * @param processor Processor để enrich posts (webhook, filtering...)
     */
    public PostService(IPostRepository repository, ISearchClient crawler, IDataProcessor processor) {
        if (repository == null || crawler == null || processor == null) {
            throw new IllegalArgumentException("All dependencies (repository, crawler, processor) must be non-null!");
        }

        this.repository = repository;
        this.crawler = crawler;
        this.processor = processor;
    }

    /**
     * Lấy danh sách posts với caching logic
     * POLYMORPHISM: Trả về List<? extends AbstractPost> (NewsPost hoặc SocialPost)
     *
     * WORKFLOW:
     * 1. Check cache
     * 2. If cached → return từ cache (FAST PATH)
     * 3. If not → crawl → process → save → return (SLOW PATH)
     */
    @Override
    public List<? extends AbstractPost> getPosts(String keyword, int limit) throws CrawlerException {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be null or empty!");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive!");
        }

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║  PostService: getPosts(\"" + keyword + "\", " + limit + ")");
        System.out.println("╚════════════════════════════════════════════════╝");

        // ========== STEP 1: CHECK CACHE ==========
        System.out.println("\n[1] Checking cache...");
        boolean isCached = repository.isCached(keyword);

        if (isCached) {
            // ========== FAST PATH: LOAD FROM CACHE ==========
            System.out.println("  ✓ Cache HIT! Loading from repository...");
            List<? extends AbstractPost> cachedPosts = repository.load(keyword);

            if (cachedPosts != null && !cachedPosts.isEmpty()) {
                System.out.println("  ✓ Loaded " + cachedPosts.size() + " posts from cache");
                System.out.println("\n╔════════════════════════════════════════════════╗");
                System.out.println("║  PostService: Returning cached posts (FAST)   ║");
                System.out.println("╚════════════════════════════════════════════════╝\n");
                return cachedPosts;
            } else {
                System.out.println("  ✗ Cache corrupted or empty, falling back to crawl...");
            }
        } else {
            System.out.println("  ✗ Cache MISS! Need to crawl...");
        }

        // ========== SLOW PATH: CRAWL → PROCESS → SAVE ==========
        try {
            // STEP 2: CRAWL
            System.out.println("\n[2] Crawling from source...");
            crawler.initialize();
            List<? extends AbstractPost> rawPosts = crawler.search(keyword, limit);
            crawler.close();

            if (rawPosts == null || rawPosts.isEmpty()) {
                System.out.println("  ✗ No posts found from crawler");
                return List.of(); // Return empty list
            }

            System.out.println("  ✓ Crawled " + rawPosts.size() + " posts");

            // STEP 3: PROCESS (Enrich with webhook)
            System.out.println("\n[3] Processing posts (webhook enrichment)...");
            List<? extends AbstractPost> enrichedPosts = processor.process(rawPosts);
            System.out.println("  ✓ Processed " + enrichedPosts.size() + " posts");

            // STEP 4: SAVE TO CACHE
            System.out.println("\n[4] Saving to cache...");
            repository.save(enrichedPosts, keyword);
            System.out.println("  ✓ Saved to cache successfully");

            System.out.println("\n╔════════════════════════════════════════════════╗");
            System.out.println("║  PostService: Returning fresh posts (SLOW)    ║");
            System.out.println("╚════════════════════════════════════════════════╝\n");

            return enrichedPosts;

        } catch (CrawlerException e) {
            System.err.println("  ✗ PostService failed: " + e.getMessage());
            throw e; // Re-throw to caller
        }
    }

    /**
     * BONUS: Clear cache cho một keyword cụ thể
     * (Có thể thêm vào IPostService interface nếu cần)
     */
    @SuppressWarnings("unused")
    public void clearCache(String keyword) {
        System.out.println("Clearing cache for keyword: " + keyword);
        // TODO: Implement repository.delete(keyword) nếu cần
    }

    /**
     * BONUS: Get cache statistics
     * (Có thể thêm vào IPostService interface nếu cần)
     */
    @SuppressWarnings("unused")
    public String getCacheStats() {
        // TODO: Implement repository.getStats() nếu cần
        return "Cache statistics not implemented yet";
    }
}
