package com.crawler.client;

/**
 * Custom Exception cho Crawler
 * Thay thế "throws Exception" chung chung bằng exception cụ thể, domain-specific
 *
 * SRP: Class này chỉ có một trách nhiệm - đại diện cho lỗi trong quá trình crawl
 * OCP: Có thể extend để tạo các exception con cụ thể hơn (NetworkException, ParseException...)
 */
public class CrawlerException extends RuntimeException {

    /**
     * Constructor với message và cause
     */
    public CrawlerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor chỉ có message
     */
    public CrawlerException(String message) {
        super(message);
    }

    /**
     * Constructor chỉ có cause
     */
    public CrawlerException(Throwable cause) {
        super(cause);
    }
}
