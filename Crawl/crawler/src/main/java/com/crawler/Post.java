package com.crawler;

import java.time.LocalDate;

/**
 * Lớp Post để đựng thông tin bài viết
 */
public class Post implements CSVFormat {
    public final LocalDate post_date;
    public final String title, content, platform;
    public final int comments;

    public static final String[] HEADER = {"date", "title", "content", "platform", "comments"};

    public Post(LocalDate post_date, String title, String content, String platform, int comments) {
        this.post_date = post_date;
        this.title = title;
        this.content = content;
        this.platform = platform;
        this.comments = comments;
    }
    /**
     * Dành cho việc viết vào file .csv.
     * @return
     */
    public String[] csvParse() {
        return new String[] {post_date.toString(), title, content, platform, Integer.toString(comments)};
    }
    
    
}
