# Đóng góp

Mở issue mô tả hành vi mong muốn và hành vi thực tế; gửi PR kèm bước tái hiện, thay đổi hook và kết quả kiểm tra.

- Chuẩn đối chiếu hiện tại: RMX3370, Android 13/SDK33, Oplus CN. ROM khác cần báo rõ điểm hook đã đối chiếu; không chỉ mở rộng SDK guard rồi tuyên bố hỗ trợ.
- Build bằng bootstrap.ps1 và build.ps1; chạy verify.py cho APK. CI tạo khóa tạm, không chứa khóa ký release.
- Kiểm tra bật/tắt/ngoại lệ, dọc và cả hai chiều ngang, IME và khoảng tránh camera. Không tự suy ra hết lỗi hiếm từ vài lần thành công.
- Giữ nguồn navigation/IME/gesture, bảo vệ nội dung và xác thực. Không thêm bộ nhận cử chỉ hoặc logic trợ lý/phím nguồn vào module này.
- Không commit .local, tools, build, research, ảnh chụp thiết bị, database LSPosed hoặc dữ liệu cá nhân.

Các script test-*.py thực sự điều khiển điện thoại qua ADB. Chỉ chạy trên thiết bị của bạn, đã mở khóa, sau khi đọc script. Chúng không nhập mật khẩu hay gửi nội dung ra dịch vụ bên ngoài.
