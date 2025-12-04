package com.crawler.processor;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.crawler.client.CrawlerException;
import com.crawler.model.NewsPost;

/**
 * NewsFilterProcessor - Tách logic lọc ra khỏi tầng crawler.
 * SRP: Chỉ chịu trách nhiệm lọc dữ liệu NewsPost theo ngày và từ khóa.
 * DIP: Được inject vào Service qua interface IDataProcessor, không gắn với crawler hay repository.
 * OCP: Sử dụng Generics <NewsPost> để loại bỏ 'instanceof'.
 */
public class NewsFilterProcessor implements IDataProcessor<NewsPost> {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<String> keywords;
    private final List<String> normalizedKeywords;

    public NewsFilterProcessor(LocalDate startDate, LocalDate endDate, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("Keywords must not be null or empty. Please provide a valid list of keywords.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.keywords = new ArrayList<>(keywords);
        this.normalizedKeywords = this.keywords.stream()
                .map(this::normalize)
                .toList();
    }

    @Override
    public List<NewsPost> process(List<NewsPost> rawPosts) throws CrawlerException {
        if (rawPosts == null || rawPosts.isEmpty()) {
            return rawPosts;
        }

        List<NewsPost> filtered = new ArrayList<>();

        // KHÔNG CẦN INSTANCEOF VÌ INTERFACE ĐÃ ÉP KIỂU LÀ NewsPost! (OCP)
        for (NewsPost post : rawPosts) { 
            if (!isWithinDateRange(post.getPostDate())) {
                continue;
            }

            if (!matchesKeywords(post)) {
                continue;
            }

            filtered.add(post);
        }

        return filtered;
    }

    private boolean isWithinDateRange(LocalDate postDate) {
        if (postDate == null) {
            return false;
        }
        if (startDate != null && postDate.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && postDate.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private boolean matchesKeywords(NewsPost post) {
        if (keywords.isEmpty()) {
            return true;
        }
        String title = post.getTitle() != null ? post.getTitle() : "";
        String content = post.getContent() != null ? post.getContent() : "";
        // Cần normalize vì NewsFilterProcessor.java trong version trước đó không có logic normalize
        String combined = normalize(title + " " + content); 

        for (String keyword : normalizedKeywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}