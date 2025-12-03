package com.crawler.client.abstracts;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.model.NewsPost;

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

    /**
     * PHƯƠNG THỨC TRỪU TƯỢNG BẮT BUỘC PHẢI NHẬN ĐẦY ĐỦ DATE ĐỂ THỎA MÃN LSP!
     */
    public abstract void getPosts(String title, LocalDate startDate, LocalDate endDate);

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
            // TRUYỀN ĐẦY ĐỦ THAM SỐ XUỐNG IMPLEMENTATION CỤ THỂ
            getPosts(query, startDate, endDate);
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
    
}
