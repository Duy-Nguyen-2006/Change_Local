package com.crawler.util;

import java.time.LocalDate;

/**
 * CacheKeyFactory - Utility class để tạo cache keys
 * 
 * SRP: Chỉ có MỘT trách nhiệm - Tạo cache keys theo format chuẩn
 * DRY: Tập trung logic tạo key ở một nơi, tránh duplicate code
 * 
 * Utility class pattern: final class với private constructor
 */
public final class CacheKeyFactory {
    
    private CacheKeyFactory() {
        // Utility class - không cho phép instantiate
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Tạo cache key từ keyword và date range
     * Format: keyword_startDate_endDate
     * 
     * @param keyword Từ khóa tìm kiếm
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Cache key string
     */
    public static String createKey(String keyword, LocalDate startDate, LocalDate endDate) {
        if (keyword == null) {
            keyword = "";
        }
        if (startDate == null) {
            startDate = LocalDate.MIN;
        }
        if (endDate == null) {
            endDate = LocalDate.MAX;
        }
        
        // Normalize keyword: thay spaces bằng underscores
        String normalizedKeyword = keyword.replaceAll("\\s+", "_");
        
        return String.format("%s_%s_%s", 
            normalizedKeyword,
            startDate.toString(),
            endDate.toString());
    }
}


