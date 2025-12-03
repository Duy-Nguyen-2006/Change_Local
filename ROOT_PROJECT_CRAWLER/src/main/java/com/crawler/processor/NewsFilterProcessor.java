package com.crawler.processor;

import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NewsFilterProcessor - Tách logic lọc ra khỏi tầng crawler.
 * SRP: Chỉ chịu trách nhiệm lọc dữ liệu NewsPost theo ngày và từ khóa.
 * DIP: Được inject vào Service qua interface IDataProcessor, không gắn với crawler hay repository.
 */
public class NewsFilterProcessor implements IDataProcessor {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<String> keywords;

    public NewsFilterProcessor(LocalDate startDate, LocalDate endDate, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("Keywords must not be null or empty. Please provide a valid list of keywords.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.keywords = new ArrayList<>(keywords);
    }

    @Override
    public List<? extends AbstractPost> process(List<? extends AbstractPost> rawPosts) throws CrawlerException {
        if (rawPosts == null || rawPosts.isEmpty()) {
            return rawPosts;
        }

        List<AbstractPost> filtered = new ArrayList<>();

        for (AbstractPost post : rawPosts) {
            if (!(post instanceof NewsPost newsPost)) {
                filtered.add(post);
                continue;
            }

            if (!isWithinDateRange(newsPost.getPostDate())) {
                continue;
            }

            if (!matchesKeywords(newsPost)) {
                continue;
            }

            filtered.add(newsPost);
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
        String combined = title + " " + content;

        for (String keyword : keywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }
}
