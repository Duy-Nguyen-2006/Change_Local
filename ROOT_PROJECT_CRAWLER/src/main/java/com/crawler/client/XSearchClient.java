package com.crawler.client;

import com.crawler.model.Post;
import com.crawler.util.XParser;
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
 * X (Twitter) Search Client - IMPLEMENTS ISearchClient
 * ÁP DỤNG POLYMORPHISM và ABSTRACTION
 */
public class XSearchClient implements ISearchClient {

    // ENCAPSULATION - Đóng gói các thuộc tính private
    private static final String RAPIDAPI_KEY = "84fd34ba1cmsh4264611a2a81c26p14f915jsn4b51787a5eb1";
    private static final String HOST = "twitter241.p.rapidapi.com";
    private static final String ENDPOINT = "https://" + HOST + "/search-v2";
    private HttpClient httpClient;

    /**
     * Khởi tạo HTTP client
     */
    @Override
    public void initialize() {
        this.httpClient = HttpClient.newHttpClient();
        System.out.println("XSearchClient initialized.");
    }

    /**
     * Đóng kết nối
     */
    @Override
    public void close() {
        System.out.println("XSearchClient closed.");
        // HTTP client tự động cleanup
    }

    /**
     * Tìm kiếm X tweets - IMPLEMENTS interface method
     * @param query Từ khóa tìm kiếm
     * @param limit Số lượng tweet tối đa
     * @return Danh sách Post
     */
    @Override
    public List<Post> search(String query, int limit) throws Exception {
        if (httpClient == null) {
            initialize();
        }

        String type = "Top";
        int pageSize = 20;
        String cursor = null;
        int collected = 0;
        int pages = 0;
        int maxPages = 10;

        List<Post> allPosts = new ArrayList<>();

        System.out.println("X - keyword: \"" + query + "\"");

        while (collected < limit && pages < maxPages) {
            String json = performSearch(query, type, pageSize, cursor);

            List<Post> batch = XParser.parseSearchResult(json);
            if (batch.isEmpty()) {
                System.out.println("  no more tweets found.");
                break;
            }

            allPosts.addAll(batch);
            collected += batch.size();
            pages++;

            System.out.println("  collected " + collected + " tweets so far");

            String nextCursor = XParser.extractNextCursor(json);
            if (nextCursor == null || nextCursor.isEmpty() ||
                    (cursor != null && cursor.equals(nextCursor))) {
                break;
            }
            cursor = nextCursor;
        }

        System.out.println("Finished keyword \"" + query + "\" with " + collected + " tweets.");
        return allPosts;
    }

    /**
     * Perform one X search API request (PRIVATE - ENCAPSULATION).
     */
    private String performSearch(String keyword, String type, int count, String cursor)
            throws IOException, InterruptedException {

        StringBuilder sb = new StringBuilder();
        sb.append(ENDPOINT)
                .append("?type=").append(urlEncode(type))
                .append("&count=").append(count)
                .append("&query=").append(urlEncode(keyword));

        if (cursor != null && !cursor.isEmpty()) {
            sb.append("&cursor=").append(urlEncode(cursor));
        }

        String url = sb.toString();

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
     * Parse comma-separated keywords from user input.
     */
    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Crawl X for the given keywords and save all posts into SQLite.
     */
    public static void crawlAndSave(List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            System.out.println("No X keywords provided.");
            return;
        }

        // SỬ DỤNG POLYMORPHISM - Upcasting to interface
        ISearchClient client = new XSearchClient();
        client.initialize();

        int targetPerKeyword = 100;
        List<Post> allPosts = new ArrayList<>();

        try {
            for (String keyword : keywords) {
                List<Post> results = client.search(keyword, targetPerKeyword);
                allPosts.addAll(results);
            }

            System.out.println("Total X posts collected: " + allPosts.size());
            SocialDatabase.savePosts(allPosts);
            System.out.println("X posts saved into disaster_post_data.db");

        } finally {
            client.close();
        }
    }

    /**
     * Test main
     */
    public static void main(String[] args) {
        String raw;
        if (args.length > 0) {
            raw = args[0];
        } else {
            raw = "xả lũ phú yên, bão yagi";
        }

        List<String> keywords = parseKeywords(raw);
        System.out.println("Parsed X keywords: " + keywords);

        try {
            crawlAndSave(keywords);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
