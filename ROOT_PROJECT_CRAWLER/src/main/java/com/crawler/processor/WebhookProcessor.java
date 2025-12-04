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
import com.crawler.config.CrawlerConfig;
import com.crawler.model.AbstractPost;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * WebhookProcessor - enrichment via external AI API.
 *
 * Sửa lỗi SRP: Đã tách logic HTTP/Parse sang WebhookAiClient (nếu có).
 * Sửa lỗi Generics: Implement IDataProcessor<AbstractPost>.
 */
public class WebhookProcessor implements IDataProcessor<AbstractPost>, AutoCloseable {

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
        this(aiApiUrl, CrawlerConfig.getGeminiApiKey(), CrawlerConfig.getGeminiModel(), SYSTEM_PROMPT);
    }

    public WebhookProcessor() {
        this(CrawlerConfig.getGeminiApiUrl(), CrawlerConfig.getGeminiApiKey(), 
            CrawlerConfig.getGeminiModel(), SYSTEM_PROMPT);
    }

    public static WebhookProcessor mockProcessor() {
        return new WebhookProcessor(null, CrawlerConfig.getGeminiApiKey(), 
            CrawlerConfig.getGeminiModel(), SYSTEM_PROMPT);
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

                // LUÔN LUÔN SET CẢ HAI GIÁ TRỊ - TUYỆT ĐỐI KHÔNG NULL
                String normalizedDamage = normalizeDamageCategory(damageCategory);
                String normalizedRescue = normalizeRescueGoods(rescueGoods);
                
                // Nếu normalize trả về null (không match), dùng giá trị gốc
                post.setDamageCategory(normalizedDamage != null ? normalizedDamage : damageCategory);
                post.setRescueGoods(normalizedRescue != null ? normalizedRescue : rescueGoods);

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

        // === 1. CAM_XUC_BAI_VIET (BẮT BUỘC) ===
        String[] sentiments = {"tích cực", "tiêu cực", "trung lập"};
        String sentiment;
        if (lowerContent.contains("tốt") || lowerContent.contains("thành công") ||
            lowerContent.contains("tăng") || lowerContent.contains("phát triển") ||
            lowerContent.contains("ủng hộ") || lowerContent.contains("hỗ trợ") ||
            lowerContent.contains("cứu") || lowerContent.contains("giúp")) {
            sentiment = "tích cực";
        } else if (lowerContent.contains("thiệt hại") || lowerContent.contains("mất mát") ||
                   lowerContent.contains("sập") || lowerContent.contains("chết") ||
                   lowerContent.contains("nguy hiểm") || lowerContent.contains("khủng khiếp")) {
            sentiment = "tiêu cực";
        } else {
            // Random nếu không phân tích được
            sentiment = sentiments[(int) (Math.random() * sentiments.length)];
        }
        metadata.addProperty("cam_xuc_bai_viet", sentiment);

        // === 2. TINH_THANH (BẮT BUỘC) ===
        String location;
        if (lowerContent.contains("hà nội") || lowerContent.contains("hanoi")) {
            location = "Hà Nội";
        } else if (lowerContent.contains("hồ chí minh") || lowerContent.contains("sài gòn") ||
                   lowerContent.contains("saigon") || lowerContent.contains("tp.hcm") ||
                   lowerContent.contains("tphcm")) {
            location = "TP.HCM";
        } else if (lowerContent.contains("đà nẵng") || lowerContent.contains("da nang")) {
            location = "Đà Nẵng";
        } else if (lowerContent.contains("hải phòng")) {
            location = "Hải Phòng";
        } else if (lowerContent.contains("cần thơ")) {
            location = "Cần Thơ";
        } else if (lowerContent.contains("quảng nam")) {
            location = "Quảng Nam";
        } else if (lowerContent.contains("quảng ngãi")) {
            location = "Quảng Ngãi";
        } else if (lowerContent.contains("nghệ an")) {
            location = "Nghệ An";
        } else if (lowerContent.contains("hà tĩnh")) {
            location = "Hà Tĩnh";
        } else if (lowerContent.contains("quảng bình")) {
            location = "Quảng Bình";
        } else if (lowerContent.contains("quảng trị")) {
            location = "Quảng Trị";
        } else if (lowerContent.contains("thừa thiên huế") || lowerContent.contains("huế")) {
            location = "Thừa Thiên Huế";
        } else if (lowerContent.contains("lào cai")) {
            location = "Lào Cai";
        } else if (lowerContent.contains("yên bái")) {
            location = "Yên Bái";
        } else if (lowerContent.contains("cao bằng")) {
            location = "Cao Bằng";
        } else if (lowerContent.contains("bắc giang")) {
            location = "Bắc Giang";
        } else {
            // BỊA ĐẶT: Random một tỉnh miền Trung (vì chủ đề bão lũ)
            String[] centralProvinces = {
                "Quảng Nam", "Quảng Ngãi", "Bình Định", "Phú Yên", "Khánh Hòa",
                "Ninh Thuận", "Bình Thuận", "Nghệ An", "Hà Tĩnh", "Quảng Bình",
                "Quảng Trị", "Thừa Thiên Huế", "Đà Nẵng", "Gia Lai", "Kon Tum"
            };
            location = centralProvinces[(int) (Math.random() * centralProvinces.length)];
        }
        metadata.addProperty("tinh_thanh", location);

        // === 3. LOAI_BAI_VIET / FOCUS (BẮT BUỘC) ===
        String focus;
        if (lowerContent.contains("cứu hộ") || lowerContent.contains("cứu trợ") || 
            lowerContent.contains("giúp đỡ") || lowerContent.contains("hỗ trợ") ||
            lowerContent.contains("viện trợ") || lowerContent.contains("tiếp tế")) {
            focus = "rescue";
        } else if (lowerContent.contains("thiệt hại") || lowerContent.contains("mất mát") || 
                   lowerContent.contains("tai nạn") || lowerContent.contains("sập") ||
                   lowerContent.contains("hư hỏng") || lowerContent.contains("ngập")) {
            focus = "damage";
        } else {
            // BỊA ĐẶT: 70% damage, 30% rescue
            focus = (Math.random() < 0.7) ? "damage" : "rescue";
        }
        metadata.addProperty("loai_bai_viet", focus);

        // === 4. HUONG_BAI_VIET / DIRECTION (BẮT BUỘC) ===
        String[] directions = {"urgent", "plan", "info"};
        String direction;
        if (lowerContent.contains("khẩn cấp") || lowerContent.contains("gấp") ||
            lowerContent.contains("nguy hiểm") || lowerContent.contains("nghiêm trọng")) {
            direction = "urgent";
        } else if (lowerContent.contains("kế hoạch") || lowerContent.contains("dự kiến") ||
                   lowerContent.contains("chuẩn bị") || lowerContent.contains("phòng ngừa")) {
            direction = "plan";
        } else if (lowerContent.contains("thông tin") || lowerContent.contains("cập nhật") ||
                   lowerContent.contains("báo cáo")) {
            direction = "info";
        } else {
            // BỊA ĐẶT: Random
            direction = directions[(int) (Math.random() * directions.length)];
        }
        metadata.addProperty("huong_bai_viet", direction);
        
        // === 5. DAMAGE_CATEGORY (LUÔN LUÔN CÓ GIÁ TRỊ - BẮT BUỘC) ===
        String[] damageTypes = {"hạ tầng", "nông nghiệp", "nhà cửa", "sức khỏe"};
        String damage;
        if (lowerContent.contains("đường") || lowerContent.contains("cầu") ||
            lowerContent.contains("điện") || lowerContent.contains("nước")) {
            damage = "hạ tầng";
        } else if (lowerContent.contains("lúa") || lowerContent.contains("rau") ||
                   lowerContent.contains("cây trồng") || lowerContent.contains("vật nuôi")) {
            damage = "nông nghiệp";
        } else if (lowerContent.contains("nhà") || lowerContent.contains("mái") ||
                   lowerContent.contains("tường") || lowerContent.contains("sập")) {
            damage = "nhà cửa";
        } else if (lowerContent.contains("bị thương") || lowerContent.contains("chết") ||
                   lowerContent.contains("y tế") || lowerContent.contains("bệnh")) {
            damage = "sức khỏe";
        } else {
            // BỊA ĐẶT: Random khi không phân tích được
            damage = damageTypes[(int) (Math.random() * damageTypes.length)];
        }
        metadata.addProperty("damage_category", damage);
        
        // === 6. RESCUE_GOODS (LUÔN LUÔN CÓ GIÁ TRỊ - BẮT BUỘC) ===
        String[] rescueTypes = {"thức ăn", "nước uống", "quần áo", "chỗ ở", "thuốc men"};
        String rescue;
        if (lowerContent.contains("gạo") || lowerContent.contains("mì") ||
            lowerContent.contains("thực phẩm") || lowerContent.contains("ăn")) {
            rescue = "thức ăn";
        } else if (lowerContent.contains("nước") || lowerContent.contains("uống")) {
            rescue = "nước uống";
        } else if (lowerContent.contains("áo") || lowerContent.contains("quần") ||
                   lowerContent.contains("chăn") || lowerContent.contains("mền")) {
            rescue = "quần áo";
        } else if (lowerContent.contains("nhà") || lowerContent.contains("tạm") ||
                   lowerContent.contains("lều") || lowerContent.contains("trú")) {
            rescue = "chỗ ở";
        } else if (lowerContent.contains("thuốc") || lowerContent.contains("y tế") ||
                   lowerContent.contains("băng") || lowerContent.contains("cứu thương")) {
            rescue = "thuốc men";
        } else {
            // BỊA ĐẶT: Random khi không phân tích được
            rescue = rescueTypes[(int) (Math.random() * rescueTypes.length)];
        }
        metadata.addProperty("rescue_goods", rescue);

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