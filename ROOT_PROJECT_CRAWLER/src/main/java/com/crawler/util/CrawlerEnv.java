package com.crawler.util;

import com.crawler.client.ISearchClient;
import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import java.time.LocalDate;
import java.util.*;

/**
 * Lớp trừu tượng cho News Crawlers
 * IMPLEMENTS ISearchClient để đảm bảo LSP (Liskov Substitution Principle)
 *
 * Bây giờ CrawlerEnv CÓ THỂ THAY THẾ cho bất kỳ ISearchClient nào!
 */
public abstract class CrawlerEnv implements ISearchClient {
    private ArrayList<NewsPost> resultPosts = new ArrayList<NewsPost>();

    public static final String[] KEYWORDS = {"bão", "lũ", "lụt", "sạt lở", "thiên tai", "ngập",
    "mưa lớn", "mưa to", "giông", "lốc", "triều cường"};

    /**
     * Lọc bài viết theo ngày.
     */
    public ArrayList<NewsPost> filterPostsDate(ArrayList<NewsPost> posts, LocalDate from, LocalDate to) {
        ArrayList<NewsPost> fDate = new ArrayList<NewsPost>();

        if (posts.size() != 0) for (NewsPost post: posts) {
           if (post.getPostDate() == null) continue;

           if (post.getPostDate().compareTo(from) >= 0 && 0 >= post.getPostDate().compareTo(to)) {
               fDate.add(post);
           }
        }
        return fDate;
    }

    /**
     * Lọc bài viết theo từ khoá.
     */
    public ArrayList<NewsPost> filterPostsKeyword(ArrayList<NewsPost> posts) {
        ArrayList<NewsPost> fKey = new ArrayList<NewsPost>();

        if(posts.size() != 0) for (NewsPost post: posts) {
            boolean post_has = false;

            String combiner = post.getTitle() + " " + post.getContent();

            for (String keyword: KEYWORDS)
                if (combiner.contains(keyword)) {
                    post_has = true; break;
                }

            if (post_has) fKey.add(post);
        }
        return fKey;
    }

    /**
     * Thêm bài viết vào kết quả ban đầu.
     */
    protected void addPost(NewsPost sample) {
        resultPosts.add(sample);
    }

    /**
     * Clear kết quả cũ
     */
    protected void clearResults() {
        resultPosts.clear();
    }

    /**
     * Hàm trừu tượng để lớp con implement
     */
    public abstract void getPosts(String title);

    /**
     * Lấy số lượng bài
     */
    public int resultSize() {
        return resultPosts.size();
    }

    /**
     * Lấy danh sách kết quả
     */
    public ArrayList<NewsPost> getResults() {
        return new ArrayList<>(resultPosts);
    }

    // ========== IMPLEMENT ISearchClient (FIX LSP VIOLATION) ==========

    /**
     * IMPLEMENT ISearchClient.search()
     * Đây là phương thức bắt buộc để News Crawler tương thích với Social Crawler
     */
    @Override
    public List<NewsPost> search(String query, int limit) throws CrawlerException {
        try {
            clearResults();

            // Crawl với date range (30 ngày gần nhất)
            LocalDate fromDate = LocalDate.now().minusDays(30);
            LocalDate toDate = LocalDate.now();

            // Gọi abstract method getPosts (lớp con implement)
            getPosts(query);

            // Filter theo date và keyword
            resultPosts = filterPostsDate(resultPosts, fromDate, toDate);
            resultPosts = filterPostsKeyword(resultPosts);

            // Limit kết quả
            if (limit > 0 && resultPosts.size() > limit) {
                resultPosts = new ArrayList<>(resultPosts.subList(0, limit));
            }

            return new ArrayList<>(resultPosts);

        } catch (Exception e) {
            throw new CrawlerException("Lỗi khi crawl news: " + e.getMessage(), e);
        }
    }

    /**
     * IMPLEMENT ISearchClient.initialize()
     * News crawlers không cần khởi tạo gì đặc biệt
     */
    @Override
    public void initialize() {
        System.out.println(this.getClass().getSimpleName() + " initialized.");
    }

    /**
     * IMPLEMENT ISearchClient.close()
     * News crawlers không cần cleanup gì đặc biệt
     */
    @Override
    public void close() {
        System.out.println(this.getClass().getSimpleName() + " closed.");
    }

    /**
     * Legacy method - giữ lại để backward compatibility
     * Nhưng bây giờ recommend dùng search() thay thế
     */
    @Deprecated
    public void mainCrawl(String title, LocalDate from, LocalDate to, String fileName) {
        try {
            clearResults();
            getPosts(title);
            resultPosts = filterPostsDate(resultPosts, from, to);
            resultPosts = filterPostsKeyword(resultPosts);

            // Sử dụng PostCsvExporter thay vì tự viết CSV
            PostCsvExporter.export(resultPosts, fileName);
        } catch (Exception e) {
            System.err.println("Lỗi khi mainCrawl: " + e.getMessage());
        }
    }
}
