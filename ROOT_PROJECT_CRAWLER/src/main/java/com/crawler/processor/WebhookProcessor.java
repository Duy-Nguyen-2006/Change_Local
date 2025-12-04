package com.crawler.processor;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.crawler.client.CrawlerException;
import com.crawler.model.AbstractPost;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * WebhookProcessor - enrichment via external AI API.
 *
 * Sửa lỗi SRP: Đã tách logic HTTP/Parse sang WebhookAiClient (nếu có).
 * Sửa lỗi Generics: Implement IDataProcessor<AbstractPost>.
 */
public class WebhookProcessor implements IDataProcessor<AbstractPost>, AutoCloseable {

    private static final String DEFAULT_AI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_API_KEY = "AIzaSyA42BH1RwgFUIQxebfH0IeTGhtu_tURGt8";
    private static final String SYSTEM_PROMPT = """
            Bạn là AI phân loại nội dung bài viết.
            Yêu cầu: trả về JSON với các trường loai_bai_viet, cam_xuc_bai_viet, tinh_thanh, huong_bai_viet.
            Chỉ trả về JSON, không giải thích thêm.
            """;

    private final String aiApiUrl;
    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final Gson gson;
    private final CloseableHttpClient httpClient;

    private static final Map<String, String> DAMAGE_CATEGORY_MAP = Map.of(
            "ha tang", "hạ tầng",
            "nong nghiep", "nông nghiệp",
            "nha cua", "nhà cửa",
            "suc khoe", "sức khỏe"
    );

    private static final Map<String, String> RESCUE_GOODS_MAP = Map.of(
            "thuc an", "thức ăn",
            "nuoc uong", "nước uống",
            "quan ao", "quần áo",
            "cho o", "chỗ ở",
            "thuoc men", "thuốc men"
    );

    public WebhookProcessor(String aiApiUrl, String apiKey, String model, String systemPrompt) {
        this.aiApiUrl = aiApiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();
    }

    public WebhookProcessor(String aiApiUrl) {
        this(aiApiUrl, DEFAULT_API_KEY, DEFAULT_MODEL, SYSTEM_PROMPT);
    }

    public WebhookProcessor() {
        this(DEFAULT_AI_URL, DEFAULT_API_KEY, DEFAULT_MODEL, SYSTEM_PROMPT);
    }

    public static WebhookProcessor mockProcessor() {
        return new WebhookProcessor(null, DEFAULT_API_KEY, DEFAULT_MODEL, SYSTEM_PROMPT);
    }

    @Override
    public List<AbstractPost> process(List<AbstractPost> rawPosts) throws CrawlerException {
        if (rawPosts == null || rawPosts.isEmpty()) {
            return rawPosts;
        }

        System.out.println("\n>>> WebhookProcessor: Enriching " + rawPosts.size() + " posts...");

        List<AbstractPost> enrichedPosts = new ArrayList<>();

        for (AbstractPost post : rawPosts) {
            try {
                JsonObject metadata = analyzeContent(post.getContent());

                String focus = null;
                String damageCategory = null;
                String rescueGoods = null;

                // SỬ DỤNG PROXY GETTER/SETTER TỪ ABSTRACTPOST
                if (metadata.has("cam_xuc_bai_viet") && !metadata.get("cam_xuc_bai_viet").isJsonNull()) {
                    post.setSentiment(metadata.get("cam_xuc_bai_viet").getAsString());
                }
                if (metadata.has("tinh_thanh") && !metadata.get("tinh_thanh").isJsonNull()) {
                    post.setLocation(metadata.get("tinh_thanh").getAsString());
                }
                if (metadata.has("loai_bai_viet") && !metadata.get("loai_bai_viet").isJsonNull()) {
                    focus = metadata.get("loai_bai_viet").getAsString();
                    post.setFocus(focus);
                }
                if (metadata.has("huong_bai_viet") && !metadata.get("huong_bai_viet").isJsonNull()) {
                    post.setDirection(metadata.get("huong_bai_viet").getAsString());
                }

                if (metadata.has("damage_category") && !metadata.get("damage_category").isJsonNull()) {
                    damageCategory = metadata.get("damage_category").getAsString();
                }
                if (metadata.has("rescue_goods") && !metadata.get("rescue_goods").isJsonNull()) {
                    rescueGoods = metadata.get("rescue_goods").getAsString();
                }

                String normalizedDamage = normalizeDamageCategory(damageCategory);
                String normalizedRescue = normalizeRescueGoods(rescueGoods);

                if ("damage".equalsIgnoreCase(focus)) {
                    post.setDamageCategory(normalizedDamage);
                    post.setRescueGoods(null);
                } else if ("rescue".equalsIgnoreCase(focus)) {
                    post.setDamageCategory(null);
                    post.setRescueGoods(normalizedRescue);
                } else {
                    post.setDamageCategory(normalizedDamage);
                    post.setRescueGoods(normalizedRescue);
                }

                enrichedPosts.add(post);

                System.out.println("  Enriched: " + post.getPlatform() +
                        " | sentiment=" + post.getSentiment() +
                        " | location=" + post.getLocation() +
                        " | focus=" + post.getFocus() +
                        " | direction=" + post.getDirection() +
                        " | damage=" + post.getDamageCategory() +
                        " | rescue=" + post.getRescueGoods());

            } catch (Exception e) {
                System.err.println("  Failed to enrich post: " + e.getMessage());
                enrichedPosts.add(post);
            }
        }

        System.out.println(">>> WebhookProcessor: Completed enrichment\n");
        return enrichedPosts;
    }


    private JsonObject analyzeContent(String content) throws CrawlerException {
        if (aiApiUrl == null) {
            return generateMockMetadata(content);
        }

        // TÁI TẠO LOGIC CŨ VÌ USER ĐÃ XÓA WebhookAiClient
        try {
            String targetUrl = aiApiUrl;
            if (apiKey != null && !apiKey.isEmpty() && !aiApiUrl.contains("key=")) {
                targetUrl = aiApiUrl + (aiApiUrl.contains("?") ? "&" : "?") + "key=" + apiKey;
            }

            HttpPost httpPost = new HttpPost(targetUrl);

            JsonObject requestBody = new JsonObject();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemInstruction = new JsonObject();
                JsonArray systemParts = new JsonArray();
                JsonObject systemText = new JsonObject();
                systemText.addProperty("text", systemPrompt);
                systemParts.add(systemText);
                systemInstruction.add("parts", systemParts);
                requestBody.add("system_instruction", systemInstruction);
            }

            JsonArray contents = new JsonArray();
            JsonObject userContent = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject userText = new JsonObject();
            userText.addProperty("text", content);
            parts.add(userText);
            userContent.add("parts", parts);
            contents.add(userContent);
            requestBody.add("contents", contents);

            StringEntity entity = new StringEntity(gson.toJson(requestBody), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();

                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return parseAiResponse(responseBody);
                } else {
                    throw new CrawlerException("AI API returned error: HTTP " + statusCode);
                }
            }

        } catch (IOException | ParseException e) {
            throw new CrawlerException("Failed to call AI API: " + e.getMessage(), e);
        }
    }

    private JsonObject parseAiResponse(String responseBody) throws CrawlerException {
        try {
            JsonObject wrapper = JsonParser.parseString(responseBody).getAsJsonObject();

            // Google Generative Language API response format
            if (wrapper.has("candidates")) {
                JsonArray candidates = wrapper.getAsJsonArray("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content")) {
                        JsonObject content = firstCandidate.getAsJsonObject("content");
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts != null && !parts.isEmpty()) {
                            JsonObject firstPart = parts.get(0).getAsJsonObject();
                            if (firstPart.has("text")) {
                                String output = firstPart.get("text").getAsString();
                                return extractJsonContent(output, responseBody);
                            }
                        }
                    }
                }
            }

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

            return extractJsonContent(responseBody, responseBody);

        } catch (Exception e) {
            throw new CrawlerException("Failed to parse AI response: " + e.getMessage() +
                    "\nResponse body: " + responseBody, e);
        }
    }

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


    private JsonObject generateMockMetadata(String content) {
        JsonObject metadata = new JsonObject();
        String lowerContent = content.toLowerCase();

        // Logic sentiment cũ giữ nguyên
        if (lowerContent.contains("tot") || lowerContent.contains("thanh cong") ||
            lowerContent.contains("tang") || lowerContent.contains("phat trien") ||
            lowerContent.contains("ung ho") || lowerContent.contains("ho tro")) {
            metadata.addProperty("cam_xuc_bai_viet", "tich_cuc");
        } else {
            metadata.addProperty("cam_xuc_bai_viet", "tieu_cuc");
        }

        // Logic tinh_thanh cũ giữ nguyên
        if (lowerContent.contains("ha noi") || lowerContent.contains("hanoi")) {
            metadata.addProperty("tinh_thanh", "ha_noi");
        } else if (lowerContent.contains("ho chi minh") || lowerContent.contains("sai gon") ||
                   lowerContent.contains("saigon")) {
            metadata.addProperty("tinh_thanh", "tp_hcm");
        } else if (lowerContent.contains("da nang")) {
            metadata.addProperty("tinh_thanh", "da_nang");
        } else {
            metadata.addProperty("tinh_thanh", "khong_xac_dinh");
        }

        // XỬ LÝ TRƯỜNG FOCUS VÀ TẠO DỮ LIỆU BỊA ĐẶT (FABRICATION)
        String focus;
        if (lowerContent.contains("cuu ho") || lowerContent.contains("cuu tro") || lowerContent.contains("giup do")) {
            focus = "rescue";
        } else if (lowerContent.contains("thiet hai") || lowerContent.contains("mat mat") || lowerContent.contains("tai nan")) {
            focus = "damage";
        } else {
            // Trường hợp không rõ, AI tự bịa đặt thành "damage" (ví dụ)
            focus = "damage";
        }
        metadata.addProperty("loai_bai_viet", focus);
        metadata.addProperty("huong_bai_viet", "");
        
        // --- LOGIC BỊA ĐẶT CHO CÁC TRƯỜNG PHỤ (BẮT BUỘC) ---
        if ("damage".equals(focus)) {
            String[] damageTypes = {"hạ tầng", "nông nghiệp", "nhà cửa", "sức khỏe"}; // LOAI BO "KHAC"
            String damage = damageTypes[(int) (Math.random() * damageTypes.length)];
            metadata.addProperty("damage_category", damage);
            // BẮT BUỘC NULL - sử dụng add() thay vì addProperty() cho JsonNull
            metadata.add("rescue_goods", JsonNull.INSTANCE);
        } else if ("rescue".equals(focus)) {
            String[] rescueTypes = {"thức ăn", "nước uống", "quần áo", "chỗ ở", "thuốc men"}; // LOAI BO "KHAC"
            String rescue = rescueTypes[(int) (Math.random() * rescueTypes.length)];
            metadata.addProperty("rescue_goods", rescue);
            // BẮT BUỘC NULL - sử dụng add() thay vì addProperty() cho JsonNull
            metadata.add("damage_category", JsonNull.INSTANCE);
        }

        return metadata;
    }


    private String normalizeDamageCategory(String value) {
        return normalizeChoice(value, DAMAGE_CATEGORY_MAP);
    }

    private String normalizeRescueGoods(String value) {
        return normalizeChoice(value, RESCUE_GOODS_MAP);
    }

    private String normalizeChoice(String value, Map<String, String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = stripAccents(value).toLowerCase().trim();
        for (Map.Entry<String, String> entry : allowed.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String stripAccents(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    @Override
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