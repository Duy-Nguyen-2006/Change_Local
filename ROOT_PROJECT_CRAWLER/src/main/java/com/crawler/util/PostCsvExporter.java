package com.crawler.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.crawler.model.AbstractPost;
import com.opencsv.CSVWriter;

/**
 * PostCsvExporter - CLASS CHUYÊN TRÁCH VIỆC XUẤT DỮ LIỆU
 * SRP: Chỉ có MỘT trách nhiệm - Export Posts to CSV
 * DIP: Phụ thuộc vào AbstractPost (abstraction), không phụ thuộc vào NewsPost/SocialPost cụ thể
 * OCP: KHÔNG CẦN sửa code khi thêm loại Post mới (ForumPost, BlogPost...)
 *
 * Data Model (Post) KHÔNG CÒN PHẢI LO LẮNG về việc nó được lưu vào đâu!
 */
public class PostCsvExporter {

    /**
     * Export danh sách posts sang CSV file
     * POLYMORPHISM: Có thể nhận List<NewsPost> hoặc List<SocialPost>
     * KHÔNG DÙNG instanceof - Dùng polymorphism thay thế!
     *
     * @param posts Danh sách bài viết (NewsPost hoặc SocialPost)
     * @param filePath Đường dẫn file CSV
     */
    public static void export(List<? extends AbstractPost> posts, String filePath) {
        if (posts == null || posts.isEmpty()) {
            System.out.println("Không có dữ liệu để export.");
            return;
        }

        try {
            // Tạo thư mục nếu chưa tồn tại
            java.io.File file = new java.io.File(filePath);
            java.io.File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thư mục: " + e.getMessage());
        }

        try (FileOutputStream fos = new FileOutputStream(filePath);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             CSVWriter writer = new CSVWriter(bw)) {

            // Ghi UTF-8 BOM để Excel đọc đúng tiếng Việt
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // POLYMORPHISM - Lấy Header từ Post đầu tiên
            // Post nào cũng là AbstractPost nên nó sẽ gọi đúng getCsvHeader() của nó!
            // NewsPost.getCsvHeader() → NewsPost.HEADER
            // SocialPost.getCsvHeader() → SocialPost.HEADER
            String[] header = posts.get(0).getCsvHeader(); // ✓ POLYMORPHISM!
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
     * Export với tên file tự động (dựa vào platform)
     * POLYMORPHISM: Dùng getPlatform() thay vì instanceof
     */
    public static void exportWithAutoName(List<? extends AbstractPost> posts, String keyword) {
        if (posts == null || posts.isEmpty()) {
            System.out.println("Không có dữ liệu để export.");
            return;
        }

        // POLYMORPHISM - Dùng platform thay vì instanceof
        String platform = posts.get(0).getPlatform().toLowerCase();
        String fileName = keyword.replaceAll("\\s+", "_") + "_" + platform + ".csv";

        export(posts, fileName);
    }
}
