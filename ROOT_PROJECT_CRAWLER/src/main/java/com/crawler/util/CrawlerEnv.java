package com.crawler.util;

import com.opencsv.CSVWriter;
import com.crawler.model.Post;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Lớp trừu tượng để lấy dữ liệu.
 */

public abstract class CrawlerEnv {
    private ArrayList<Post> resultPosts = new ArrayList<Post>();

    public static final String[] KEYWORDS = {"bão", "lũ", "lụt", "sạt lở", "thiên tai", "ngập",
    "mưa lớn", "mưa to", "giông", "lốc", "triều cường"};

    /**
     * Lọc bài viết theo ngày.
     * Hàm a.compareTo(b) > 0 khi ngày a sau ngày b, nhỏ hơn 0 nếu ngược lại.
     */

    public ArrayList<Post> filterPostsDate(ArrayList<Post> posts, LocalDate from, LocalDate to) {
        ArrayList<Post> fDate = new ArrayList<Post>();

        if (posts.size() != 0) for (Post post: posts) {
           if (post.getPostDate() == null) continue;

           if (post.getPostDate().compareTo(from) >= 0 && 0 >= post.getPostDate().compareTo(to)) fDate.add(post);
        }
        return fDate;
    }

    /**
     * Lọc bài viết theo từ khoá.
     */

    public ArrayList<Post> filterPostsKeyword(ArrayList<Post> posts) {
        ArrayList<Post> fKey = new ArrayList<Post>();

        if(posts.size() != 0) for (Post post: posts) {
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
    public void addPost(Post sample) {
        resultPosts.add(sample);
    }
    /**
     * Hàm tổng quát để cào dữ liệu, khi muốn cào từ Báo Thanh niên cũng không vấn đề gì.
     * @param title
     */
    public abstract void getPosts(String title);

    /**
     * Hàm để tạo file .csv sau khi lọc bài viết xong
     * @param file_name
     */
    public void extractToCSV(String file_name) {
        try (CSVWriter pen = new CSVWriter(new FileWriter(file_name))) {
            pen.writeNext(Post.HEADER);
            for (Post result: resultPosts) pen.writeNext(result.csvParse());

        } catch (IOException e) {
            System.err.println("Không viết được file CSV");
        }
    }
    /**
     * Lấy số lượng bài
     * @return Số lượng bài trong size;
     */

    public int resultSize() {
        return resultPosts.size();
    }
    /**
     * Hàm xử lý các việc trên.
     * Chú ý lấy ngày to sau ngày from.
     * @param title
     * @param from
     * @param to
     * @param file_name
     */
    public void mainCrawl(String title, LocalDate from, LocalDate to, String file_name) {
        getPosts(title);

        resultPosts = filterPostsDate(resultPosts, from, to);

        resultPosts = filterPostsKeyword(resultPosts);

        extractToCSV(file_name);
    }

}
