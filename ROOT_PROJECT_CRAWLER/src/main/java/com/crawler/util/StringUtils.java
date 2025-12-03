package com.crawler.util;

import java.util.Arrays;
import java.util.List;

/**
 * StringUtils - Utility class for string processing operations.
 * 
 * UTILITY PATTERN:
 * - Chứa các hàm static helper functions
 * - Không có state (stateless)
 * - Constructor private để ngăn instantiation
 * 
 * SRP: Chỉ chịu trách nhiệm xử lý chuỗi, không liên quan đến crawling logic
 */
public class StringUtils {

    /**
     * Private constructor to prevent instantiation
     */
    private StringUtils() {
        throw new UnsupportedOperationException("Utility class - không được phép khởi tạo");
    }

    /**
     * Parse a comma-separated keyword string into a list.
     * 
     * @param raw Chuỗi keywords phân cách bằng dấu phẩy
     * @return Danh sách keywords đã được trim và filter empty
     * 
     * @example
     * StringUtils.parseKeywords("bão lũ, lũ lụt, mưa lũ")
     * → ["bão lũ", "lũ lụt", "mưa lũ"]
     */
    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
