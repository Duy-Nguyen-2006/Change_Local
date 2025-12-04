package com.crawler.model;

import java.util.Objects;

/**
 * PostMetadata - gói toàn bộ dữ liệu enrichment (Webhook/heuristics).
 *
 * SRP:
 * - Tách riêng metadata khỏi core model (AbstractPost).
 *
 * Thiết kế:
 * - Đơn giản là một POJO với getter/setter để tương thích tốt với Gson.
 */
public class PostMetadata {

    private String sentiment;
    private String location;
    private String focus;
    private String direction;
    private String damageCategory;
    private String rescueGoods;

    public PostMetadata() {
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = normalize(sentiment);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = normalize(location);
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = normalize(focus);
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = normalize(direction);
    }

    public String getDamageCategory() {
        return damageCategory;
    }

    public void setDamageCategory(String damageCategory) {
        this.damageCategory = normalize(damageCategory);
    }

    public String getRescueGoods() {
        return rescueGoods;
    }

    public void setRescueGoods(String rescueGoods) {
        this.rescueGoods = normalize(rescueGoods);
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostMetadata that)) return false;
        return Objects.equals(sentiment, that.sentiment)
                && Objects.equals(location, that.location)
                && Objects.equals(focus, that.focus)
                && Objects.equals(direction, that.direction)
                && Objects.equals(damageCategory, that.damageCategory)
                && Objects.equals(rescueGoods, that.rescueGoods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sentiment, location, focus, direction, damageCategory, rescueGoods);
    }
}