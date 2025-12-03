package com.crawler.model;

import java.time.LocalDate;

/**
 * NewsPost - đại diện cho bài viết từ báo chí.
 */
public class NewsPost extends AbstractPost {
    private LocalDate postDate;
    private String title;
    private int comments;

    public static final String[] HEADER = {"date", "title", "content", "platform", "comments", "engagement_score"};

    public NewsPost(String sourceId, LocalDate postDate, String title, String content, String platform, int comments) {
        super(sourceId, content, platform);
        this.postDate = postDate;
        this.title = title;
        // GỌI SETTER ĐỂ ĐẢM BẢO VALIDATION ĐƯỢC THỰC THI NGAY TRONG CONSTRUCTOR
        setComments(comments);
    }

    public LocalDate getPostDate() {
        return postDate;
    }

    public String getTitle() {
        return title;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        if (comments < 0) {
            throw new IllegalArgumentException("Comments must be non-negative");
        }
        this.comments = comments;
    }

    @Override
    public String getDisplayDate() {
        return postDate != null ? postDate.toString() : "N/A";
    }

    @Override
    public long getEngagementScore() {
        return comments;
    }

    @Override
    public String[] toCsvArray() {
        return new String[] {
            getDisplayDate(),
            title,
            getContent(),
            getPlatform(),
            Integer.toString(comments),
            Long.toString(getEngagementScore())
        };
    }

    @Override
    public String[] getCsvHeader() {
        return HEADER;
    }

    @Override
    public String toString() {
        return String.format("NewsPost[platform=%s, sourceId=%s, title=%s, date=%s, comments=%d]",
                getPlatform(), getSourceId(), title, getDisplayDate(), comments);
    }
}
