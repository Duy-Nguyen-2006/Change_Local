package com.crawler.client;

import com.crawler.model.Post;
import com.crawler.util.CrawlerEnv;
import java.io.IOException;
import java.time.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

/**
 * VNExpress News Crawler - Kế thừa CrawlerEnv
 * ÁP DỤNG INHERITANCE và POLYMORPHISM
 */
public class VNExpressClient extends CrawlerEnv {
    /**
     * Hàm xử lý chính - OVERRIDE abstract method
     */
    @Override
    public void getPosts(String search_keyword) {
        final int PAGES = 5;

        for (int i = 0; i < PAGES; ++i) {
            try {
                Document pages = Jsoup.connect(String.format("https://timkiem.vnexpress.net/?q=%s&page=%d",
                                search_keyword, i + 1)).get();

                Elements posts = pages.select("article.item-news[data-publishtime]");

                for (Element post: posts) {
                    long instant; int comments; String title, summary;

                    try {
                        title = post.select("h3.title-news a").get(0).text();
                        instant = Long.parseLong(post.attr("data-publishtime").strip());
                        summary = post.select("p.description").get(0).text();
                    } catch (Exception e) {
                        continue;
                    }

                    try {
                        comments = Integer.parseInt(post.select("p.meta-news").get(0)
                                                    .getElementsByTag("span").get(0).text().strip());
                    } catch (Exception e) {
                        comments = 0;
                    }

                    addPost(new Post(LocalDate.ofInstant(Instant.ofEpochSecond(instant), ZoneId.systemDefault()),
                                title, summary, "VNExpress", comments));
                    System.out.println("VNExpress: Crawled 1 post");
                }

            } catch (IOException u) {
                System.err.println("Không lấy được bài từ VNExpress");
            }
        }
    }
}
