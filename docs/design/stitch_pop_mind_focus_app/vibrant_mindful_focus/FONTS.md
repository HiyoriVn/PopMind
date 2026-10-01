# Phông chữ cho POP-MIND

Repo hiện chưa có tệp phông Plus Jakarta Sans hoặc Be Vietnam Pro. Code đã khai báo hai họ phông và đang dùng Sans Serif của Android làm dự phòng để app vẫn build được.

Để khớp bản thiết kế, tải các tệp TTF tĩnh có hỗ trợ tiếng Việt và đặt tại:

- `app/src/main/res/font/plus_jakarta_sans_regular.ttf`
- `app/src/main/res/font/plus_jakarta_sans_semibold.ttf`
- `app/src/main/res/font/plus_jakarta_sans_bold.ttf`
- `app/src/main/res/font/be_vietnam_pro_regular.ttf`
- `app/src/main/res/font/be_vietnam_pro_medium.ttf`
- `app/src/main/res/font/be_vietnam_pro_semibold.ttf`
- `app/src/main/res/font/be_vietnam_pro_bold.ttf`

Sau khi có file, đổi `PlusJakartaSans` và `BeVietnamPro` trong `ui/theme/Type.kt` sang `FontFamily(Font(R.font....))`, khai báo đủ các weight ở trên. Không tải font tự động trong app. Đồng hồ đã dùng `fontFeatureSettings = "tnum"`.
