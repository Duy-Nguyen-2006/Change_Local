package com.crawler;

import java.time.LocalDate;

public class Main {
    /*
    Hàm này xử lý các yêu cầu đầu vào, từ UI về CT Java này và ngược lại

    Chú ý: File đã ghi trong vnexpress.csv và dantri.csv, nếu cần xử lý 
    offline thì chi cần đọc file csv là đủ.
     */

    public static void main(String[] args) {
        CrawlerEnv crVNE = new CrawlVNExpress(), crvDT = new CrawlDantri();

        crvDT.mainCrawl("Bão lũ Đắk Lắk", LocalDate.of(2025, 11, 19), LocalDate.now(), "dantri.csv");
        crVNE.mainCrawl("Bão lũ Đắk Lắk", LocalDate.of(2025, 11, 19), LocalDate.now(), "vnexpress.csv");

    }

}