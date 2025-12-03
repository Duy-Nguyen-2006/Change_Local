package com.crawler.client;

import com.crawler.client.abstracts.CrawlerEnv;
import com.crawler.model.NewsPost;
import java.io.IOException;
import java.time.LocalDate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Dantri News Crawler - kế thừa CrawlerEnv.
 */
public class DantriClient extends CrawlerEnv {
    private LocalDate dateExtract(String label) {
        String year = label.substring(0, 4), month = label.substring(4, 6), day = label.substring(6, 8);
        return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
    }

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

                    try {
                        comments = Integer.parseInt(excerpt_box.getElementsByTag("button").text());
                    } catch (Exception e) {
                        comments = 0;
                    }

                    addPost(new NewsPost(
                            sourceId,
                            (date == null || date.isEmpty()) ? LocalDate.now() : dateExtract(date),
                            title,
                            excerpt,
                            "DA›n trA-",
                            comments
                    ));

                    System.out.println("Dantri: Crawled 1 post");
                }

            } catch (IOException u) {
                System.err.println("KhA'ng l §y Ž`’ø ¯œc trang t ¯® Dantri");
            }
        }
    }
}
