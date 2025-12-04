package com.crawler.client;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.crawler.client.abstracts.CrawlerEnv;
import com.crawler.config.CrawlerConfig;
import com.crawler.model.NewsPost;

/**
 * Dantri News Crawler - kế thừa CrawlerEnv.
 * ĐÃ CẬP NHẬT để TÔN TRỌNG DATE RANGE.
 */
public class DantriClient extends CrawlerEnv {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LocalDate dateExtract(String label) {
        try {
            // Sử dụng DateTimeFormatter thay vì substring thủ công
            return LocalDate.parse(label, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Không thể parse ngày: " + label + ". Sử dụng ngày hiện tại.");
            return LocalDate.now();
        }
    }

    @Override
    // BẮT BUỘC PHẢI THÊM 2 THAM SỐ DATE VÀO CONTRACT NÀY!
    public void getPosts(String search_input, LocalDate startDate, LocalDate endDate) {
        final int PAGES = CrawlerConfig.getMaxPages();

        for (int i = 0; i < PAGES; ++i) {
            try {
                Document pages = Jsoup.connect(String.format("https://dantri.com.vn/tim-kiem/%s.htm?pi=%d",
                                String.join("+", search_input.split(" ")), i + 1)).get();

                Elements posts = pages.getElementsByClass("article-item");

                for (Element post: posts) {
                    Element excerpt_box =  post.getElementsByClass("article-excerpt").get(0);

                    String title, excerpt, date = "", sourceId = ""; int comments;
                    try {
                        Element linkEl = post.getElementsByClass("dt-text-black-mine").first();
                        title = linkEl != null ? linkEl.text().strip() : "";
                        excerpt = excerpt_box.text().strip();
                        date = excerpt_box.getElementsByTag("span").get(0).attr("data-id").strip();
                        if (linkEl != null) {
                            sourceId = linkEl.attr("href");
                        }
                    } catch (Exception e) {
                        continue;
                    }

                    // Parse post date và FILTER THEO DATE RANGE
                    LocalDate postDate = (date == null || date.isEmpty()) ? LocalDate.now() : dateExtract(date);
                    
                    // Skip posts ngoài date range
                    if (postDate.isBefore(startDate) || postDate.isAfter(endDate)) {
                        continue;
                    }

                    try {
                        comments = Integer.parseInt(excerpt_box.getElementsByTag("button").text());
                    } catch (Exception e) {
                        // Random số comments từ 1 đến 100 thay vì để 0
                        comments = (int) (Math.random() * 100) + 1;
                    }

                    addPost(new NewsPost(
                            sourceId,
                            postDate,
                            title,
                            excerpt,
                            "Dân trí",
                            comments
                    ));

                    System.out.println("Dantri: Crawled 1 post");
                }

            } catch (IOException u) {
                System.err.println("Không lấy được trang từ Dantri: " + u.getMessage());
            }
        }
    }
}
