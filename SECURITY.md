# Báo cáo bảo mật

Không đăng khóa ký, mật khẩu, token, nội dung thông báo, database thiết bị hoặc log chưa lọc vào issue công khai.

Với lỗi bảo mật, dùng mục **Report a vulnerability** trong Security của repository nếu khả dụng. Nếu chưa có kênh riêng, mở issue chỉ yêu cầu một kênh trao đổi riêng, không công khai chi tiết khai thác hay dữ liệu nhạy cảm.

APK sử dụng LSPosed để can thiệp các tiến trình hệ thống; chỉ hỗ trợ phiên bản ROM được đối chiếu. ConfigProvider là giao diện chỉ đọc; không được thêm phương thức ghi exported. ScopeTool chỉ đọc database bằng root khi người dùng bấm chẩn đoán. Công cụ ghi scope nằm riêng trong device-tools, không đóng gói trong APK; chỉ chạy thủ công sau khi kiểm tra schema và backup.

Khóa ký release gốc không nằm trong repository. APK tự build hoặc CI có chữ ký riêng và không cập nhật đè được APK release đang cài nếu khác khóa; đừng gỡ app đang dùng mà chưa sao lưu cấu hình.
