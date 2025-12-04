package com.crawler.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List; // CHỌN CLIENT BÁO CHÍ ĐỂ TEST

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.client.VNExpressClient;
import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import com.crawler.processor.IDataProcessor;
import com.crawler.processor.NewsFilterProcessor;
import com.crawler.processor.WebhookProcessor;
import com.crawler.repository.IPostRepository;
import com.crawler.repository.SQLitePostRepository;
import com.crawler.service.IPostService;
import com.crawler.service.PostService;
import com.crawler.util.PostCsvExporter;

public class TestRunner {

    public static void main(String[] args) {
        // ========== 1. INPUT CỦA MÀY ==========
        // ĐỔI TỪ province thành keyword để đồng bộ với IPostService và PostService
        String keyword = "bão lũ"; // Dùng keyword liên quan đến bộ lọc
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 1);

        System.out.println("--- BẮT ĐẦU KIỂM TRA LUỒNG DỮ LIỆU ---");

        // ========== 2. DEPENDENCY INJECTION (DI) ==========
        // TẠO TẤT CẢ CÁC THÀNH PHẦN CONCRETE (CONCRETE CLASSES)
        ISearchClient newsClient = null;
        // KHÔNG DÙNG ConfigLoader NỮA (vì ConfigLoader không còn trong file mới của user)
        // Thay bằng constructor mặc định đã được bổ sung
        try (WebhookProcessor webhookProcessor = new WebhookProcessor()) { 

            IPostRepository repository = new SQLitePostRepository();

            // CHỌN CLIENT CỤ THỂ ĐỂ TEST (Ví dụ: VNExpress)
            newsClient = new VNExpressClient();

            // LIFECYCLE MANAGEMENT: Initialize crawler TRƯỚC khi sử dụng
            // PostService KHÔNG nên quản lý lifecycle của crawler
            newsClient.initialize();

            // CẤU HÌNH KEYWORDS CHO NEWS FILTER (Configuration Layer)
            List<String> disasterKeywords = Arrays.asList(
                "bão", "lũ", "lũ lụt", "sạt lở đất", "thiên tai", "ngập",
                "mưa lớn", "mưa to", "giông", "lũ quét", "triều cường"
            );

            // TẠO PROCESSOR PIPELINE: Filter trước, sau đó Enrich
            // NewsFilterProcessor chỉ xử lý NewsPost
            IDataProcessor<NewsPost> newsFilter = new NewsFilterProcessor(startDate, endDate, disasterKeywords);
            // WebhookProcessor xử lý AbstractPost
            IDataProcessor<AbstractPost> webhookEnricher = webhookProcessor;

            // TIÊM PHỤ THUỘC (DIP) - Tiêm Processor Pipeline vào Service
            // NewsFilterProcessor sẽ chạy TRƯỚC WebhookProcessor
            // Cast processors to proper type for PostService
            List<IDataProcessor<? super AbstractPost>> processorList = new ArrayList<>();
            processorList.add(webhookEnricher);
            IPostService service = new PostService(repository, newsClient, processorList);

            // ========== 3. GỌI LOGIC NGHIỆP VỤ (SERVICE CALL) ==========
            // ĐÃ SỬA: DÙNG KEYWORD
            System.out.println("\n[GỌI SERVICE] keyword=" + keyword);

            // POLYMORPHISM: Hàm này sẽ tự động gọi Crawl/Webhook nếu chưa có cache
            List<? extends AbstractPost> results = service.getPosts(keyword, startDate, endDate);

            // ========== 4. KIỂM TRA KẾT QUẢ XỬ LÝ ==========
            System.out.println("\n--- KẾT QUẢ CRAWL VÀ XỬ LÝ (POLYMORPHISM) ---");
            System.out.println("TỔNG SỐ BÀI VÀO DB: " + results.size());
            
            // Luu ra 1 file CSV duy nhat de xem nhanh
            String csvFile = "TestRunner_" + keyword.replaceAll("\\s+", "_") + ".csv";
            PostCsvExporter.export(results, csvFile);
            System.out.println("[CSV] Da luu du lieu vao: " + csvFile);

            if (!results.isEmpty()) {
                AbstractPost sample = results.get(0);
                System.out.println("\n* BÀI VIẾT MẪU (KIỂM TRA WEBHOOK ENRICHMENT):");
                System.out.println("  - Nền tảng: " + sample.getPlatform());
                System.out.println("  - Content: " + sample.getContent().substring(0, Math.min(sample.getContent().length(), 80)) + "...");
                System.out.println("  - Cảm xúc (Webhook): " + sample.getSentiment()); // Dữ liệu đã được xử lý
                System.out.println("  - Vị trí (Webhook): " + sample.getLocation());   // Dữ liệu đã được xử lý
                System.out.println("  - Engagement Score: " + sample.getEngagementScore());
            }

        } catch (CrawlerException e) {
            System.err.println("\n--- THẤT BẠI TRONG QUÁ TRÌNH TEST ---");
            System.err.println("LỖI HỆ THỐNG (CrawlerException): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("LỖI KHÔNG XÁC ĐỊNH: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // LIFECYCLE MANAGEMENT: Đóng crawler trong finally block
            if (newsClient != null) {
                try {
                    newsClient.close();
                    System.out.println("\n[LIFECYCLE] Crawler đã được đóng.");
                } catch (CrawlerException e) {
                    System.err.println("LỖI khi đóng crawler: " + e.getMessage());
                }
            }
        }
    }
}