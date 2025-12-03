package com.crawler.processor;
import org.apache.hc.core5.http.ParseException;
import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

    private static final String DEFAULT_AI_URL = "https://api.volunteer-community.io.vn/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_API_KEY = "duy-demo-key";
    private static final String SYSTEM_PROMPT = "Bạn là một AI phân loại nội dung bài viết.\n"
            + "Nhiệm vụ của bạn: nhận đầu vào là nội dung bài viết dạng văn bản và trả về kết quả theo đúng cấu trúc JSON sau:\n\n"
            + "{\n"
            + "  \"loai_bai_viet\": \"\",\n"
            + "  \"cam_xuc_bai_viet\": \"\",\n"
            + "  \"tinh_thanh\": \"\",\n"
            + "  \"huong_bai_viet\": \"\"\n"
            + "}\n\n"
            + "Trong đó:\n\n"
            + "1. loai_bai_viet:\n"
            + "   - \"cuu_ho\" nếu bài viết nói về cứu hộ, hỗ trợ, giúp đỡ.\n"
            + "   - \"thiet_hai\" nếu bài viết nói về thiệt hại, mất mát, tai nạn, hư hỏng.\n\n\n"
            + "2. cam_xuc_bai_viet:\n"
            + "   - \"tich_cuc\"\n"
            + "   - \"tieu_cuc\"\n\n\n"
            + "3. tinh_thanh:\n"
            + "   - Xác định tỉnh/thành xuất hiện trong bài viết.\n"
            + "   - Nếu không có địa phương nào, trả về \"khong_xac_dinh\".\n\n\n"
            + "Yêu cầu:\n"
            + "- Chỉ trả về JSON, không giải thích thêm.";

    private final String aiApiUrl;
    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final Gson gson;
    private final CloseableHttpClient httpClient;

    /**
     * Constructor với thông số AI endpoint
     * @param aiApiUrl URL của AI API (ví dụ: "https://api.example.com/v1/chat/completions")
     * @param apiKey API key để xác thực
     * @param model Model AI cần sử dụng
     * @param systemPrompt Prompt hệ thống để model trả về JSON đúng format
     */
    public WebhookProcessor(String aiApiUrl, String apiKey, String model, String systemPrompt) {
        this.aiApiUrl = aiApiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Constructor rút gọn - chỉ cần truyền URL AI, dùng default model và API key
     * @param aiApiUrl URL của AI API (ví dụ: "https://api.example.com/v1/chat/completions")
     */
    public WebhookProcessor(String aiApiUrl) {
        this(aiApiUrl, DEFAULT_API_KEY, DEFAULT_MODEL, SYSTEM_PROMPT);
    }

    /**
     * Constructor mặc định - dùng AI endpoint mặc định
     */
    public WebhookProcessor() {
        this(DEFAULT_AI_URL, DEFAULT_API_KEY, DEFAULT_MODEL, SYSTEM_PROMPT);
    }

    /**
     * Factory tạo processor ở mock mode (không gọi API thật)
     */
    public static WebhookProcessor mockProcessor() {
        return new WebhookProcessor(null, DEFAULT_API_KEY, DEFAULT_MODEL, SYSTEM_PROMPT);
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
                if (metadata.has("huong_bai_viet") && !metadata.get("huong_bai_viet").isJsonNull()) {
                    post.setDirection(metadata.get("huong_bai_viet").getAsString());
                }

                enrichedPosts.add(post);

                System.out.println("  ✓ Enriched: " + post.getPlatform() +
                        " | sentiment=" + post.getSentiment() +
                        " | location=" + post.getLocation() +
                        " | focus=" + post.getFocus() +
                        " | direction=" + post.getDirection());

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
        if (aiApiUrl == null) {
            return generateMockMetadata(content);
        }

        // REAL MODE - Gọi AI API để phân tích nội dung
        try {
            HttpPost httpPost = new HttpPost(aiApiUrl);

            // Tạo request body theo format chat completions
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);

            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", content);
            messages.add(userMessage);

            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.0);

            String jsonPayload = gson.toJson(requestBody);

            // Set request entity
            StringEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            // Auth header nếu có API key
            if (apiKey != null && !apiKey.isEmpty()) {
                httpPost.addHeader("Authorization", "Bearer " + apiKey);
            }

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();

                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return parseAiResponse(responseBody);
                } else {
                    throw new CrawlerException("AI API returned error: HTTP " + statusCode);
                }
            }

        } catch (IOException | ParseException e) { // BẮT THÊM ParseException
        throw new CrawlerException("Failed to call AI API: " + e.getMessage(), e);
        }
    }

    /**
     * Parse AI response - xử lý format đặc biệt
     * AI trả về content ở field choices[0].message.content (giống OpenAI style)
     * Có thể chứa code block markdown, cần extract JSON bên trong
     */
    private JsonObject parseAiResponse(String responseBody) throws CrawlerException {
        try {
            // Parse response wrapper
            JsonObject wrapper = JsonParser.parseString(responseBody).getAsJsonObject();

            // Ưu tiên format kiểu OpenAI: choices[0].message.content
            if (wrapper.has("choices")) {
                JsonArray choices = wrapper.getAsJsonArray("choices");
                if (!choices.isEmpty()) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message.has("content")) {
                            String output = message.get("content").getAsString();
                            return extractJsonContent(output, responseBody);
                        }
                    }
                }
            }

            // Fallback: nếu responseBody đã là JSON đúng format
            return extractJsonContent(responseBody, responseBody);

        } catch (Exception e) {
            throw new CrawlerException("Failed to parse AI response: " + e.getMessage() +
                                     "\nResponse body: " + responseBody, e);
        }
    }

    /**
     * Extract JSON content, loại bỏ markdown code block nếu có
     */
    private JsonObject extractJsonContent(String output, String rawResponse) throws CrawlerException {
        try {
            String jsonContent = output.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring("```json".length()).trim();
            } else if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring("```".length()).trim();
            }

            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3).trim();
            }

            return JsonParser.parseString(jsonContent).getAsJsonObject();
        } catch (Exception e) {
            throw new CrawlerException("Failed to parse AI JSON content: " + e.getMessage() +
                    "\nResponse body: " + rawResponse, e);
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
            lowerContent.contains("tăng") || lowerContent.contains("phát triển") ||
            lowerContent.contains("ủng hộ") || lowerContent.contains("hỗ trợ thành công")) {
            metadata.addProperty("cam_xuc_bai_viet", "tich_cuc");
        } else {
            metadata.addProperty("cam_xuc_bai_viet", "tieu_cuc");
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

        // Mock loai_bai_viet - Phân loại theo system prompt
        if (lowerContent.contains("cứu hộ") || lowerContent.contains("cứu trợ") ||
            lowerContent.contains("giúp đỡ") || lowerContent.contains("hỗ trợ") ||
            lowerContent.contains("hỗ trợ nhân dân")) {
            metadata.addProperty("loai_bai_viet", "cuu_ho");
        } else if (lowerContent.contains("thiệt hại") || lowerContent.contains("mất mát") ||
                   lowerContent.contains("tai nạn") || lowerContent.contains("hư hỏng") ||
                   lowerContent.contains("tử vong") || lowerContent.contains("bị thương")) {
            metadata.addProperty("loai_bai_viet", "thiet_hai");
        } else {
            metadata.addProperty("loai_bai_viet", "");
        }

        // Mock huong_bai_viet - Để trống theo system prompt (chỉ cần 4 trường cơ bản)
        metadata.addProperty("huong_bai_viet", "");

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
