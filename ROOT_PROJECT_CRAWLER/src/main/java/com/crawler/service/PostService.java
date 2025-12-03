package com.crawler.service;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.model.AbstractPost;
import com.crawler.processor.IDataProcessor;
import com.crawler.processor.NewsFilterProcessor;
import com.crawler.repository.IPostRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final List<IDataProcessor> processors;

    /**
     * Constructor Injection - DEPENDENCY INJECTION PATTERN
     * Client phải cung cấp dependencies khi tạo PostService
     *
     * @param repository Repository để lưu/load posts
     * @param crawler Crawler để crawl posts mới
     * @param processor Processor để enrich posts (webhook, filtering...)
     */
    public PostService(IPostRepository repository, ISearchClient crawler, IDataProcessor processor) {
        this(repository, crawler, List.of(processor));
    }

    /**
     * Constructor Injection - CHAIN OF PROCESSORS
     * @param repository Repository Ž` ¯Ÿ l’øu/load posts
     * @param crawler Crawler Ž` ¯Ÿ crawl posts m ¯>i
     * @param processors Danh sA­ch processor (Filter/Webhook/Validation...)
     */
    public PostService(IPostRepository repository, ISearchClient crawler, List<IDataProcessor> processors) {
        if (repository == null || crawler == null || processors == null) {
            throw new IllegalArgumentException("All dependencies (repository, crawler, processors) must be non-null!");
        }

        this.repository = repository;
        this.crawler = crawler;
        this.processors = new ArrayList<>(processors);
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
    public List<? extends AbstractPost> getPosts(String province, LocalDate startDate, LocalDate endDate) throws CrawlerException {
        // TẠO CACHE KEY: Keyword + DateRange
        String cacheKey = String.format("%s_%s_%s", province, startDate, endDate).replace(" ", "_");

        System.out.println("[PostService] Checking cache for key: " + cacheKey);

        // 1. Check Cache
        if (repository.isCached(cacheKey)) {
            System.out.println("-> Cache HIT!");
            return repository.load(cacheKey);
        }

        System.out.println("-> Cache MISS! Crawling...");

        // 2. Crawl (DÙNG HÀM SEARCH MỚI VỚI DATE)
        crawler.initialize();
        List<? extends AbstractPost> rawPosts = crawler.search(province, startDate, endDate);
        crawler.close();

        // 3. Process (Filter + enrichment)
        List<? extends AbstractPost> processedPosts = applyProcessors(rawPosts, startDate, endDate);

        // 4. Save Cache
        repository.save(processedPosts, cacheKey);

        return processedPosts;
    }

    private List<? extends AbstractPost> applyProcessors(List<? extends AbstractPost> rawPosts,
                                                         LocalDate startDate,
                                                         LocalDate endDate) throws CrawlerException {
        List<IDataProcessor> pipeline = new ArrayList<>();
        pipeline.add(new NewsFilterProcessor(startDate, endDate));
        pipeline.addAll(processors);

        List<? extends AbstractPost> current = rawPosts;
        for (IDataProcessor processor : pipeline) {
            if (processor == null) {
                continue;
            }
            current = processor.process(current);
        }
        return current;
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
