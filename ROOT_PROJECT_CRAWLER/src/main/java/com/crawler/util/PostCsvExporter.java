package com.crawler.util;

import com.crawler.model.AbstractPost;
import com.crawler.model.NewsPost;
import com.crawler.model.SocialPost;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * PostCsvExporter - CLASS CHUYÊN TRÁCH VIỆC XUẤT DỮ LIỆU
 * SRP: Chỉ có MỘT trách nhiệm - Export Posts to CSV
 * DIP: Phụ thuộc vào AbstractPost (abstraction), không phụ thuộc vào NewsPost/SocialPost cụ thể
 *
 * Data Model (Post) KHÔNG CÒN PHẢI LO LẮNG về việc nó được lưu vào đâu!
 */
public class PostCsvExporter {

    /**
     * Export danh sách posts sang CSV file
     * Sử dụng POLYMORPHISM: Có thể nhận List<NewsPost> hoặc List<SocialPost>
     *
     * @param posts Danh sách bài viết (NewsPost hoặc SocialPost)
     * @param filePath Đường dẫn file CSV
     */
    public static void export(List<? extends AbstractPost> posts, String filePath) {
        if (posts == null || posts.isEmpty()) {
            System.out.println("Không có dữ liệu để export.");
            return;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {

            // Xác định header dựa vào loại Post (POLYMORPHISM)
            String[] header = determineHeader(posts.get(0));
            writer.writeNext(header);

            // Viết từng dòng dữ liệu
            for (AbstractPost post : posts) {
                String[] row = post.toCsvArray();
                writer.writeNext(row);
            }

            System.out.println("Đã export " + posts.size() + " posts vào " + filePath);

        } catch (IOException e) {
            System.err.println("Lỗi khi viết file CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Xác định CSV header dựa vào loại Post
     * POLYMORPHISM: Runtime type checking
     */
    private static String[] determineHeader(AbstractPost post) {
        if (post instanceof NewsPost) {
            return NewsPost.HEADER;
        } else if (post instanceof SocialPost) {
            return SocialPost.HEADER;
        } else {
            // Default header
            return new String[]{"date", "content", "platform", "engagement_score"};
        }
    }

    /**
     * Export với tên file tự động (dựa vào loại Post)
     */
    public static void exportWithAutoName(List<? extends AbstractPost> posts, String keyword) {
        if (posts == null || posts.isEmpty()) {
            System.out.println("Không có dữ liệu để export.");
            return;
        }

        // Tạo tên file dựa vào loại Post và keyword
        String type = posts.get(0) instanceof NewsPost ? "news" : "social";
        String fileName = keyword.replaceAll("\\s+", "_") + "_" + type + ".csv";

        export(posts, fileName);
    }
}
