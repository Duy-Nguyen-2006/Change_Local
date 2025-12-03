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

public class TikTokSearchClient {

    // TODO: put your real key here
    private static final String RAPIDAPI_KEY = "84fd34ba1cmsh4264611a2a81c26p14f915jsn4b51787a5eb1";
    private static final String HOST = "tiktok-scraper7.p.rapidapi.com";

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /**
     * Perform one TikTok search API request.
     *
     * @param keyword search keywords
     * @param region  region code such as "vn"
     * @param count   number of videos per page (max 30)
     * @param cursor  pagination cursor (0 for first page)
     */
    public static String search(String keyword, String region, int count, int cursor)
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

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-RapidAPI-Key", RAPIDAPI_KEY)
                .header("X-RapidAPI-Host", HOST)
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP Error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    /**
     * Parse a comma-separated keyword string into a list.
     * Example: "bão lũ, lũ lụt ,mưa lũ" -> ["bão lũ", "lũ lụt", "mưa lũ"]
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
     * This is what your GUI should call.
     */
    public static void crawlAndSave(List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            System.out.println("No keywords provided.");
            return;
        }

        String region = "vn";
        int targetPerKeyword = 120; // desired videos per keyword
        int pageSize = 30;          // max per API call

        List<Post> allVideos = new ArrayList<>();

        for (String keyword : keywords) {
            int cursor = 0;
            int collected = 0;

            System.out.println("Keyword: \"" + keyword + "\"");

            while (collected < targetPerKeyword) {
                String json = search(keyword, region, pageSize, cursor);

                List<Post> batch = TikTokParser.parseSearchResult(json);
                if (batch.isEmpty()) {
                    break; // nothing more for this keyword
                }

                allVideos.addAll(batch);
                collected += batch.size();
                System.out.println("  collected " + collected + " videos so far");

                if (!TikTokParser.hasMore(json)) {
                    break; // API says no more pages
                }
                int nextCursor = TikTokParser.extractNextCursor(json);
                if (nextCursor <= cursor) {
                    break; // safety against infinite loop
                }
                cursor = nextCursor;
            }

            System.out.println(
                    "Finished keyword \"" + keyword + "\" with " + collected + " videos.");
        }

        System.out.println(
                "Total videos collected across all keywords: " + allVideos.size());

        // Store directly into SQLite using your generic SocialDatabase
        SocialDatabase.savePosts(allVideos);
        System.out.println(
                "All videos saved into disaster_post_data.db (table posts).");
    }

    /**
     * Simple demo main: let you test quickly in console.
     * Later the GUI will call crawlAndSave(...) directly.
     */
    public static void main(String[] args) {
        // If args[0] given: use it as raw keyword string
        // Otherwise use a default list for testing.
        String raw;
        if (args.length > 0) {
            raw = args[0];  // e.g. "bão lũ,lũ lụt,mưa lũ"
        } else {
            raw = "bão lũ, lũ lụt, mưa lũ, cứu trợ bão lũ, hướng về miền trung";
        }

        List<String> keywords = parseKeywords(raw);
        System.out.println("Parsed keywords: " + keywords);

        try {
            crawlAndSave(keywords);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
