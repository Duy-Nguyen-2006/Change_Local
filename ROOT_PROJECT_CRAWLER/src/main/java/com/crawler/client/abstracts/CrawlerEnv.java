package com.crawler.client.abstracts;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.crawler.client.CrawlerException;
import com.crawler.client.ISearchClient;
import com.crawler.model.NewsPost;

/**
 * Lớp trừu tượng cho News Crawlers.
 * Tách logic lọc ra khỏi crawler (SRP) - chỉ chịu trách nhiệm crawl thô.
 * 
 * ENCAPSULATION: Protected field được đóng gói với validation và defensive copy
 */
public abstract class CrawlerEnv implements ISearchClient {
    // ENCAPSULATION: Private field thay vì protected
    private final List<NewsPost> resultPosts = new ArrayList<>();

    /**
     * Protected method để subclass thêm post
     * ENCAPSULATION: Validation và null check
     */
    protected void addPost(NewsPost post) {
        if (post == null) {
            throw new IllegalArgumentException("Post cannot be null");
        }
        resultPosts.add(post);
    }

    /**
     * Protected method để subclass clear results
     */
    protected void clearResults() {
        resultPosts.clear();
    }

    /**
     * PHƯƠNG THỨC TRỪU TƯỢNG BẮT BUỘC PHẢI NHẬN ĐẦY ĐỦ DATE ĐỂ THỎA MÃN LSP!
     */
    public abstract void getPosts(String title, LocalDate startDate, LocalDate endDate);

    /**
     * Get result size
     */
    public int resultSize() {
        return resultPosts.size();
    }

    /**
     * Get results với defensive copy
     * ENCAPSULATION: Trả về unmodifiable list để tránh external modification
     */
    public List<NewsPost> getResults() {
        return Collections.unmodifiableList(new ArrayList<>(resultPosts));
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
