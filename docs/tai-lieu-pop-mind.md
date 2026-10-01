# Tài liệu đặc tả và phát triển phần mềm POP-MIND

> Tài liệu được tổng hợp từ `AGENTS.md`, mã nguồn `app/src/main` và cấu hình Gradle hiện có trong dự án. Nội dung phản ánh trạng thái mã tại thời điểm soạn; những điểm không thể khẳng định chỉ từ mã được ghi **[cần xác nhận]**.

## 1. Tổng quan và mục tiêu

### 1.1. Mục tiêu sản phẩm

POP-MIND là ứng dụng Android hướng tới học sinh THPT, hỗ trợ tạo phiên học tập tập trung, quan sát thói quen dùng ứng dụng và xây dựng các mục tiêu thay đổi nhỏ. Ứng dụng dùng tiếng Việt, giọng điệu động viên, không phán xét. Đây là bản demo, không phải công cụ chẩn đoán y khoa; các điểm tự đánh giá chỉ được trình bày như mức độ phân tán chú ý/khả năng tập trung.

Theo cấu hình dự án, ứng dụng dùng Kotlin, Jetpack Compose và Material 3; lưu dữ liệu cục bộ bằng Room/KSP, tùy chọn cá nhân hóa bằng Preferences DataStore, và điều hướng Compose. `minSdk` là 26, `targetSdk` và `compileSdk` hiện cấu hình là 37.

### 1.2. Phạm vi chức năng hiện có

| Khu vực | Hành vi có trong mã nguồn |
|---|---|
| Tập trung | Chọn nhiệm vụ, Pomodoro hoặc đếm xuôi, thời lượng tập trung/nghỉ, âm thanh nền; mở phiên chạy trong foreground service. |
| Tiến độ | Tổng hợp phiên từ Room: chuỗi ngày, phút hôm nay/7 ngày, biểu đồ 7 ngày, trung bình gián đoạn, thử thách 21 ngày và bốn huy hiệu. |
| Lộ trình | Sinh tối đa hai mục tiêu tuần bằng luật, dựa trên phiên, hồ sơ cá nhân, UsageStats và lần tự đánh giá mới nhất; có cán cân ngày tương tác. |
| Hồ sơ | Đọc thời gian sử dụng ứng dụng nếu có quyền đặc biệt; làm bài tự đánh giá 10 câu; xem điểm nhóm; chỉnh sửa hồ sơ cá nhân; nạp dữ liệu demo hoặc xóa phiên. |
| Phiên nền | Foreground service cập nhật đồng hồ, notification, âm thanh lặp, trạng thái phiên bền vững và xử lý DND tùy quyền. |
| Gói Plus | **Chỉ là giao diện mô phỏng**, nút nâng cấp chỉ hiển thị thông báo demo; không tích hợp thanh toán. |

### 1.3. Môi trường và cấu hình build

| Thuộc tính | Giá trị đọc từ dự án |
|---|---|
| Module ứng dụng | `:app` |
| Namespace / application ID | `com.example.popmind` |
| Phiên bản ứng dụng | `1.0` (versionCode 1) |
| minSdk | 26 |
| compileSdk / targetSdk | 37 / 37 |
| Java source/target compatibility | 11 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.9.6 |
| Room | 2.8.5 |
| KSP | 2.2.10-2.0.2 |

Các phiên bản trên là cấu hình trong `gradle/libs.versions.toml` và `app/build.gradle.kts`; khả năng tương thích với Android Studio/JDK cụ thể cần đối chiếu môi trường cài đặt **[cần xác nhận]**.

## 2. Kiến trúc

### 2.1. Sơ đồ thành phần

| Thành phần | Trách nhiệm | Kết nối chính |
|---|---|---|
| `MainActivity` | Khởi tạo Room và các ViewModel, cài Compose content; xử lý intent mở từ notification. | `PopMindNavigation`, `PopMindTheme`, `FocusSessionRepository` |
| `PopMindNavigation` | NavHost, 4 tab dưới, luồng onboarding/chỉnh hồ sơ, dialog xin quyền, điều phối bắt đầu/kết thúc phiên. | Các màn hình, ViewModel, Android Settings, `FocusSessionService` |
| Compose screens | Hiển thị giao diện, thu tương tác, quan sát StateFlow bằng Compose lifecycle APIs. | Progress/Profile/Roadmap/Personalization ViewModel; state phiên singleton |
| ViewModel | Tính và cung cấp UI state; khởi chạy coroutine cho truy vấn/ghi dữ liệu. | Repository, DAO, UsageStats, Preferences DataStore |
| Repository / engine | Đóng gói truy cập dữ liệu phiên, hồ sơ; luật tạo mục tiêu lộ trình. | Room, DataStore, `UsageStatsRepository` |
| Room database | Lưu phiên, bài tự đánh giá, mục tiêu lộ trình, phiên đang hoạt động. | Bốn DAO trong `PopMindDatabase` |
| `FocusSessionService` | Đồng hồ phiên nền, notification, nhạc, DND, checkpoint, wake lock, lưu kết quả. | State singleton, Room, Android system services |
| `PopMindApplication` | Phát hiện Activity rời foreground sau khoảng chờ và gửi sự kiện cho service. | `FocusSessionService` |
| `FocusReminderScheduler` | Đặt báo thức nhắc theo hồ sơ và tự đặt lần nhắc tiếp theo sau khi phát. | `AlarmManager`, BroadcastReceiver, notification |

### 2.2. Luồng dữ liệu

- **Phiên tập trung:** người dùng chọn thông số tại màn Tập trung → Navigation yêu cầu POST_NOTIFICATIONS (Android 13+) và hỏi về DND/ghim màn hình → service bắt đầu → Room tạo bản ghi phiên và lưu bản checkpoint → service cập nhật bản ghi khi kết thúc hoặc checkpoint → StateFlow cập nhật UI → Tiến độ/Lộ trình quan sát danh sách phiên.
- **Dữ liệu tiến độ:** `ProgressViewModel` biến `Flow<List<SessionEntity>>` thành `ProgressUiState`; Compose vẽ thẻ, biểu đồ Canvas, thử thách và huy hiệu.
- **UsageStats:** `UsageStatsRepository` kiểm tra AppOps và tổng hợp 7 ngày; dữ liệu được Profile/Roadmap ViewModel đưa vào state. Quyền được cấp qua màn Cài đặt đặc biệt của Android, không phải hộp thoại runtime thông thường.
- **Hồ sơ cá nhân:** `PersonalizationStore` lưu Preferences DataStore; ViewModel phát state cho onboarding, chỉnh sửa hồ sơ và lời nhắc.
- **Lộ trình:** `RoadmapEngine` áp dụng các luật trên phiên, assessment, UsageStats và hồ sơ; DAO lưu mục tiêu tuần cùng trạng thái hoàn thành.

### 2.3. Cấu trúc mã nguồn chính

| Thư mục / tệp | Nội dung |
|---|---|
| `ui/screens/` | Màn hình Tập trung, Tiến độ, Lộ trình, Hồ sơ, phiên, tổng kết, thở, tự đánh giá, onboarding và Plus. |
| `ui/theme/` | Color scheme sáng/tối, shape và typography. Tên font Plus Jakarta Sans/Be Vietnam Pro hiện trỏ tới SansSerif hệ thống; font .ttf riêng chưa được nhúng. |
| `navigation/` | NavHost, 4 tab, luồng dialog, quyền và route phụ. |
| `ui/progress/`, `ui/profile/`, `ui/roadmap/` | ViewModel và UI state cho dữ liệu các màn tương ứng. |
| `data/local/` | Room entities, DAO, database và migration. |
| `data/`, `data/usage/` | Repository phiên, hồ sơ DataStore và UsageStats. |
| `session/`, `service/` | Trạng thái phiên, repository StateFlow và foreground service. |
| `reminder/` | AlarmManager và receiver gửi lời nhắc định kỳ. |

## 3. Đặc tả từng màn hình

### 3.1. Điều hướng và khởi động

Bốn tab chính dùng biểu tượng Bolt, Insights, AutoStories và AccountCircle tương ứng Tập trung, Tiến độ, Lộ trình, Hồ sơ. NavHost khởi động ở route `bootstrap`, chờ trạng thái khôi phục phiên và hồ sơ được nạp:

- Nếu có phiên đang hoạt động, mở thẳng màn phiên và yêu cầu service khôi phục.
- Nếu chưa hoàn tất hồ sơ, mở onboarding bốn bước.
- Nếu hồ sơ đã hoàn tất, mở Tập trung.

Ngoài bốn tab có route phụ: `session`, `summary`, `breathing`, `assessment`, `plus` và `personalization/edit`. Các route phụ không hiển thị thanh tab trong mã điều hướng hiện tại.

### 3.2. Tập trung — “Ngồi vào bàn”

Màn hình chào theo tên đã lưu và khung giờ trong ngày; có chip nhiệm vụ từ môn/mục tiêu cá nhân và nhiệm vụ gần đây. Người dùng có thể chọn thời lượng tập trung 15/25/45/60 phút hoặc nhập số phút (5–120), thời gian nghỉ 5/10 phút, bật/tắt Pomodoro, chọn Mưa/Lo-fi/Ồn trắng/Tắt và chỉnh âm lượng. Lựa chọn cài đặt được lưu bởi `FocusSettingsViewModel`.

Nút “Bắt đầu tập trung” đi qua các dialog thông báo, DND và ghim màn hình rồi mở phiên. Nút “Cứu nguy lướt video” mở màn thở ba phút. Một số con số mặc định trên UI là cấu hình lựa chọn ban đầu chứ không phải số liệu lịch sử; giá trị thời lượng ưu tiên có thể được cập nhật từ phiên hoàn thành nhiều nhất.

### 3.3. Đang tập trung / Giờ nghỉ ngắn

Màn phiên hiện nhiệm vụ, pha tập trung/nghỉ, đồng hồ lớn, chu kỳ hoàn thành, số gián đoạn và lời động viên. Nếu Pomodoro bật, service chạy thời lượng tập trung và nghỉ theo mốc elapsed realtime; nếu tắt, UI hiển thị thời gian đã trôi qua. Có nút ghi nhận “Mình vừa muốn xem điện thoại”, điều khiển nhạc/âm lượng và nút Kết thúc có xác nhận.

Service duy trì notification foreground; notification mở lại route phiên. Khi người dùng rời app trong pha tập trung, cơ chế khóa mềm hiện tại tăng gián đoạn và nhắc quay lại; pha nghỉ không tính sự kiện này. Nếu bật ghim màn hình, Activity gọi `startLockTask()` trong pha tập trung và gọi `stopLockTask()` khi nghỉ/kết thúc. Thời gian và trạng thái được checkpoint Room để khôi phục sau khi tiến trình bị hệ thống dừng.

### 3.4. Tổng kết phiên

Khi phiên hoàn thành, màn tổng kết hiển thị thời lượng và số lần gián đoạn, cùng lời khen và nút quay lại. Nếu phiên kết thúc sớm, điều hướng trở về Tập trung; mã service cập nhật phiên với `completed=false`. Không có màn tổng kết hoàn thành cho trường hợp bỏ phiên sớm trong luồng hiện tại.

### 3.5. Tiến độ

Dữ liệu đọc từ Room, gồm:

- Chuỗi ngày có phiên; nếu hôm nay chưa có phiên, chuỗi có thể neo từ hôm qua.
- Tổng phút hôm nay và bảy ngày gần nhất.
- Biểu đồ cột bảy ngày vẽ bằng Compose Canvas.
- Trung bình số gián đoạn trên tất cả phiên.
- Thử thách 21 ngày với ô ngày sáng khi có ít nhất một phiên hoàn thành.
- Huy hiệu: phiên đầu tiên, ba ngày liên tiếp, một phiên hoàn thành không gián đoạn, mười phiên hoàn thành.

Màn có dữ liệu dùng thử; nút nạp dữ liệu demo 7 ngày và xóa dữ liệu nằm trong Hồ sơ. **[cần xác nhận]** việc nút “Xóa dữ liệu” hiện gọi repository xóa toàn bộ bảng `sessions`; không thấy thao tác xóa assessment, roadmap goals hoặc active focus trong callback này.

### 3.6. Lộ trình — “Sống chậm, sống sâu”

Màn hình hiển thị Giờ vàng (giờ bắt đầu có nhiều phiên hoàn thành nhất), Giờ dễ xao nhãng (giờ có nhiều nội dung ngắn nhất nếu đọc được UsageStats), thời lượng phiên khuyến nghị và tổng kết tuần so với tuần trước. `RoadmapEngine` tạo tối đa hai mục tiêu tuần, mỗi mục tiêu có tiêu đề, lý do gắn với dữ liệu và hoạt động thay thế; người dùng đánh dấu hoàn thành và trạng thái được lưu Room.

Mục tiêu có thể dựa trên thời gian dùng nhóm app nội dung ngắn >2 giờ/ngày, trung bình gián đoạn >2/phiên, ít hơn 21 phiên/tuần, điểm nhóm học tập/giấc ngủ cao, giờ dùng nội dung ngắn, hoặc mục tiêu mặc định nhẹ nhàng. Màn có “Cán cân một ngày” với thanh trượt ngủ, học, vận động và màn hình giải trí, kèm đồ họa cân bằng Canvas. State cán cân hiện lưu trong `remember` của màn hình, không có DAO/DataStore để lưu lâu dài.

### 3.7. Hồ sơ và tự đánh giá

Hồ sơ trình bày UsageStats 7 ngày: tổng thời lượng nhóm package nội dung ngắn gồm TikTok, TikTok Lite, YouTube, Facebook và Instagram, cùng top 5 ứng dụng theo thời gian foreground. Nếu không có quyền hoặc dữ liệu, repository trả state trống; giao diện có trạng thái thân thiện và đường dẫn mở Cài đặt quyền sử dụng.

Bài “Hồ sơ tập trung” có 10 câu thang điểm 1–5, thuộc bốn nhóm: tiếp nhận nội dung số nhanh (2 câu), duy trì chú ý (3), tự kiểm soát hành vi số (2), ảnh hưởng đến học tập và giấc ngủ (3). Điểm là trung bình từng nhóm, lưu cùng câu trả lời và thời điểm vào Room; có thể làm lại. Hồ sơ hiển thị biểu đồ điểm dạng thanh và tối đa hai nhóm điểm cao nhất là vấn đề ưu tiên, kèm lưu ý không phải chẩn đoán y khoa.

Ngoài ra có onboarding/chỉnh sửa bốn bước: tên, lớp 10–12, môn/mục tiêu học (có thể tự thêm), khung giờ dễ xao nhãng và app giải trí chọn trong danh sách app có thể mở đã cài. Nút dữ liệu demo/xóa phiên và đường dẫn Gói Plus có trong Hồ sơ.

### 3.8. Onboarding và chỉnh sửa thông tin

Nếu chưa có hồ sơ hoàn chỉnh, ứng dụng mở form bốn bước, yêu cầu tên và tối thiểu một môn/mục tiêu trước khi hoàn tất. Sau lưu có thể hỏi quyền thông báo để lên lịch nhắc. Mã nguồn có `WelcomeScreen` giới thiệu ba quyền, nhưng NavHost hiện tại điều hướng lần đầu tới `PersonalizationScreen` trực tiếp; route/chỗ gọi `WelcomeScreen` không xuất hiện trong NavHost được kiểm tra. Vì vậy, việc màn chào quyền có được mở từ luồng khác hay không **[cần xác nhận]**.

### 3.9. Cứu nguy lướt video

Màn thở chạy ba phút với vòng tròn phình/xẹp theo chu kỳ 4–4–6 giây, nút thoát và câu động viên khi hoàn thành. Đây là hoạt động hỗ trợ tự điều chỉnh, không đọc hoặc khóa ứng dụng khác.

### 3.10. Gói Plus (mô phỏng)

Màn Plus so sánh Miễn phí và Plus (lộ trình nâng cao, báo cáo cho phụ huynh, nhiều âm thanh nền). Nút “Nâng cấp” chỉ hiện dialog “Đây là bản demo, chưa có thanh toán thật”. Không có billing, giao dịch, gói đăng ký hay thu phí trong mã nguồn.

## 4. Cơ sở dữ liệu

### 4.1. Cấu hình Room

`PopMindDatabase` mở file `popmind_sessions.db`, khai báo version 5, `exportSchema = false`, gồm bốn entity. Các DAO được lấy từ singleton database; khởi tạo Room không bật `fallbackToDestructiveMigration`, mà khai báo migration 1→2→3→4→5.

### 4.2. Bảng Room

| Bảng | Entity | Cột và ý nghĩa |
|---|---|---|
| `sessions` | `SessionEntity` | `id` khóa chính tự tăng; `startTime` epoch millis; `durationSeconds`; `task`; `interruptions`; `usedPomodoro`; `completed`. |
| `assessments` | `AssessmentEntity` | `id` tự tăng; `completedAt`; bốn điểm trung bình nhóm (`rapidContentScore`, `attentionScore`, `digitalControlScore`, `studySleepScore`); `answers` dạng chuỗi phân tách dấu phẩy. |
| `roadmap_goals` | `RoadmapGoalEntity` | `id` chuỗi khóa chính; `weekStart`; `title`; `reason`; `activity`; `completed`. |
| `active_focus` | `ActiveFocusEntity` | Một hàng `id=1`; nhiệm vụ, lựa chọn Pomodoro/thời lượng/pha, mốc wall-clock và elapsed realtime, gián đoạn/chu kỳ, nhạc/âm lượng, ghim, DND và sessionId. Dùng để khôi phục phiên đang chạy. |

### 4.3. DAO và vòng đời dữ liệu

- `SessionDao`: thêm phiên, quan sát tất cả phiên theo thời gian mới nhất, lấy phiên theo khoảng thời gian (Flow hoặc suspend), cập nhật kết quả, xóa dữ liệu demo, xóa tất cả phiên.
- `AssessmentDao`: thêm đánh giá, quan sát đánh giá mới nhất và toàn bộ lịch sử.
- `RoadmapGoalDao`: quan sát mục tiêu tuần, thêm mục tiêu không ghi đè, xóa mục tiêu trong tuần và cập nhật hoàn thành.
- `ActiveFocusDao`: lấy/lưu/xóa checkpoint phiên hoạt động.

Phiên được thêm khi service bắt đầu; checkpoint/kết quả cuối cập nhật cùng hàng. Khi kết thúc sớm, `completed=false`; khi hoàn tất, `completed=true`. Assessment có thể được lưu nhiều lần; DAO chỉ cung cấp quan sát chứ không có xóa. Mục tiêu tuần được lưu theo weekStart.

Hồ sơ cá nhân và thiết lập tập trung không nằm trong Room: hồ sơ dùng Preferences DataStore; các thiết lập thời lượng/âm thanh dùng `FocusSettingsViewModel` và DataStore **[cần xác nhận chính xác các khóa và giá trị mặc định trong `FocusSettingsViewModel`]**.

## 5. Quyền Android và quyền riêng tư

### 5.1. Khai báo và quyền truy cập

| Quyền / truy cập | Khai báo hoặc cách yêu cầu | Mục đích |
|---|---|---|
| `POST_NOTIFICATIONS` | Manifest; runtime từ Android 13+. Có thể từ chối và vẫn chạy phiên. | Notification phiên và lời nhắc. |
| `FOREGROUND_SERVICE` | Manifest. | Chạy đồng hồ phiên ở foreground. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Manifest; service khai `specialUse` và subtype trong manifest. | Foreground service đồng hồ do người dùng khởi chạy. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Manifest; service khai `mediaPlayback` khi phát nhạc trên API mới. | Playback âm thanh nền. |
| `WAKE_LOCK` | Manifest. | Partial wake lock trong phiên để CPU tiếp tục xử lý. |
| `PACKAGE_USAGE_STATS` | Khai báo protected permission; người dùng cấp quyền đặc biệt thủ công tại Usage Access Settings. | Đọc thời gian foreground của app 7 ngày và giờ dùng nhóm app ngắn. |
| Notification Policy Access (DND) | Không phải runtime permission; app giải thích rồi mở `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`. | Tạm đổi interruption filter trong phiên và khôi phục filter cũ khi kết thúc. App chạy nếu từ chối. |
| Ghim màn hình / Lock task | Không có quyền runtime riêng; người dùng xác nhận giao diện Android khi app gọi `startLockTask()`. | Tùy chọn giữ app ở màn hình phiên. |

Manifest có `<queries>` intent launcher và năm package nội dung ngắn, phục vụ khám phá ứng dụng đã cài và tra nhãn package. Không thấy quyền Internet được khai báo trong manifest. **[cần xác nhận]** thiết bị/nhà sản xuất có thể áp dụng thêm hạn chế nền, tiết kiệm pin hoặc chính sách notification riêng.

### 5.2. Dữ liệu và quyền riêng tư

- Dữ liệu phiên, đánh giá và mục tiêu được lưu trong cơ sở dữ liệu Room cục bộ; hồ sơ và thiết lập được lưu bằng Preferences DataStore cục bộ.
- UsageStats được truy vấn qua Android system API và được dùng để tạo số tổng hợp top app/nội dung ngắn/lộ trình. Mã nguồn đọc label/package ứng dụng để hiển thị; không có backend hay truyền dữ liệu mạng thấy trong phần mã đã rà.
- Bài tự đánh giá là thông tin cá nhân nhạy cảm về thói quen; câu trả lời và điểm lưu trên thiết bị. Màn hình ghi rõ đây không phải chẩn đoán y khoa.
- Nút xóa dữ liệu hiện thấy xóa bản ghi phiên; không xóa assessment, roadmap goals, personalization hoặc active focus theo callback. Người dùng nên hiểu phạm vi này khi dùng bản hiện tại.
- Cấu hình backup Android bật `allowBackup=true` và có file quy tắc backup/data extraction; nội dung quy tắc cụ thể cần xem xét theo yêu cầu phát hành **[cần xác nhận]**.

## 6. Hướng dẫn build và xuất APK

### 6.1. Chuẩn bị

1. Mở thư mục dự án `PopMind` bằng Android Studio tương thích với cấu hình AGP/Kotlin hiện tại.
2. Cài Android SDK có platform/API 37 và một JDK tương thích với Gradle/AGP trong dự án **[cần xác nhận phiên bản JDK tối thiểu dựa trên môi trường build thực tế]**.
3. Chờ Gradle Sync hoàn tất. Dự án khai báo repository Google và Maven Central; build lần đầu có thể cần tải các dependency đã cấu hình.

### 6.2. Build APK debug

Tại thư mục gốc dự án, dùng Gradle wrapper:

```bash
./gradlew assembleDebug
```

Trên Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

APK debug thường được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Đường dẫn đầu ra theo cấu hình Android Gradle Plugin thông dụng; hãy kiểm tra thông báo build/output của phiên bản AGP đang dùng **[cần xác nhận]**.

### 6.3. Xuất APK release có ký

Để tạo APK phát hành, cấu hình signing cho build type release bằng keystore do chủ dự án quản lý, không ghi mật khẩu/khóa riêng vào Git; sau đó chạy task `assembleRelease` trong Android Studio hoặc Gradle wrapper. Mã hiện tại không khai báo `signingConfig` release riêng; đồng thời build type release đang tắt tối ưu hóa. Vì thế APK release có thể cần cấu hình ký trước khi phân phối. Tuyệt đối không chia sẻ keystore hoặc mật khẩu ký trong tài liệu/kho mã.

### 6.4. Kiểm tra trên điện thoại

Cài APK debug trên thiết bị Android API 26 trở lên. Kiểm tra riêng các luồng phụ thuộc quyền: thông báo, Usage Access, DND Policy Access và xác nhận ghim màn hình. Tên menu có thể khác theo hãng Android. Bản demo không yêu cầu tài khoản hoặc kết nối mạng theo chức năng đã đọc.

## 7. Hạn chế và hướng phát triển

### 7.1. Hạn chế hiện tại

- UsageStats phụ thuộc quyền người dùng, độ chính xác/chi tiết do phiên bản Android và chính sách hệ thống; khi không có dữ liệu UI trống. Giờ dùng theo giờ được ước tính từ sự kiện foreground/background.
- Cán cân một ngày là state trong UI và chưa được lưu sau khi rời màn hình.
- Gói Plus là giao diện demo, không có thanh toán.
- Cài đặt nhạc tham chiếu các resource `ambient_rain`, `ambient_lofi`, `ambient_white`; có placeholder trong `res/raw` và hướng dẫn thay âm thanh. Chất lượng placeholder không đại diện âm thanh phát hành.
- Tên font Plus Jakarta Sans và Be Vietnam Pro được khai báo trong typography nhưng hiện dùng SansSerif hệ thống; font ttf chưa nhúng.
- Hành vi DND có thể khác theo thiết bị/chính sách Do Not Disturb. App chỉ khôi phục filter cũ nếu vẫn được cấp policy access lúc kết thúc.
- Bản ghi đang hoạt động dùng `SystemClock.elapsedRealtime` cùng checkpoint persisted; sau khởi động lại thiết bị, mốc elapsed realtime có thể không còn cùng hệ quy chiếu **[cần xác nhận xử lý phục hồi qua reboot trong implementation hiện tại]**.
- Chuỗi phiên đang hoạt động được lưu trong Room riêng; callback “Xóa dữ liệu” nhìn thấy hiện chỉ xóa bảng sessions.
- Không thấy bộ test tự động bao phủ các luật roadmap, phục hồi tiến trình, UsageStats và DND trong danh sách mã nguồn đã đọc **[cần xác nhận ngoài phạm vi `app/src/main`]**.

### 7.2. Hướng phát triển đề xuất

- Bổ sung màn quản lý và xóa toàn bộ dữ liệu cá nhân có xác nhận, bao gồm session, assessment, mục tiêu và hồ sơ.
- Lưu cán cân ngày và cho xem lịch sử cân bằng nếu phù hợp.
- Bổ sung kiểm thử đơn vị cho `RoadmapEngine`, tính streak/badge, chuyển pha Pomodoro và kiểm thử tích hợp lưu/khôi phục phiên.
- Rà soát phục hồi phiên sau reboot, giới hạn foreground service theo từng phiên bản Android, chính sách pin và hành vi DND trên các thiết bị mục tiêu.
- Đánh giá nội dung/giải thích tự đánh giá để giữ ngôn ngữ không chẩn đoán, dễ hiểu và minh bạch về cách tính điểm.
- Nhúng font có giấy phép phù hợp; thay placeholder âm thanh bằng file được cấp quyền sử dụng, giữ playback ngoại tuyến.
- Trước phát hành, hoàn thiện ký APK, rà backup/data extraction, thông tin quyền riêng tư và thử nghiệm người dùng trên nhiều phiên bản Android.
