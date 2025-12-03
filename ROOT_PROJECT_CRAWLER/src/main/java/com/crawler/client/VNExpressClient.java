package com.crawler.client;

import com.crawler.client.abstracts.CrawlerEnv;
import com.crawler.model.NewsPost;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * VNExpress News Crawler - kế thừa CrawlerEnv.
 */
public class VNExpressClient extends CrawlerEnv {
    @Override
    public void getPosts(String search_keyword) {
        final int PAGES = 5;

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

                    try {
                        comments = Integer.parseInt(post.select("p.meta-news").get(0)
                                                    .getElementsByTag("span").get(0).text().strip());
                    } catch (Exception e) {
                        comments = 0;
                    }

                    addPost(new NewsPost(
                            sourceId,
                            LocalDate.ofInstant(Instant.ofEpochSecond(instant), ZoneId.systemDefault()),
                            title,
                            summary,
                            "VNExpress",
                            comments
                    ));
                    System.out.println("VNExpress: Crawled 1 post");
                }

            } catch (IOException u) {
                System.err.println("KhA'ng l §y Ž`’ø ¯œc bAÿi t ¯® VNExpress");
            }
        }
    }
}
