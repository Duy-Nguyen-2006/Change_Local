from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.remote.remote_connection import RemoteConnection
from pathlib import Path
import os
import sys
import time
chrome_options = Options()
chrome_options.add_argument("--headless=new")
chrome_options.add_argument("--no-sandbox")
chrome_options.add_argument("--disable-dev-shm-usage")
chrome_options.add_argument("--disable-gpu")
chrome_options.add_argument("--window-size=1920,1080")
chrome_options.add_argument("--disable-blink-features=AutomationControlled")
chrome_options.add_argument("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
chrome_options.add_argument("--log-level=3")  # suppress verbose Chromium logs
chrome_options.add_argument("--disable-logging")
chrome_options.add_experimental_option("excludeSwitches", ["enable-automation", "enable-logging"])
chrome_options.add_experimental_option('useAutomationExtension', False)

try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
except AttributeError:
    pass

def extract_data(driver):
    posts = driver.find_elements(By.CSS_SELECTOR, "div.bg-white.rounded-xl.p-4.shadow-sm.border.cursor-pointer")
    if not posts :
        print("No post")
        return
    else:
        for i,post in enumerate(posts, start=1):
            try:
                content = post.find_element(By.CSS_SELECTOR, "div.text-black.mb-3 span").text.strip()
                link_facebook  = post.find_element(By.XPATH,".//a[contains(@href,'facebook.com')]" ).get_attribute("href")
                location = post.find_element(By.CSS_SELECTOR, "div.text-sm.text-gray-600").text.strip()

                print(f"Nội dung : {content}")
                print(f"Link bài viết : {link_facebook}")
                print(f"Địa điểm : {location}")
            except Exception as e:
                print(f"Error :{e} " )

province_to_find = input("Nhập tỉnh vào đây :  ")
# Tìm chromedriver.exe ở thư mục gốc (thư mục cha của win_code)
chromedriver_path = Path(__file__).resolve().parent.parent / "chromedriver.exe"
if not chromedriver_path.exists():
    raise FileNotFoundError(f"Không tìm thấy chromedriver.exe tại: {chromedriver_path}")
service = Service(executable_path=str(chromedriver_path), log_path=os.devnull)
RemoteConnection.set_timeout(30)  # Tránh lỗi Timeout sentinel trên Python/Selenium mới
driver = webdriver.Chrome(service=service, options = chrome_options)

driver.get("https://thongtincuuho.org/")
wait = WebDriverWait(driver , 10)
try:
    all_provinces = wait.until(EC.element_to_be_clickable((By.XPATH, "/html/body/div[2]/header/nav/div[2]/div/button")))
    all_provinces.click()
    time.sleep(2)
    province_option = wait.until(EC.element_to_be_clickable((By.XPATH, f"//button[@data-slot='button' and contains(., '{province_to_find}')]")))
    province_option.click()
    time.sleep(5)  # ← CHỈ SỬA DUY NHẤT CHỖ NÀY: tăng từ 3 lên 5 giây
    extract_data(driver)
except TimeoutException:
    print("Có 0 bài viết được tìm thấy")
finally:
    time.sleep(2)
    driver.quit()
