package com.crawler.config;

/**
 * CrawlerConfig - Centralized configuration management
 * 
 * SRP: Chỉ có MỘT trách nhiệm - Quản lý configuration
 * OCP: Có thể thay đổi config source (env vars, properties file) mà không sửa code
 * 
 * Configuration được đọc từ:
 * 1. Environment variables (ưu tiên cao nhất)
 * 2. System properties
 * 3. Default values (fallback)
 */
public final class CrawlerConfig {
    
    private CrawlerConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ========== API KEYS ==========
    
    /**
     * RapidAPI Key cho TikTok scraper
     * Environment variable: RAPIDAPI_KEY
     * System property: crawler.rapidapi.key
     */
    public static String getRapidApiKey() {
        return getConfig("RAPIDAPI_KEY", "crawler.rapidapi.key", 
            "84fd34ba1cmsh4264611a2a81c26p14f915jsn4b51787a5eb1");
    }
    
    /**
     * RapidAPI Host cho TikTok scraper
     * Environment variable: RAPIDAPI_HOST
     * System property: crawler.rapidapi.host
     */
    public static String getRapidApiHost() {
        return getConfig("RAPIDAPI_HOST", "crawler.rapidapi.host", 
            "tiktok-scraper7.p.rapidapi.com");
    }
    
    /**
     * Google Gemini API Key cho WebhookProcessor
     * Environment variable: GEMINI_API_KEY
     * System property: crawler.gemini.api.key
     */
    public static String getGeminiApiKey() {
        return getConfig("GEMINI_API_KEY", "crawler.gemini.api.key", 
            "AIzaSyA42BH1RwgFUIQxebfH0IeTGhtu_tURGt8");
    }
    
    /**
     * Google Gemini API URL
     * Environment variable: GEMINI_API_URL
     * System property: crawler.gemini.api.url
     */
    public static String getGeminiApiUrl() {
        return getConfig("GEMINI_API_URL", "crawler.gemini.api.url", 
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent");
    }
    
    /**
     * Google Gemini Model name
     * Environment variable: GEMINI_MODEL
     * System property: crawler.gemini.model
     */
    public static String getGeminiModel() {
        return getConfig("GEMINI_MODEL", "crawler.gemini.model", 
            "gemini-1.5-flash");
    }
    
    // ========== CRAWLER LIMITS ==========
    
    /**
     * Default limit cho TikTok search
     * Environment variable: CRAWLER_DEFAULT_LIMIT
     * System property: crawler.default.limit
     */
    public static int getDefaultLimit() {
        return getIntConfig("CRAWLER_DEFAULT_LIMIT", "crawler.default.limit", 120);
    }
    
    /**
     * Maximum pages để crawl cho news sites
     * Environment variable: CRAWLER_MAX_PAGES
     * System property: crawler.max.pages
     */
    public static int getMaxPages() {
        return getIntConfig("CRAWLER_MAX_PAGES", "crawler.max.pages", 5);
    }
    
    // ========== OUTPUT PATHS ==========
    
    /**
     * Output directory cho CSV files
     * Environment variable: CRAWLER_OUTPUT_DIR
     * System property: crawler.output.dir
     */
    public static String getOutputDir() {
        return getConfig("CRAWLER_OUTPUT_DIR", "crawler.output.dir", "output");
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Get config value từ environment variable hoặc system property
     * Priority: Environment variable > System property > Default value
     */
    private static String getConfig(String envVar, String sysProp, String defaultValue) {
        // Check environment variable first
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Check system property
        value = System.getProperty(sysProp);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Return default
        return defaultValue;
    }
    
    /**
     * Get integer config value
     */
    private static int getIntConfig(String envVar, String sysProp, int defaultValue) {
        String value = getConfig(envVar, sysProp, null);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer config value for " + envVar + 
                " or " + sysProp + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }
}


