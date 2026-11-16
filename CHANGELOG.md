# Changelog

## 1.2 — 2026-11-16

- Cho app nhận chạm trong vùng status bar đã ẩn; giữ vuốt mở thông báo qua native ROM.
- Thêm log chạm trong ProbeActivity để kiểm tra DOWN/UP/CANCEL.
- Kiểm tra dọc và hai chiều ngang; còn một lần vuốt ngay sau xoay không mở bảng. Xem TESTING.md.

## 1.1 — 2026-11-13

- Khôi phục icon pin/sóng/Wi-Fi khi mở bảng thông báo hoặc Cài đặt nhanh.
- Bỏ hook vẽ con của QS, giữ header theo ROM; hàng trạng thái ngoài bảng vẫn ẩn.
- Giữ nguyên hook framework/cutout/IME và cấu hình hiện có.
- Đã kiểm tra dọc trên máy thật; chi tiết và giới hạn trong TESTING.md.

## 1.0 — 2026-11-13

- Module độc lập dev.codex.realmefullscreen cho Android 13/Oplus.
- Ẩn hàng status bar trên app/Launcher và giải phóng vùng nội dung.
- Mở rộng app ngang và Gboard qua vùng cutout; giữ tương thích legacy IME resize.
- Công tắc chung, hide/cutout độc lập, ngoại lệ từng app và chẩn đoán scope root chỉ đọc.
- Giữ giao diện khóa; không bổ sung logic trợ lý, phím nguồn hoặc bộ nhận cử chỉ.
- Công bố source, toolchain chốt hash, script kiểm tra và quy trình hoàn tác.

Xem TESTING.md để phân biệt ca đã kiểm tra và phần chưa nghiệm thu đầy đủ.
