package com.crawler.app;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.client.VNExpressClient; // CHỌN CLIENT BÁO CHÍ ĐỂ TEST
import com.crawler.model.AbstractPost;
import com.crawler.processor.IDataProcessor;
import com.crawler.processor.WebhookProcessor;
import com.crawler.repository.IPostRepository;
import com.crawler.repository.SQLitePostRepository;
import com.crawler.service.IPostService;
import com.crawler.service.PostService;

import java.time.LocalDate;
import java.util.List;

public class TestRunner {

    public static void main(String[] args) {
        // ========== 1. INPUT CỦA MÀY ==========
        String province = "Hà Nội";
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 1);

        System.out.println("--- BẮT ĐẦU KIỂM TRA LUỒNG DỮ LIỆU ---");

        // ========== 2. DEPENDENCY INJECTION (DI) ==========
        // TẠO TẤT CẢ CÁC THÀNH PHẦN CONCRETE (CONCRETE CLASSES)
        try (WebhookProcessor webhookProcessor = new WebhookProcessor("https://7jk103q70xnk.ezbase.vn/webhook/run")) {
            
            IPostRepository repository = new SQLitePostRepository();
            
            // CHỌN CLIENT CỤ THỂ ĐỂ TEST (Ví dụ: VNExpress)
            ISearchClient newsClient = new VNExpressClient(); 
            
            // Dùng WebhookProcessor làm IDataProcessor
            IDataProcessor processor = webhookProcessor;

            // TIÊM PHỤ THUỘC (DIP) - Tiêm Interface vào Service
            IPostService service = new PostService(repository, newsClient, processor);

            // ========== 3. GỌI LOGIC NGHIỆP VỤ (SERVICE CALL) ==========
            System.out.println("\n[GỌI SERVICE] province=" + province);
            
            // POLYMORPHISM: Hàm này sẽ tự động gọi Crawl/Webhook nếu chưa có cache
            List<? extends AbstractPost> results = service.getPosts(province, startDate, endDate);

            // ========== 4. KIỂM TRA KẾT QUẢ XỬ LÝ ==========
            System.out.println("\n--- KẾT QUẢ CRAWL VÀ XỬ LÝ (POLYMORPHISM) ---");
            System.out.println("TỔNG SỐ BÀI VÀO DB: " + results.size());

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
        }
    }
}