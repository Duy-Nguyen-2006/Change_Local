"""
Crawler tìm kiếm và lọc các bài viết về bão lũ tại địa điểm được người dùng chỉ định.
Chương trình sẽ tìm kiếm trên VnExpress và chỉ trả về các bài viết liên quan đến bão lũ.
"""

import logging
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple
from urllib.parse import quote

from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.remote.remote_connection import RemoteConnection
from selenium.common.exceptions import NoSuchElementException, TimeoutException
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium import webdriver

# Cấu hình logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Bắt console Windows dùng UTF-8 để in/nhập tiếng Việt không lỗi.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
except AttributeError:
    pass

VN_TZ = timezone(timedelta(hours=7))
DATE_INPUT_FORMAT = "%d/%m/%Y"


def prompt_date(prompt_text: str, end_of_day: bool = False) -> Optional[datetime]:
    while True:
        value = input(prompt_text).strip()
        if not value:
            return None
        try:
            parsed = datetime.strptime(value, DATE_INPUT_FORMAT)
            if end_of_day:
                return datetime(
                    parsed.year,
                    parsed.month,
                    parsed.day,
                    23,
                    59,
                    59,
                    tzinfo=VN_TZ,
                )
            return datetime(parsed.year, parsed.month, parsed.day, tzinfo=VN_TZ)
        except ValueError:
            logger.error("Định dạng ngày không hợp lệ. Vui lòng nhập theo dạng dd/mm/yyyy.")


def prompt_time_range() -> Tuple[Optional[datetime], Optional[datetime]]:
    print("Nhap ngay theo dang dd/mm/yyyy (VD: 01/11/2025). Bo trong neu khong gioi han.")
    while True:
        start_dt = prompt_date("Nhap ngay bat dau: ")
        end_dt = prompt_date("Nhap ngay ket thuc: ", end_of_day=True)
        if start_dt and end_dt and end_dt < start_dt:
            logger.error("Ngay ket thuc phai lon hon hoac bang ngay bat dau.")
            continue
        return start_dt, end_dt


def build_driver() -> webdriver.Chrome:
    """
    Tạo và cấu hình Chrome WebDriver với các tùy chọn tối ưu cho việc crawl.
    
    Returns:
        webdriver.Chrome: Instance của Chrome WebDriver đã được cấu hình
    """
    # RemoteConnection.set_timeout no longer works reliably on some Selenium builds,
    # so we rely on the default client timeout.
    chrome_options = Options()
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--window-size=1920,1080")
    chrome_options.add_argument("--disable-blink-features=AutomationControlled")
    chrome_options.add_argument(
        "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
    chrome_options.add_experimental_option("useAutomationExtension", False)

    # Driver được đặt ở thư mục cha (Crawl) để dùng chung cho các script Windows.
    chromedriver_path = Path(__file__).resolve().parent.parent / "chromedriver.exe"
    if not chromedriver_path.exists():
        raise FileNotFoundError(
            f"Không tìm thấy chromedriver.exe tại: {chromedriver_path}"
        )
    
    service = Service(executable_path=str(chromedriver_path))
    return webdriver.Chrome(service=service, options=chrome_options)


def filter_flood_related_posts(posts_data: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Lọc các bài viết chỉ giữ lại những bài liên quan đến bão lũ.
    
    Args:
        posts_data: Danh sách các bài viết ban đầu
        
    Returns:
        List[Dict[str, Any]]: Danh sách các bài viết đã được lọc
    """
    # Các từ khóa liên quan đến bão lũ
    flood_keywords = [
        "bão", "lũ", "lụt", "ngập", "mưa lớn", "thiên tai",
        "bão lũ", "lũ lụt", "ngập lụt", "mưa bão", "bão tố",
        "lũ quét", "sạt lở", "lũ ống", "bão số",
        "áp thấp", "bão nhiệt đới", "siêu bão", "bão mạnh"
    ]
    
    filtered_posts = []
    
    for post in posts_data:
        title = post.get("title", "").lower()
        summary = post.get("summary", "").lower()
        
        # Kiểm tra xem title hoặc summary có chứa từ khóa bão lũ không
        text_to_check = f"{title} {summary}"
        
        if any(keyword in text_to_check for keyword in flood_keywords):
            filtered_posts.append(post)
            logger.debug(f"Giữ lại bài viết: {post['title'][:50]}...")
        else:
            logger.debug(f"Loại bỏ bài viết (không liên quan bão lũ): {post['title'][:50]}...")
    
    logger.info(f"Đã lọc từ {len(posts_data)} bài viết xuống còn {len(filtered_posts)} bài về bão lũ")
    return filtered_posts


def filter_posts_by_time_range(
    posts_data: List[Dict[str, Any]],
    start_dt: Optional[datetime],
    end_dt: Optional[datetime],
) -> List[Dict[str, Any]]:
    if not start_dt and not end_dt:
        return posts_data

    filtered_posts = []

    for post in posts_data:
        published_at = post.get("published_at")
        if not published_at:
            continue
        if start_dt and published_at < start_dt:
            continue
        if end_dt and published_at > end_dt:
            continue
        filtered_posts.append(post)

    logger.info(
        "Đã lọc theo thời gian từ %d bài viết xuống còn %d bài",
        len(posts_data),
        len(filtered_posts),
    )
    return filtered_posts


def get_posts(driver: webdriver.Chrome, timeout: int = 10) -> List[Dict[str, Any]]:
    """
    Lấy danh sách các bài viết từ trang kết quả tìm kiếm VnExpress.
    
    Args:
        driver: Chrome WebDriver instance
        timeout: Thời gian chờ tối đa (giây) cho các phần tử xuất hiện
        
    Returns:
        List[Dict[str, Any]]: Danh sách các bài viết, mỗi bài viết là một dict
        chứa 'index' (int), 'title' (str), 'link' (str), 'summary' (str),
        và 'published_at' (datetime | None)
    """
    wait = WebDriverWait(driver, timeout)
    posts_data = []
    
    try:
        # Chờ các bài viết xuất hiện
        wait.until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "article.item-news"))
        )
        posts = driver.find_elements(By.CSS_SELECTOR, "article.item-news")
        
        if not posts:
            logger.warning("Không tìm thấy bài viết nào")
            return posts_data

        logger.info(f"Tìm thấy {len(posts)} bài viết")
        
        for index, post in enumerate(posts, start=1):
            try:
                title_link = post.find_element(By.CSS_SELECTOR, "h3.title-news a")
                title = title_link.text.strip()
                link = title_link.get_attribute("href")
                
                try:
                    summary_element = post.find_element(By.CSS_SELECTOR, "p.description")
                    summary = summary_element.text.strip()
                except NoSuchElementException:
                    summary = "Không có tóm tắt"
                    logger.debug(f"Bài viết {index} không có tóm tắt")

                published_at = None
                raw_timestamp = post.get_attribute("data-publishtime")
                if raw_timestamp:
                    try:
                        published_at = datetime.fromtimestamp(int(raw_timestamp), tz=VN_TZ)
                    except (ValueError, OSError):
                        logger.debug(
                            "Không thể parse thời gian xuất bản cho bài viết %d với giá trị %s",
                            index,
                            raw_timestamp,
                        )

                post_data = {
                    "index": index,
                    "title": title,
                    "link": link,
                    "summary": summary,
                    "published_at": published_at,
                }
                posts_data.append(post_data)
                
            except NoSuchElementException as e:
                logger.warning(f"Không tìm thấy phần tử trong bài viết {index}: {e}")
                continue
            except Exception as exc:
                logger.error(f"Lỗi khi xử lý bài viết {index}: {exc}")
                continue
                
    except TimeoutException:
        logger.error("Timeout: Không tìm thấy phần tử article.item-news")
    except Exception as exc:
        logger.error(f"Lỗi không mong đợi: {exc}")
    
    return posts_data


def print_posts(posts_data: List[Dict[str, Any]]) -> None:
    """
    In danh sách bài viết ra console.
    
    Args:
        posts_data: Danh sách các bài viết
    """
    if not posts_data:
        print("Không có bài viết nào để hiển thị.")
        return
    
    for post in posts_data:
        print(f"Bai viet {post['index']}: {post['title']}")
        print(f"Link: {post['link']}")
        if post.get("published_at"):
            print(
                "Thoi gian: "
                f"{post['published_at'].astimezone(VN_TZ).strftime('%d/%m/%Y %H:%M')}"
            )
        print(f"Tom tat bai viet:\n{post['summary']}\n")


def main():
    """
    Hàm chính để chạy crawler tìm kiếm bài viết về bão lũ tại địa điểm được chỉ định.
    """
    driver = None
    try:
        driver = build_driver()
        
        # Nhận địa điểm từ người dùng
        location = input("Nhap dia diem ban muon tim kiem (VD: Ha Noi, Ho Chi Minh, Quang Nam): ").strip()
        
        if not location:
            logger.error("Địa điểm không được để trống")
            return
        
        start_dt, end_dt = prompt_time_range()

        # Tạo từ khóa tìm kiếm: "bão lũ" + địa điểm
        keyword = f"bão lũ {location}"
        
        # Sử dụng URL encoding đúng cách
        search_query = quote(keyword)
        search_url = f"https://timkiem.vnexpress.net/?q={search_query}"
        
        logger.info(f"Đang tìm kiếm bài viết về bão lũ tại: '{location}'")
        logger.info(f"Từ khóa tìm kiếm: '{keyword}'")
        logger.info(f"URL: {search_url}")
        
        driver.get(search_url)
        
        # Chờ trang load
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.TAG_NAME, "body"))
        )
        
        # Lấy dữ liệu bài viết
        all_posts = get_posts(driver)
        
        # Lọc chỉ giữ lại các bài viết về bão lũ
        flood_posts = filter_flood_related_posts(all_posts)

        # Tiếp tục lọc theo khoảng thời gian người dùng chọn
        filtered_posts = filter_posts_by_time_range(flood_posts, start_dt, end_dt)
        
        # Đánh số lại sau khi lọc
        for idx, post in enumerate(filtered_posts, start=1):
            post['index'] = idx
        
        # In kết quả
        print_posts(filtered_posts)
        
        logger.info(f"Hoàn thành. Tìm thấy {len(filtered_posts)} bài viết về bão lũ tại {location}.")
        
    except KeyboardInterrupt:
        logger.info("Người dùng đã dừng chương trình")
    except FileNotFoundError as e:
        logger.error(f"Lỗi: {e}")
    except Exception as exc:
        logger.error(f"Lỗi không mong đợi: {exc}", exc_info=True)
    finally:
        if driver:
            driver.quit()
            logger.info("Đã đóng WebDriver")


if __name__ == "__main__":
    main()
