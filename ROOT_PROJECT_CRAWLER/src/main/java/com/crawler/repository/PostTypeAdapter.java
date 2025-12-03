package com.crawler.repository;

import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import com.crawler.model.SocialPost;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Custom Gson adapter to handle AbstractPost polymorphic serialization/deserialization.
 * Adds a "type" metadata field so Gson can reconstruct the correct subclass.
 */
public class PostTypeAdapter implements JsonSerializer<AbstractPost>, JsonDeserializer<AbstractPost> {

    private static final String TYPE_FIELD = "type";

    @Override
    public JsonElement serialize(AbstractPost src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement element = context.serialize(src, src.getClass());
        if (!element.isJsonObject()) {
            return element;
        }

        JsonObject object = element.getAsJsonObject();
        object.addProperty(TYPE_FIELD, src.getClass().getSimpleName());
        return object;
    }

    @Override
    public AbstractPost deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonParseException("Post JSON must be an object");
        }

        JsonObject object = json.getAsJsonObject();
        JsonElement typeElement = object.get(TYPE_FIELD);
        if (typeElement == null || !typeElement.isJsonPrimitive()) {
            throw new JsonParseException("Missing post type information");
        }

        String type = typeElement.getAsString();
        Class<? extends AbstractPost> targetClass = resolveTargetClass(type);
        return context.deserialize(json, targetClass);
    }

    private Class<? extends AbstractPost> resolveTargetClass(String type) {
        return switch (type) {
            case "NewsPost" -> NewsPost.class;
            case "SocialPost" -> SocialPost.class;
            default -> throw new JsonParseException("Unknown post type: " + type);
        };
    }
}
