# POP-MIND – App Android hỗ trợ học sinh THPT tập trung

## Mục tiêu

App giúp học sinh THPT tăng khả năng tập trung và giảm tiếp xúc nội dung số ngắn
(TikTok, YouTube Shorts, Facebook Reels). Đây là bản DEMO làm trong 24 giờ.
Không phải công cụ chẩn đoán y khoa; không dùng từ "bệnh", "mắc hội chứng".
Dùng từ: "mức độ phân tán chú ý", "khả năng tập trung".

## Công nghệ (bắt buộc)

- Kotlin, Jetpack Compose, Material 3
- Room (lưu dữ liệu cục bộ, dùng KSP), ViewModel + StateFlow
- Navigation Compose, thanh điều hướng dưới 4 tab
- minSdk 26. KHÔNG nâng cấp phiên bản AGP/Kotlin/Gradle đang có trong project.
- Không dùng thư viện nặng hay cần API key. Không cần internet.

## Đối tượng và phong cách

Học sinh THPT. Giao diện sáng, tươi, bo góc lớn, chữ to, tiếng Việt toàn bộ,
giọng văn thân thiện, trẻ trung, không giáo điều. Có hỗ trợ dark mode.
Màu chính: xanh ngọc (teal) + điểm nhấn cam ấm.

## Cấu trúc 4 tab

1. Tập trung (màn chính "Ngồi vào bàn")
2. Tiến độ
3. Lộ trình (sống chậm, sống sâu)
4. Hồ sơ

## Tính năng cốt lõi

- Bắt đầu phiên: chọn nhiệm vụ, bật/tắt Pomodoro, bật/tắt nhạc, rồi tự bật
  chế độ Không làm phiền (DND) khi phiên bắt đầu, tắt khi kết thúc.
- Mỗi phiên tự động lưu vào Room: thời điểm, thời lượng, nhiệm vụ, số lần gián đoạn.
- Đọc dữ liệu dùng app qua UsageStatsManager, nhóm nội dung ngắn: TikTok, YouTube,
  Facebook, Instagram.
- Lộ trình cá nhân hóa bằng luật (rule-based), không dùng AI.
- Tính năng trả phí ("Gói Plus") chỉ là giao diện mô phỏng, không có thanh toán thật.

## Quy tắc code

- Mỗi lần chỉ làm đúng phạm vi của prompt; không tự thêm tính năng ngoài yêu cầu.
- Code có comment tiếng Việt ngắn ở chỗ quan trọng.
- Sau khi sửa phải đảm bảo `./gradlew assembleDebug` chạy thành công.
- Cuối mỗi lần làm, liệt kê: file đã tạo/sửa, quyền cần cấp, cách tự kiểm tra trên điện thoại.
