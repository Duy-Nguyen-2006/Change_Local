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

public class XSearchClient {

    private static final String RAPIDAPI_KEY = "84fd34ba1cmsh4264611a2a81c26p14f915jsn4b51787a5eb1";
    private static final String HOST = "twitter241.p.rapidapi.com";
    private static final String ENDPOINT = "https://" + HOST + "/search-v2";

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /**
     * Perform one X (Twitter) search API request.
     *
     * @param keyword query string
     * @param type    "Top", "Latest", "Media" ...
     * @param count   number of tweets per page
     * @param cursor  pagination cursor from previous response (null/empty = first page)
     */
    public static String search(String keyword, String type, int count, String cursor)
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
     * Parse comma-separated keywords from user input.
     * Example: "bão lũ, lũ lụt, bão Yagi" -> ["bão lũ", "lũ lụt", "bão Yagi"]
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
     * This mirrors TikTokSearchClient.crawlAndSave.
     */
    public static void crawlAndSave(List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            System.out.println("No X keywords provided.");
            return;
        }

        String type = "Top";      // or "Latest" / "Media" – can later be chosen from GUI
        int targetPerKeyword = 100;  // your choice
        int pageSize = 20;           // depends on API rules

        List<Post> allPosts = new ArrayList<>();

        for (String keyword : keywords) {
            System.out.println("X: keyword \"" + keyword + "\"");

            String cursor = null;
            int collected = 0;
            int pages = 0;
            int maxPages = 10;   // soft safety limit for testing

            String json = null;
            while (collected < targetPerKeyword && pages < maxPages) {
                json = search(keyword, type, pageSize, cursor);

                List<Post> batch = XParser.parseSearchResult(json);
                if (batch.isEmpty()) {
                    System.out.println("  no more tweets found for this keyword.");
                    break;
                }

                allPosts.addAll(batch);
                collected += batch.size();
                pages++;

                System.out.println("  collected " + collected + " tweets so far");

                String nextCursor = XParser.extractNextCursor(json);
                if (nextCursor == null || nextCursor.isEmpty() ||
                        (cursor != null && cursor.equals(nextCursor))) {
                    // No more pages or cursor didn't change
                    break;
                }
                cursor = nextCursor;
            }
            List<Post> batch = XParser.parseSearchResult(json);
            System.out.println("  batch size = " + batch.size());

            if (batch.isEmpty()) {
                // Optional: print a small part of the json to see structure
                System.out.println(json.substring(0, Math.min(400, json.length())));
                break;
            }

            System.out.println("Finished keyword \"" + keyword + "\" with " + collected + " tweets.");
        }

        System.out.println("Total X posts collected: " + allPosts.size());
        SocialDatabase.savePosts(allPosts);
        System.out.println("X posts saved into disaster_post_data.db (table posts).");
    }

    /**
     * Test main. Later, GUI will call crawlAndSave(...) directly.
     */
    public static void main(String[] args) {
        String raw;
        if (args.length > 0) {
            raw = args[0];   // e.g. "bão lũ,lũ lụt,bão Yagi"
        } else {
            raw = " xả lũ phú yên, bão yagi ";  // just example
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
