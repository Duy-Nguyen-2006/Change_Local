# 🕷️ Java Web Crawler - Ứng Dụng Thu Thập Dữ Liệu Từ Nhiều Nguồn

## 📋 Mô Tả Dự Án

Dự án này là một ứng dụng **web crawler đa nguồn** được xây dựng bằng Java, áp dụng đầy đủ các nguyên tắc **OOP** và **SOLID** để thu thập dữ liệu từ:
- 📰 **Báo chí**: VNExpress, Dân Trí
- 📱 **Mạng xã hội**: TikTok, X (Twitter)

Ứng dụng thu thập dữ liệu về các chủ đề thiên tai (bão lũ, sạt lở, ngập lụt...), xử lý và lưu trữ vào database SQLite, đồng thời xuất ra file CSV với mã hóa UTF-8.

## 🎯 Các Tính Năng Chính

### ✨ Chức Năng
- ✅ Thu thập dữ liệu từ 4 nguồn khác nhau (VNExpress, Dân Trí, TikTok, X)
- ✅ Lọc dữ liệu theo từ khóa và khoảng thời gian
- ✅ Làm giàu dữ liệu với metadata AI (sentiment, location, focus, damage category, rescue goods)
- ✅ Cache thông minh (tránh crawl lại dữ liệu đã có)
- ✅ Lưu trữ vào SQLite database
- ✅ Xuất CSV với UTF-8 BOM (hiển thị đúng tiếng Việt trong Excel)
- ✅ Dữ liệu engagement ngẫu nhiên (1-100) cho demo

### 🏗️ Kiến Trúc & Design Patterns

**Nguyên tắc OOP được áp dụng:**
1. **Encapsulation** - Tất cả fields đều private với getter/setter
2. **Inheritance** - `AbstractPost` → `NewsPost` / `SocialPost`
3. **Polymorphism** - Tất cả crawler implement `ISearchClient`
4. **Abstraction** - Sử dụng interface thay vì concrete class

**SOLID Principles:**
- **SRP** (Single Responsibility) - Mỗi class có một trách nhiệm duy nhất
- **OCP** (Open/Closed) - Mở cho mở rộng, đóng cho sửa đổi
- **LSP** (Liskov Substitution) - Tất cả crawler có thể thay thế cho nhau
- **ISP** (Interface Segregation) - Interface nhỏ gọn, tập trung
- **DIP** (Dependency Inversion) - Phụ thuộc vào abstraction

**Design Patterns:**
- Strategy Pattern (ISearchClient implementations)
- Template Method (CrawlerEnv abstract class)
- Dependency Injection (Constructor injection)
- Factory Pattern (Config management)
- Repository Pattern (Data access layer)

## 📁 Cấu Trúc Thư Mục

```
ROOT_PROJECT_CRAWLER/
├── src/main/java/com/crawler/
│   ├── app/                    # Application layer
│   │   ├── Main.java          # Entry point - Demo polymorphism
│   │   └── TestRunner.java    # Test runner với processor pipeline
│   ├── client/                 # Crawler layer (Data Source)
│   │   ├── ISearchClient.java      # Interface chung cho tất cả crawler
│   │   ├── CrawlerEnv.java         # Abstract base cho news crawlers
│   │   ├── VNExpressClient.java    # VNExpress crawler
│   │   ├── DantriClient.java       # Dân Trí crawler
│   │   ├── TikTokSearchClient.java # TikTok crawler
│   │   └── XSearchClient.java      # X (Twitter) crawler
│   ├── config/                 # Configuration layer
│   │   └── CrawlerConfig.java      # Centralized config management
│   ├── model/                  # Data models
│   │   ├── AbstractPost.java       # Base class cho tất cả posts
│   │   ├── NewsPost.java           # Model cho bài báo
│   │   └── SocialPost.java         # Model cho social media post
│   ├── processor/              # Data processing layer
│   │   ├── IDataProcessor.java         # Interface cho processors
│   │   ├── NewsFilterProcessor.java    # Lọc NewsPost theo ngày & keyword
│   │   └── WebhookProcessor.java       # Làm giàu dữ liệu với AI metadata
│   ├── repository/             # Data access layer
│   │   ├── IPostRepository.java        # Repository interface
│   │   ├── SQLitePostRepository.java   # SQLite implementation
│   │   ├── PostTypeAdapter.java        # Gson adapter cho AbstractPost
│   │   └── LocalDateAdapter.java       # Gson adapter cho LocalDate
│   ├── service/                # Business logic layer
│   │   ├── IPostService.java       # Service interface
│   │   └── PostService.java        # Service với caching logic
│   └── util/                   # Utilities
│       ├── PostCsvExporter.java    # CSV export với UTF-8 BOM
│       ├── StringUtils.java        # String utilities (parseKeywords)
│       ├── TikTokParser.java       # Parse TikTok JSON response
│       └── XParser.java            # Parse X (Twitter) JSON response
├── pom.xml                     # Maven dependencies
└── README.md                   # Documentation
```

## 🛠️ Yêu Cầu Hệ Thống

### Phần Mềm Cần Cài Đặt
1. **Java Development Kit (JDK) 17 hoặc cao hơn**
   - Download: https://adoptium.net/
   - Kiểm tra: `java -version`

2. **Apache Maven 3.6+**
   - Download: https://maven.apache.org/download.cgi
   - Kiểm tra: `mvn -version`

3. **Git** (optional, để clone project)
   - Download: https://git-scm.com/

### Thư Viện Dependencies (được Maven tự động tải)
- `opencsv 5.12.0` - CSV processing
- `selenium-java 4.38.0` - Web automation
- `jsoup 1.21.2` - HTML parsing
- `gson 2.10.1` - JSON parsing
- `sqlite-jdbc 3.46.0.0` - SQLite database
- `httpclient5 5.3` - HTTP client

## 🚀 Hướng Dẫn Chạy Dự Án

### Cách 1: Sử dụng Maven Exec Plugin (Khuyến nghị)

```powershell
# Di chuyển vào thư mục project
cd D:\OOP_Local_Change\ROOT_PROJECT_CRAWLER

# Biên dịch project
mvn clean compile

# Chạy Main.java (Demo tất cả crawler)
mvn exec:java "-Dexec.mainClass=com.crawler.app.Main"

# HOẶC chạy TestRunner.java (Demo với processor pipeline)
mvn exec:java "-Dexec.mainClass=com.crawler.app.TestRunner"
```

### Cách 2: Build JAR và Chạy

```powershell
# Build JAR file
mvn clean package

# Chạy JAR
java -cp target/crawler-1.0-SNAPSHOT.jar com.crawler.app.Main
```

### Cách 3: Chạy Từ IDE (IntelliJ IDEA / Eclipse)

1. Import project vào IDE (File → Open → chọn thư mục project)
2. Đợi Maven tải dependencies
3. Right-click vào `Main.java` → Run 'Main.main()'

## 📊 Kết Quả Đầu Ra

### 1. Console Output
Ứng dụng sẽ in ra màn hình:
- Tiến trình crawl từ từng nguồn
- Số lượng bài viết thu thập được
- Mẫu dữ liệu (2 bài đầu tiên từ mỗi nguồn)
- Đường dẫn file CSV output

### 2. CSV File
**File output:** `D:\OOP_Local_Change\ROOT_PROJECT_CRAWLER\AllClients_results_utf8.csv`

**Cột dữ liệu (12 cột):**
1. `platform` - Nguồn (vnexpress, dantri, tiktok, x)
2. `title` - Tiêu đề bài viết
3. `content` - Nội dung
4. `url` - Link gốc
5. `date` - Ngày đăng
6. `engagement` - Điểm tương tác (comments hoặc reactions)
7. `sentiment` - Cảm xúc (positive, negative, neutral)
8. `location` - Địa điểm
9. `focus` - Trọng tâm (damage, rescue, none)
10. `direction` - Hướng xử lý (urgent, plan, info)
11. `damage_category` - Loại thiệt hại (nếu focus=damage)
12. `rescue_goods` - Hàng cứu trợ (nếu focus=rescue)

**Encoding:** UTF-8 với BOM để Excel hiển thị đúng tiếng Việt

### 3. SQLite Database
**File:** `posts.db` (tự động tạo)

Chứa 2 bảng:
- `news_posts` - Dữ liệu từ báo chí
- `social_posts` - Dữ liệu từ mạng xã hội

## 🔧 Cấu Hình (Configuration)

Ứng dụng hỗ trợ cấu hình thông qua:

### 1. Environment Variables (Ưu tiên cao nhất)
```powershell
# Thiết lập API keys
$env:RAPIDAPI_KEY = "your_rapidapi_key_here"
$env:GEMINI_API_KEY = "your_gemini_key_here"

# Thiết lập output directory
$env:CRAWLER_OUTPUT_DIR = "D:\custom_output"

# Chạy ứng dụng
mvn exec:java "-Dexec.mainClass=com.crawler.app.Main"
```

### 2. System Properties
```powershell
mvn exec:java "-Dexec.mainClass=com.crawler.app.Main" `
  "-Dcrawler.output.dir=D:\custom_output" `
  "-Dcrawler.default.limit=200"
```

### 3. Default Values (Hardcoded)
Nếu không set, sẽ dùng giá trị mặc định trong `CrawlerConfig.java`

## 🧪 Testing & Debugging

### Kiểm Tra Compilation Errors
```powershell
mvn clean compile
```

### Chạy Với Debug Logging
```powershell
mvn -X exec:java "-Dexec.mainClass=com.crawler.app.Main"
```

### Test Riêng Từng Crawler
Sửa `Main.java` để chỉ chạy crawler cần test:
```java
List<ISearchClient> allCrawlers = new ArrayList<>();
allCrawlers.add(new VNExpressClient());  // Chỉ test VNExpress
```

## 🐛 Xử Lý Lỗi Thường Gặp

### Lỗi 1: `mvn: command not found`
**Nguyên nhân:** Maven chưa được cài đặt hoặc chưa add vào PATH

**Giải pháp:**
1. Download Maven từ https://maven.apache.org/download.cgi
2. Extract và add thư mục `bin` vào PATH
3. Restart PowerShell

### Lỗi 2: `java.lang.UnsupportedClassVersionError`
**Nguyên nhân:** JDK version < 17

**Giải pháp:**
1. Download JDK 17+: https://adoptium.net/
2. Set JAVA_HOME: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"`
3. Kiểm tra: `java -version`

### Lỗi 3: Tiếng Việt bị lỗi font trong Excel
**Nguyên nhân:** Excel không nhận diện UTF-8

**Giải pháp:**
- File đã có UTF-8 BOM, mở trực tiếp bằng Excel sẽ OK
- Nếu vẫn lỗi: Excel → Data → Get Data → From Text/CSV → chọn UTF-8

### Lỗi 4: API rate limit exceeded
**Nguyên nhân:** Gọi API quá nhiều lần

**Giải pháp:**
- Giảm `DEFAULT_LIMIT` trong các Client class
- Hoặc đợi vài phút rồi thử lại

### Lỗi 5: Compilation error về generics
**Nguyên nhân:** Type mismatch giữa `NewsPost` và `AbstractPost`

**Giải pháp:** Đã được fix trong `TestRunner.java` - chỉ dùng `WebhookProcessor` trong pipeline

## 📚 Kiến Thức Liên Quan

### OOP Concepts Demonstrated
1. **Encapsulation:** Private fields, public getters/setters
2. **Inheritance:** `AbstractPost` → `NewsPost`/`SocialPost`
3. **Polymorphism:** `ISearchClient` interface với nhiều implementations
4. **Abstraction:** Abstract methods, interfaces

### SOLID Principles Applied
- **S** - Mỗi class có một nhiệm vụ duy nhất
- **O** - Extend qua inheritance/interface, không modify code cũ
- **L** - Tất cả crawler có thể thay thế cho nhau
- **I** - Interface nhỏ gọn, không ép client implement thừa
- **D** - Depend on abstraction (ISearchClient), not concrete

### Design Patterns Used
- **Strategy:** Different crawling strategies for different sources
- **Template Method:** `CrawlerEnv` defines skeleton, subclass fills in
- **Dependency Injection:** Constructor injection in `PostService`
- **Repository:** Abstraction layer for data access
- **Factory:** `CrawlerConfig` for configuration management

## 📝 Ghi Chú Quan Trọng

1. **API Keys:** Dự án sử dụng free tier API, có thể bị rate limit
2. **Internet Required:** Cần kết nối internet để crawl dữ liệu
3. **UTF-8 BOM:** File CSV có BOM để Excel hiển thị đúng tiếng Việt
4. **Random Data:** Engagement metrics được random (1-100) cho demo
5. **Caching:** Dữ liệu đã crawl sẽ được cache trong SQLite

## 👨‍💻 Tác Giả & Đóng Góp

**Mục đích:** Dự án học tập về OOP và SOLID principles trong Java

**Đóng góp:** Mọi đóng góp đều được chào đón! Tạo Pull Request hoặc Issue trên GitHub.

## 📄 License

Dự án này được phát triển cho mục đích học tập và demo. Không dùng cho mục đích thương mại.

---

## 🎓 Tổng Kết

Dự án này minh họa cách áp dụng **đầy đủ các nguyên tắc OOP và SOLID** trong một ứng dụng thực tế:
- ✅ Clean Architecture với phân tầng rõ ràng
- ✅ Dependency Injection cho testability
- ✅ Interface-based programming cho flexibility
- ✅ Proper error handling và logging
- ✅ Configuration management
- ✅ Data persistence với SQLite
- ✅ CSV export với proper encoding

**Happy Coding! 🚀**
