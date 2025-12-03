package com.crawler.repository;

import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import com.crawler.model.SocialPost;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;

/**
 * FIX LỖI STACKOVERFLOW:
 * Sử dụng 'delegateGson' riêng biệt để serialize/deserialize nội dung,
 * tránh việc gọi đệ quy vô tận vào chính Adapter này.
 */
public class PostTypeAdapter implements JsonSerializer<AbstractPost>, JsonDeserializer<AbstractPost> {

    private static final String TYPE_FIELD = "type";
    
    // TẠO GSON RIÊNG ĐỂ XỬ LÝ DỮ LIỆU THÔ (Tránh đệ quy)
    private final Gson delegateGson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    @Override
    public JsonElement serialize(AbstractPost src, Type typeOfSrc, JsonSerializationContext context) {
        // DÙNG delegateGson THAY VÌ context !!
        JsonObject object = delegateGson.toJsonTree(src).getAsJsonObject();
        object.addProperty(TYPE_FIELD, src.getClass().getSimpleName());
        return object;
    }

    @Override
    public AbstractPost deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        JsonElement typeElement = object.get(TYPE_FIELD);
        
        if (typeElement == null) {
            throw new JsonParseException("Missing post type information");
        }

        String type = typeElement.getAsString();
        Class<? extends AbstractPost> targetClass = switch (type) {
            case "NewsPost" -> NewsPost.class;
            case "SocialPost" -> SocialPost.class;
            default -> throw new JsonParseException("Unknown post type: " + type);
        };

        // DÙNG delegateGson ĐỂ DESERIALIZE (Tránh đệ quy)
        return delegateGson.fromJson(object, targetClass);
    }
}
