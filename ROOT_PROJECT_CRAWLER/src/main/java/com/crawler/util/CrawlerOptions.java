package com.crawler.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.*;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.*;


/**
* Tạo môi trường cài đặt để cào dữ liệu
*/
public class CrawlerOptions {
    WebDriver chromeCr;

    public void envInit() {
        ChromeOptions init_options = new ChromeOptions();

        init_options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        init_options.addArguments("--disable-gpu", "--window-size=1920,1080",
            "--disable-blink-features=AutomationControlled"
        );

        init_options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        String[] exclude_sw = {"enable-automation"};
        init_options.setExperimentalOption("excludeSwitches", exclude_sw);
        init_options.setExperimentalOption("useAutomationExtension", false);

        try {
            /**
             * Chú ý thay đổi path ở đây nếu chromedriver.exe ở vị trí khác
             */
            Path chrome_path = Paths.get("").toAbsolutePath().resolve("src/main/resources/drivers/chromedriver.exe");

            if (!Files.exists(chrome_path)) throw new FileNotFoundException("chromedriver.exe không có");

            ChromeDriverService chromeDr = new ChromeDriverService.Builder()
                                                .usingDriverExecutable(new File(chrome_path.toString()))
                                                .usingAnyFreePort().build();

            chromeCr = new ChromeDriver(chromeDr, init_options);
            System.out.println("OK");
        } catch (FileNotFoundException u) {
            System.err.println("Lỗi Chrome Driver");
        } catch (Exception e) {
            System.err.println("Loi khong xac dinh");
        }
    }

    public void envKill() {
        System.out.println("Cào xong");
        chromeCr.close();
    }
}
