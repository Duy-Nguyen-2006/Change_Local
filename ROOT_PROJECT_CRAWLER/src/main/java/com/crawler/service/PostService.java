package com.crawler.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.model.AbstractPost;
import com.crawler.processor.IDataProcessor;
import com.crawler.repository.IPostRepository;
import com.crawler.util.CacheKeyFactory;

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
 * a. Crawl từ ISearchClient
 * b. Enrich qua IDataProcessor (gọi webhook)
 * c. Save vào Repository
 * d. Return enriched posts
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
    // Chấp nhận IDataProcessor với Wildcard Super để cho phép các processor 
    // làm việc với AbstractPost (Webhook) hoặc lớp con của nó (Filter).
    private final List<IDataProcessor<? super AbstractPost>> processors;

    /**
     * Constructor Injection - DEPENDENCY INJECTION PATTERN
     * Client phải cung cấp dependencies khi tạo PostService
     *
     * @param repository Repository để lưu/load posts
     * @param crawler Crawler để crawl posts mới
     * @param processor Processor để enrich posts (webhook, filtering...)
     */
    public PostService(IPostRepository repository, ISearchClient crawler, IDataProcessor<? super AbstractPost> processor) {
        this(repository, crawler, List.of(processor));
    }

    /**
     * Constructor Injection - CHAIN OF PROCESSORS
     * @param repository Repository để lưu/load posts
     * @param crawler Crawler để crawl posts mới
     * @param processors Danh sách processor (Filter/Webhook/Validation...)
     */
    public PostService(IPostRepository repository, ISearchClient crawler, List<IDataProcessor<? super AbstractPost>> processors) {
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
     *
     * NOTE: Crawler lifecycle (initialize/close) KHÔNG được quản lý ở đây.
     * Application layer (Main/TestRunner) phải đảm bảo crawler đã được initialize trước khi gọi service.
     */
    @Override
    public List<? extends AbstractPost> getPosts(String keyword, LocalDate startDate, LocalDate endDate) throws CrawlerException {
        // 1. TẠO CACHE KEY: Keyword + DateRange (uỷ quyền cho CacheKeyFactory để tách SRP)
        String cacheKey = CacheKeyFactory.createKey(keyword, startDate, endDate);

        System.out.println("[PostService] Checking cache for key: " + cacheKey);

        List<? extends AbstractPost> cached = repository.load(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            System.out.println("-> Cache HIT!");
            return new ArrayList<>(cached);
        }

        System.out.println("-> Cache MISS! Crawling...");

        // 2. Crawl (DÙNG HÀM SEARCH MỚI VỚI DATE)
        // Crawler đã được initialize bởi application layer
        List<? extends AbstractPost> rawPosts = crawler.search(keyword, startDate, endDate);

        // 3. Process (Filter + enrichment)
        List<? extends AbstractPost> processedPosts = applyProcessors(rawPosts);

        // 4. Save Cache
        repository.save(processedPosts, cacheKey);

        return processedPosts;
    }

    /**
     * Apply processors với type safety
     * Loại bỏ unsafe casting bằng cách copy elements một cách an toàn
     */
    private List<? extends AbstractPost> applyProcessors(List<? extends AbstractPost> rawPosts) throws CrawlerException {
        // Copy elements một cách an toàn (không cast unsafe)
        List<AbstractPost> current = new ArrayList<>();
        for (AbstractPost post : rawPosts) {
            current.add(post);
        }

        for (IDataProcessor<? super AbstractPost> processor : this.processors) {
            if (processor == null) {
                continue;
            }
            
            // Process với type-safe approach
            // Processor có wildcard super AbstractPost nên có thể nhận List<AbstractPost>
            current = processWithProcessor(processor, current);
        }
        return current;
    }
    
    /**
     * Helper method để process với type safety
     * Sử dụng wildcard capture để tránh unsafe cast
     */
    @SuppressWarnings("unchecked")
    private <T extends AbstractPost> List<AbstractPost> processWithProcessor(
            IDataProcessor<? super AbstractPost> processor, 
            List<AbstractPost> posts) throws CrawlerException {
        
        // Safe cast: Processor với wildcard super AbstractPost có thể nhận List<AbstractPost>
        // Và trả về List<AbstractPost> (vì T extends AbstractPost)
        IDataProcessor<AbstractPost> concreteProcessor = (IDataProcessor<AbstractPost>) processor;
        return concreteProcessor.process(posts);
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