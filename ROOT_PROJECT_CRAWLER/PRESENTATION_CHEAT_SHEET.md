# 📝 CHEAT SHEET - THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN

> **Bản tóm tắt ngắn gọn - In ra và xem khi thuyết trình**

---

## 🎯 TOP 10 ĐIỂM MẠNH CẦN NHẤN MẠNH

### 1. **Unified Polymorphism** ⭐⭐⭐
```
"Tất cả crawlers đều implement ISearchClient → chỉ cần 1 vòng lặp xử lý tất cả"
Code: Main.java - demoUnifiedPolymorphism()
```

### 2. **Layered Architecture** ⭐⭐⭐
```
"7 layers rõ ràng: app → service → repository → client → processor → model → util"
Lợi ích: Separation of Concerns, dễ test, dễ maintain
```

### 3. **Repository Pattern** ⭐⭐
```
"IPostRepository interface → có thể đổi SQLite → MySQL mà không sửa service"
Code: IPostRepository.java, SQLitePostRepository.java
```

### 4. **Chain of Responsibility** ⭐⭐
```
"Processor pipeline: Filter → Enrich → Validate → ..."
Code: IDataProcessor.java, PostService.applyProcessors()
```

### 5. **SOLID Principles** ⭐⭐⭐
```
- SRP: Mỗi class 1 trách nhiệm (PostCsvExporter, CacheKeyFactory)
- OCP: Thêm crawler mới không sửa code cũ
- LSP: Tất cả crawler thay thế được cho nhau
- DIP: Phụ thuộc interface, không phụ thuộc concrete class
```

### 6. **Generics & Type Safety** ⭐⭐
```
"List<? extends AbstractPost> → Type-safe, không cần instanceof"
Code: ISearchClient.search(), IPostRepository.save()
```

### 7. **Centralized Configuration** ⭐
```
"CrawlerConfig với 3-level priority: Env Var → System Prop → Default"
Lợi ích: Security (không hardcode API keys), flexibility
```

### 8. **Caching Strategy** ⭐⭐
```
"Cache → Crawl → Process → Save workflow"
Lợi ích: Performance, cost saving (giảm API calls)
Code: PostService.getPosts()
```

### 9. **Template Method Pattern** ⭐
```
"CrawlerEnv abstract class → định nghĩa workflow chung"
Code: CrawlerEnv.search() → getPosts() (abstract)
```

### 10. **Defensive Programming** ⭐
```
"Unmodifiable collections, null safety, validation trong setter"
Code: CrawlerEnv.getResults(), AbstractPost constructor
```

---

## 🗣️ CÂU NÓI MỞ ĐẦU

> "Thưa thầy, dự án của em là một **multi-source crawler** thu thập dữ liệu về thiên tai từ 4 nguồn: TikTok, X/Twitter, VNExpress, và Dantri. 
> 
> **Điểm mạnh của dự án** là em đã áp dụng đầy đủ các nguyên tắc **OOP và SOLID**, cùng với các **Design Patterns** phù hợp để đảm bảo code dễ maintain, dễ test, và dễ mở rộng.
> 
> Em xin phép trình bày các điểm nổi bật..."

---

## 📋 CẤU TRÚC TRÌNH BÀY (5-7 phút)

### **Phần 1: Architecture (1 phút)**
- Layered Architecture với 7 layers
- Package naming convention
- Separation of Concerns

### **Phần 2: OOP & Design Patterns (2-3 phút)**
- Unified Polymorphism (ISearchClient)
- Repository Pattern
- Chain of Responsibility (Processor Pipeline)
- Template Method Pattern (CrawlerEnv)

### **Phần 3: SOLID Principles (1-2 phút)**
- SRP: Mỗi class 1 trách nhiệm
- OCP: Mở cho mở rộng, đóng cho sửa đổi
- LSP: Tất cả implementations thay thế được
- DIP: Phụ thuộc abstraction

### **Phần 4: Advanced Features (1 phút)**
- Generics với bounded wildcards
- Centralized Configuration
- Caching Strategy

---

## 💬 CÁC CÂU HỎI THƯỜNG GẶP & CÁCH TRẢ LỜI

### ❓ "Tại sao dùng interface thay vì abstract class?"
**Trả lời:**
> "Em dùng interface `ISearchClient` vì:
> - Java chỉ cho phép single inheritance → nếu dùng abstract class, các crawler không thể kế thừa class khác
> - Interface cho phép multiple implementation → linh hoạt hơn
> - Interface là contract rõ ràng hơn → enforce implementation"

### ❓ "Tại sao tách CSV logic ra PostCsvExporter?"
**Trả lời:**
> "Theo nguyên tắc **Single Responsibility Principle**:
> - `AbstractPost` chỉ nên lo về data model
> - `PostCsvExporter` chỉ lo về export logic
> - Tách biệt giúp dễ test, dễ maintain, dễ reuse"

### ❓ "Caching hoạt động như thế nào?"
**Trả lời:**
> "Em implement caching với 3 bước:
> 1. Tạo cache key từ keyword + date range (CacheKeyFactory)
> 2. Check cache trong Repository
> 3. Nếu miss → crawl → process → save cache
> 
> Lợi ích: Giảm API calls, tăng performance"

### ❓ "Làm sao đảm bảo type safety?"
**Trả lời:**
> "Em dùng Generics với bounded wildcards:
> - `List<? extends AbstractPost>` → chấp nhận AbstractPost và mọi subclass
> - `IDataProcessor<? super AbstractPost>` → chấp nhận AbstractPost và superclass
> 
> Compiler kiểm tra type tại compile-time → không cần instanceof"

---

## 🎯 DEMO CODE NÊN SHOW

### 1. **Unified Polymorphism** (Main.java)
```java
List<ISearchClient> allCrawlers = new ArrayList<>();
allCrawlers.add(new TikTokSearchClient());
allCrawlers.add(new VNExpressClient());

for (ISearchClient crawler : allCrawlers) {
    List<? extends AbstractPost> results = crawler.search(...);
}
```

### 2. **Repository Pattern** (PostService.java)
```java
private final IPostRepository repository;  // ← Interface
private final ISearchClient crawler;        // ← Interface
```

### 3. **Processor Pipeline** (PostService.java)
```java
List<IDataProcessor<? super AbstractPost>> processors;
// Chain: Filter → Enrich → Validate
```

### 4. **Configuration** (CrawlerConfig.java)
```java
public static String getRapidApiKey() {
    return getConfig("RAPIDAPI_KEY", "crawler.rapidapi.key", defaultValue);
}
```

---

## ✅ CHECKLIST TRƯỚC KHI TRÌNH BÀY

- [ ] Đã đọc kỹ SELLING_POINTS.md
- [ ] Đã chuẩn bị demo code (mở IDE sẵn)
- [ ] Đã test chạy được project
- [ ] Đã chuẩn bị trả lời các câu hỏi thường gặp
- [ ] Đã in cheat sheet này ra
- [ ] Đã tập nói trước gương

---

## 🎤 TIPS TRÌNH BÀY

1. **Tự tin**: Nói rõ ràng, không nói quá nhanh
2. **Show code**: Mở IDE và show code thật, không chỉ nói suông
3. **Giải thích "tại sao"**: Không chỉ nói "làm gì", mà nói "tại sao làm như vậy"
4. **So sánh**: So sánh với cách làm thông thường để highlight điểm mạnh
5. **Lợi ích thực tế**: Nói về lợi ích cụ thể (dễ test, dễ maintain, dễ mở rộng)

---

## 📊 SƠ ĐỒ KIẾN TRÚC (Vẽ trên bảng)

```
┌─────────────┐
│   Main.java │  ← Application Layer
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ PostService │  ← Service Layer (Orchestration)
└──────┬──────┘
       │
   ┌───┴───┬──────────┬──────────┐
   ▼       ▼          ▼          ▼
┌──────┐ ┌──────┐ ┌─────────┐ ┌──────────┐
│ Repo │ │Client│ │Processor│ │  Model   │
└──────┘ └──────┘ └─────────┘ └──────────┘
```

---

**Chúc bạn thuyết trình thành công! 🚀**

