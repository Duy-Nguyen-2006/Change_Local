package com.crawler.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.crawler.config.CrawlerConfig;
import com.crawler.model.SocialPost;
import com.crawler.util.TikTokParser;

/**
 * TikTok Search Client - IMPLEMENTS ISearchClient
 * ÁP DỤNG POLYMORPHISM và ABSTRACTION
 * Return type: List<SocialPost> (áp dụng INHERITANCE)
 * ĐÃ DỌN DẸP LOGIC ỨNG DỤNG (main, crawlAndSave) RA KHỎI TẦNG CLIENT (SRP)
 * ĐÃ CHUYỂN UTILITY (parseKeywords) SANG PACKAGE UTIL (SRP)
 */
public class TikTokSearchClient implements ISearchClient {

    // ENCAPSULATION - Đóng gói các thuộc tính private
    // Configuration được đọc từ CrawlerConfig (environment variables hoặc system properties)
    private HttpClient httpClient;

    /**
     * Khởi tạo HTTP client
     */
    @Override
    public void initialize() {
        this.httpClient = HttpClient.newHttpClient();
        System.out.println("TikTokSearchClient initialized.");
    }

    /**
     * Đóng kết nối
     */
    @Override
    public void close() {
        System.out.println("TikTokSearchClient closed.");
        // HTTP client tự động cleanup
    }

    /**
     * Tìm kiếm TikTok videos - IMPLEMENTS interface method
     * @param query Từ khóa tìm kiếm
     * @return Danh sách SocialPost
     * @throws CrawlerException Nếu có lỗi khi crawl
     */
    @Override
    public List<SocialPost> search(String query, LocalDate startDate, LocalDate endDate) throws CrawlerException {
        try {
            if (httpClient == null) {
                initialize();
            }

            String region = "vn";
            int pageSize = 30;
            int cursor = 0;
            int collected = 0;
            int limit = CrawlerConfig.getDefaultLimit();

            List<SocialPost> allVideos = new ArrayList<>();

            System.out.println("TikTok - Keyword: \"" + query + "\"");

            while (collected < limit) {
                String json = performSearch(query, region, pageSize, cursor);

                List<SocialPost> batch = TikTokParser.parseSearchResult(json);
                if (batch.isEmpty()) {
                    break;
                }

                allVideos.addAll(batch);
                collected += batch.size();
                System.out.println("  collected " + collected + " videos so far");

                if (!TikTokParser.hasMore(json)) {
                    break;
                }
                int nextCursor = TikTokParser.extractNextCursor(json);
                if (nextCursor <= cursor) {
                    break;
                }
                cursor = nextCursor;
            }

            System.out.println("Finished keyword \"" + query + "\" with " + collected + " videos.");
            return allVideos;

        } catch (Exception e) {
            throw new CrawlerException("Lỗi khi crawl TikTok: " + e.getMessage(), e);
        }
    }

    /**
     * Perform one TikTok search API request (PRIVATE - ENCAPSULATION).
     */
    private String performSearch(String keyword, String region, int count, int cursor)
            throws IOException, InterruptedException {

        int publishTime = 0; // 0 = ALL
        int sortType = 0;    // 0 = relevance

        String queryParams =
                "keywords=" + urlEncode(keyword) +
                        "&region=" + region +
                        "&count=" + count +
                        "&cursor=" + cursor +
                        "&publish_time=" + publishTime +
                        "&sort_type=" + sortType;

        String host = CrawlerConfig.getRapidApiHost();
        String url = "https://" + host + "/feed/search?" + queryParams;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-RapidAPI-Key", CrawlerConfig.getRapidApiKey())
                .header("X-RapidAPI-Host", host)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP Error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
