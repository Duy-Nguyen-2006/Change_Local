import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import java.util.HashSet;
import java.util.Set;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class NitterSeleniumDemo {

    private static final String NITTER_BASE = "https://nitter.net";
    private static final String OUTPUT_CSV = "raw_posts_nitter.csv";



    public static void main(String[] args) throws Exception {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        try {
            // choose 6–10 keywords related to floods / Yagi / cứu trợ
            List<String> keywords = List.of(
                    "sập cầu phong châu",
                    "tin giả mưa lũ",
                    "tin giả câu view"
            );

            int maxTotal = 150;        // overall target
            int maxPerQuery = 40;      // cap per keyword

            List<Tweet> allTweets = new ArrayList<>();
            Set<String> seenUrls = new HashSet<>();

            for (String kw : keywords) {
                if (allTweets.size() >= maxTotal) break;

                System.out.println("=== Query: " + kw + " ===");
                int remaining = maxTotal - allTweets.size();
                int limitForThisQuery = Math.min(maxPerQuery, remaining);

                List<Tweet> batch = searchTweets(driver, kw, limitForThisQuery, seenUrls);
                int delay = ThreadLocalRandom.current().nextInt(3000, 6000);
                System.out.println("Sleeping for " + delay + " ms...");
                Thread.sleep(delay);
                allTweets.addAll(batch);
            }

            System.out.println("TOTAL tweets collected: " + allTweets.size());
            saveToCSV(allTweets);
            System.out.println("DONE. Saved to " + OUTPUT_CSV);

        } finally {
            driver.quit();
        }
    }


    public static List<Tweet> searchTweets(WebDriver driver, String query,
                                           int maxPosts, Set<String> seenUrls) throws Exception {
        List<Tweet> results = new ArrayList<>();

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = NITTER_BASE + "/search?f=tweets&q=" + encoded;

        System.out.println("Opening: " + url);
        driver.get(url);
        Thread.sleep(2000);

        String html = driver.getPageSource();
        Document doc = Jsoup.parse(html);

        Elements tweetElems = doc.select("div.timeline-item");
        System.out.println("  tweetElems found: " + tweetElems.size());

        for (Element el : tweetElems) {
            if (results.size() >= maxPosts) break;

            // username
            String username = textOf(el, "a.username");

            // date
            Element timeEl = el.selectFirst("span.tweet-date a");
            String dateStr = timeEl != null ? timeEl.attr("title") : "";
            if (dateStr.isEmpty() && timeEl != null) dateStr = timeEl.text();

            // only 2024–2025
            if (!(dateStr.contains("2024") || dateStr.contains("2025"))) continue;

            // url
            String rel = timeEl != null ? timeEl.attr("href") : "";
            String fullUrl = rel.isEmpty() ? "" :
                    (rel.startsWith("http") ? rel : NITTER_BASE + rel);
            if (fullUrl.isEmpty() || seenUrls.contains(fullUrl)) continue;

            // text
            String text = textOf(el, "div.tweet-content.media-body");
            if (text.isEmpty()) continue;

            // stats
            Stats stats = extractStats(el);

            results.add(new Tweet(
                    username,
                    text,
                    dateStr,
                    fullUrl,
                    stats.likes,
                    stats.retweets,
                    stats.replies,
                    stats.views
            ));
            seenUrls.add(fullUrl);
        }

        System.out.println("  collected from this query: " + results.size());
        return results;
    }




    private static Stats extractStats(Element tweetEl) {
        Stats s = new Stats();

        // Each stat span has an icon + a number
        Elements statSpans = tweetEl.select("div.tweet-stats span.tweet-stat");
        for (Element span : statSpans) {

            // numbers like "1,161" or " 37 "
            String numText = span.text().replaceAll("[^0-9]", "").trim();
            if (numText.isEmpty()) continue;

            int value;
            try {
                value = Integer.parseInt(numText);
            } catch (NumberFormatException e) {
                continue;
            }

            // decide which type of stat this is, based on the icon inside
            if (span.selectFirst("span.icon-comment") != null) {
                s.replies = value;
            } else if (span.selectFirst("span.icon-retweet") != null) {
                s.retweets = value;
            } else if (span.selectFirst("span.icon-heart") != null) {
                s.likes = value;
            } else if (span.selectFirst("span.icon-views") != null) {
                s.views = value;
            }
        }

        return s;
    }


    private static String textOf(Element parent, String css) {
        Element el = parent.selectFirst(css);
        return el != null ? el.text().trim() : "";
    }

    public static class Stats {
        public int replies;
        public int retweets;
        public int likes;
        public int views;
    }

    public static void saveToCSV(List<Tweet> tweets) throws Exception {
        boolean append = new File(OUTPUT_CSV).exists();

        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT_CSV, StandardCharsets.UTF_8, true))) {

            // If appending and file didn't exist, write BOM + header
            if (!append) {
                pw.write('\uFEFF');
                pw.println("platform,username,date,url,likes,retweets,comments,views,content");
            }

            // Now append data
            for (Tweet t : tweets) {
                String safeText = t.text.replace("\"", "\"\"");
                pw.printf(
                        "\"x\",\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,\"%s\"\n",
                        t.username, t.date, t.url,
                        t.likes, t.retweets, t.comments, t.views,
                        safeText
                );
            }
        }
    }


    public static class Tweet {
        public final String username, text, date, url;
        public final int likes, retweets, comments, views;

        public Tweet(String username, String text, String date, String url,
                     int likes, int retweets, int comments, int views) {
            this.username = username;
            this.text = text;
            this.date = date;
            this.url = url;
            this.likes = likes;
            this.retweets = retweets;
            this.comments = comments;
            this.views = views;
        }
    }
}


