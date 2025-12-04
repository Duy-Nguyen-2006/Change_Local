# ROOT_PROJECT_CRAWLER

## 📖 Mô tả

Dự án crawler thu thập dữ liệu từ các nguồn tin tức (VNExpress, Dantri) và mạng xã hội (TikTok, X/Twitter) về các sự kiện thiên tai. Dự án được thiết kế theo các nguyên tắc OOP và SOLID principles để đảm bảo tính mở rộng, bảo trì và tái sử dụng code.

## 🏗️ Cấu trúc dự án

```
ROOT_PROJECT_CRAWLER/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── crawler/
│       │           ├── app/                    # Application entry points
│       │           │   ├── Main.java           # Main application
│       │           │   └── TestRunner.java     # Test runner
│       │           ├── client/                 # Crawler clients
│       │           │   ├── abstracts/
│       │           │   │   └── CrawlerEnv.java # Abstract base class cho news crawlers
│       │           │   ├── ISearchClient.java  # Interface cho social media crawlers
│       │           │   ├── TikTokSearchClient.java
│       │           │   ├── XSearchClient.java
│       │           │   ├── VNExpressClient.java
│       │           │   ├── DantriClient.java
│       │           │   └── CrawlerException.java
│       │           ├── config/                 # Configuration management
│       │           │   └── CrawlerConfig.java  # Centralized config (API keys, limits, paths)
│       │           ├── model/                  # Data models
│       │           │   ├── AbstractPost.java   # Abstract base class cho posts
│       │           │   ├── NewsPost.java       # News post model
│       │           │   ├── SocialPost.java     # Social media post model
│       │           │   └── PostMetadata.java   # Post metadata
│       │           ├── processor/              # Data processors
│       │           │   ├── IDataProcessor.java # Processor interface
│       │           │   ├── NewsFilterProcessor.java
│       │           │   └── WebhookProcessor.java
│       │           ├── repository/             # Data persistence layer
│       │           │   ├── IPostRepository.java
│       │           │   ├── SQLitePostRepository.java
│       │           │   ├── LocalDateAdapter.java
│       │           │   └── PostTypeAdapter.java
│       │           ├── service/                # Business logic layer
│       │           │   ├── IPostService.java
│       │           │   └── PostService.java
│       │           └── util/                   # Utility classes
│       │               ├── CacheKeyFactory.java # Cache key generation
│       │               ├── PostCsvExporter.java  # CSV export utility
│       │               ├── StringUtils.java
│       │               ├── TikTokParser.java
│       │               └── XParser.java
│       └── resources/
│           └── drivers/                        # Selenium drivers (chromedriver)
├── pom.xml
├── .gitignore
├── README.md
├── FIXES_SUMMARY.md                           # Tóm tắt các cải tiến đã thực hiện
└── OOP_REVIEW_REPORT.md                       # Báo cáo đánh giá OOP
```

## 🎯 Kiến trúc và Design Patterns

### Package Structure

- **`app/`**: Entry points của ứng dụng
- **`client/`**: Crawler implementations cho các nguồn dữ liệu khác nhau
- **`config/`**: Quản lý cấu hình tập trung (API keys, limits, paths)
- **`model/`**: Data models với inheritance hierarchy
- **`processor/`**: Data processing pipeline (filtering, webhooks)
- **`repository/`**: Data persistence layer (SQLite)
- **`service/`**: Business logic layer
- **`util/`**: Utility classes và helpers

### OOP Principles

#### 1. **ENCAPSULATION (Tính đóng gói)**
- Tất cả fields trong model classes đều là `private`
- Sử dụng getter/setter với validation
- Protected fields đã được chuyển sang private với proper accessors

#### 2. **ABSTRACTION (Tính trừu tượng)**
- `ISearchClient`: Interface cho social media crawlers
- `CrawlerEnv`: Abstract class cho news crawlers
- `AbstractPost`: Abstract base class cho posts
- `IDataProcessor`: Interface cho data processors
- `IPostRepository`: Interface cho data persistence

#### 3. **POLYMORPHISM (Tính đa hình)**
- Sử dụng interface/abstract class để reference concrete implementations
- Runtime method resolution
- Ví dụ: `List<? extends AbstractPost>` có thể chứa `NewsPost` hoặc `SocialPost`

#### 4. **INHERITANCE (Tính kế thừa)**
- `VNExpressClient`, `DantriClient` extends `CrawlerEnv`
- `NewsPost`, `SocialPost` extends `AbstractPost`
- `TikTokSearchClient`, `XSearchClient` implements `ISearchClient`

### SOLID Principles

#### **Single Responsibility Principle (SRP)**
- Mỗi class có một trách nhiệm duy nhất:
  - `PostCsvExporter`: Chỉ export CSV
  - `CacheKeyFactory`: Chỉ tạo cache keys
  - `CrawlerConfig`: Chỉ quản lý config
  - `PostService`: Chỉ xử lý business logic

#### **Open/Closed Principle (OCP)**
- Mở cho mở rộng: Thêm crawler mới bằng cách implement `ISearchClient`
- Đóng cho sửa đổi: Không cần sửa code cũ khi thêm crawler mới

#### **Liskov Substitution Principle (LSP)**
- `NewsPost` và `SocialPost` có thể thay thế `AbstractPost` ở mọi nơi
- Các client implementations có thể thay thế `ISearchClient`

#### **Interface Segregation Principle (ISP)**
- Interfaces nhỏ, focused (`ISearchClient`, `IDataProcessor`, `IPostRepository`)
- Clients không phụ thuộc vào methods họ không sử dụng

#### **Dependency Inversion Principle (DIP)**
- High-level modules phụ thuộc vào abstractions
- `PostService` phụ thuộc vào `IPostRepository`, không phụ thuộc vào `SQLitePostRepository`

## 🚀 Cài đặt và Sử dụng

### Yêu cầu

- Java 17+
- Maven 3.6+
- Chrome/Chromium browser (cho Selenium)
- ChromeDriver (đặt vào `src/main/resources/drivers/`)

### Build Project

```bash
mvn clean compile
```

### Chạy ứng dụng

```bash
# Chạy main application
mvn exec:java -Dexec.mainClass="com.crawler.app.Main"

# Hoặc với Maven exec plugin
mvn exec:java
```

### Cấu hình (Configuration)

Dự án sử dụng `CrawlerConfig` class để quản lý cấu hình tập trung. Có thể override config bằng:

#### 1. Environment Variables (Ưu tiên cao nhất)

```bash
# Windows PowerShell
$env:RAPIDAPI_KEY="your-rapidapi-key"
$env:GEMINI_API_KEY="your-gemini-key"
$env:CRAWLER_OUTPUT_DIR="output"
$env:CRAWLER_MAX_PAGES="10"
$env:CRAWLER_DEFAULT_LIMIT="120"

# Linux/Mac
export RAPIDAPI_KEY="your-rapidapi-key"
export GEMINI_API_KEY="your-gemini-key"
export CRAWLER_OUTPUT_DIR="output"
export CRAWLER_MAX_PAGES="10"
export CRAWLER_DEFAULT_LIMIT="120"
```

#### 2. System Properties

```bash
java -Dcrawler.rapidapi.key="your-key" \
     -Dcrawler.gemini.api.key="your-key" \
     -Dcrawler.output.dir="output" \
     -Dcrawler.max.pages="10" \
     -Dcrawler.default.limit="120" \
     -cp target/classes com.crawler.app.Main
```

#### 3. Default Values

Nếu không set env vars hoặc system properties, sẽ dùng default values:
- `RAPIDAPI_KEY`: `""` (empty, cần set)
- `GEMINI_API_KEY`: `""` (empty, cần set)
- `CRAWLER_OUTPUT_DIR`: `"output"`
- `CRAWLER_MAX_PAGES`: `5`
- `CRAWLER_DEFAULT_LIMIT`: `120`

## 📦 Dependencies

Dự án sử dụng các thư viện sau (xem `pom.xml`):

- **OpenCSV** (5.12.0): CSV file processing
- **Selenium** (4.38.0): Web automation và scraping
- **Jsoup** (1.21.2): HTML parsing cho news crawlers
- **Gson** (2.10.1): JSON parsing cho API responses
- **SQLite JDBC** (3.46.0.0): Database storage
- **Apache HttpClient** (5.3): HTTP client cho webhook calls

## 🔑 Tính năng chính

### 1. Multi-source Crawling
- **News Sources**: VNExpress, Dantri
- **Social Media**: TikTok, X/Twitter
- Hỗ trợ date range filtering

### 2. Data Processing Pipeline
- Filtering processors
- Webhook processors
- Extensible processor architecture

### 3. Data Persistence
- SQLite database storage
- CSV export với UTF-8 BOM (Excel compatible)
- Polymorphic post handling

### 4. Configuration Management
- Centralized config class
- Environment variable support
- System property override
- Default values fallback

## ✅ Các cải tiến đã thực hiện

Xem chi tiết trong [FIXES_SUMMARY.md](FIXES_SUMMARY.md)

1. ✅ **Bảo mật**: API keys không còn hardcoded, sử dụng environment variables
2. ✅ **Maintainability**: Magic numbers được tập trung vào `CrawlerConfig`
3. ✅ **Type Safety**: Loại bỏ unsafe type casts
4. ✅ **Encapsulation**: Protected fields được đóng gói tốt hơn
5. ✅ **Portability**: File paths không còn hardcoded
6. ✅ **Contract Compliance**: Date filtering được implement đúng
7. ✅ **SRP**: Tách CSV logic ra `PostCsvExporter` class
8. ✅ **Utility Classes**: Tạo `CacheKeyFactory` cho cache key generation

## 📝 Lưu ý

1. **ChromeDriver**: Đặt `chromedriver.exe` (Windows) hoặc `chromedriver` (Linux/Mac) vào `src/main/resources/drivers/`

2. **API Keys**: Cần set `RAPIDAPI_KEY` và `GEMINI_API_KEY` trước khi chạy:
   ```bash
   $env:RAPIDAPI_KEY="your-key"
   $env:GEMINI_API_KEY="your-key"
   ```

3. **Database**: SQLite database sẽ được tạo tự động ở `disaster_post_data.db`

4. **Output**: CSV files sẽ được export vào thư mục được cấu hình trong `CrawlerConfig` (mặc định: `output/`)

## 🧪 Testing

```bash
# Chạy test runner
mvn exec:java -Dexec.mainClass="com.crawler.app.TestRunner"
```

## 📚 Tài liệu tham khảo

- [OOP_REVIEW_REPORT.md](OOP_REVIEW_REPORT.md): Báo cáo đánh giá OOP chi tiết
- [FIXES_SUMMARY.md](FIXES_SUMMARY.md): Tóm tắt các cải tiến đã thực hiện

## 👥 Đóng góp

Dự án tuân thủ các nguyên tắc OOP và SOLID. Khi thêm tính năng mới:
- Implement interfaces thay vì sửa code cũ (OCP)
- Mỗi class chỉ có một trách nhiệm (SRP)
- Sử dụng abstractions thay vì concrete classes (DIP)

## 📄 License

[Thêm license nếu có]
