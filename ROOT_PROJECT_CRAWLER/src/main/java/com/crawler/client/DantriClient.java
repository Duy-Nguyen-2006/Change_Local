package com.crawler.client;

import com.crawler.model.NewsPost;
import com.crawler.util.CrawlerEnv;
import java.io.IOException;
import java.time.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

/**
 * Dantri News Crawler - Kế thừa CrawlerEnv (IMPLEMENTS ISearchClient qua CrawlerEnv)
 * ÁP DỤNG INHERITANCE và POLYMORPHISM
 * Return type: List<NewsPost> (áp dụng INHERITANCE)
 */
public class DantriClient extends CrawlerEnv {
    /**
     * Xử lý số liệu ID, ví dụ 20251020160336 thành 2025-10-20 (Dạng YYYY-MM-DD)
     * @param label
     * @return Ngày đã được xử lý
     */
    private LocalDate dateExtract(String label) {
        String year = label.substring(0, 4), month = label.substring(4, 6), day = label.substring(6, 8);
        return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
    }

    /**
     * Hàm xử lý chính - OVERRIDE abstract method
     */
    @Override
    public void getPosts(String search_input) {
        final int PAGES = 5;

        for (int i = 0; i < PAGES; ++i) {
            try {
                Document pages = Jsoup.connect(String.format("https://dantri.com.vn/tim-kiem/%s.htm?pi=%d",
                                String.join("+", search_input.split(" ")), i + 1)).get();

                Elements posts = pages.getElementsByClass("article-item");

                for (Element post: posts) {
                    Element excerpt_box =  post.getElementsByClass("article-excerpt").get(0);

                    String title, excerpt, date = ""; int comments;
                    try {
                        title = post.getElementsByClass("dt-text-black-mine").text().strip();
                        excerpt = excerpt_box.text().strip();
                        date = excerpt_box.getElementsByTag("span").get(0).attr("data-id").strip();
                    } catch (Exception e) {
                        continue;
                    }

                    try {
                        comments = Integer.parseInt(excerpt_box.getElementsByTag("button").text());
                    } catch (Exception e) {
                        comments = 0;
                    }

                    // SỬ DỤNG NewsPost thay vì Post
                    addPost(new NewsPost(
                            (date == "") ? LocalDate.now() : dateExtract(date),
                            title,
                            excerpt,
                            "Dân trí",
                            comments
                    ));

                    System.out.println("Dantri: Crawled 1 post");
                }

            } catch (IOException u) {
                System.err.println("Không lấy được trang từ Dantri");
            }
        }
    }
}
