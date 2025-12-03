# ROOT_PROJECT_CRAWLER

## 🏗️ Cấu trúc dự án đã được TÁI CẤU TRÚC

Dự án này đã được tái cấu trúc hoàn toàn để tuân thủ các nguyên tắc OOP và SOLID principles.

### Cấu trúc thư mục

```
ROOT_PROJECT_CRAWLER/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── crawler/
│       │           ├── model/          # Chứa các class dữ liệu
│       │           │   └── Post.java
│       │           ├── client/         # Chứa interface và implementations
│       │           │   ├── ISearchClient.java
│       │           │   ├── TikTokSearchClient.java
│       │           │   ├── XSearchClient.java
│       │           │   ├── VNExpressClient.java
│       │           │   └── DantriClient.java
│       │           ├── util/           # Chứa các class hỗ trợ
│       │           │   ├── CSVFormat.java
│       │           │   ├── TikTokParser.java
│       │           │   ├── XParser.java
│       │           │   ├── CrawlerEnv.java
│       │           │   ├── CrawlerOptions.java
│       │           │   └── SocialDatabase.java
│       │           └── app/            # Chứa hàm main
│       │               └── Main.java
│       └── resources/
│           └── drivers/                # Chứa chromedriver
├── pom.xml
├── .gitignore
└── README.md
```

## 🎯 Các nguyên tắc OOP đã áp dụng

### 1. ENCAPSULATION (Tính đóng gói)
- **Post.java**: Tất cả fields đã được chuyển từ `public` sang `private`
- Cung cấp getter/setter để kiểm soát truy cập
- Validation trong setter để đảm bảo dữ liệu hợp lệ

### 2. ABSTRACTION (Tính trừu tượng)
- **ISearchClient**: Interface định nghĩa contract cho tất cả social media crawlers
- **CrawlerEnv**: Abstract class cho news crawlers
- Client code chỉ cần biết interface, không cần biết implementation

### 3. POLYMORPHISM (Tính đa hình)
- Sử dụng interface/abstract class để reference các concrete classes
- Late binding: Method được gọi phụ thuộc vào kiểu thực tế tại runtime
- Ví dụ: `ISearchClient client = new TikTokSearchClient();`

### 4. INHERITANCE (Tính kế thừa)
- VNExpressClient và DantriClient kế thừa CrawlerEnv
- TikTokSearchClient và XSearchClient implements ISearchClient

## 📋 SOLID Principles

### Single Responsibility Principle (SRP)
- Mỗi package có một trách nhiệm rõ ràng:
  - `model`: Chỉ chứa data classes
  - `client`: Chỉ chứa crawler implementations
  - `util`: Chỉ chứa utility classes
  - `app`: Chỉ chứa application logic

### Open/Closed Principle (OCP)
- Mở cho mở rộng: Có thể thêm crawler mới bằng cách implement ISearchClient
- Đóng cho sửa đổi: Không cần sửa code cũ khi thêm crawler mới

### Dependency Inversion Principle (DIP)
- High-level modules (Main) phụ thuộc vào abstractions (ISearchClient)
- Không phụ thuộc vào concrete classes (TikTokSearchClient, XSearchClient)

## 🚀 Cách sử dụng

### Chạy ứng dụng demo
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.crawler.app.Main"
```

### Chạy TikTok crawler riêng lẻ
```bash
mvn exec:java -Dexec.mainClass="com.crawler.client.TikTokSearchClient" -Dexec.args="bão lũ,lũ lụt"
```

### Chạy X crawler riêng lẻ
```bash
mvn exec:java -Dexec.mainClass="com.crawler.client.XSearchClient" -Dexec.args="bão yagi"
```

## 📦 Dependencies

- **Jsoup**: HTML parsing cho news crawlers
- **Selenium**: Web automation
- **OpenCSV**: CSV file processing
- **Gson**: JSON parsing cho API responses
- **SQLite JDBC**: Database storage

## 🔑 Key Improvements

1. ✅ **Consolidated**: 3 Maven modules → 1 Maven project
2. ✅ **Encapsulation**: All fields in Post.java are now private
3. ✅ **Abstraction**: ISearchClient interface for social media crawlers
4. ✅ **Polymorphism**: Main.java demonstrates polymorphic usage
5. ✅ **SRP**: Clear package structure with single responsibilities
6. ✅ **OCP**: Easy to extend with new crawlers without modifying existing code
7. ✅ **DIP**: Depends on abstractions, not concretions

## 📝 Lưu ý

- Đặt `chromedriver.exe` vào thư mục `src/main/resources/drivers/`
- Cấu hình API keys trong các client classes nếu cần
- Database SQLite sẽ được tạo tự động ở `disaster_post_data.db`
