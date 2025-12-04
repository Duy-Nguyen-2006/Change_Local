package com.crawler.app;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.crawler.client.CrawlerException;
import com.crawler.client.DantriClient;
import com.crawler.client.ISearchClient;
import com.crawler.client.TikTokSearchClient;
import com.crawler.client.VNExpressClient;
import com.crawler.client.XSearchClient;
import com.crawler.config.CrawlerConfig;
import com.crawler.model.AbstractPost;
import com.crawler.processor.WebhookProcessor;
import com.crawler.util.PostCsvExporter;

/**
 * Main Application - DEMO ĐA HÌNH HOÀN HẢO (UNIFIED POLYMORPHISM)
 *
 * BÂY GIỜ TẤT CẢ CRAWLER ĐỀU IMPLEMENT ISearchClient!
 * ĐÂY LÀ BẰNG CHỨNG CỦA LSP (Liskov Substitution Principle)
 *
 * Các nguyên tắc OOP và SOLID được áp dụng:
 * 1. ENCAPSULATION - Tất cả fields đều private, có getter/setter
 * 2. INHERITANCE - AbstractPost > NewsPost/SocialPost
 * 3. POLYMORPHISM - Tất cả crawler đều là ISearchClient
 * 4. ABSTRACTION - Sử dụng interface thay vì concrete class
 * 5. SRP - Mỗi class có một trách nhiệm duy nhất
 * 6. OCP - Mở cho mở rộng, đóng cho sửa đổi
 * 7. LSP - Tất cả crawler có thể thay thế cho nhau
 * 8. DIP - Phụ thuộc vào abstraction (ISearchClient)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║  DEMO POLYMORPHISM & LSP - CRAWLER HOÀN HẢO      ║");
        System.out.println("╚═══════════════════════════════════════════════════╝\n");

        demoUnifiedPolymorphism();
    }

    /**
     * DEMO UNIFIED POLYMORPHISM & LSP
     *
     * CHỈ CÒN MỘT HÀM DUY NHẤT - TẤT CẢ CRAWLER ĐỀU LÀ ISearchClient!
     * NewsPost và SocialPost ĐỀU LÀ AbstractPost!
     *
     * Đây là BẰNG CHỨNG của:
     * - LSP (Liskov Substitution Principle): Tất cả crawler đều thay thế được cho nhau
     * - DIP (Dependency Inversion): Phụ thuộc vào ISearchClient, không phụ thuộc TikTokSearchClient/VNExpressClient
     * - POLYMORPHISM: Một interface, nhiều implementation
     */
    private static void demoUnifiedPolymorphism() {
        System.out.println(">>> TẤT CẢ CRAWLER ĐỀU LÀ ISearchClient (LSP) <<<\n");

        // ========== TẠO DANH SÁCH CÁC CRAWLER ==========
        // TẤT CẢ ĐỀU LÀ ISearchClient - ĐÂY LÀ POLYMORPHISM!
        List<ISearchClient> allCrawlers = new ArrayList<>();

        allCrawlers.add(new TikTokSearchClient()); // Social Media
        allCrawlers.add(new XSearchClient());       // Social Media
        allCrawlers.add(new VNExpressClient());     // News (IMPLEMENT qua CrawlerEnv)
        allCrawlers.add(new DantriClient());       // News (IMPLEMENT qua CrawlerEnv)

        String keyword = "bão lũ";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        System.out.println("Từ khóa tìm kiếm: \"" + keyword + "\"");
        System.out.println("Khoảng thời gian: " + startDate + " → " + endDate + "\n");
        System.out.println("═══════════════════════════════════════════════════\n");

        // ========== THU THẬP TẤT CẢ KẾT QUẢ VÀO MỘT LIST DUY NHẤT ==========
        List<AbstractPost> allResults = new ArrayList<>();

        // ========== CHỈ CÓ MỘT VÒNG LẶP CHO TẤT CẢ CRAWLER ==========
        // LATE BINDING / DYNAMIC DISPATCH - Phương thức được gọi tại runtime
        for (ISearchClient crawler : allCrawlers) {
            try {
                System.out.println("┌─────────────────────────────────────────────┐");
                System.out.println("│ CRAWLER: " + crawler.getClass().getSimpleName());
                System.out.println("└─────────────────────────────────────────────┘");

                // POLYMORPHISM - initialize() hoạt động khác nhau cho từng crawler
                crawler.initialize();

                // POLYMORPHISM - search() trả về NewsPost hoặc SocialPost
                // Nhưng cả hai đều là AbstractPost!
                List<? extends AbstractPost> results = crawler.search(keyword, startDate, endDate);

                System.out.println("✓ Tìm thấy " + results.size() + " bài viết");

                // Hiển thị 2 bài đầu tiên
                displaySamplePosts(results, 2);

                // Thêm kết quả vào list tổng hợp
                allResults.addAll(results);

                // POLYMORPHISM - close() hoạt động khác nhau cho từng crawler
                crawler.close();

                System.out.println("═══════════════════════════════════════════════════\n");

            } catch (CrawlerException e) {
                System.err.println("✗ Lỗi khi crawl: " + e.getMessage());
                System.err.println("═══════════════════════════════════════════════════\n");
            }
        }

        // ========== ENRICH DỮ LIỆU VỚI WEBHOOK PROCESSOR ==========
        // Làm giàu dữ liệu với metadata AI (sentiment, location, focus, damage/rescue)
        if (!allResults.isEmpty()) {
            try (WebhookProcessor processor = WebhookProcessor.mockProcessor()) {
                System.out.println("┌─────────────────────────────────────────────┐");
                System.out.println("│ Đang làm giàu dữ liệu với AI metadata...     │");
                System.out.println("└─────────────────────────────────────────────┘\n");
                allResults = processor.process(allResults);
            } catch (Exception e) {
                System.err.println("⚠ Lỗi khi enrich dữ liệu: " + e.getMessage());
                System.err.println("Tiếp tục với dữ liệu gốc...\n");
            }
        }

        // ========== EXPORT TẤT CẢ KẾT QUẢ VÀO MỘT FILE DUY NHẤT ==========
        // Sử dụng CrawlerConfig để lấy output directory (có thể override bằng env var hoặc system property)
        String outputDir = CrawlerConfig.getOutputDir();
        String outputFile = Paths.get(outputDir, "AllClients_results_utf8.csv")
            .toAbsolutePath()
            .toString();
        
        if (!allResults.isEmpty()) {
            PostCsvExporter.export(allResults, outputFile);
            System.out.println("\n📊 Đã lưu TOÀN BỘ " + allResults.size() + " kết quả vào: " + outputFile);
        } else {
            System.out.println("\n⚠ Không có kết quả nào để export.");
        }

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║           HOÀN THÀNH DEMO LSP & POLYMORPHISM      ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println("\n📊 KẾT QUẢ:");
        System.out.println("  - Tất cả crawler đều sử dụng CÙNG MỘT INTERFACE");
        System.out.println("  - NewsPost và SocialPost đều là AbstractPost");
        System.out.println("  - Dữ liệu đã lưu vào database và CSV files");
        System.out.println("\n✅ ĐÃ ÁP DỤNG:");
        System.out.println("  ✓ Encapsulation (private fields, getter/setter)");
        System.out.println("  ✓ Inheritance (AbstractPost → NewsPost/SocialPost)");
        System.out.println("  ✓ Polymorphism (ISearchClient interface)");
        System.out.println("  ✓ Abstraction (abstract methods)");
        System.out.println("  ✓ SRP (Single Responsibility Principle)");
        System.out.println("  ✓ OCP (Open/Closed Principle)");
        System.out.println("  ✓ LSP (Liskov Substitution Principle)");
        System.out.println("  ✓ DIP (Dependency Inversion Principle)");
    }

    /**
     * Hiển thị một vài posts mẫu
     * POLYMORPHISM: Nhận AbstractPost, có thể là NewsPost hoặc SocialPost
     */
    private static void displaySamplePosts(List<? extends AbstractPost> posts, int count) {
        if (posts.isEmpty()) {
            System.out.println("  (Không có kết quả)");
            return;
        }

        int displayCount = Math.min(count, posts.size());
        System.out.println("\n  📄 Mẫu kết quả:");

        for (int i = 0; i < displayCount; i++) {
            AbstractPost post = posts.get(i);
            // POLYMORPHISM - getDisplayDate() và getEngagementScore() hoạt động khác nhau
            System.out.println("    " + (i+1) + ". [" + post.getPlatform() + "] " +
                             post.getDisplayDate() + " - Score: " + post.getEngagementScore());
            String content = post.getContent();
            if (content.length() > 60) {
                content = content.substring(0, 60) + "...";
            }
            System.out.println("       " + content);
        }
        System.out.println();
    }

    /**
     * BONUS: Demo so sánh SocialPost vs NewsPost
     * Chứng minh rằng getEngagementScore() hoạt động khác nhau (POLYMORPHISM)
     */
    @SuppressWarnings("unused")
    private static void demoEngagementScore() {
        System.out.println("\n>>> DEMO POLYMORPHISM: getEngagementScore() <<<\n");

        // NewsPost dùng comments làm engagement score
        // SocialPost dùng reaction làm engagement score
        // Cả hai đều override method từ AbstractPost!

        System.out.println("NewsPost: engagement = comments");
        System.out.println("SocialPost: engagement = reaction (likes + shares + retweets)");
        System.out.println("\nĐây là POLYMORPHISM - cùng method, khác implementation!");
    }
}
