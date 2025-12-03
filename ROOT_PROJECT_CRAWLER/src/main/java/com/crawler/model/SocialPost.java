package com.crawler.model;

import java.time.LocalDate;

/**
 * SocialPost - đại diện cho bài viết mạng xã hội.
 */
public class SocialPost extends AbstractPost {
    private long reaction;
    private LocalDate createdDate;

    public static final String[] HEADER = {"date", "content", "platform", "reaction", 
        "engagement_score", "sentiment", "location", "focus", "direction",
        "damage_category", "rescue_goods"};

    public SocialPost(String sourceId, String content, String platform, LocalDate createdDate, long reaction) {
        super(sourceId, content, platform);
        this.createdDate = createdDate;
        // GỌI SETTER ĐỂ ĐẢM BẢO VALIDATION ĐƯỢC THỰC THI NGAY TRONG CONSTRUCTOR
        setReaction(reaction);
    }

    public long getReaction() {
        return reaction;
    }

    public void setReaction(long reaction) {
        if (reaction < 0) {
            throw new IllegalArgumentException("Reaction must be non-negative");
        }
        this.reaction = reaction;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    @Override
    public String getDisplayDate() {
        return createdDate != null ? createdDate.toString() : "N/A";
    }

    @Override
    public long getEngagementScore() {
        return reaction;
    }

    @Override
    public String[] toCsvArray() {
        return new String[] {
            getDisplayDate(),
            getContent(),
            getPlatform(),
            Long.toString(reaction),
            Long.toString(getEngagementScore()),
            getSentiment() != null ? getSentiment() : "N/A",
            getLocation() != null ? getLocation() : "N/A",
            getFocus() != null ? getFocus() : "N/A",
            getDirection() != null ? getDirection() : "N/A",
            getDamageCategory() != null ? getDamageCategory() : "N/A",
            getRescueGoods() != null ? getRescueGoods() : "N/A"
        };
    }

    @Override
    public String[] getCsvHeader() {
        return HEADER;
    }

    @Override
    public String toString() {
        return String.format("SocialPost[platform=%s, sourceId=%s, date=%s, reaction=%d]",
                getPlatform(), getSourceId(), getDisplayDate(), reaction);
    }
}
