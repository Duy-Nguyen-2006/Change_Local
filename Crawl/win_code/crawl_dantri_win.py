
from logging import *

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.keys import *
from selenium.webdriver.chrome.options import Options
from selenium import webdriver
from selenium.common.exceptions import TimeoutException, NoSuchElementException

from pathlib import *

chrome_options = Options()
logger = getLogger(__name__)

chrome_options.add_argument("--headless=new")
chrome_options.add_argument("--no-sandbox")
chrome_options.add_argument("--disable-dev-shm-usage")
chrome_options.add_argument("--disable-gpu")
chrome_options.add_argument("--window-size=1920,1080")
chrome_options.add_argument("--disable-blink-features=AutomationControlled")
chrome_options.add_argument("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
chrome_options.add_experimental_option('useAutomationExtension', False)

chromedriver_path = Path(__file__).resolve().parent.parent / "chromedriver.exe"
if not chromedriver_path.exists():
    raise FileNotFoundError(
        f"{chromedriver_path} thieu chromedriver.exe"
    )

chrome_service = Service(executable_path=str(chromedriver_path))

driver = webdriver.Chrome(service = chrome_service, options = chrome_options)

def filterPosts(posts: list[dict[str, str]]) -> list[dict[str, str]]:
    """
    Lấy danh sách các bài viết từ trang kết quả tìm kiếm Dân Trí.
    
    Tham số:
        posts: Các bài viết đầu vào
    
    Trả về:
        list[dict[str, str]]: Danh sách các bài viết, đã được lọc ra bởi keywords bên dưới
    """
    keywords = [
        "bão", "lũ", "lụt", "ngập", "mưa lớn", "thiên tai",
        "bão lũ", "lũ lụt", "ngập lụt", "mưa bão", "bão tố",
        "lũ quét", "sạt lở", "lũ ống", "bão số",
        "áp thấp", "bão nhiệt đới", "siêu bão", "bão mạnh"
    ]

    filtered_posts = []
    
    for post in posts:
        title = post.get("title", "").lower()
        summary = post.get("summary", "").lower()
        
        text_to_check = f"{title} {summary}"
        
        if any(keyword in text_to_check for keyword in keywords) and place.lower() in text_to_check:
            filtered_posts.append(post)
    
    print(f"Có {len(filtered_posts)} bài về bão lũ")
    return filtered_posts

def printPosts(posts: list[dict[str, str]]):
    """
    In ra các bài viết có được
    """
    if not posts:
        print("Danh sach trong"); return
    
    
    for i, post in enumerate(posts):
        print(f"Bai viet {i + 1}: {post['title']}")
        print(f"Link: {post['link']}")
        print(f"Tom tat bai viet:\n{post['summary']}\n")

    print(f"Danh sach gom {len(posts)} bai viet")


def getPosts(pages: int = 3, date: int = 21, timeout: int = 10) -> list[dict[str, str]]:
    """
    Lấy danh sách các bài viết từ trang kết quả tìm kiếm Dân Trí.
    
    Tham số:
        pages: Số trang cần tìm
        date: Số ngày trước ngày hôm nay (điều chỉnh phạm vi tìm kiếm)
        timeout: Thời gian chờ tối đa (giây) cho các phần tử xuất hiện
        
    Trả về:
        list[dict[str, str]]: Danh sách các bài viết, mỗi bài viết là một dict
        chứa 'title' (str), 'link' (str), và 'summary' (str)
    
    """
    global place
    place = input("Place:  "); keyword = "bão lũ " + place
    
    posts_found: list[dict[str, str]] = []

    wait = WebDriverWait(driver, timeout)

    for i in range(pages):
        try:
            driver.get("https://dantri.com.vn/tim-kiem/" + '+'.join(keyword.split()) + f".htm?date={date}&pi={i + 1}")

            wait.until(EC.presence_of_element_located((By.CLASS_NAME, "article-item")))

            posts = driver.find_elements(By.CLASS_NAME, "article-item")

            if not posts: return
            for post in posts:
                try: 
                    pbox = post.find_element(By.CLASS_NAME, "dt-text-black-mine")

                    try:
                        summary = post.find_element(By.CLASS_NAME, "article-excerpt").text.strip()
                    except NoSuchElementException:
                        summary = "Không có tóm tắt"
                        logger.error("Bài viết không có tóm tắt")

                    post_attr = {"title": pbox.text.strip(),
                                "link": pbox.get_attribute("href"),
                                "summary": summary}

                    posts_found.append(post_attr)
                    
                
                except NoSuchElementException as e:
                    logger.error(f"Không tìm thấy phần tử trong bài viết: {e}")
                    continue
                except Exception as e: print("Loi: ", e)
        
        except TimeoutException:
            logger.error("Timeout: Không tìm thấy phần tử")
        except Exception as exc:
            logger.error(f"Lỗi không mong đợi: {exc}")
    return posts_found


posts_from_DT = filterPosts(getPosts())

printPosts(posts_from_DT)

driver.quit()