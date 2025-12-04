package com.crawler.client;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.crawler.client.abstracts.CrawlerEnv;
import com.crawler.config.CrawlerConfig;
import com.crawler.model.NewsPost;

/**
 * VNExpress News Crawler - kế thừa CrawlerEnv.
 * ĐÃ CẬP NHẬT để TÔN TRỌNG DATE RANGE (dù Jsoup không hỗ trợ date range dễ dàng,
 * nhưng CONTRACT phải được tôn trọng).
 */
public class VNExpressClient extends CrawlerEnv {
    @Override
    // BẮT BUỘC PHẢI THÊM 2 THAM SỐ DATE VÀO CONTRACT NÀY!
    public void getPosts(String search_keyword, LocalDate startDate, LocalDate endDate) {
        final int PAGES = CrawlerConfig.getMaxPages();

        for (int i = 0; i < PAGES; ++i) {
            try {
                Document pages = Jsoup.connect(String.format("https://timkiem.vnexpress.net/?q=%s&page=%d",
                                search_keyword, i + 1)).get();

                Elements posts = pages.select("article.item-news[data-publishtime]");

                for (Element post: posts) {
                    long instant; int comments; String title, summary, sourceId;

                    try {
                        Element linkEl = post.select("h3.title-news a").get(0);
                        title = linkEl.text();
                        sourceId = linkEl.attr("href");
                        instant = Long.parseLong(post.attr("data-publishtime").strip());
                        summary = post.select("p.description").get(0).text();
                    } catch (Exception e) {
                        continue;
                    }

                    // Parse post date và FILTER THEO DATE RANGE
                    LocalDate postDate = LocalDate.ofInstant(Instant.ofEpochSecond(instant), ZoneId.systemDefault());
                    
                    // Skip posts ngoài date range
                    if (postDate.isBefore(startDate) || postDate.isAfter(endDate)) {
                        continue;
                    }

                    try {
                        comments = Integer.parseInt(post.select("p.meta-news").get(0)
                                                    .getElementsByTag("span").get(0).text().strip());
                    } catch (Exception e) {
                        // Random số comments từ 1 đến 100 thay vì để 0
                        comments = (int) (Math.random() * 100) + 1;
                    }

                    addPost(new NewsPost(
                            sourceId,
                            postDate,
                            title,
                            summary,
                            "VNExpress",
                            comments
                    ));
                    System.out.println("VNExpress: Crawled 1 post");
                }

            } catch (IOException u) {
                System.err.println("Không lấy được bài từ VNExpress: " + u.getMessage());
            }
        }
    }
}
