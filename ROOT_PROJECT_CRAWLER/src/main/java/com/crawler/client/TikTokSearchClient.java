package com.crawler.client;

import com.crawler.model.SocialPost;
import com.crawler.util.TikTokParser;
import com.crawler.util.SocialDatabase;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TikTok Search Client - IMPLEMENTS ISearchClient
 * ÁP DỤNG POLYMORPHISM và ABSTRACTION
 * Return type: List<SocialPost> (áp dụng INHERITANCE)
 */
public class TikTokSearchClient implements ISearchClient {

    // ENCAPSULATION - Đóng gói các thuộc tính private
    private static final String RAPIDAPI_KEY = "84fd34ba1cmsh4264611a2a81c26p14f915jsn4b51787a5eb1";
    private static final String HOST = "tiktok-scraper7.p.rapidapi.com";
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
     * @param limit Số lượng video tối đa
     * @return Danh sách SocialPost
     * @throws CrawlerException Nếu có lỗi khi crawl
     */
    @Override
    public List<SocialPost> search(String query, int limit) throws CrawlerException {
        try {
            if (httpClient == null) {
                initialize();
            }

            String region = "vn";
            int pageSize = 30;
            int cursor = 0;
            int collected = 0;

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

        String url = "https://" + HOST + "/feed/search?" + queryParams;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-RapidAPI-Key", RAPIDAPI_KEY)
                .header("X-RapidAPI-Host", HOST)
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

    /**
     * Parse a comma-separated keyword string into a list.
     */
    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Crawl TikTok for the given keywords and save all posts into SQLite.
     */
    public static void crawlAndSave(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            System.out.println("No keywords provided.");
            return;
        }

        // SỬ DỤNG POLYMORPHISM - Upcasting to interface
        ISearchClient client = new TikTokSearchClient();
        client.initialize();

        int targetPerKeyword = 120;
        List<SocialPost> allVideos = new ArrayList<>();

        try {
            for (String keyword : keywords) {
                List<SocialPost> results = (List<SocialPost>) client.search(keyword, targetPerKeyword);
                allVideos.addAll(results);
            }

            System.out.println("Total videos collected: " + allVideos.size());
            SocialDatabase.savePosts(allVideos);

        } catch (CrawlerException e) {
            System.err.println("Lỗi crawl: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
        }
    }

    /**
     * Simple demo main
     */
    public static void main(String[] args) {
        String raw;
        if (args.length > 0) {
            raw = args[0];
        } else {
            raw = "bão lũ, lũ lụt, mưa lũ";
        }

        List<String> keywords = parseKeywords(raw);
        System.out.println("Parsed keywords: " + keywords);

        crawlAndSave(keywords);
    }
}
