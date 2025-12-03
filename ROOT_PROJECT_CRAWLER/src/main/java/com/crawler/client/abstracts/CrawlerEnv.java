package com.crawler.client.abstracts;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.model.NewsPost;
import com.crawler.processor.NewsFilterProcessor;
import com.crawler.util.PostCsvExporter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp trừu tượng cho News Crawlers.
 * Tách logic lọc ra khỏi crawler (SRP) - chỉ chịu trách nhiệm crawl thô.
 */
public abstract class CrawlerEnv implements ISearchClient {
    protected ArrayList<NewsPost> resultPosts = new ArrayList<>();

    protected void addPost(NewsPost sample) {
        resultPosts.add(sample);
    }

    protected void clearResults() {
        resultPosts.clear();
    }

    public abstract void getPosts(String title);

    public int resultSize() {
        return resultPosts.size();
    }

    public ArrayList<NewsPost> getResults() {
        return new ArrayList<>(resultPosts);
    }

    @Override
    public List<NewsPost> search(String query, LocalDate startDate, LocalDate endDate) throws CrawlerException {
        try {
            clearResults();
            getPosts(query);
            return new ArrayList<>(resultPosts);
        } catch (Exception e) {
            throw new CrawlerException("Lỗi khi crawl news: " + e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        System.out.println(this.getClass().getSimpleName() + " initialized.");
    }

    @Override
    public void close() {
        System.out.println(this.getClass().getSimpleName() + " closed.");
    }

    /**
     * Legacy method - giữ lại cho backward compatibility.
     */
    @Deprecated
    public void mainCrawl(String title, LocalDate from, LocalDate to, String fileName) {
        try {
            clearResults();
            getPosts(title);
            List<? extends NewsPost> filtered = new NewsFilterProcessor(from, to).process(resultPosts);
            PostCsvExporter.export(filtered, fileName);
        } catch (Exception e) {
            System.err.println("Lỗi khi mainCrawl: " + e.getMessage());
        }
    }
}
