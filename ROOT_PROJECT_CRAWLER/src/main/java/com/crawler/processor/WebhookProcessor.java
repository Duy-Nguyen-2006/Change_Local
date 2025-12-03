package com.crawler.processor;
import org.apache.hc.core5.http.ParseException; 
import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * WebhookProcessor - CONCRETE IMPLEMENTATION của IDataProcessor
 *
 * DATA ENRICHMENT PATTERN:
 * - Gọi API bên ngoài (webhook) để phân tích nội dung bài viết
 * - Nhận về sentiment, location, focus
 * - Cập nhật metadata vào từng Post
 *
 * CHAIN OF RESPONSIBILITY:
 * - Có thể chain nhiều processor (Webhook → Filter → Validation)
 * - Mỗi processor xử lý một khía cạnh của data
 *
 * DIP: Implement interface IDataProcessor (abstraction)
 * SRP: Chỉ có MỘT trách nhiệm - Enrichment qua Webhook API
 * OCP: Có thể thêm FilterProcessor, ValidationProcessor mà KHÔNG SỬA code này
 *
 * WEBHOOK API CONTRACT (giả định):
 * POST /analyze
 * Request Body: { "content": "..." }
 * Response Body ví dụ:
 * {
 *   "loai_bai_viet": "cuu_ho",
 *   "cam_xuc_bai_viet": "tieu_cuc",
 *   "tinh_thanh": "khong_xac_dinh"
 * }
 */
public class WebhookProcessor implements IDataProcessor, AutoCloseable {

    private final String webhookUrl;
    private final Gson gson;
    private final CloseableHttpClient httpClient;

    /**
     * Constructor với URL của webhook API
     * @param webhookUrl URL của webhook API (ví dụ: "https://api.example.com/analyze")
     */
    public WebhookProcessor(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Constructor mặc định - dùng mock webhook (không gọi API thật)
     * Dùng cho testing và demo
     */
    public WebhookProcessor() {
        this(null); // null = mock mode
    }

    /**
     * Xử lý và làm giàu dữ liệu posts bằng webhook API
     * POLYMORPHISM: Nhận List<? extends AbstractPost> (NewsPost hoặc SocialPost)
     *
     * Strategy:
     * 1. Với mỗi Post, gọi webhook API với nội dung của Post
     * 2. Parse response để lấy sentiment, location, focus
     * 3. Cập nhật metadata vào Post qua setSentiment(), setLocation(), setFocus()
     * 4. Trả về danh sách Posts đã được enriched
     */
    @Override
    public List<? extends AbstractPost> process(List<? extends AbstractPost> rawPosts) throws CrawlerException {
        if (rawPosts == null || rawPosts.isEmpty()) {
            return rawPosts;
        }

        System.out.println("\n>>> WebhookProcessor: Enriching " + rawPosts.size() + " posts...");

        List<AbstractPost> enrichedPosts = new ArrayList<>();

        for (AbstractPost post : rawPosts) {
            try {
                // Gọi webhook để phân tích nội dung
                JsonObject metadata = analyzeContent(post.getContent());

                // Map metadata theo key tiếng Việt từ webhook
                if (metadata.has("cam_xuc_bai_viet") && !metadata.get("cam_xuc_bai_viet").isJsonNull()) {
                    post.setSentiment(metadata.get("cam_xuc_bai_viet").getAsString());
                }
                if (metadata.has("tinh_thanh") && !metadata.get("tinh_thanh").isJsonNull()) {
                    post.setLocation(metadata.get("tinh_thanh").getAsString());
                }
                if (metadata.has("loai_bai_viet") && !metadata.get("loai_bai_viet").isJsonNull()) {
                    post.setFocus(metadata.get("loai_bai_viet").getAsString());
                }

                enrichedPosts.add(post);

                System.out.println("  ✓ Enriched: " + post.getPlatform() +
                        " | sentiment=" + post.getSentiment() +
                        " | location=" + post.getLocation() +
                        " | focus=" + post.getFocus());

            } catch (Exception e) {
                // Nếu webhook thất bại, vẫn giữ Post (chỉ không có metadata)
                System.err.println("  ✗ Failed to enrich post: " + e.getMessage());
                enrichedPosts.add(post); // Keep original post without metadata
            }
        }

        System.out.println(">>> WebhookProcessor: Completed enrichment\n");
        return enrichedPosts;
    }

    /**
     * Gọi webhook API để phân tích nội dung
     * MOCK MODE: Nếu webhookUrl == null, trả về mock data (không gọi API thật)
     *
     * @param content Nội dung bài viết cần phân tích
     * @return JsonObject chứa sentiment, location, focus
     * @throws CrawlerException Nếu HTTP request thất bại
     */
    private JsonObject analyzeContent(String content) throws CrawlerException {
        // MOCK MODE - Dùng cho testing và demo
        if (webhookUrl == null) {
            return generateMockMetadata(content);
        }

        // REAL MODE - Gọi HTTP POST đến webhook API
        try {
            HttpPost httpPost = new HttpPost(webhookUrl);

            // Tạo request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("content", content);
            String jsonPayload = gson.toJson(requestBody);

            // Set request entity
            StringEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();

                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return parseWebhookResponse(responseBody);
                } else {
                    throw new CrawlerException("Webhook returned error: HTTP " + statusCode);
                }
            }

        } catch (IOException | ParseException e) { // BẮT THÊM ParseException
        throw new CrawlerException("Failed to call webhook API: " + e.getMessage(), e);
        }
    }

    /**
     * Parse webhook response - xử lý format đặc biệt
     * Webhook trả về: { "output": "```json\n{...}\n```" }
     * Cần extract JSON thật từ bên trong markdown code block
     */
    private JsonObject parseWebhookResponse(String responseBody) throws CrawlerException {
        try {
            // Parse response wrapper
            JsonObject wrapper = JsonParser.parseString(responseBody).getAsJsonObject();

            // Kiểm tra có field "output" không
            if (!wrapper.has("output")) {
                // Nếu không có, giả sử đây là JSON trực tiếp
                return wrapper;
            }

            // Lấy output string
            String output = wrapper.get("output").getAsString();

            // Remove markdown code block wrapper: ```json ... ```
            String jsonContent = output.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring("```json".length()).trim();
            } else if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring("```".length()).trim();
            }

            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3).trim();
            }

            // Parse JSON thật
            return JsonParser.parseString(jsonContent).getAsJsonObject();

        } catch (Exception e) {
            throw new CrawlerException("Failed to parse webhook response: " + e.getMessage() +
                                     "\nResponse body: " + responseBody, e);
        }
    }

    /**
     * Generate mock metadata cho testing và demo
     * Rule-based phân tích đơn giản dựa vào từ khóa trong content
     */
    private JsonObject generateMockMetadata(String content) {
        JsonObject metadata = new JsonObject();

        // Mock sentiment - Dựa vào từ khóa cảm xúc
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("tốt") || lowerContent.contains("thành công") ||
            lowerContent.contains("tăng") || lowerContent.contains("phát triển")) {
            metadata.addProperty("cam_xuc_bai_viet", "tich_cuc");
        } else if (lowerContent.contains("xấu") || lowerContent.contains("thất bại") ||
                   lowerContent.contains("giảm") || lowerContent.contains("khủng hoảng")) {
            metadata.addProperty("cam_xuc_bai_viet", "tieu_cuc");
        } else {
            metadata.addProperty("cam_xuc_bai_viet", "trung_lap");
        }

        // Mock location - Dựa vào tên địa danh
        if (lowerContent.contains("hà nội") || lowerContent.contains("hanoi")) {
            metadata.addProperty("tinh_thanh", "ha_noi");
        } else if (lowerContent.contains("hồ chí minh") || lowerContent.contains("sài gòn") ||
                   lowerContent.contains("ho chi minh") || lowerContent.contains("saigon")) {
            metadata.addProperty("tinh_thanh", "tp_hcm");
        } else if (lowerContent.contains("đà nẵng") || lowerContent.contains("da nang")) {
            metadata.addProperty("tinh_thanh", "da_nang");
        } else {
            metadata.addProperty("tinh_thanh", "khong_xac_dinh");
        }

        // Mock loai_bai_viet - Dựa vào chủ đề
        if (lowerContent.contains("chính trị") || lowerContent.contains("chính phủ") ||
            lowerContent.contains("bầu cử")) {
            metadata.addProperty("loai_bai_viet", "chinh_tri");
        } else if (lowerContent.contains("kinh tế") || lowerContent.contains("doanh nghiệp") ||
                   lowerContent.contains("thị trường")) {
            metadata.addProperty("loai_bai_viet", "kinh_te");
        } else if (lowerContent.contains("thể thao") || lowerContent.contains("bóng đá")) {
            metadata.addProperty("loai_bai_viet", "the_thao");
        } else if (lowerContent.contains("công nghệ") || lowerContent.contains("ai") ||
                   lowerContent.contains("startup")) {
            metadata.addProperty("loai_bai_viet", "cong_nghe");
        } else {
            metadata.addProperty("loai_bai_viet", "tong_hop");
        }

        return metadata;
    }

    /**
     * Close HTTP client khi không dùng nữa
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close HttpClient: " + e.getMessage());
        }
    }
}
