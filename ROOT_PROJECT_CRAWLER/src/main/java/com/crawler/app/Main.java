package com.crawler.app;

import com.crawler.client.*;
import com.crawler.model.Post;
import com.crawler.util.CrawlerEnv;
import com.crawler.util.SocialDatabase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Application - DEMO ĐA HÌNH (POLYMORPHISM)
 *
 * Class này minh họa cách sử dụng:
 * 1. POLYMORPHISM - Sử dụng interface ISearchClient thay vì concrete classes
 * 2. LATE BINDING - Quyết định implementation tại runtime
 * 3. UPCASTING - Chuyển đổi từ concrete class sang interface
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== DEMO POLYMORPHISM VỚI CRAWLER ===\n");

        // ===== PHẦN 1: DEMO ĐA HÌNH VỚI SOCIAL MEDIA CRAWLERS =====
        demoSocialMediaCrawlers();

        // ===== PHẦN 2: DEMO ĐA HÌNH VỚI NEWS CRAWLERS =====
        demoNewsCrawlers();
    }

    /**
     * DEMO 1: Sử dụng POLYMORPHISM với ISearchClient
     * QUAN TRỌNG: Thay vì gọi:
     *     TikTokSearchClient tiktok = new TikTokSearchClient();
     * Mày phải gọi:
     *     ISearchClient client = new TikTokSearchClient(); // UPCASTING
     */
    private static void demoSocialMediaCrawlers() {
        System.out.println(">>> DEMO SOCIAL MEDIA CRAWLERS (ÁP DỤNG INTERFACE) <<<\n");

        // Tạo danh sách các crawler - TẤT CẢ ĐỀU LÀ ISearchClient
        // ĐÂY LÀ POLYMORPHISM - Một interface, nhiều implementation
        List<ISearchClient> crawlers = new ArrayList<>();
        crawlers.add(new TikTokSearchClient()); // Upcasting
        crawlers.add(new XSearchClient());       // Upcasting

        String keyword = "bão lũ";
        int limit = 10;

        // LATE BINDING / DYNAMIC DISPATCH
        // Phương thức search() được gọi phụ thuộc vào kiểu thực tế tại runtime
        for (ISearchClient crawler : crawlers) {
            try {
                System.out.println("Khởi tạo crawler...");
                crawler.initialize();

                System.out.println("Đang tìm kiếm: \"" + keyword + "\"");
                List<Post> results = crawler.search(keyword, limit);

                System.out.println("Tìm thấy " + results.size() + " bài viết");
                System.out.println("Đang lưu vào database...");
                SocialDatabase.savePosts(results);

                crawler.close();
                System.out.println("Hoàn thành!\n");

            } catch (Exception e) {
                System.err.println("Lỗi khi crawl: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * DEMO 2: Sử dụng POLYMORPHISM với CrawlerEnv (Abstract Class)
     * News crawlers kế thừa CrawlerEnv
     */
    private static void demoNewsCrawlers() {
        System.out.println(">>> DEMO NEWS CRAWLERS (ÁP DỤNG ABSTRACT CLASS) <<<\n");

        // Tạo danh sách các news crawler - TẤT CẢ ĐỀU LÀ CrawlerEnv
        // ĐÂY LÀ POLYMORPHISM - Một abstract class, nhiều implementation
        List<CrawlerEnv> newsCrawlers = new ArrayList<>();
        newsCrawlers.add(new VNExpressClient()); // Upcasting
        newsCrawlers.add(new DantriClient());     // Upcasting

        String searchKeyword = "Bão lũ Đắk Lắk";
        LocalDate fromDate = LocalDate.of(2025, 11, 19);
        LocalDate toDate = LocalDate.now();

        // LATE BINDING / DYNAMIC DISPATCH
        // Phương thức mainCrawl() được gọi phụ thuộc vào kiểu thực tế tại runtime
        int index = 1;
        for (CrawlerEnv crawler : newsCrawlers) {
            try {
                String fileName = "news_crawler_" + index + ".csv";
                System.out.println("Đang crawl từ " + crawler.getClass().getSimpleName() + "...");

                crawler.mainCrawl(searchKeyword, fromDate, toDate, fileName);

                System.out.println("Đã lưu " + crawler.resultSize() + " bài viết vào " + fileName);
                System.out.println("Hoàn thành!\n");
                index++;

            } catch (Exception e) {
                System.err.println("Lỗi khi crawl news: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * BONUS: Demo cách sử dụng một crawler cụ thể thông qua interface
     */
    public static void useSingleCrawler() {
        // SỬ DỤNG INTERFACE - KHÔNG PHẢI CONCRETE CLASS
        ISearchClient client = new TikTokSearchClient(); // Upcasting

        try {
            client.initialize();
            List<Post> results = client.search("thiên tai", 50);
            SocialDatabase.savePosts(results);
            client.close();

            System.out.println("Đã crawl " + results.size() + " posts");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
